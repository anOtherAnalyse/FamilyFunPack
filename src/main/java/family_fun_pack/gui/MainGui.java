package family_fun_pack.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.Module;
import family_fun_pack.modules.Modules;
import family_fun_pack.modules.interfaces.InterfaceModule;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.interfaces.RightPanel;

@SideOnly(Side.CLIENT)
public class MainGui extends GuiScreen {

  public static final int BACKGROUND_COLOR = (216 << 24);
  public static final int LABEL_COLOR = 0xffeeeeee;
  public static final int BORDER_COLOR = 0xffbbbbbb;

  public static final int guiWidth = 148;
  public static final int guiHeight = 200;

  private int x, y, x_end, y_end;

  private int exitKey;

  private List<MainGuiComponent> lines;

  private RightPanel right_panel;

  public MainGui(Modules modules, List<InterfaceModule> interfaces) {
    this.lines = new ArrayList<MainGuiComponent>();
    for(Module i : modules.getModules()) {
      this.lines.add((MainGuiComponent) i);
      for(int j = 0; j < interfaces.size(); j ++) {
        if(interfaces.get(j).dependsOn() == i) {
          this.lines.add((MainGuiComponent) interfaces.remove(j));
          j--;
        }
      }
    }
    for(InterfaceModule i : interfaces) {
      this.lines.add((MainGuiComponent) i);
    }
    this.right_panel = null;
    this.exitKey = -1;
  }

  public void setExitKey(int exitKey) {
    this.exitKey = exitKey;
  }

  public void initGui() {
    // Windows position
    this.x = 12;
    this.y = 12;
    this.x_end = MainGui.guiWidth + this.x;
    this.y_end = MainGui.guiHeight + this.y;

    this.labelList.clear();
    this.buttonList.clear();

    // Window title
    GuiLabel title = (new GuiLabel(this.fontRenderer, 0, this.x + 2, this.y + 4, MainGui.guiWidth - 4, 16, MainGui.LABEL_COLOR)).setCentered();
    title.addLine("Family Fun Pack");
    this.labelList.add(title);

    // Add lines
    int i = 0, y = this.y + 24;

    for(MainGuiComponent line : this.lines) {
      // label
      GuiLabel label = new GuiLabel(this.fontRenderer, i, this.x + 6, y, MainGui.guiWidth - 4, 16, MainGui.LABEL_COLOR);
      label.addLine(line.getLabel());
      this.labelList.add(label);

      // Button
      GuiButton button = (GuiButton)line.getAction();
      button.x = this.x_end - 6 - button.width;
      button.y = y + 2;
      this.buttonList.add(button);

      if(button instanceof OpenGuiButton && this.right_panel != null && ((OpenGuiButton) button).getPanel() == this.right_panel.getClass()) {
        ((OpenGuiButton)button).setClicked();
      }

      int height = button.height > this.fontRenderer.FONT_HEIGHT ? button.height + 4 : this.fontRenderer.FONT_HEIGHT + 4;
      y += height;

      i ++;
    }
  }

  public void onGuiClosed() {
    FamilyFunPack.getModules().save();
  }

  protected void actionPerformed(GuiButton button) throws IOException {
    if(button instanceof ActionButton) {
      ActionButton action = (ActionButton) button;
      action.onClick((GuiScreen) this);
    }
  }

  public void setRightPanel(RightPanel panel) {
    this.right_panel = panel;
  }

  public void resetOpenBtn() {
    for(GuiButton b : this.buttonList) {
      if(b instanceof OpenGuiButton) {
        ((OpenGuiButton)b).resetState();
      }
    }
  }

  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if(keyCode == Keyboard.KEY_ESCAPE || keyCode == this.exitKey) {
      this.mc.displayGuiScreen(null);
      if (this.mc.currentScreen == null) {
        this.mc.setIngameFocus();
      }
    } else if(this.right_panel != null) this.right_panel.keyTyped(typedChar, keyCode);
  }

  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    Gui.drawRect(this.x, this.y, this.x_end, this.y_end, MainGui.BACKGROUND_COLOR); // GUI background

    // borders
    Gui.drawRect(this.x, this.y, this.x_end, this.y + 2, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x, this.y, this.x + 2, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end - 2, this.y, this.x_end, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x, this.y_end - 2, this.x_end, this.y_end, MainGui.BORDER_COLOR);

    super.drawScreen(mouseX, mouseY, partialTicks);

    // Draw packets intercept selection
    if(this.right_panel != null) {
      this.right_panel.drawScreen(mouseX, mouseY, partialTicks);
    }
  }

  public boolean doesGuiPauseGame() {
	    return true;
	}

  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if(this.right_panel != null) {
      this.right_panel.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  protected void mouseReleased(int mouseX, int mouseY, int state) {
    super.mouseReleased(mouseX, mouseY, state);
    if(this.right_panel != null) {
      this.right_panel.mouseReleased(mouseX, mouseY, state);
    }
  }

  public void updateScreen() {
    if(this.right_panel != null) {
      this.right_panel.updateScreen();
    }
  }

  public void reset() {
    this.right_panel = null;
  }
}
