package family_fun_pack.gui.components.actions;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

/* Action of an OnOff button */

@SideOnly(Side.CLIENT)
public interface OnOffAction {

  void toggle(boolean state);

}
