package family_fun_pack.modules;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.gui.MainGuiComponent;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.OnOffButton;
import family_fun_pack.gui.components.actions.OnOffAction;

/* An abstract Module */

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

  /* On Module enabled */
  protected abstract void enable();

  /* On Module disabled */
  protected abstract void disable();

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

  /* Is the Module enabled by default (first use) */
  public boolean defaultState() {
    return false;
  }

  /* Display this module in Gui */
  public boolean displayInGui() {
    return true;
  }

  /* Save basic Module state (enable / disabled) */
  public void save_state(Configuration configuration) {
    configuration.get(this.name, "state", false).set(this.isEnabled());
  }

  /* Save all Module configurations */
  public void save(Configuration configuration) {
    this.save_state(configuration);
  }

  /* Load all Module configuration */
  public void load(Configuration configuration) {
    boolean state = configuration.get(this.name, "state", this.defaultState()).getBoolean();
    this.toggle(state);
  }

  /* Called when disconnecting from server */
  public void onDisconnect() {}

  public boolean isEnabled() {
    return this.enabled;
  }

  public String getLabel() {
    return this.name;
  }

  /* ActionButton to be displayed on gui next to Module label */
  public ActionButton getAction() {
    OnOffButton ret = new OnOffButton(0, 0, this);
    ret.setState(this.isEnabled());
    return ret;
  }

  // Get dependent GuiComponent, i.e components used to open the configuration GUI associated to this Module
  public MainGuiComponent getChild() {
    return null;
  }

}
