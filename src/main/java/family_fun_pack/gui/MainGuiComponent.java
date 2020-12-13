package family_fun_pack.gui;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;

import family_fun_pack.gui.components.ActionButton;

/* A line in the main GUI, a label & a button */

@SideOnly(Side.CLIENT)
public interface MainGuiComponent {

  // Get Label of the line
  public abstract String getLabel();

  // Get action button to be displayed after label
  public abstract ActionButton getAction();

  /* Get child component, a component depending on this one
     Returns null if component has no child */
  public abstract MainGuiComponent getChild();

}
