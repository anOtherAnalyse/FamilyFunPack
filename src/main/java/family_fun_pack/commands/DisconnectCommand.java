package family_fun_pack.commands;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;

/* Disconnect from server, close the socket */

@SideOnly(Side.CLIENT)
public class DisconnectCommand extends Command {

  public DisconnectCommand() {
    super("disconnect");
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    FamilyFunPack.getNetworkHandler().disconnect();
    return null;
  }
}
