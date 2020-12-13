package family_fun_pack.key;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.LinkedList;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.MainGui;
import family_fun_pack.modules.Module;

/* Key listener */

@SideOnly(Side.CLIENT)
public class KeyListener {

  private KeyBinding gui_key;
  private MainGui gui;

  private List<KeyBinding> keys;
  private List<Module> actions;

  public KeyListener() {
    this.gui_key = new KeyBinding("Open GUI", Keyboard.KEY_BACKSLASH, FamilyFunPack.NAME);
    ClientRegistry.registerKeyBinding(this.gui_key);
    this.gui = null;
    this.keys = new LinkedList<KeyBinding>();
    this.actions = new LinkedList<Module>();
  }

  public void setGui(MainGui gui) {
    this.gui = gui;
    this.gui.setExitKey(this.gui_key.getKeyCode());
  }

  public void addModuleKey(int key, Module module) {
    KeyBinding bind = new KeyBinding(module.getLabel(), key, FamilyFunPack.NAME);
    ClientRegistry.registerKeyBinding(bind);
    this.keys.add(bind);
    this.actions.add(module);
  }

  @SubscribeEvent
  public void onKey(KeyInputEvent event) {
    if(this.gui_key.isPressed()) {
      Minecraft mc = Minecraft.getMinecraft();
      if(mc.currentScreen != this.gui) {
        mc.displayGuiScreen(this.gui);
      }
    } else {
      int length = this.keys.size();
      for(int i = 0; i < length; i ++) {
        if(this.keys.get(i).isPressed()) {
          Module m = this.actions.get(i);
          m.toggle();
          m.save_state(FamilyFunPack.getModules().getConfiguration());
          FamilyFunPack.getModules().getConfiguration().save();
          return;
        }
      }
    }
  }

}
