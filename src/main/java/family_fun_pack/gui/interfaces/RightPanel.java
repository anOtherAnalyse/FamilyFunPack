package family_fun_pack.gui.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.modules.Module;

/* Right panel, special module configuration GUI displayed next to Main GUI */

@SideOnly(Side.CLIENT)
public abstract class RightPanel extends GuiScreen {

  protected Module dependence; // Module dependence

  protected GuiScreen parent; // GUI parent that generated this one

  public RightPanel() {
    ScaledResolution scale = new ScaledResolution(Minecraft.getMinecraft());
    this.setWorldAndResolution(Minecraft.getMinecraft(), scale.getScaledWidth(), scale.getScaledHeight());
    this.parent = null;
    this.dependence = null;
  }

  public void setParent(GuiScreen parent) {
    this.parent = parent;
  }

  public void transition(RightPanel next) {
    FamilyFunPack.getMainGui().setRightPanel(next);
  }

  public void dependsOn(Module dependence) {
    this.dependence = dependence;
  }

  protected void actionPerformed(GuiButton btn) throws IOException {
    if(btn instanceof ActionButton) {
      ((ActionButton) btn).onClick((GuiScreen)this);
    }
  }

  public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  public void mouseReleased(int mouseX, int mouseY, int state) {
    super.mouseReleased(mouseX, mouseY, state);
  }

  public void mouseWheel(int wheel) {
  }

  public void keyTyped(char keyChar, int keyCode) throws IOException {
  }

}
