package true_durability;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.channel.ChannelHandlerContext;
import io.netty.buffer.ByteBuf;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.network.play.server.SPacketWindowItems;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.util.List;

@SideOnly(Side.CLIENT)
public class PacketListener extends NettyPacketDecoder {

  public PacketListener(EnumPacketDirection direction) {
    super(direction);
  }

  protected void decode(ChannelHandlerContext channel, ByteBuf in, List<Object> out) throws IOException, InstantiationException, IllegalAccessException, Exception {
    if (in.readableBytes() != 0) {

      int start_index = in.readerIndex(); // Mark start index
      super.decode(channel, in, out); // Computer packet

      if(out.size() > 0) {
        Object packet = out.get(0);
        if(packet instanceof SPacketSetSlot) { // we got a set slot packet
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
        } else if(packet instanceof SPacketWindowItems) { // We got a list of item stacks
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
      }
    }
  }

}
