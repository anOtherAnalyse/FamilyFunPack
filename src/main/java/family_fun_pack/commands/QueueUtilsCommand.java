package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketTeams;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

@SideOnly(Side.CLIENT)
public class QueueUtilsCommand extends Command implements PacketListener {

  public QueueUtilsCommand() {
    super("queue");
  }

  public String usage() {
    return this.getName() + " show|hide|teams|<team_name>";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      Minecraft mc = Minecraft.getMinecraft();
      switch(args[1]) {
        case "show":
          FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 68);
          break;
        case "hide":
          FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 68);
          break;
        case "teams":
          {
            Scoreboard score = mc.world.getScoreboard();
            for(ScorePlayerTeam team : score.getTeams()) {
              FamilyFunPack.printMessage(team.getColor() + team.getName() + "[" + team.getDisplayName() + "] " + Integer.toString(team.getMembershipCollection().size()) + " players");
            }
          }
        break;
        default:
          {
            Scoreboard score = mc.world.getScoreboard();
            ScorePlayerTeam team = score.getTeam(args[1]);
            if(team == null) return "no such team";
            for(String member : team.getMembershipCollection()) {
              FamilyFunPack.printMessage(team.getColor() + member);
            }
          }
      }
      return null;
    }
    return this.usage();
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketTeams teams = (SPacketTeams) packet;
    switch(teams.getAction()) {
      case 0:
        FamilyFunPack.printMessage("Team " + teams.getName() + "[" + teams.getDisplayName() + "] created");
        break;
      case 1:
        FamilyFunPack.printMessage("Team " + teams.getName() + " removed");
        break;
      case 3:
        for(String player : teams.getPlayers()) {
          FamilyFunPack.printMessage(TextFormatting.GREEN + "Player " + player + " added to team " + teams.getName());
        }
        break;
      case 4:
        for(String player : teams.getPlayers()) {
          FamilyFunPack.printMessage(TextFormatting.RED + "Player " + player + " removed from team " + teams.getName());
        }
        break;
    }
    return packet;
  }
}
