package family_fun_pack.gui.components.actions;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

/* Action of a color button */

@SideOnly(Side.CLIENT)
public interface NumberAction {

  void setNumber(int number);

}
