package family_fun_pack.modules;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.commands.Command;
import family_fun_pack.commands.Commands;

@SideOnly(Side.CLIENT)
public class CommandsModule extends Module {

  private static String ESCAPE_CHARACTER = ".";

  private Commands commands;

  public CommandsModule() {
    super("FFP Commands", "Enable Family Fun Pack commands");
    this.commands = new Commands();
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
    if(message.startsWith(CommandsModule.ESCAPE_CHARACTER)) {
      String[] args = message.substring(1).split("[ ]+");
      if(args.length <= 0) return;
      Command cmd = this.commands.getCommand(args[0]);
      if(cmd != null) {
        String ret = cmd.execute(args);
        if(ret != null) FamilyFunPack.printMessage(ret);
        event.setCanceled(true);
        Minecraft.getMinecraft().ingameGUI.getChatGUI().addToSentMessages(message);
      }
    }
  }

  public boolean defaultState() {
    return true;
  }
}
