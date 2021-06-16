package family_fun_pack.network;

import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.ProtocolType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.lang.Exception;
import java.io.IOException;

/* Intercept packets sent by client to server */

@OnlyIn(Dist.CLIENT)
public class OutboundInterceptor extends NettyPacketEncoder {

  private final PacketDirection direction;
  private NetworkHandler handler;

  public OutboundInterceptor(NetworkHandler handler, PacketDirection direction) {
    super(direction);
    this.handler = handler;
    this.direction = direction; // let's save it twice
  }

  protected void encode(ChannelHandlerContext context, IPacket<?> packet, ByteBuf out) throws IOException, Exception {

    int id = ((ProtocolType) context.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get()).getPacketId(this.direction, packet);

    packet = this.handler.packetReceived(this.direction, id, packet, null);

    if(packet == null) return;

    super.encode(context, packet, out);
  }

}
