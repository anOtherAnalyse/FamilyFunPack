package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.entities.EntityVoid;
import family_fun_pack.network.PacketListener;

@SideOnly(Side.CLIENT)
public class UnloadedRideCommand extends Command implements PacketListener {

  private static final int MAX_TRIES = 15;

  private int first_id;
  private int last_id;
  private int max_tries;

  public UnloadedRideCommand() {
    super("unload");
  }

  public String usage() {
    return this.getName() + " <block_x> <block_y> <block_z>";
  }

  public String execute(String[] args) {
    if(args.length > 3) {
      Minecraft mc = Minecraft.getMinecraft();
      try {
        int x = Integer.parseInt(args[1]);
        int y = Integer.parseInt(args[2]);
        int z = Integer.parseInt(args[3]);
        if(args.length > 4) this.max_tries = Integer.parseInt(args[4]);
        else this.max_tries = UnloadedRideCommand.MAX_TRIES;

        int id = 0;
        boolean found = false;
        for(Slot slot : mc.player.inventoryContainer.inventorySlots) {
          if(slot.getStack().getItem() == Item.getItemFromBlock(Blocks.DIRT)) {
            found = true;
            break;
          }
          id ++;
        }

        if(! found) return "No dirt in your inventory";

        Vec3d look = Minecraft.getMinecraft().player.getLookVec();

        this.first_id = -1;
        this.last_id = -1;
        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 0);

        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketClickWindow(0, id, 0, ClickType.THROW, ItemStack.EMPTY, (short)0));
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(new BlockPos(x, y, z), EnumFacing.UP, EnumHand.MAIN_HAND, (float)look.x, (float)look.y, (float)look.z));
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketClickWindow(0, id, 0, ClickType.THROW, ItemStack.EMPTY, (short)1));

      } catch(NumberFormatException e) {
        return "Wrong block coordinates";
      }
    } else return "Which coordinates ?";
    return null;
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 0);
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketSpawnObject spawn = (SPacketSpawnObject) packet;
    if(spawn.getType() == 2) { // Object
      if(this.first_id == -1) this.first_id = spawn.getEntityID();
      else if(this.last_id == -1) {
        FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 0);
        this.last_id = spawn.getEntityID();
        FamilyFunPack.printMessage("Entity id from " + Integer.toString(this.first_id) + " to " + Integer.toString(this.last_id));

        if(! ItemStack.areItemStacksEqual(Minecraft.getMinecraft().player.getHeldItem(EnumHand.MAIN_HAND), ItemStack.EMPTY)) {
          FamilyFunPack.printMessage("Your main hand is not empty !");
        } else {
          int start = this.first_id + 1;
          int stop = start + this.max_tries;
          if(stop > this.last_id) stop = this.last_id;
          for(int i = start; i < stop; i ++) {
            FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUseEntity(new EntityVoid(Minecraft.getMinecraft().world, i), EnumHand.MAIN_HAND));
          }
        }
      }
    }
    return packet;
  }
}
