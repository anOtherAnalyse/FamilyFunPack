package family_fun_pack.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.Class;
import java.lang.IllegalAccessException;
import java.lang.InstantiationException;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.interfaces.RightPanel;
import family_fun_pack.modules.Module;

/* Button used to open a GUI */

@SideOnly(Side.CLIENT)
public class OpenGuiButton extends ActionButton {

  public static final int BACKGROUND = (216 << 24);
  public static final int COLOR = 0xffbbbbbb;
  public static final int ACTIVE_BACKGROUND = 0xffbbbbbb;
  public static final int ACTIVE_COLOR = 0xff000000;

  private FontRenderer fontRenderer;
  private boolean clicked;
  private Class<? extends RightPanel> panel; // GUI to be opened
  private Module dependsOn; // GUI dependence
  private float scale; // Button scale

  public OpenGuiButton(int id, int x, int y, String text, Class<? extends RightPanel> panel, Module dependsOn, float scale) {
    super(id, x, y, 0, 0, text);
    this.fontRenderer = Minecraft.getMinecraft().fontRenderer;
    this.width = (int)((float)(this.fontRenderer.getStringWidth(this.displayString) + 4) * scale);
    this.height = (int)((float)(this.fontRenderer.FONT_HEIGHT + 4) * scale);
    this.panel = panel;
    this.dependsOn = dependsOn;
    this.clicked = false;
    this.scale = scale;
  }

  public OpenGuiButton(int x, int y, String text, Class<? extends RightPanel> panel, Module dependsOn) {
    this(0, x, y, text, panel, dependsOn, 1f);
  }

  public OpenGuiButton(int x, int y, String text, Class<? extends RightPanel> panel, Module dependsOn, float scale) {
    this(0, x, y, text, panel, dependsOn, scale);
  }

  public void setTarget(Class<? extends RightPanel> panel) {
    this.panel = panel;
  }

  public boolean isClicked() {
    return this.clicked;
  }

  public void resetState() {
    this.clicked = false;
  }

  public void setClicked() {
    this.clicked = true;
  }

  public Class<? extends RightPanel> getPanel() {
    return this.panel;
  }

  public void drawButton(Minecraft client, int mouseX, int mouseY, float partialTicks) {
    if(this.visible) {
      int background = (this.clicked ? OpenGuiButton.ACTIVE_BACKGROUND : OpenGuiButton.BACKGROUND);
      int border = (this.clicked ? OpenGuiButton.ACTIVE_COLOR : OpenGuiButton.COLOR);

      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

      int width = (int)((float)this.width / this.scale);
      int height = (int)((float)this.height / this.scale);

      if(this.scale == 1f) {
        int x_end = this.x + width;
        int y_end = this.y + height;

        this.drawRect(this.x, this.y, x_end, y_end, background);
        this.drawRect(this.x, this.y, x_end, this.y + 1, border);
        this.drawRect(this.x, this.y, this.x + 1, y_end, border);
        this.drawRect(this.x, y_end - 1, x_end, y_end, border);
        this.drawRect(x_end - 1, this.y, x_end, y_end, border);
        this.fontRenderer.drawString(this.displayString, this.x + 2, this.y + 2, border);
      } else {
        int x = (int)((float)this.x / this.scale);
        int y = (int)((float)this.y / this.scale);
        int x_end = (int)((float)(this.x) / this.scale) + width;
        int y_end = (int)((float)(this.y) / this.scale) + height;

        GlStateManager.pushMatrix();
        GlStateManager.scale(this.scale, this.scale, this.scale);
        this.drawRect(x, y, x_end, y_end, background);
        this.drawRect(x, y, x_end, y + 1, border);
        this.drawRect(x, y, x + 1, y_end, border);
        this.drawRect(x, y_end - 1, x_end, y_end, border);
        this.drawRect(x_end - 1, y, x_end, y_end, border);
        this.fontRenderer.drawString(this.displayString, (int)((float)(this.x + 2) / this.scale), (int)((float)(this.y + 2) / this.scale), border);
        GlStateManager.popMatrix();
      }
    }
  }

  public void onClick(GuiScreen parent) {
    MainGui main = FamilyFunPack.getMainGui();
    this.clicked = !this.clicked;
    if(this.clicked) {
      RightPanel panel = null;
      try {
        panel = this.panel.newInstance();
      } catch (InstantiationException e) {
        throw new RuntimeException("FFP error while initializing instance of " + this.panel.toString() + ": " + e.getMessage());
      } catch (IllegalAccessException e) {
        throw new RuntimeException("FFP can not initialize instance of " + this.panel.toString() + ": " + e.getMessage());
      }
      panel.dependsOn(this.dependsOn);
      panel.setParent(parent);
      main.setRightPanel(panel);
    } else main.setRightPanel(null);
  }
}
