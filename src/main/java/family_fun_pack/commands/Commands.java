package family_fun_pack.commands;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/* Commands record */

@SideOnly(Side.CLIENT)
public class Commands {
  private Map<String, Command> commands;

  /* I'm too lazy to develop interfaces so let's add a lot of commands */

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
    this.registerCommand(new ItemSizeCommand());
    this.registerCommand(new FillBookCommand());
    this.registerCommand(new EditSignCommand());
    this.registerCommand(new RollbackCommand());
    this.registerCommand(new StealCommand());
    this.registerCommand(new KillDupeCommand());
    this.registerCommand(new RemoteRideCommand());
    this.registerCommand(new BlockAtCommand());
    this.registerCommand(new RemoteCaptureCommand());
    this.registerCommand(new LoadChunkCommand());
    this.registerCommand(new PopulateCommand());
    this.registerCommand(new WorldDownloadCommand());
    this.registerCommand(new TrackCommand());
    this.registerCommand(new IgnoreCommand());
    this.registerCommand(new ReOpenCommand());
    this.registerCommand(new NearestStrongholdCommand());
    this.registerCommand(new RollbackDupeCommand());
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
