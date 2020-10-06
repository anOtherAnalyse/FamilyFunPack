package family_fun_pack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import org.lwjgl.input.Keyboard;

import family_fun_pack.gui.MainGui;

@SideOnly(Side.CLIENT)
public class KeyListener {

  private KeyBinding gui_key;
  private MainGui gui;

  public KeyListener(MainGui gui) {
    this.gui_key = new KeyBinding("Open GUI", Keyboard.KEY_BACKSLASH, FamilyFunPack.NAME);
    this.gui = gui;
    this.gui.setExitKey(this.gui_key.getKeyCode());
  }

  @SubscribeEvent
  public void onKey(KeyInputEvent event) {
    if(this.gui_key.isPressed()) {
      Minecraft mc = Minecraft.getMinecraft();
      if(mc.currentScreen != this.gui) {
        mc.displayGuiScreen(this.gui);
      }
    }
  }

}
