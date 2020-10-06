package family_fun_pack.modules.interfaces;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.gui.MainGuiComponent;
import family_fun_pack.modules.Module;

@SideOnly(Side.CLIENT)
public abstract class InterfaceModule implements MainGuiComponent {

  protected Module dependence;

  public Module dependsOn() {
    return this.dependence;
  }

  public InterfaceModule dependsOn(Module dependence) {
    this.dependence = dependence;
    return this;
  }

}
