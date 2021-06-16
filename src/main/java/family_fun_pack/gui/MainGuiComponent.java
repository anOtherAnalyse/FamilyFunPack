package family_fun_pack.gui;

import net.minecraft.client.gui.widget.Widget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/* A line in the main GUI, a label & a button */

@OnlyIn(Dist.CLIENT)
public interface MainGuiComponent {

  // Get Label of the line
  public abstract String getLabel();

  // Get action button to be displayed after label
  public abstract Widget getAction();

  /* Get child component, a component depending on this one
     Returns null if component has no child */
  public abstract MainGuiComponent getChild();
  
}
