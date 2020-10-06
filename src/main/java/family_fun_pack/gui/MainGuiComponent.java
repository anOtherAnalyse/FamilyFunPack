package family_fun_pack.gui;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.gui.components.ActionButton;

@SideOnly(Side.CLIENT)
public interface MainGuiComponent {

  public String getLabel();

  public ActionButton getAction();

}
