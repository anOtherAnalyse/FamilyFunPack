package family_fun_pack.modules.interfaces;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.interfaces.PacketsSelectionGui;

@SideOnly(Side.CLIENT)
public class PacketsSelectionModule extends InterfaceModule {

  public String getLabel() {
    return "which packets ?";
  }

  public ActionButton getAction() {
    return new OpenGuiButton(0, 0, "select", PacketsSelectionGui.class, this.dependence);
  }

}
