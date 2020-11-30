package family_fun_pack.commands;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class Commands {
  private Map<String, Command> commands;

  public Commands() {
    this.commands = new HashMap<String, Command>();
    this.registerCommand(new DiffCommand());
    this.registerCommand(new GetCommand());
    this.registerCommand(new UseCommand());
    this.registerCommand(new VanishCommand());
    this.registerCommand(new VClipCommand());
    this.registerCommand(new HClipCommand());
    this.registerCommand(new RaytraceCommand());
    this.registerCommand(new UnloadedRideCommand());
    this.registerCommand(new StalkCommand());
    this.registerCommand(new OpenDonkeyCommand());
    this.registerCommand(new DisconnectCommand());
    this.registerCommand(new SyncMountCommand());
    this.registerCommand(new PacketDumpCommand());
    this.registerCommand(new InfoCommand());
    this.registerCommand(new SpectateCommand());
    this.registerCommand(new RespawnCommand());
    this.registerCommand(new VoidMountCommand());
    this.registerCommand(new PeekCommand());
    this.registerCommand(new QueueUtilsCommand());
  }

  public void registerCommand(Command cmd) {
    this.commands.put(cmd.getName(), cmd);
  }

  public Command getCommand(String name) {
    return this.commands.get(name);
  }

  public Collection<Command> getCommands() {
    return this.commands.values();
  }
}
