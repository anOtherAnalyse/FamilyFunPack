package family_fun_pack.key;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.MainGui;

@OnlyIn(Dist.CLIENT)
public interface KeyManager {

  public static void addGuiKey(MainGui gui) {
    KeyManager.registerKey("Open GUI", 92, new KeyActionOpenGui(gui));
    gui.setExitKey(92);
  }

  public static void registerKey(String label, int keysym, KeyAction action) {
    ActiveKeyBinding bind = new ActiveKeyBinding(label, keysym, FamilyFunPack.NAME, action);
    ClientRegistry.registerKeyBinding(bind);
  }
}
