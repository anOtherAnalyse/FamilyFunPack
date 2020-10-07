package family_fun_pack.modules.interfaces;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.interfaces.SearchSelectionGui;

@SideOnly(Side.CLIENT)
public class SearchSelectionModule extends InterfaceModule {

  public String getLabel() {
    return "Search selection";
  }

  public ActionButton getAction() {
    return new OpenGuiButton(0, 0, "open", SearchSelectionGui.class, this.dependence);
  }

}
