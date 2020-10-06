package family_fun_pack.gui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Set;
import java.util.HashSet;

import family_fun_pack.gui.MainGui;

@SideOnly(Side.CLIENT)
public class OverlayGui extends Gui {

  private static final int BORDER = 0xff000000;

  private FontRenderer fontRenderer;

  private Set<String> labels;

  private int height;

  public OverlayGui() {
    this.zLevel = 1;
    this.fontRenderer = Minecraft.getMinecraft().fontRenderer;
    this.labels = new HashSet<String>();
    this.height = this.fontRenderer.FONT_HEIGHT + 4;
  }

  public void drawOverlay() {
    int y = 4;
    for(String l : this.labels) {
      int width = this.fontRenderer.getStringWidth(l) + 4;
      int x_end = 4 + width;
      int y_end = y + this.height;

      this.drawRect(4, y, x_end, y_end, MainGui.BACKGROUND_COLOR);
      this.drawRect(4, y, x_end, y + 1, OverlayGui.BORDER);
      this.drawRect(4, y, 5, y_end, OverlayGui.BORDER);
      this.drawRect(4, y_end - 1, x_end, y_end, OverlayGui.BORDER);
      this.drawRect(x_end - 1, y, x_end, y_end, OverlayGui.BORDER);
      this.drawString(this.fontRenderer, l, 6, y + 2, 0xffffffff);

      y = y_end + 2;
    }
  }

  @SubscribeEvent
  public void drawOverlay(RenderGameOverlayEvent.Text event) {
    this.drawOverlay();
  }

  public void addLabel(String l) {
    this.labels.add(l);
  }

  public void removeLabel(String l) {
    this.labels.remove(l);
  }

}
