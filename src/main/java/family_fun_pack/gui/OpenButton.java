package family_fun_pack.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class OpenButton extends GuiButton implements ActionButton {

  public FontRenderer fontRenderer;
  public boolean clicked;

  public int background_color = CommandGui.BACKGROUND_COLOR;
  public int color = 0xffbbbbbb;
  public int clicked_background_color = 0xffbbbbbb;
  public int clicked_border_color = 0xff000000;

  public OpenButton(int id, int x, int y, FontRenderer fontRenderer, String text) {
    super(id, x, y, 0, 0, text);
    this.fontRenderer = fontRenderer;
    this.width = this.fontRenderer.getStringWidth(this.displayString) + 4;
    this.height = this.fontRenderer.FONT_HEIGHT + 4;
    this.clicked = false;
  }

  public void drawButton(Minecraft client, int mouseX, int mouseY, float partialTicks) {
    int x_end = this.x + this.width;
    int y_end = this.y + this.height;

    int background = (this.clicked ? this.clicked_background_color : this.background_color);
    int border = (this.clicked ? this.clicked_border_color : this.color);

    this.drawRect(this.x, this.y, x_end, y_end, background);
    this.drawRect(this.x, this.y, x_end, this.y + 1, border);
    this.drawRect(this.x, this.y, this.x + 1, y_end, border);
    this.drawRect(this.x, y_end - 1, x_end, y_end, border);
    this.drawRect(x_end - 1, this.y, x_end, y_end, border);
    this.fontRenderer.drawString(this.displayString, this.x + 2, this.y + 2, border);
  }

  public void changeState() {
    this.clicked = !this.clicked;
    this.performAction();
  }

  public void performAction() {};
}
