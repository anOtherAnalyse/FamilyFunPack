package family_fun_pack.modules;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/* All Modules record */

@SideOnly(Side.CLIENT)
public class Modules {

  private List<Module> modules;

  private Configuration configuration;

  public Modules(File configuration_file) {

    this.configuration = new Configuration(configuration_file);

    this.modules = new ArrayList<Module>();
    this.modules.add(new BookFormatModule());
    this.modules.add(new CommandsModule());
    this.modules.add(new IgnoreModule());
    this.modules.add(new PacketInterceptionModule());
    this.modules.add(new PigPOVModule());
    this.modules.add(new PortalInvulnerabilityModule());
    this.modules.add(new SearchModule());
    this.modules.add(new NoCloseModule());
    this.modules.add(new StalkModule());
    this.modules.add(new TraceModule());
    this.modules.add(new TrueDurabilityModule());
    this.modules.add(new UndeadModule());
    this.load();
  }

  public List<Module> getModules() {
    return this.modules;
  }

  public void onDisconnect() {
    for(Module i : this.modules) {
      i.onDisconnect();
    }
  }

  public Configuration getConfiguration() {
    return this.configuration;
  }

  public Module getByName(String name) {
    for(Module m : this.modules) {
      if(m.getLabel().equals(name)) return m;
    }
    return null;
  }

  private void load() {
    for(Module i : this.modules) {
      i.load(this.configuration);
    }
  }

  public void save() {
    for(Module i : this.modules) {
      i.save(this.configuration);
    }
    this.configuration.save();
  }

}
