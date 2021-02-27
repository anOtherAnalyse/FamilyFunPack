package family_fun_pack.commands;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.Module;
import family_fun_pack.modules.IgnoreModule;

/* Manage ignored players list */

@SideOnly(Side.CLIENT)
public class IgnoreCommand extends Command {

  public IgnoreCommand() {
    super("ignore");
  }

  public String usage() {
    return this.getName() + " <player>";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      IgnoreModule module = (IgnoreModule) FamilyFunPack.getModules().getByName("Ignore players");
      module.togglePlayer(args[1]);
      return null;
    }
    return this.getUsage();
  }
}
