package family_fun_pack.gui.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.Module;

/* Right panel, special module configuration GUI displayed next to Main GUI */

@OnlyIn(Dist.CLIENT)
public abstract class RightPanel extends Screen {

  protected int x, y, x_end, y_end;

  public RightPanel(int x, int y, int width, int height, ITextComponent title) {
    super(title);

    this.x = x;
    this.y = y;
    this.x_end = x + width;
    this.y_end = y + height;

    Minecraft mc = Minecraft.getInstance();
    this.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
  }

  public void transition(RightPanel next) {
    FamilyFunPack.getMainGui().setRightPanel(next, -1);
  }

  public boolean isMouseOver(double mouseX, double mouseY) {
    return mouseX >= this.x && mouseX < this.x_end && mouseY >= this.y && mouseY < this.y_end;
  }

  // When gui is re-opened, actualize state
  public void onReopen() {}
}
