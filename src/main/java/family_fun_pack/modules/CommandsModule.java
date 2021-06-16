package family_fun_pack.modules;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.commands.Command;
import family_fun_pack.commands.Commands;

/* Handles FFP commands */

@OnlyIn(Dist.CLIENT)
public class CommandsModule extends Module {

  private static final String ESCAPE_CHARACTER = ".";

  private Commands commands;

  public CommandsModule() {
    super("commands", "FFP Commands");
    this.commands = new Commands();
  }

  public Command getCommand(String name) {
    return this.commands.getCommand(name);
  }

  protected void enable() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  protected void disable() {
    MinecraftForge.EVENT_BUS.unregister(this);
  }

  public void onDisconnect() {
    for(Command c : this.commands.getCommands()) {
      c.onDisconnect();
    }
  }

  @SubscribeEvent
  public void onChat(ClientChatEvent event) {
    String message = event.getMessage();
    if(message.startsWith(CommandsModule.ESCAPE_CHARACTER) && this.handleCommand(message.substring(1))) {
      event.setCanceled(true);
      Minecraft.getInstance().gui.getChat().addRecentChat(message);
    }
  }

  public boolean handleCommand(String command) {
    String[] args = command.split("[ ]+");
    if(args.length <= 0) return false;

    Command cmd = this.commands.getCommand(args[0]);
    if(cmd == null) return false;

    String ret = cmd.execute(args);
    if(ret != null) FamilyFunPack.printMessage(ret);

    return true;
  }

  public boolean displayInGui() {
    return false;
  }

  public boolean defaultState() {
    return true;
  }
}
