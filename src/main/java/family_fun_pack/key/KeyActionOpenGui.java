package family_fun_pack.key;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KeyActionOpenGui implements KeyAction {

  private Screen gui;

  public KeyActionOpenGui(Screen gui) {
    this.gui = gui;
  }

  public void onKeyDown(int keysym) {
    Minecraft mc = Minecraft.getInstance();
    if(mc.screen != this.gui) mc.setScreen(this.gui);
  }
}
