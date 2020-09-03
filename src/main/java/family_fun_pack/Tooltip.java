package family_fun_pack;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

import org.lwjgl.input.Keyboard;

import family_fun_pack.gui.CommandGui;
import family_fun_pack.gui.OverlayGui;

@SideOnly(Side.CLIENT)
public class Tooltip {

  private boolean firstConnection;
  public KeyBinding openGUIKey;
  private KeyBinding intercept;

  private OverlayGui overlay;

  private Minecraft mc;

  public Tooltip() {
    this.firstConnection = true;
    this.openGUIKey = new KeyBinding("Open GUI", Keyboard.KEY_BACKSLASH, FamilyFunPack.NAME);
    this.intercept = new KeyBinding("Intercept network packets", Keyboard.KEY_C, FamilyFunPack.NAME);
    ClientRegistry.registerKeyBinding(this.openGUIKey);
    ClientRegistry.registerKeyBinding(this.intercept);
    this.mc = Minecraft.getMinecraft();
    this.overlay = new OverlayGui(this.mc.fontRenderer, FamilyFunPack.configuration);
  }

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

    TextFormatting color;
    if(damage < 0) color = TextFormatting.DARK_PURPLE;
    else if(damage > max) color = TextFormatting.DARK_RED;
    else color = TextFormatting.BLUE;

    toolTip.add("");
    toolTip.add(color.toString() + "Durability: " + Long.toString(count) + " [Max: " + Long.toString(max) + "]" + TextFormatting.RESET.toString());
  }

  @SubscribeEvent
  public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
    if(this.firstConnection) {

      ChannelPipeline pipeline = event.getManager().channel().pipeline();

      try {
        // Install receive interception
        ChannelHandler old = pipeline.get("decoder");
        if(old != null && old instanceof NettyPacketDecoder) {
          PacketListener spoof = new PacketListener(EnumPacketDirection.CLIENTBOUND);
          pipeline.replace("decoder", "decoder", spoof);
          this.firstConnection = false;
        }

        // Install send interception
        old = pipeline.get("encoder");
        if(old != null && old instanceof NettyPacketEncoder) {
          PacketIntercept intercept = new PacketIntercept(EnumPacketDirection.SERVERBOUND);
          pipeline.replace("encoder", "encoder", intercept);
          this.firstConnection = false;
        }

        // Record NetworkManager
        FamilyFunPack.setNetworkManager(event.getManager());
      } catch (java.util.NoSuchElementException e) {}
    }
  }

  @SubscribeEvent
	public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
    this.firstConnection = true;
    FamilyFunPack.configuration.resetVolatileConf();
  }

  @SubscribeEvent
  public void onKey(KeyInputEvent event) {
    if(this.openGUIKey.isPressed()) {
      if(! (this.mc.currentScreen instanceof CommandGui)) {
        this.mc.displayGuiScreen(new CommandGui(this));
      }
    } else if(this.intercept.isPressed()) {
      FamilyFunPack.configuration.block_player_packets = ! FamilyFunPack.configuration.block_player_packets;
      FamilyFunPack.configuration.save();
    }
  }

  @SubscribeEvent
  public void drawOverlay(RenderGameOverlayEvent.Text event) {
    this.overlay.drawOverlay();
  }

}
