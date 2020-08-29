package true_durability.gui;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public interface ActionButton {

  public abstract void changeState();

  public abstract void performAction();
}
