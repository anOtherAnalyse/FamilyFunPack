package family_fun_pack.modules.interfaces;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.interfaces.InfoItemGui;

@SideOnly(Side.CLIENT)
public class InfoItemModule extends InterfaceModule {

  public String getLabel() {
    return "Info items";
  }

  public ActionButton getAction() {
    return new OpenGuiButton(0, 0, "view", InfoItemGui.class, this.dependence);
  }

}
