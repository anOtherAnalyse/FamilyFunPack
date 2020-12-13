package family_fun_pack.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.network.play.client.CPacketClientStatus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Don't show game over interface after dying, use .respawn to respawn
 * In vanilla you can still interact with the environment while beeing dead */

@SideOnly(Side.CLIENT)
public class UndeadModule extends Module {

  private boolean isDead;

  public UndeadModule() {
    super("Undead", "Keep moving after death");
    this.isDead = false;
  }

  protected void enable() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  protected void disable() {
    MinecraftForge.EVENT_BUS.unregister(this);
    if(this.isDead) {
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketClientStatus(CPacketClientStatus.State.PERFORM_RESPAWN));
      this.isDead = false;
    }
  }

  public void onDisconnect() {
    this.isDead = false;
  }

  @SubscribeEvent
  public void onGuiOpened(GuiOpenEvent event) {
    if(event.getGui() instanceof GuiGameOver) {
      event.setGui(null);
      this.isDead = true;
      Minecraft.getMinecraft().player.setHealth(20);
    }
  }

}
