package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.server.SPacketConfirmTransaction;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.NoCloseModule;
import family_fun_pack.network.PacketListener;

/* Vanilla entity kill dupe
 * Works with horses, does not with minecart w/ chest
*/

@SideOnly(Side.CLIENT)
public class KillDupeCommand extends Command implements PacketListener {

  private int window_id, slot;

  public KillDupeCommand() {
    super("kdupe");
  }

  public String usage() {
    return this.getName() + " <slot_id>";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      try {
        this.slot = Integer.parseInt(args[1]);
      } catch(NumberFormatException e) {
        return this.usage();
      }

      if(!Minecraft.getMinecraft().player.inventory.getStackInSlot(0).isEmpty()) return "Keep your first hotbar slot clear";

      this.window_id = ((NoCloseModule) FamilyFunPack.getModules().getByName("Silent close")).getWindowId();
      if(this.window_id != -1) {
        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.SERVERBOUND, this, 10);
      } else return "First keep entity inventory open";
    } else return this.getUsage();
    return "kdupe ready";
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    if(id == 10) { // CPacketUseEntity
      CPacketUseEntity use = (CPacketUseEntity) packet;
      if(use.getAction() == CPacketUseEntity.Action.ATTACK) {
        FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.SERVERBOUND, this, 10);
        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 17);

        CPacketClickWindow get = new CPacketClickWindow(this.window_id, this.slot, 0, ClickType.SWAP, ItemStack.EMPTY, (short) -42);
        FamilyFunPack.getNetworkHandler().sendPacket(use);
        FamilyFunPack.getNetworkHandler().sendPacket(get);

        return null;
      }
    } else { // SPacketConfirmTransaction
      SPacketConfirmTransaction confirm = (SPacketConfirmTransaction) packet;
      if(confirm.getWindowId() == this.window_id && confirm.getActionNumber() == -42) {
        if(confirm.wasAccepted()) {
          FamilyFunPack.printMessage("Swap " + Integer.toString(slot) + TextFormatting.GREEN + " succeeded");
        } else {
          FamilyFunPack.printMessage("Swap " + Integer.toString(slot) + TextFormatting.RED + " failed");
        }
      }
      FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 17);
    }
    return packet;
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.SERVERBOUND, this, 10);
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 17);
  }
}
