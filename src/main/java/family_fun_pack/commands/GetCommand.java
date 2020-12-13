package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.network.play.server.SPacketTabComplete;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Try to fetch all available commands on server */

@SideOnly(Side.CLIENT)
public class GetCommand extends Command implements PacketListener {

  public GetCommand() {
    super("commands");
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    RayTraceResult target_ray = Minecraft.getMinecraft().objectMouseOver;
    BlockPos target = null;
    if(target_ray != null && target_ray.typeOfHit == RayTraceResult.Type.BLOCK) {
      target = target_ray.getBlockPos();
    }
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 14);
    FamilyFunPack.getNetworkHandler().sendPacket(new CPacketTabComplete("/", target, (target != null)));
    return null;
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 14);
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 14);
    SPacketTabComplete completion = (SPacketTabComplete) packet;
    FamilyFunPack.printMessage("Available commands:");
    for(String cmd : completion.getMatches()) {
      FamilyFunPack.printMessage(cmd);
    }
    return null;
  }

}
