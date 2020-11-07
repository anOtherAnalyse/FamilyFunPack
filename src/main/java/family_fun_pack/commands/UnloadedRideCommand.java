package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketConfirmTransaction;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.inventory.ClickType;
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
import family_fun_pack.modules.CommandsModule;


// Packets order on item throw (all done when receiving client packet) :
// CPacketClickWindow

// SPacketSpawnObject(this.trackedEntity, 2, 1) - creates entity item
// SPacketEntityMetadata - contains ItemStack info

// SPacketConfirmTransaction
// SPacketSetSlot

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

  private BlockPos target;

  public UnloadedRideCommand() {
    super("ldride");
    this.limits = new int[2];
    this.slots = new int[2];
    this.target = null;
  }

  public String usage() {
    return this.getName() + " <reg|exe|get> [break] [nb_tries]";
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
      } else if(args[1].equals("exe")) {

        this.max_tries = UnloadedRideCommand.MAX_TRIES;
        boolean to_break = false;

        for(int i = 2; i < args.length; i ++) {
          if(args[i].equals("break")) to_break = true;
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
    return this.usage();
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 0, 17);
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

          // Try to ride
          int start = this.limits[0] + 1;
          int stop = start + this.max_tries;
          if(stop > this.limits[1]) stop = this.limits[1];

          if(stop <= start) {
            FamilyFunPack.printMessage("Fail, no new entities were loaded");
            return packet;
          }

          FamilyFunPack.printMessage("Entity id from " + Integer.toString(start) + " to " + Integer.toString(stop - 1));

          for(int i = start; i < stop; i ++) {
            FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUseEntity(new EntityVoid(mc.world, i), EnumHand.MAIN_HAND));
          }

          // Try to open donkey inventory - Yes you can / could dupe with this command
          // ((CommandsModule)FamilyFunPack.getModules().getByName("FFP Commands")).getCommand("open").execute(new String[0]);
        }
      }
    }
    return packet;
  }
}
