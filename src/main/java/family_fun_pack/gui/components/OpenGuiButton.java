package family_fun_pack.gui.components;

import net.minecraft.client.gui.widget.button.Button;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OpenGuiButton extends GenericButton {

  private boolean clicked;

  public OpenGuiButton(int x, int y, String text, Button.IPressable action) {
    super(x, y, text, action, 1f);
  }

  public OpenGuiButton(int x, int y, String text, Button.IPressable action, float scale) {
    super(x, y, text, action, scale);
  }

  public boolean isClicked() {
    return this.clicked;
  }

  public void setClicked(boolean clicked) {
    this.clicked = clicked;
    if(this.clicked) {
      this.background = GenericButton.COLOR;
      this.text = GenericButton.BACKGROUND;
    } else {
      this.background = GenericButton.BACKGROUND;
      this.text = GenericButton.COLOR;
    }
  }

  public void onPress() {
    this.setClicked(! this.clicked);
    super.onPress();
  }
}
