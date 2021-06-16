package family_fun_pack.network;

import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.ProtocolType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import io.netty.channel.ChannelHandlerContext;
import io.netty.buffer.ByteBuf;

import java.lang.Exception;
//import java.lang.IllegalAccessException;
//import java.lang.InstantiationException;
import java.io.IOException;
import java.util.List;

/* Intercept packet sent by server to client */

@OnlyIn(Dist.CLIENT)
public class InboundInterceptor extends NettyPacketDecoder {

  private final PacketDirection direction;
  private NetworkHandler handler;

  public InboundInterceptor(NetworkHandler handler, PacketDirection direction) {
    super(direction);
    this.handler = handler;
    this.direction = direction; // let's save it twice
  }

  protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) throws IOException, Exception {//, InstantiationException, IllegalAccessException, Exception {
    if (in.readableBytes() != 0) {

      int start_index = in.readerIndex(); // Mark start index
      super.decode(context, in, out); // Compute packet

      if(out.size() > 0) {
        IPacket packet = (IPacket) out.get(0);
        int id = ((ProtocolType) context.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get()).getPacketId(this.direction, packet);
        int end_index = in.readerIndex();

        in.readerIndex(start_index);
        packet = this.handler.packetReceived(this.direction, id, packet, in);
        in.readerIndex(end_index);

        if(packet == null) out.clear();
        else out.set(0, packet);
      }
    }
  }

}
