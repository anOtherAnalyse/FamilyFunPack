package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.lang.StringBuilder;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Fill a book to its max size (32767 bytes) */

@SideOnly(Side.CLIENT)
public class FillBookCommand extends Command implements PacketListener {

  public FillBookCommand() {
    super("fill");
  }

  public String usage() {
    return this.getName() + " [sign]";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();

    boolean sign = (args.length > 1 && args[1].equals("sign"));

    if(mc.player.getHeldItemMainhand().getItem() == Items.WRITABLE_BOOK) {
      ItemStack stack = mc.player.getHeldItemMainhand().copy();
      NBTTagCompound tag = new NBTTagCompound();

      // Pages
      NBTTagList pages = new NBTTagList();
      for(int i = 0; i < 50; i ++) {
        StringBuilder build = new StringBuilder(String.format("§cFILLED_BOOK_LINE_%d_§r", (i+1)));
        for(int j = 0; j < 34; j ++) build.append("§§§§§§§§§");
        pages.appendTag(new NBTTagString(build.toString()));
      }
      tag.setTag("pages", pages);

      if(sign) {
        // Title
        tag.setString("title", "32kb book");
        tag.setString("author", "unused");
      }

      stack.setTagCompound(tag);

      PacketBuffer data = new PacketBuffer(Unpooled.buffer());
      data.writeItemStack(stack);

      FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 22);

      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketCustomPayload(sign ? "MC|BSign" : "MC|BEdit", data));
      return Integer.toString(data.writerIndex()) + " bytes written to book";
    }
    return "Please hold a writable book";
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketSetSlot setSlot = (SPacketSetSlot) packet;
    if(setSlot.getStack().getItem() == Items.WRITABLE_BOOK || setSlot.getStack().getItem() == Items.WRITTEN_BOOK) {
      FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 22);
      PacketBuffer buff = new PacketBuffer(in);
      buff.readerIndex(buff.readerIndex() + 4);
      int size = buff.readerIndex();
      try {
        buff.readItemStack();
      } catch (IOException e) {
        return packet;
      }
      size = buff.readerIndex() - size;
      FamilyFunPack.printMessage("Server recorded " + Integer.toString(size) + " bytes");
    }
    return packet;
  }
}
