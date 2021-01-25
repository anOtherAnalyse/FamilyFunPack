package family_fun_pack.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.interfaces.BookEditingGui;

/* Button describing a text format */

@SideOnly(Side.CLIENT)
public class FormatButton extends ActionButton {

  private TextFormatting format;

  private FontRenderer fontRenderer;

  public FormatButton(int id, int x, int y, FontRenderer fontRenderer, TextFormatting format) {
    super(id, x, y, 0, 0, null);
    this.fontRenderer = fontRenderer;
    this.format = format;
    this.width = 7;
    this.height = 7;
  }

  public void drawButton(Minecraft client, int mouseX, int mouseY, float partialTicks) {
    GlStateManager.pushMatrix();
    float scale = 0.7f;
    GlStateManager.scale(scale, scale, scale);

    int x = (int)((float)this.x / scale);
    int y = (int)((float)this.y / scale);
    int x_end = (int)((float)(this.x + this.width) / scale);
    int y_end = (int)((float)(this.y + this.height) / scale);

    if(this.format.isColor()) {
      this.drawRect(x, y, x_end, y_end, 0xff000000 | this.fontRenderer.getColorCode(this.format.toString().charAt(1)));
    } else {
      this.fontRenderer.drawString(this.format.toString().substring(1), x + 2, y + 1, 0xffffffff);
    }

    // Borders
    this.drawRect(x, y, x_end, y + 1, 0xffffffff);
    this.drawRect(x, y, x + 1, y_end, 0xffffffff);
    this.drawRect(x, y_end - 1, x_end, y_end, 0xffffffff);
    this.drawRect(x_end - 1, y, x_end, y_end, 0xffffffff);

    GlStateManager.popMatrix();
  }

  public void onClick(GuiScreen parent) {
    ((BookEditingGui) parent).appendFormat(this.format.toString());
  }

}
