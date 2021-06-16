package family_fun_pack.modules;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/* All Modules record */

@OnlyIn(Dist.CLIENT)
public class Modules {

  private File configFile;
  private FileConfig config;

  private Map<String, Module> modules;

  public Modules(File configuration) {
    this.modules = new HashMap<String, Module>();
    this.register(new PacketInterceptionModule());
    this.register(new CommandsModule());

    this.configFile = configuration;
    this.loadConfig();
  }

  private void register(Module module) {
    this.modules.put(module.getId(), module);
  }

  public Collection<Module> getModules() {
    return this.modules.values();
  }

  public Module getById(String id) {
    return this.modules.get(id);
  }

  public void onDisconnect() {
    for(Module module : this.getModules()) {
      module.onDisconnect();
    }
  }

  private void loadConfig() {
    this.config = FileConfig.of(this.configFile);
    this.config.load();

    for(Module module : this.getModules()) {
      Config c = this.config.<Config>get(module.getId());
      if(c == null) {
        c = Config.inMemory();
        this.config.<Config>set(module.getId(), c);
      }
      module.load(c);
    }
  }

  public void saveConfig() {
    this.config.save();
  }

  protected void finalize() throws Throwable {
    this.config.close();
  }
}
