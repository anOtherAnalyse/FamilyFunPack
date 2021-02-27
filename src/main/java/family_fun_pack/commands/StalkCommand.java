package family_fun_pack.commands;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;
import java.util.LinkedList;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.Module;
import family_fun_pack.modules.StalkModule;

/* Manage stalked players list */

@SideOnly(Side.CLIENT)
public class StalkCommand extends Command {

  private List<String> users;

  public StalkCommand() {
    super("stalk");
  }

  public String usage() {
    return this.getName() + " (add|del) [player_name]";
  }

  public String execute(String[] args) {
    if(args.length > 2) {
      StalkModule module = (StalkModule) FamilyFunPack.getModules().getByName("Stalk players");
      if(args[1].equals("add") || args[1].equals("+")) {
        module.addPlayer(args[2]);
        return null;
      } else if(args[1].equals("del") || args[1].equals("-")) {
        module.delPlayer(args[2]);
        return null;
      }
    }
    return this.getUsage();
  }
}
