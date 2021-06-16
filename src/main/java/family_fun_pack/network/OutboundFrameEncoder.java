package family_fun_pack.network;

import net.minecraft.network.NettyVarint21FrameEncoder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

@OnlyIn(Dist.CLIENT)
public class OutboundFrameEncoder extends NettyVarint21FrameEncoder {
  protected void encode(ChannelHandlerContext context, ByteBuf in, ByteBuf out) throws Exception {
    if(in.readableBytes() > 1 || (in.readableBytes() == 1 && in.readByte() != (byte) 0)) { // Don't send empty packets (makes viaversion cry)
      super.encode(context, in, out);
    }
  }
}
