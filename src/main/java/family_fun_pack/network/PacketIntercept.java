package family_fun_pack.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.play.client.*;
import net.minecraft.util.EnumFacing;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.Exception;
import java.io.IOException;

import family_fun_pack.FamilyFunPack;

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
            if(FamilyFunPack.configuration.currently_invulnerable) {
              return;
            }
          }
          break;
        case 12: // CPacketPlayer
        case 13:
        case 14:
          {
            if(FamilyFunPack.configuration.ride != null) {
              Minecraft mc = Minecraft.getMinecraft();
              if(!mc.player.isRiding()) {
                mc.player.onGround = true;
                FamilyFunPack.configuration.ride.setPosition(mc.player.posX, mc.player.posY, mc.player.posZ);
                FamilyFunPack.sendPacket(new CPacketVehicleMove(FamilyFunPack.configuration.ride));
              } else FamilyFunPack.configuration.ride = null;
            }
          }
          break;
        case 20: // CPacketPlayerDigging
          {
            if(FamilyFunPack.configuration.reverse_face) {
              CPacketPlayerDigging dig = (CPacketPlayerDigging) packet;
              EnumFacing reverse = EnumFacing.VALUES[((dig.getFacing().getIndex() / 2) * 2) + ((dig.getFacing().getIndex() + 1) % 2)];
              CPacketPlayerDigging new_dig = new CPacketPlayerDigging(dig.getAction(), dig.getPosition(), reverse);
              packet = new_dig;
              FamilyFunPack.printMessage("dig facing from " + dig.getFacing().toString() + " to " + new_dig.getFacing().toString());
            }
          }
          break;
      }

      // packets interception
      if(FamilyFunPack.configuration.block_player_packets && FamilyFunPack.configuration.outbound_block.contains(id)) {
        return;
      }
    }

    super.encode(context, packet, out);
  }

}
