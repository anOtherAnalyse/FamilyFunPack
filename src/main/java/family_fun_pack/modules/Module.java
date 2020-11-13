package family_fun_pack.modules;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.gui.MainGuiComponent;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.OnOffButton;
import family_fun_pack.gui.components.actions.OnOffAction;

@SideOnly(Side.CLIENT)
public abstract class Module implements OnOffAction, MainGuiComponent {

  private boolean enabled;

  protected String name;
  protected String description;

  public Module(String name, String description) {
    this.name = name;
    this.description = description;
    this.enabled = false;
  }

  public void toggle() {
    this.toggle(! this.enabled);
  }

  public void toggle(boolean state) {
    if(state) {
      if(!this.enabled) {
        this.enable();
        this.enabled = true;
      }
    } else {
      if(this.enabled) {
        this.disable();
        this.enabled = false;
      }
    }
  }

  protected abstract void enable();

  protected abstract void disable();

  public boolean defaultState() {
    return false;
  }

  public void save_state(Configuration configuration) {
    configuration.get(this.name, "state", false).set(this.isEnabled());
  }

  public void save(Configuration configuration) {
    this.save_state(configuration);
  }

  public void load(Configuration configuration) {
    boolean state = configuration.get(this.name, "state", this.defaultState()).getBoolean();
    this.toggle(state);
  }

  public void onDisconnect() {}

  public boolean isEnabled() {
    return this.enabled;
  }

  public String getLabel() {
    return this.name;
  }

  public ActionButton getAction() {
    OnOffButton ret = new OnOffButton(0, 0, this);
    ret.setState(this.isEnabled());
    return ret;
  }

}
