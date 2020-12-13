package family_fun_pack.commands;

import net.minecraft.network.play.client.CPacketClientStatus;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;

/* Send a respawn packet */

@SideOnly(Side.CLIENT)
public class RespawnCommand extends Command {

  public RespawnCommand() {
    super("respawn");
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    FamilyFunPack.getNetworkHandler().sendPacket(new CPacketClientStatus(CPacketClientStatus.State.PERFORM_RESPAWN));
    return null;
  }
}
