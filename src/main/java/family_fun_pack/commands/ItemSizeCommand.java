package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.Unpooled;

/* Get currently hold ItemStack size in bytes (network size) */

@SideOnly(Side.CLIENT)
public class ItemSizeCommand extends Command {

  public static int getItemSize(ItemStack stack) {
    PacketBuffer buff = new PacketBuffer(Unpooled.buffer());
    buff.writeItemStack(stack);
    int size = buff.writerIndex();
    buff.release();
    return size;
  }

  public ItemSizeCommand() {
    super("size");
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    ItemStack stack = Minecraft.getMinecraft().player.getHeldItemMainhand();
    if(stack.isEmpty()) return "You are not holding any item";
    return "Item weights " + Integer.toString(ItemSizeCommand.getItemSize(stack)) + " bytes";
  }
}
