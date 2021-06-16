package family_fun_pack.network;

import net.minecraft.network.PacketDirection;
import net.minecraft.network.IPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import io.netty.buffer.ByteBuf;

/* A class listening for network packets, register a listener on NetworkHandler */

@OnlyIn(Dist.CLIENT)
public interface PacketListener {

  public IPacket<?> packetReceived(PacketDirection direction, int id, IPacket<?> packet, ByteBuf in);

}
