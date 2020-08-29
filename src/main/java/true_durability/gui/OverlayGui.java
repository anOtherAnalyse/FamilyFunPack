package true_durability.gui;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.FontRenderer;

import true_durability.Configuration;

@SideOnly(Side.CLIENT)
public class OverlayGui extends Gui {

  private FontRenderer fontRenderer;
  private Configuration configuration;

  public static final String INVULNERABLE_STATE = "Invulnerable";
  public static final String INTERCEPT_STATE = "Interception: On";

  private int height;
  private int invulnerable_width;
  private int intercept_width;

  public OverlayGui(FontRenderer fontRenderer, Configuration configuration) {
    this.zLevel = 1;
    this.fontRenderer = fontRenderer;
    this.configuration = configuration;
    this.invulnerable_width = this.fontRenderer.getStringWidth(OverlayGui.INVULNERABLE_STATE) + 4;
    this.intercept_width = this.fontRenderer.getStringWidth(OverlayGui.INTERCEPT_STATE) + 4;
    this.height = this.fontRenderer.FONT_HEIGHT + 4;
  }

  public void drawOverlay() {
    int y = 5;
    if(this.configuration.currently_invulnerable) {
      int x_end = 5 + this.invulnerable_width;
      int y_end = y + this.height;

      this.drawRect(5, y, x_end, y_end, CommandGui.BACKGROUND_COLOR);
      this.drawRect(5, y, x_end, y + 1, 0xffbbbbbb);
      this.drawRect(5, y, 6, y_end, 0xffbbbbbb);
      this.drawRect(5, y_end - 1, x_end, y_end, 0xffbbbbbb);
      this.drawRect(x_end - 1, y, x_end, y_end, 0xffbbbbbb);
      this.drawString(this.fontRenderer, OverlayGui.INVULNERABLE_STATE, 7, y + 2, 0xffffffff);
      y += 16;
    }

    if(this.configuration.block_player_packets) {
      int x_end = 5 + this.intercept_width;
      int y_end = y + this.height;

      this.drawRect(5, y, x_end, y_end, CommandGui.BACKGROUND_COLOR);
      this.drawRect(5, y, x_end, y + 1, 0xffbbbbbb);
      this.drawRect(5, y, 6, y_end, 0xffbbbbbb);
      this.drawRect(5, y_end - 1, x_end, y_end, 0xffbbbbbb);
      this.drawRect(x_end - 1, y, x_end, y_end, 0xffbbbbbb);
      this.drawString(this.fontRenderer, OverlayGui.INTERCEPT_STATE, 7, y + 2, 0xffffffff);
    }
  }

}
