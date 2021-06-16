package family_fun_pack.key;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface KeyAction {

  public abstract void onKeyDown(int keysym);
  
}
