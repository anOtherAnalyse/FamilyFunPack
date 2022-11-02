package family_fun_pack.modules;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.commands.Command;
import family_fun_pack.commands.Commands;

/* Get commands from chat & find associated FFP command */

@SideOnly(Side.CLIENT)
public class CommandsModule extends Module {

  public static String ESCAPE_CHARACTER = ".";

  private static Commands commands;

  public CommandsModule() {
    super("FFP Commands", "Enable Family Fun Pack commands");
    commands = new Commands();
  }

  public Command getCommand(String name) {
    return commands.getCommand(name);
  }

  protected void enable() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  protected void disable() {
    MinecraftForge.EVENT_BUS.unregister(this);
  }

  public void onDisconnect() {
    for(Command c : commands.getCommands()) {
      c.onDisconnect();
    }
  }

  public boolean displayInGui() {
    return false;
  }

  public void save(Configuration configuration) {
    configuration.get(this.name, "escape", ".").set(CommandsModule.ESCAPE_CHARACTER);
    super.save(configuration);
  }

  public void load(Configuration configuration) {
    CommandsModule.ESCAPE_CHARACTER = configuration.get(this.name, "escape", ".").getString().substring(0, 1);
    super.load(configuration);
  }

  @SubscribeEvent
  public void onChat(ClientChatEvent event) {
    String message = event.getMessage();
    if(message.startsWith(CommandsModule.ESCAPE_CHARACTER) && this.handleCommand(message.substring(1))) {
      event.setCanceled(true);
      Minecraft.getMinecraft().ingameGUI.getChatGUI().addToSentMessages(message);
    }
  }

  public boolean handleCommand(String command) {
    String[] args = command.split(" +");
    if (args.length == 0) return false;

    Command cmd = commands.getCommand(args[0]);
    if (cmd == null) return false;

    final String lines = cmd.execute(args);
    if(lines != null) {
      for (String line : lines.split("\\r?\\n")) {
        FamilyFunPack.printMessage(line);
      }
    }

    return true;
  }

  public boolean defaultState() {
    return true;
  }

  public static Commands getCommands() {
    return commands;
  }
}
