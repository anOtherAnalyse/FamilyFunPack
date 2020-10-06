package family_fun_pack.modules;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;
import java.util.ArrayList;

@SideOnly(Side.CLIENT)
public class Modules {

  private List<Module> modules;

  public Modules() {
    this.modules = new ArrayList<Module>();
    this.modules.add(new CommandsModule());
    this.modules.add(new PacketInterceptionModule());
    this.modules.add(new PigPOVModule());
    this.modules.add(new PortalInvulnerabilityModule());
    this.modules.add(new NoCloseModule());
    this.modules.add(new TrueDurabilityModule());
  }

  public List<Module> getModules() {
    return this.modules;
  }

  public void onDisconnect() {
    for(Module i : this.modules) {
      i.onDisconnect();
    }
  }

  public Module getByName(String name) {
    for(Module m : this.modules) {
      if(m.getLabel().equals(name)) return m;
    }
    return null;
  }

}
