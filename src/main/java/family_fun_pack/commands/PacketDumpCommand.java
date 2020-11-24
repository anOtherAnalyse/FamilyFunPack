package family_fun_pack.commands;

import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

@SideOnly(Side.CLIENT)
public class PacketDumpCommand extends Command implements PacketListener {

  public PacketDumpCommand() {
    super("pckdump");
  }

  public String usage() {
    return this.getName() + " <on|off>";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      if(args[1].equals("on")) {
        for(int i = 0; i < 80; i ++) {
          FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, i);
        }
        return "pckdump on";
      } else {
        FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this);
        return "pckdump off";
      }
    }
    return this.usage();
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    FamilyFunPack.printMessage("Received packet [" + packet.getClass().getSimpleName() + "]");
    return packet;
  }
}
