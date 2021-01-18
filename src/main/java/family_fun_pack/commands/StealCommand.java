package family_fun_pack.commands;

import net.minecraft.block.BlockContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.server.SPacketConfirmTransaction;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Swift steal from ride container or from the container you are aiming at
 * Try to steal before the next tick update */

/* Information about horse inventory slots number
 * 0 -> saddle slot
 * 1 -> armor / carpet slot
 * 2..n -> horse chest inventory, row by row, left to right
 * n..(n+27) -> player inventory
 * (n+27)..(n+36) -> player hot bar */

@SideOnly(Side.CLIENT)
public class StealCommand extends Command implements PacketListener {

  private boolean enabled;
  private int start, stop;
  private int next_id;

  public StealCommand() {
    super("steal");
    this.next_id = -1;
    this.enabled = false;
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 19);
  }

  public String usage() {
    return this.getName() + " <slot> | <start_slot> <stop_slot>";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();

    if(args.length > 1) {
      try {
        this.start = Integer.parseInt(args[1]);
        if(args.length > 2) this.stop = Integer.parseInt(args[2]);
        else this.stop = this.start;

        if(this.next_id == -1) return "Please open any container first, for initialisation purpose";

        Entity ride = mc.player.getRidingEntity();
        if(ride != null && ride instanceof AbstractHorse) { // Steal from ride
          this.enabled = true;
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketEntityAction(ride, CPacketEntityAction.Action.OPEN_INVENTORY));
        } else { // Steal from chest
          RayTraceResult target_ray = Minecraft.getMinecraft().objectMouseOver;
          if(target_ray != null && target_ray.typeOfHit == RayTraceResult.Type.BLOCK && mc.world.getBlockState(target_ray.getBlockPos()).getBlock() instanceof BlockContainer) {
            this.enabled = true;
            Vec3d look = mc.player.getLookVec();
            FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(target_ray.getBlockPos(), EnumFacing.UP, EnumHand.MAIN_HAND, (float)look.x, (float)look.y, (float)look.z));
          } else return "Not riding horse or looking at a chest";
        }

        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 17);

        // Send use window
        for(int i = this.start; i <= this.stop; i ++) {
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketClickWindow(this.next_id, i, 0, ClickType.QUICK_MOVE, ItemStack.EMPTY, (short)(i * -1)));
        }

      } catch(NumberFormatException e) {
        return this.getUsage();
      }
    } else return this.getUsage();

    return null;
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    if(id == 19) { // SPacketOpenWindow
      SPacketOpenWindow open = (SPacketOpenWindow) packet;

      if(this.enabled) {

        this.enabled = false;

        if(this.next_id != open.getWindowId()) {
          FamilyFunPack.printMessage("Window id guessed wrongly, expected " + Integer.toString(this.next_id) + " ,was " + Integer.toString(open.getWindowId()));
        }

        this.next_id = open.getWindowId() % 100 + 1;

        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketCloseWindow(open.getWindowId()));
        return null;
      }

      this.next_id = open.getWindowId() % 100 + 1;
    } else { // SPacketConfirmTransaction
      SPacketConfirmTransaction confirm = (SPacketConfirmTransaction) packet;
      short slot = (short)(confirm.getActionNumber() * -1);
      if(slot >= this.start && slot <= this.stop) {
        if(confirm.wasAccepted()) {
          FamilyFunPack.printMessage("Steal " + Integer.toString(slot) + TextFormatting.GREEN + " SUCCESS");
        } else {
          FamilyFunPack.printMessage("Steal " + Integer.toString(slot) + TextFormatting.RED + " FAIL");
        }
        return null;
      } else FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 17);
    }

    return packet;
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 17);
    this.next_id = -1;
    this.enabled = false;
  }

  public int getNextId() {
    return this.next_id;
  }
}
