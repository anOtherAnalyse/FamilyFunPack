package family_fun_pack.commands;

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
import net.minecraft.network.play.server.SPacketConfirmTransaction;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Swift steal from ride container - Steal before server closes the container */

/* Information about horse inventory slots number
 * 0 -> saddle slot
 * 1 -> armor / carpet slot
 * 2..n -> horse chest inventory, row by row, left to right
 * n..(n+27) -> player inventory
 * (n+27)..(n+36) -> player hot bar */

@SideOnly(Side.CLIENT)
public class StealCommand extends Command implements PacketListener {

  private int start, stop;
  private int next_id;

  public StealCommand() {
    super("steal");
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

        this.next_id = -1;

        Entity ride = mc.player.getRidingEntity();
        if(ride != null && ride instanceof AbstractHorse) {
          FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 19);
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketEntityAction(ride, CPacketEntityAction.Action.OPEN_INVENTORY));
        } else return "Not riding any entity you could steal from";

      } catch(NumberFormatException e) {
        return this.usage();
      }
    } else return this.usage();

    return null;
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    if(id == 19) { // SPacketOpenWindow
      SPacketOpenWindow open = (SPacketOpenWindow) packet;

      if(this.next_id != -1) {
        FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 19);
        if(open.getWindowId() != this.next_id) {
          FamilyFunPack.printMessage("Window id guessed wrongly, expected " + Integer.toString(this.next_id) + " ,was " + Integer.toString(open.getWindowId()));
        }
      } else {
        this.next_id = open.getWindowId() % 100 + 1;
        MinecraftForge.EVENT_BUS.register(this); // Do the steal next tick, avoid spam
      }

      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketCloseWindow(open.getWindowId()));
    } else { // SPacketConfirmTransaction
      SPacketConfirmTransaction confirm = (SPacketConfirmTransaction) packet;
      short slot = (short)(confirm.getActionNumber() * -1);
      if(confirm.getWindowId() == this.next_id && slot >= this.start && slot <= this.stop) {
        if(confirm.wasAccepted()) {
          FamilyFunPack.printMessage("Steal " + Integer.toString(slot) + TextFormatting.GREEN + " SUCCESS");
        } else {
          FamilyFunPack.printMessage("Steal " + Integer.toString(slot) + TextFormatting.RED + " FAIL");
        }
      } else FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 17);
    }
    return null;
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    MinecraftForge.EVENT_BUS.unregister(this);

    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 17);

    FamilyFunPack.getNetworkHandler().sendPacket(new CPacketEntityAction(Minecraft.getMinecraft().player.getRidingEntity(), CPacketEntityAction.Action.OPEN_INVENTORY));
    for(int i = this.start; i <= this.stop; i ++) {
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketClickWindow(this.next_id, i, 0, ClickType.QUICK_MOVE, ItemStack.EMPTY, (short)(i * -1)));
    }
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 17, 19);
    MinecraftForge.EVENT_BUS.unregister(this);
  }
}
