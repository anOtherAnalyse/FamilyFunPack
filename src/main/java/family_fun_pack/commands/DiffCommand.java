package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.network.play.server.SPacketTabComplete;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Get vanished players (not shown in player list, available in chat auto-completion) */

@SideOnly(Side.CLIENT)
public class DiffCommand extends Command implements PacketListener {

  public DiffCommand() {
    super("diff");
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 14);
    FamilyFunPack.getNetworkHandler().sendPacket(new CPacketTabComplete("", null, false));
    return null;
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 14);
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 14);

    SPacketTabComplete completion = (SPacketTabComplete) packet;
    NetHandlerPlayClient handler = (NetHandlerPlayClient)(FamilyFunPack.getNetworkHandler().getNetHandler());
    if(handler != null) {
      Minecraft client = Minecraft.getMinecraft();
      boolean add = true;
      for(String name : completion.getMatches()) {
        if(handler.getPlayerInfo(name) == null) {
          FamilyFunPack.printMessage("Addtional player: " + name);
            add = false;
        }
      }
      if(add) {
        FamilyFunPack.printMessage("No additional players over " + Integer.toString(completion.getMatches().length) + " players");
      } else {
        int total_tab = handler.getPlayerInfoMap().size();
        FamilyFunPack.printMessage("Board contains [" + Integer.toString(total_tab) + "/" + Integer.toString(completion.getMatches().length) + "] players");
      }
    }
    return null;
  }

}
