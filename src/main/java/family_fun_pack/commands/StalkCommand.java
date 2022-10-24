package family_fun_pack.commands;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.Module;
import family_fun_pack.modules.StalkModule;

/* Manage stalked players list */

@SideOnly(Side.CLIENT)
public class StalkCommand extends Command {

  public StalkCommand() {
    super("stalk");
  }

  public String usage() {
    return this.getName() + " <player>";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      StalkModule module = (StalkModule) FamilyFunPack.getModules().getByClass(StalkModule.class);
      module.togglePlayer(args[1]);
      return null;
    }
    return this.getUsage();
  }
}
