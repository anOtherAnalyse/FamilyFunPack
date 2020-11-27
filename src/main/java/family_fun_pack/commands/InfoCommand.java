package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;

@SideOnly(Side.CLIENT)
public class InfoCommand extends Command {

  public InfoCommand() {
    super("info");
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();
    EntityPlayerSP player = mc.player;
    WorldInfo info = mc.world.getWorldInfo();

    String game_type = mc.playerController.getCurrentGameType().toString();
    boolean hardcore = info.isHardcoreModeEnabled();
    boolean reduce_debug = player.hasReducedDebug();
    String world_type = info.getTerrainType().getName();
    String difficulty = info.getDifficulty().toString();
    String entity_id = Integer.toString(player.getEntityId());
    String max_players = Integer.toString(((NetHandlerPlayClient) FamilyFunPack.getNetworkHandler().getNetHandler()).currentServerMaxPlayers);

    String dimension = null;
    switch(player.dimension) {
      case 0: dimension = "overworld"; break;
      case -1: dimension = "nether"; break;
      case 1: dimension = "end"; break;
      default: dimension = "unknown";
    }

    String stat = player.getGameProfile().getName() + "id[" + entity_id + "] (" + game_type + ") in " + dimension + "[" + world_type + "], " + difficulty;
    if(hardcore) stat += " [hardcore]";
    if(reduce_debug) stat += " [reduce debug info]";
    stat += " max players: " + max_players;

    if(mc.player.isRiding()) {
      stat += ", riding: " + Integer.toString(mc.player.getRidingEntity().getEntityId());
    }

    return stat;
  }
}
