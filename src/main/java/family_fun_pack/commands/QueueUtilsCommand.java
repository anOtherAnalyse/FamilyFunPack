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

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Inspect 2b2t queue, who is in, can not know position */

@SideOnly(Side.CLIENT)
public class QueueUtilsCommand extends Command implements PacketListener {

  public QueueUtilsCommand() {
    super("queue");
  }

  public String usage() {
    return this.getName() + " show|hide|<player>";
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
        default:
          if(mc.world.getScoreboard().getPlayersTeam(args[1]) != null) return args[0] + " is in queue";
          return args[0] + " not in queue";
      }
      return null;
    }
    return this.usage();
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketTeams teams = (SPacketTeams) packet;
    switch(teams.getAction()) {
      case 3:
        for(String player : teams.getPlayers()) {
          FamilyFunPack.printMessage(TextFormatting.GREEN + "Player " + player + " added to queue");
        }
        break;
      case 4:
        for(String player : teams.getPlayers()) {
          FamilyFunPack.printMessage(TextFormatting.RED + "Player " + player + " removed from queue");
        }
        break;
    }
    return packet;
  }
}
