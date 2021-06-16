package family_fun_pack.modules;

import com.electronwill.nightconfig.core.Config;

import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import family_fun_pack.gui.MainGuiComponent;
import family_fun_pack.gui.components.OnOffButton;
import family_fun_pack.key.KeyAction;

/* An abstract Module */

@OnlyIn(Dist.CLIENT)
public abstract class Module implements MainGuiComponent, Button.IPressable, KeyAction {

  protected Config config;

  protected String id;
  protected String name;

  public Module(String id, String name) {
    this.id = id;
    this.name = name;
  }

  /* On Module enabled */
  protected abstract void enable();

  /* On Module disabled */
  protected abstract void disable();

  public void onPress(Button btn) {
    if(btn instanceof OnOffButton) {
      this.toggle(((OnOffButton) btn).getState());
    }
  }

  public void onKeyDown(int keysym) {
    this.toggle();
  }

  public void toggle() {
    this.toggle(! this.isEnabled());
  }

  public void toggle(boolean state) {
    if(state) {
      if(! this.isEnabled()) {
        this.enable();
        this.setEnabled(true);
      }
    } else if(this.isEnabled()) {
      this.disable();
      this.setEnabled(false);
    }
  }

  public boolean isEnabled() {
    return this.config.getByteOrElse("enabled", this.defaultState() ? (byte) 1 : (byte) 0) == 1;
  }

  private void setEnabled(boolean enabled) {
    this.config.set("enabled", enabled ? (byte) 1 : (byte) 0);
  }

  // Get config, record a new one if none exists
  protected <T> T getOrElse(String path, T defaultV) {
    T value = this.config.<T>get(path);
    if(value == null) {
      this.config.<T>set(path, defaultV);
      return defaultV;
    }
    return value;
  }

  /* Is the Module enabled by default (first use) */
  public boolean defaultState() {
    return false;
  }

  public void load(Config config) {
    this.config = config;
    if(this.isEnabled()) {
      this.enable();
    }
  }

  /* Called when disconnecting from server */
  public void onDisconnect() {}

  /* Display this module in Main Gui */
  public boolean displayInGui() {
    return true;
  }

  public String getLabel() {
    return this.name;
  }

  public String getId() {
    return this.id;
  }

  public MainGuiComponent getChild() {
    return null;
  }

  public Widget getAction() {
    OnOffButton act = new OnOffButton(0, 0, this);
    act.setState(this.isEnabled());
    return act;
  }
}
