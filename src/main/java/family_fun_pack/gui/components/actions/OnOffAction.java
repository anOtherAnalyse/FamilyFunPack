package family_fun_pack.gui.components.actions;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public interface OnOffAction {

  public void toggle(boolean state);

}
