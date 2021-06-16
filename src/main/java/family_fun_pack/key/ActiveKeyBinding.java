package family_fun_pack.key;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ActiveKeyBinding extends KeyBinding {

  private KeyAction action;

  public ActiveKeyBinding(String name, int keysym, String category, KeyAction action) {
    super(name, keysym, category);
    this.action = action;
  }

  public void setDown(boolean down) {
    super.setDown(down);
    if(this.isDown()) {
      this.action.onKeyDown(this.getKey().getValue());
    }
  }
}
