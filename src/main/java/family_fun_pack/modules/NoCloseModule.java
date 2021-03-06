package family_fun_pack.modules;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Silent close, keep containers open and access them with the inventory key bind */

@SideOnly(Side.CLIENT)
public class NoCloseModule extends Module implements PacketListener {

  private GuiContainer last_gui;

  public NoCloseModule() {
    super("Silent close", "Don't tell the server when closing container, be able to re-open it");
    this.last_gui = null;
  }

  protected void enable() {
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.SERVERBOUND, this, 8);
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 18);
    MinecraftForge.EVENT_BUS.register(this);
  }

  protected void disable() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.SERVERBOUND, this, 8);
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 18);
    MinecraftForge.EVENT_BUS.unregister(this);
    if(this.last_gui != null) {
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketCloseWindow(this.last_gui.inventorySlots.windowId));
      this.last_gui = null;
    }
  }

  public void onDisconnect() {
    this.last_gui = null;
  }

  @SubscribeEvent
  public void onGuiOpened(GuiOpenEvent event) {
    GuiScreen opened = event.getGui();
    if(opened instanceof InventoryEffectRenderer) {
      if(this.last_gui != null) {
        event.setGui(this.last_gui);
      }
    } else if(opened instanceof GuiContainer) {
      this.last_gui = (GuiContainer) opened;
    }
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    if(direction == EnumPacketDirection.CLIENTBOUND) {
      FamilyFunPack.printMessage("Container closed by server");
      this.last_gui = null;
      return packet;
    }
    return null;
  }

  public int getWindowId() {
    if(this.last_gui != null) return this.last_gui.inventorySlots.windowId;
    return -1;
  }

  public GuiScreen getOpenedContainer() {
    return this.last_gui;
  }
}
