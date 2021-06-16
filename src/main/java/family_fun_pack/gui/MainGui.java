package family_fun_pack.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.ArrayList;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.Module;
import family_fun_pack.modules.Modules;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.interfaces.InfoItemGui;
import family_fun_pack.gui.interfaces.RightPanel;

@OnlyIn(Dist.CLIENT)
public class MainGui extends Screen {

  public static final int BACKGROUND_COLOR = (216 << 24);
  public static final int LABEL_COLOR = 0xffeeeeee;
  public static final int BORDER_COLOR = 0xffbbbbbb;

  public static final int guiWidth = 148;
  public static final int guiHeight = 200;

  private int x, y, x_end, y_end;

  private int exitKey;

  // List of lines in the GUI (label + button)
  private List<MainGuiComponent> lines;

  // GUI right panel
  private RightPanel right_panel;
  private int active_btn;

  public MainGui(Modules modules) {
    super(new StringTextComponent(FamilyFunPack.NAME));

    this.lines = new ArrayList<MainGuiComponent>();
    for(Module i : modules.getModules()) {
      if(i.displayInGui()) {
        this.lines.add((MainGuiComponent) i);
        MainGuiComponent child = i.getChild();
        if(child != null) this.lines.add(child);
      }
    }

    // Add interfaces not bound to any modules
    this.lines.add(InfoItemGui.getMainGuiComponent());
  }

  public void init() {
    // Windows position
    this.x = 12;
    this.y = 12;
    this.x_end = MainGui.guiWidth + this.x;
    this.y_end = MainGui.guiHeight + this.y;

    // Add lines
    int y = this.y + 24;
    for(MainGuiComponent line : this.lines) {

      // Button
      Widget button = line.getAction();
      button.x = this.x_end - 6 - button.getWidth();
      button.y = y;
      this.<Widget>addButton(button);

      // Reset previous active button
      if(button instanceof OpenGuiButton) {
        OpenGuiButton guiBtn = (OpenGuiButton) button;
        guiBtn.setId(y);
        if(this.right_panel != null && guiBtn.getId() == this.active_btn) {
          guiBtn.setClicked(true);
        }
      }

      int height = button.getHeight() > this.font.lineHeight ? button.getHeight() + 4 : this.font.lineHeight + 4;
      y += height;
    }

    if(this.right_panel != null) this.right_panel.onReopen();
    this.setRightPanel(this.right_panel, this.active_btn); // Add it again to child widgets on re-opening
  }

  public void render(MatrixStack mStack, int mouseX, int mouseY, float partialTicks) {
    // Background
    AbstractGui.fill(mStack, this.x, this.y, this.x_end, this.y_end, MainGui.BACKGROUND_COLOR);
    AbstractGui.fill(mStack, this.x, this.y, this.x_end, this.y + 2, MainGui.BORDER_COLOR);
    AbstractGui.fill(mStack, this.x, this.y, this.x + 2, this.y_end, MainGui.BORDER_COLOR);
    AbstractGui.fill(mStack, this.x_end - 2, this.y, this.x_end, this.y_end, MainGui.BORDER_COLOR);
    AbstractGui.fill(mStack, this.x, this.y_end - 2, this.x_end, this.y_end, MainGui.BORDER_COLOR);

    // Title
    AbstractGui.drawCenteredString(mStack, this.font, this.getTitle().getString(), this.x + MainGui.guiWidth / 2, this.y + 4, MainGui.LABEL_COLOR);

    // Labels
    int y = this.y + 24;
    for(MainGuiComponent line : this.lines) {
      Widget button = line.getAction();

      int label_y = (button.getHeight() - this.font.lineHeight) / 2 + y;
      AbstractGui.drawString(mStack, this.font, line.getLabel(), this.x + 6, label_y, MainGui.LABEL_COLOR);

      int height = button.getHeight() > this.font.lineHeight ? button.getHeight() + 4 : this.font.lineHeight + 4;
      y += height;
    }

    // Buttons
    super.render(mStack, mouseX, mouseY, partialTicks);

    // Draw second panel
    if(this.right_panel != null) {
      this.right_panel.render(mStack, mouseX, mouseY, partialTicks);
    }
  }

  public boolean keyPressed(int keysym, int scancode, int mouse) {
      if(keysym == this.exitKey || keysym == 256) {
        this.onClose();
        return true;
      } else if(this.right_panel != null) return this.right_panel.keyPressed(keysym, scancode, mouse);
    return false;
  }

  public boolean mouseReleased(double mouseX, double mouseY, int action) {
    setDragging(false);
    if(this.right_panel != null) return this.right_panel.mouseReleased(mouseX, mouseY, action);
    return false;
  }

  // On gui closed
  public void removed() {
    FamilyFunPack.getModules().saveConfig();
  }

  public void setExitKey(int exitKey) {
    this.exitKey = exitKey;
  }

  public void removeRightPanel() {
    this.setRightPanel(null, 0);
  }

  public void setRightPanel(RightPanel panel, int btn_id) {
    if(this.right_panel != null) {
      this.children.remove(this.right_panel);
    }
    this.right_panel = panel;
    if(btn_id >= 0) {
      this.active_btn = btn_id;
      for(Widget w : this.buttons) {
        if(w instanceof OpenGuiButton && ((OpenGuiButton) w).getId() != btn_id) ((OpenGuiButton) w).setClicked(false);
      }
    }
    if(panel != null) this.<Screen>addWidget(this.right_panel);
  }
}
