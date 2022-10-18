package family_fun_pack.gui.components;

import family_fun_pack.gui.components.actions.NumberAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;

@SideOnly(Side.CLIENT)
public class SliderButton extends ActionButton {

  private static final int BORDER = 0xffcccccc;
  private boolean drag;
  private int number;

  private NumberAction action;

  public SliderButton(int id, int x, int y, NumberAction action) {
    super(id, x, y, 32, 7, null);
    this.action = action;
    this.drag = false;
  }

  public SliderButton(int x, int y, NumberAction action) {
    this(0, x, y, action);
  }

  public void reset() {
    this.drag = false;
  }

  public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
    int x_end = this.x + this.width;
    int y_end = this.y + this.height;

    if(this.drag) this.dragged(mouseX, mouseY);

    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    this.drawString(
            mc.fontRenderer,
            String.valueOf(number),
            this.x - (mc.fontRenderer.getStringWidth(String.valueOf(number)) + 8),
            this.y,
            Color.WHITE.getRGB());
    this.drawRect(this.x, this.y, x_end, y_end, Color.DARK_GRAY.getRGB());
    this.drawRect(this.x, this.y, x_end, this.y + 1, SliderButton.BORDER);
    this.drawRect(this.x, this.y, this.x + 1, y_end, SliderButton.BORDER);
    this.drawRect(this.x, y_end - 1, x_end, y_end, SliderButton.BORDER);
    this.drawRect(x_end - 1, this.y, x_end, y_end, SliderButton.BORDER);

    int index = (((this.number & 0xff) / 85) + ((((this.number >> 8) & 0xff) / 85) * 4) + ((((this.number >> 16) & 0xff) / 85) * 16)) / (64 / this.width);
    this.drawRect(this.x + index, this.y - 2, this.x + index + 1, y_end + 2, SliderButton.BORDER);
    this.drawRect(this.x + index - 1, this.y, this.x + index + 2, y_end, SliderButton.BORDER);

    if(!this.enabled) this.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0x99333333);
  }

  public void dragged(int mouseX, int mouseY) {
    int cursor = (mouseX < this.x ? this.x : (mouseX >= this.x + this.width ? this.x + this.width - 1 : mouseX));
    int index = (cursor - this.x) * (64 / this.width);
    if (index > 32) index += (64 / this.width) - 1;
//    this.number = ((index & 3) * 85) + ((((index >> 2) & 3) * 85) * 256) + (((index >> 4) * 85) * 65536) + 0xff000000;
    if (this.action != null) this.action.setNumber(index);
  }

  public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
    if(super.mousePressed(mc, mouseX, mouseY)) {
      this.dragged(mouseX, mouseY);
      this.drag = true;
      return true;
    }
    return false;
  }

  public void mouseReleased(int mouseX, int mouseY) {
    this.drag = false;
  }

  public void onClick(GuiScreen parent) {

  }
}
