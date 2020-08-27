package true_durability;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.channel.ChannelHandlerContext;
import io.netty.buffer.ByteBuf;

import net.minecraft.network.Packet;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.play.server.*;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class PacketListener extends NettyPacketDecoder {

  private final EnumPacketDirection direction;
  private boolean isPlay;

  public PacketListener(EnumPacketDirection direction) {
    super(direction);
    this.direction = direction; // let's save it twice
    this.isPlay = false;
  }

  protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) throws IOException, InstantiationException, IllegalAccessException, Exception {
    if (in.readableBytes() != 0) {

      int start_index = in.readerIndex(); // Mark start index
      super.decode(context, in, out); // Computer packet

      if(!this.isPlay) { // don't go fetch the attr every time)
        EnumConnectionState state = (EnumConnectionState)(context.channel().attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get());
        this.isPlay = (state == EnumConnectionState.PLAY);
      }

      if(this.isPlay && out.size() > 0) {
        Packet packet = (Packet)out.get(0);

        int id = ((EnumConnectionState)context.channel().attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get()).getPacketId(this.direction, packet);

        switch(id) {
          case 20: // Windows items
            {
              SPacketWindowItems packet_window = (SPacketWindowItems) packet;
              int end_index = in.readerIndex();

              PacketBuffer buf = new PacketBuffer(in);
              buf.readerIndex(start_index + 4);
              for(ItemStack i : packet_window.getItemStacks()) {
                if(buf.readShort() >= 0) {
                  buf.readerIndex(buf.readerIndex() + 1);
                  short true_damage = buf.readShort();
                  if(true_damage < 0) {
                    i.setTagCompound(new SpecialTagCompound(buf.readCompoundTag(), (int)true_damage));
                  } else buf.readCompoundTag();
                }
              }

              in.readerIndex(end_index);
            }
            break;
          case 22: // Set slot packet
            {
              SPacketSetSlot packet_slot = (SPacketSetSlot) packet;
              int end_index = in.readerIndex();

              // Read real item durability
              PacketBuffer buf = new PacketBuffer(in);
              buf.readerIndex(start_index + 4);
              if(buf.readShort() >= 0) {
                buf.readerIndex(buf.readerIndex() + 1);
                short real_damage = buf.readShort();
                if(real_damage < 0) { // We want to save this value
                  ItemStack stack = packet_slot.getStack();
                  stack.setTagCompound(new SpecialTagCompound(stack.getTagCompound(), (int)real_damage));
                }
              }

              in.readerIndex(end_index);
            }
            break;
          case 47: // Player position
            {
              if(TrueDurability.configuration.invulnerable) {
                SPacketPlayerPosLook old = (SPacketPlayerPosLook) packet;
                Set<SPacketPlayerPosLook.EnumFlags> flags = old.getFlags();
                flags.add(SPacketPlayerPosLook.EnumFlags.Y_ROT);
                flags.add(SPacketPlayerPosLook.EnumFlags.X_ROT);

                TrueDurability.configuration.last_teleport_id = old.getTeleportId();

                SPacketPlayerPosLook spoof = new SPacketPlayerPosLook(old.getX(), old.getY(), old.getZ(), 0, 0, flags, old.getTeleportId());
                out.set(0, spoof);
              }
            }
            break;
        }
      }
    }
  }

}
