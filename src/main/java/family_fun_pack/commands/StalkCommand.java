package family_fun_pack.commands;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;
import java.util.LinkedList;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.Module;
import family_fun_pack.modules.StalkModule;

@SideOnly(Side.CLIENT)
public class StalkCommand extends Command {

  private List<String> users;

  public StalkCommand() {
    super("stalk");
  }

  public String usage() {
    return this.getName() + " (add|del|list) [player_name]";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      StalkModule module = (StalkModule) FamilyFunPack.getModules().getByName("Stalk players");
      if(args[1].equals("list")) {
        for(String name : module.getPlayers()) {
          FamilyFunPack.printMessage("> " + name);
        }
        return null;
      } else if(args.length > 2) {
        if(args[1].equals("add")) {
          module.addPlayer(args[2]);
          module.save(FamilyFunPack.getModules().getConfiguration());
          return "Player " + args[2] + " added to stalking list";
        } else if(args[1].equals("del")) {
          module.delPlayer(args[2]);
          module.save(FamilyFunPack.getModules().getConfiguration());
          return "Player " + args[2] + " removed from stalking list";
        }
      }
    }
    return this.usage();
  }
}
