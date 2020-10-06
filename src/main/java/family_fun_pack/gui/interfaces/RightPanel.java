package family_fun_pack.gui.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.modules.Module;

@SideOnly(Side.CLIENT)
public abstract class RightPanel extends GuiScreen {

  protected Module dependence;

  protected MainGui parent;

  public RightPanel() {
    ScaledResolution scale = new ScaledResolution(Minecraft.getMinecraft());
    this.setWorldAndResolution(Minecraft.getMinecraft(), scale.getScaledWidth(), scale.getScaledHeight());
    this.parent = null;
    this.dependence = null;
  }

  public void setParent(MainGui parent) {
    this.parent = parent;
  }

  public void transition(RightPanel next) {
    this.parent.setRightPanel(next);
  }

  public void dependsOn(Module dependence) {
    this.dependence = dependence;
  }

  protected void actionPerformed(GuiButton btn) throws IOException {
    if(btn instanceof ActionButton) {
      ((ActionButton) btn).onClick(this);
    }
  }

  public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  public void mouseReleased(int mouseX, int mouseY, int state) {
    super.mouseReleased(mouseX, mouseY, state);
  }

}
