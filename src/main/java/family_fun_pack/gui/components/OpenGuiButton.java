package family_fun_pack.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.Class;
import java.lang.IllegalAccessException;
import java.lang.InstantiationException;

import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.interfaces.RightPanel;
import family_fun_pack.modules.Module;

@SideOnly(Side.CLIENT)
public class OpenGuiButton extends ActionButton {

  public static final int BACKGROUND = (216 << 24);
  public static final int COLOR = 0xffbbbbbb;
  public static final int ACTIVE_BACKGROUND = 0xffbbbbbb;
  public static final int ACTIVE_COLOR = 0xff000000;

  private FontRenderer fontRenderer;
  private boolean clicked;
  private Class<? extends RightPanel> panel;
  private Module dependsOn;

  public OpenGuiButton(int id, int x, int y, String text, Class<? extends RightPanel> panel, Module dependsOn) {
    super(id, x, y, 0, 0, text);
    this.fontRenderer = Minecraft.getMinecraft().fontRenderer;
    this.width = this.fontRenderer.getStringWidth(this.displayString) + 4;
    this.height = this.fontRenderer.FONT_HEIGHT + 4;
    this.panel = panel;
    this.dependsOn = dependsOn;
    this.clicked = false;
  }

  public OpenGuiButton(int x, int y, String text, Class<? extends RightPanel> panel, Module dependsOn) {
    this(0, x, y, text, panel, dependsOn);
  }

  public void resetState() {
    this.clicked = false;
  }

  public void drawButton(Minecraft client, int mouseX, int mouseY, float partialTicks) {
    int x_end = this.x + this.width;
    int y_end = this.y + this.height;

    int background = (this.clicked ? OpenGuiButton.ACTIVE_BACKGROUND : OpenGuiButton.BACKGROUND);
    int border = (this.clicked ? OpenGuiButton.ACTIVE_COLOR : OpenGuiButton.COLOR);

    this.drawRect(this.x, this.y, x_end, y_end, background);
    this.drawRect(this.x, this.y, x_end, this.y + 1, border);
    this.drawRect(this.x, this.y, this.x + 1, y_end, border);
    this.drawRect(this.x, y_end - 1, x_end, y_end, border);
    this.drawRect(x_end - 1, this.y, x_end, y_end, border);
    this.fontRenderer.drawString(this.displayString, this.x + 2, this.y + 2, border);
  }

  public void onClick(Gui parent) {
    this.clicked = !this.clicked;
    if(this.clicked) {
      MainGui main = (MainGui)parent;
      main.resetOpenBtn();
      this.clicked = true;
      RightPanel panel = null;
      try {
        panel = this.panel.newInstance();
      } catch (InstantiationException e) {
        throw new RuntimeException("FFP error while initializing instance of " + this.panel.toString() + ": " + e.getMessage());
      } catch (IllegalAccessException e) {
        throw new RuntimeException("FFP can not initialize instance of " + this.panel.toString() + ": " + e.getMessage());
      }
      panel.dependsOn(this.dependsOn);
      panel.setParent(main);
      main.setRightPanel(panel);
    } else ((MainGui)parent).setRightPanel(null);
  }

}
