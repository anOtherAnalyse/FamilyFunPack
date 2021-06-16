package family_fun_pack.commands;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/* Commands record */

@OnlyIn(Dist.CLIENT)
public class Commands {
  private Map<String, Command> commands;

  public Commands() {
    this.commands = new HashMap<String, Command>();
    this.register(new InfoCommand());
  }

  public void register(Command cmd) {
    this.commands.put(cmd.getName(), cmd);
  }

  public Command getCommand(String name) {
    return this.commands.get(name);
  }

  public Collection<Command> getCommands() {
    return this.commands.values();
  }
}
