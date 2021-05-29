package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractChestHorse;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketConfirmTransaction;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerHorseChest;
import net.minecraft.inventory.Slot;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.entities.EntityVoid;
import family_fun_pack.network.PacketListener;


/* Mount / use entity from unloaded chunk */

@SideOnly(Side.CLIENT)
public class UnloadedRideCommand extends Command implements PacketListener {

  private static final int MAX_TRIES = 15;

  public static int getItemSlot(Item item) {
    Minecraft mc = Minecraft.getMinecraft();
    int i = 0;
    for(Slot slot : mc.player.inventoryContainer.inventorySlots) {
      if(slot.getStack().getItem() == item) {
        return i;
      }
      i ++;
    }
    return -1;
  }

  private int saved_id;
  private int[] limits;
  private int[] slots;
  private int max_tries;
  private boolean success;
  private boolean sneak_use;
  private BlockPos target;

  private int window_count;

  public UnloadedRideCommand() {
    super("ldride");
    this.limits = new int[2];
    this.slots = new int[2];
    this.target = null;
  }

  public String usage() {
    return this.getName() + " <reg|exe|get> [break] [sneak] [nb_tries]";
  }

  public String execute(String[] args) {
    if(args.length > 1) {

      if(this.target == null) {
        Configuration configuration = FamilyFunPack.getModules().getConfiguration();
        this.target = new BlockPos(configuration.get(this.getName(), "target_x", 0d).getDouble(), configuration.get(this.getName(), "target_y", 0d).getDouble(), configuration.get(this.getName(), "target_z", 0d).getDouble());
      }

      Minecraft mc = Minecraft.getMinecraft();
      if(args[1].equals("get")) {
        return String.format("Registered block: (%d, %d, %d)", this.target.getX(), this.target.getY(), this.target.getZ());
      } else if(args[1].equals("reg")) {
        if(args.length > 3) {
          int offset;
          try {
            offset = Integer.parseInt(args[3]);
          } catch(NumberFormatException e) {
            return "This is not an integer";
          }
          switch(args[2]) {
            case "x": this.target = this.target.add(offset, 0, 0); break;
            case "y": this.target = this.target.add(0, offset, 0); break;
            case "z": this.target = this.target.add(0, 0, offset); break;
            default:
              return "x, z or y ?";
          }
          return String.format("Move block to: (%d, %d, %d), [%s + (%d)]", this.target.getX(), this.target.getY(), this.target.getZ(), args[2], offset);
        } else {
          RayTraceResult target_ray = mc.objectMouseOver;
          if(target_ray != null) {
            if(target_ray.typeOfHit == RayTraceResult.Type.BLOCK) {
              this.target = target_ray.getBlockPos();

              // save in configuration file
              Configuration configuration = FamilyFunPack.getModules().getConfiguration();
              configuration.get(this.getName(), "target_x", 0d).set(this.target.getX());
              configuration.get(this.getName(), "target_y", 0d).set(this.target.getY());
              configuration.get(this.getName(), "target_z", 0d).set(this.target.getZ());
              configuration.save();

              return String.format("Registering block: (%d, %d, %d)", this.target.getX(), this.target.getY(), this.target.getZ());
            }
          }
          return "You need to look at a block";
        }
      } else if(args[1].equals("exe")) {

        this.max_tries = UnloadedRideCommand.MAX_TRIES;
        boolean to_break = false;
        this.sneak_use = false;

        for(int i = 2; i < args.length; i ++) {
          if(args[i].equals("break")) to_break = true;
          else if(args[i].equals("sneak")) this.sneak_use = true;
          else {
            try {
              this.max_tries = Integer.parseInt(args[i]);
            } catch(NumberFormatException e) {
              return "nb_tries should be a number";
            }
          }
        }

        this.slots[0] = UnloadedRideCommand.getItemSlot(Item.getItemFromBlock(Blocks.DIRT));
        if(this.slots[0] == -1) return "No dirt in your inventory";

        this.slots[1] = UnloadedRideCommand.getItemSlot(Item.getItemFromBlock(Blocks.COBBLESTONE));
        if(this.slots[1] == -1) return "No cobblestone in your inventory";

        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 0, 17);

        this.success = true;
        this.window_count = 0;

        // Drop first item
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketClickWindow(0, this.slots[0], 0, ClickType.THROW, ItemStack.EMPTY, (short)0));

        // Load unloaded chunk (by breaking / using block)
        if(to_break)
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, this.target, EnumFacing.UP));
        else {
          Vec3d look = mc.player.getLookVec();
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(this.target, EnumFacing.UP, EnumHand.MAIN_HAND, (float)look.x, (float)look.y, (float)look.z));
        }

        // Drop second item
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketClickWindow(0, this.slots[1], 0, ClickType.THROW, ItemStack.EMPTY, (short)1));

        return null;
      }
    }
    return this.getUsage();
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 0, 17, 19);
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    if(id == 0) { // SPacketSpawnObject
      SPacketSpawnObject spawn = (SPacketSpawnObject) packet;
      if(spawn.getType() == 2) { // Object
        this.saved_id = spawn.getEntityID();
      }
    } else if(id == 17) { // SPacketConfirmTransaction
      SPacketConfirmTransaction confirm = (SPacketConfirmTransaction) packet;
      if(confirm.getWindowId() == 0 && (confirm.getActionNumber() == 0 || confirm.getActionNumber() == 1)) {

        Minecraft mc = Minecraft.getMinecraft();

        if(! confirm.wasAccepted()) {
          FamilyFunPack.printMessage("Fail, unable to drop " + (confirm.getActionNumber() == 0 ? "first" : "second") + " item");
          this.success = false;
        } else {
          mc.player.inventoryContainer.getSlot(this.slots[confirm.getActionNumber()]).getStack().shrink(1);
          this.limits[confirm.getActionNumber()] = this.saved_id;
        }

        if(confirm.getActionNumber() == 1) {
          FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 0, 17);

          if(! this.success) return packet;

          if(this.limits[1] - this.limits[0] <= 1) {
            FamilyFunPack.printMessage("Fail, no new entities were loaded");
            return packet;
          }

          FamilyFunPack.printMessage("Entity id from " + Integer.toString(this.limits[0] + 1) + " to " + Integer.toString(this.limits[1] - 1));

          if(this.sneak_use) {
            FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 19);
            FamilyFunPack.getNetworkHandler().sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SNEAKING));
          }

          if(this.max_tries < 0) {
            int start = this.limits[1] - 1;
            int stop = start + this.max_tries;
            if(stop < this.limits[0]) stop = this.limits[0];

            for(int i = start; i > stop; i --) {
              FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUseEntity(new EntityVoid(mc.world, i), EnumHand.MAIN_HAND));
            }
          } else if(this.max_tries > 0) {
            int start = this.limits[0] + 1;
            int stop = start + this.max_tries;
            if(stop > this.limits[1]) stop = this.limits[1];

            for(int i = start; i < stop; i ++) {
              FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUseEntity(new EntityVoid(mc.world, i), EnumHand.MAIN_HAND));
            }
          }
        }
      }
    } else { // SPacketOpenWindow
      SPacketOpenWindow open = (SPacketOpenWindow) packet;

      FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 19);

      if("EntityHorse".equals(open.getGuiId())) {
        Minecraft mc = Minecraft.getMinecraft();

        Entity entity = mc.world.getEntityByID(open.getEntityId());

        if(entity == null) { // desync between client & server

          if(this.success || (mc.currentScreen != null && mc.currentScreen instanceof GuiScreenHorseInventory)) {
            this.success = false; // re-use boolean to track the fact we already opened the gui once

            OpenWindowHandler handler = new OpenWindowHandler(this, this.window_count ++);
            try {
              PacketThreadUtil.<INetHandlerPlayClient>checkThreadAndEnqueue(open, handler, mc);
            } catch (ThreadQuickExitException except) {
              return null;
            }

            handler.handleOpenWindow(open);
          }

          return null;
        }
      }
    }
    return packet;
  }

  private static class OpenWindowHandler extends NetHandlerPlayClient {

    private UnloadedRideCommand parent;
    private int counter;

    public OpenWindowHandler(UnloadedRideCommand parent, int counter) {
      super(Minecraft.getMinecraft(), null, null, null);
      this.parent = parent;
      this.counter = counter;
    }

    public void handleOpenWindow(SPacketOpenWindow open) {
      Minecraft mc = Minecraft.getMinecraft();
      int entity_id = open.getEntityId();

      AbstractChestHorse fake = null;

      if(open.getSlotCount() > 2 && open.getSlotCount() < 17) { // llama
        fake = new EntityLlama(mc.world);
        fake.setChested(true);
        fake.getDataManager().set(new DataParameter(16, DataSerializers.VARINT), Integer.valueOf((open.getSlotCount() - 2) / 3));
      } else { // donkey ?
        fake = new EntityDonkey(mc.world);
        fake.setHorseSaddled(true);
        fake.setChested(true);
      }

      fake.setEntityId(entity_id);

      mc.player.openGuiHorseInventory(fake, new ContainerHorseChest(open.getWindowTitle().appendText(String.format(" [%d] - #%d", entity_id, this.counter)), open.getSlotCount()));
      mc.player.openContainer.windowId = open.getWindowId();

      // again
      FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this.parent, 19);
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SNEAKING));
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUseEntity(new EntityVoid(mc.world, entity_id), EnumHand.MAIN_HAND));
    }
  }
}
