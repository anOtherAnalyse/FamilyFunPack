package true_durability;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Packet;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.play.client.*;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.Exception;
import java.io.IOException;

@SideOnly(Side.CLIENT)
public class PacketIntercept extends NettyPacketEncoder {

  private final EnumPacketDirection direction;
  private boolean isPlay;

  public PacketIntercept(EnumPacketDirection direction) {
    super(direction);
    this.direction = direction; // let's save it twice
    this.isPlay = false;
  }

  protected void encode(ChannelHandlerContext context, Packet<?> packet, ByteBuf out) throws IOException, Exception {

    if(!this.isPlay) {
      EnumConnectionState state = (EnumConnectionState)(context.channel().attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get());
      this.isPlay = (state == EnumConnectionState.PLAY);
    }

    if(this.isPlay) {
      int id = ((EnumConnectionState)(context.channel().attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get())).getPacketId(this.direction, packet);

      switch(id) {
        case 0: // CPacketConfirmTeleport
          {
            if(TrueDurability.configuration.invulnerable) {
              return;
            }
          }
          break;
      }
    }

    super.encode(context, packet, out);
  }

}
