package family_fun_pack.commands;

import net.minecraft.network.play.client.CPacketSpectate;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.IllegalArgumentException;
import java.util.UUID;

import family_fun_pack.FamilyFunPack;

/* Send a spectate packet */

@SideOnly(Side.CLIENT)
public class SpectateCommand extends Command {

  public SpectateCommand() {
    super("spectate");
  }

  public String usage() {
    return this.getName() + " <uuid>";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      UUID uuid = null;
      try {
        uuid = UUID.fromString(args[1]);
      } catch(IllegalArgumentException e) {
        return "Wrong UUID format";
      }
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketSpectate(uuid));
      return "trying to spectate " + uuid.toString();
    }
    return this.getUsage();
  }
}
