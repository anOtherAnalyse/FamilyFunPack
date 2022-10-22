package family_fun_pack.gui.components;

import family_fun_pack.gui.components.actions.NumberAction;
import family_fun_pack.gui.components.actions.NumberPumpkinAura;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;

@SideOnly(Side.CLIENT)
public class SliderButton extends ActionButton {

  private static final int BORDER = 0xffcccccc;
  private boolean drag;
  private float max, min, number;

  private int index;

  private final NumberAction action;

  public SliderButton(int id, int x, int y, NumberAction action) {
    super(id, x, y, 32, 7, null);
    this.action = action;
    this.drag = false;
  }

  public SliderButton(int x, int y, NumberAction action) {
    this(0, x, y, action);
  }

  public SliderButton setIndex(int index) {
    this.index = index;
    return this;
  }

  public SliderButton setValue(int value) {
    this.number = value;
    return this;
  }

  public SliderButton setMax(int max) {
    this.max = max;
    return this;
  }

  public SliderButton setMin(int min) {
    this.min = min;
    return this;
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
            String.valueOf(Math.round(number)),
            this.x - (mc.fontRenderer.getStringWidth(String.valueOf(Math.round(number))) + 8),
            this.y,
            Color.WHITE.getRGB());
    drawRect(this.x, this.y, x_end, y_end, Color.DARK_GRAY.getRGB());
    drawRect(this.x, this.y, x_end, this.y + 1, SliderButton.BORDER);
    drawRect(this.x, this.y, this.x + 1, y_end, SliderButton.BORDER);
    drawRect(this.x, y_end - 1, x_end, y_end, SliderButton.BORDER);
    drawRect(x_end - 1, this.y, x_end, y_end, SliderButton.BORDER);

    drawRect(this.x + index, this.y - 2, this.x + index + 1, y_end + 2, SliderButton.BORDER);
    drawRect(this.x + index - 1, this.y, this.x + index + 2, y_end, SliderButton.BORDER);

    if(!this.enabled) drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0x99333333);
  }

  public void dragged(int mouseX, int mouseY) {
    // If it works it works ¯\_(ツ)_/¯ - TODO fix
    int cursor = (mouseX < this.x ? this.x : (mouseX >= this.x + this.width ? this.x + this.width - 1 : mouseX));
    int index = (cursor - this.x) * (64 / this.width);
    if (index > 32) index += (64 / this.width) - 1;
    index = ((index & 3) * 85) + ((((index >> 2) & 3) * 85) * 256) + (((index >> 4) * 85) * 65536) + 0xff000000;
    this.index = (((index & 0xff) / 85) + ((((index >> 8) & 0xff) / 85) * 4) + ((((index >> 16) & 0xff) / 85) * 16)) / (64 / this.width);
    // If it works it works ¯\_(ツ)_/¯ - end
    float value = (mouseX - (x + 5f)) * (max - min) / (width - 10f) + min;
    this.number = MathHelper.clamp(value, min, max);
    if (this.action != null) {
      if (this.action instanceof NumberPumpkinAura) {
        ((NumberPumpkinAura) this.action).setIndex(this.index); // If it works it works ¯\_(ツ)_/¯
      }
      this.action.setNumber(Math.round(this.number));
    }
  }

  public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
    if (super.mousePressed(mc, mouseX, mouseY)) {
      this.dragged(mouseX, mouseY);
      this.drag = true;
      return true;
    }
    return false;
  }

  public void mouseReleased(int mouseX, int mouseY) {
    this.drag = false;
  }

  public void onClick(GuiScreen parent) { }
}
