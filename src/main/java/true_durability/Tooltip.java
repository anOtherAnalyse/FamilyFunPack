package true_durability;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import java.util.List;

@SideOnly(Side.CLIENT)
public class Tooltip {

  public static boolean firstConnection = true;

  @SubscribeEvent
  public void itemToolTip(ItemTooltipEvent event) {
    ItemStack stack = event.getItemStack();
    int max = stack.getMaxDamage();

    if(stack.isEmpty() || max <= 0) return;
    if(stack.hasTagCompound() && stack.getTagCompound().getBoolean("Unbreakable")) return;

    List<String> toolTip = event.getToolTip();

    int damage;
    NBTTagCompound tag = stack.getTagCompound();
    if(tag != null && tag instanceof SpecialTagCompound) {
      damage = ((SpecialTagCompound)tag).getTrueDamage();
    } else damage = stack.getItemDamage();

    long count = (long)max - (long)damage;

    String color;
    if(damage < 0) color = "\u00A75";
    else if(damage > max) color = "\u00A74";
    else color = "\u00A79";

    toolTip.add("");
    toolTip.add(color + "Durability: " + Long.toString(count) + " [Max: " + Long.toString(max) + "]");
  }

  @SubscribeEvent
  public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
    if(Tooltip.firstConnection) {

      ChannelPipeline pipeline = event.getManager().channel().pipeline();

      try {
        ChannelHandler old = pipeline.get("decoder");
        if(old != null && old instanceof NettyPacketDecoder) {
          PacketListener spoof = new PacketListener(EnumPacketDirection.CLIENTBOUND);
          pipeline.replace("decoder", "decoder", spoof);
          Tooltip.firstConnection = false;
        }
      } catch (java.util.NoSuchElementException e) {}
    }
  }

  @SubscribeEvent
	public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
    Tooltip.firstConnection = true;
  }

}
