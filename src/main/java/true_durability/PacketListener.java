package true_durability;

import io.netty.channel.ChannelHandlerContext;
import io.netty.buffer.ByteBuf;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.nbt.NBTTagCompound;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

public class PacketListener extends NettyPacketDecoder {

  public PacketListener(EnumPacketDirection direction) {
    super(direction);
  }

  protected void decode(ChannelHandlerContext channel, ByteBuf in, List<Object> out) throws IOException, InstantiationException, IllegalAccessException, Exception {
    if (in.readableBytes() != 0) {
      Integer real_damage = null;

      in.markReaderIndex();
      PacketBuffer buf = new PacketBuffer(in);
      if(buf.readVarInt() == 22) { // SetSlot Packet
        buf.readerIndex(buf.readerIndex() + 3);
        if(buf.readShort() >= 0) {
          buf.readerIndex(buf.readerIndex() + 1);
          real_damage = (int)buf.readShort();
        }
      }
      in.resetReaderIndex();

      super.decode(channel, in, out);

      if(real_damage != null && out.size() > 0) {
        List<Object> replace = new LinkedList<Object>();
        for(Object i : out) {
          if(i instanceof SPacketSetSlot) {
            SPacketSetSlot pslt = (SPacketSetSlot) i;
            ItemStack stack = pslt.getStack();

            int true_damage = real_damage.intValue();

            if(true_damage < 0 || true_damage > stack.getMaxDamage()) {
              NBTTagCompound nbt = stack.getTagCompound();
              if(nbt == null) nbt = new NBTTagCompound();
              nbt.setInteger("true_damage", true_damage);
              stack.setTagCompound(nbt);
            }
          } else replace.add(i);
        }
        out = replace;
      }
    }
  }

}
