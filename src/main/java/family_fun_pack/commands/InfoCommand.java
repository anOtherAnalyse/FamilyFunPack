package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Get information about player, world, plugins, ... */

@SideOnly(Side.CLIENT)
public class InfoCommand extends Command implements PacketListener {

  private String[] plugins;
  private boolean rcv_plugins;

  public InfoCommand() {
    super("info");
    this.plugins = null;
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 24);
  }

  public String usage() {
    return this.getName() + " [plugins]";
  }

  public String execute(String[] args) {
    if(args.length > 1) { // Plugins listening for custom messages

      if(args[1].startsWith("plugin")) {

        if(! this.rcv_plugins) return "No info received about listening plugins";

        if(this.plugins == null || this.plugins.length == 0) return "No listening plugins";

        return "Listening plugins: [" + String.join(", ", this.plugins) + "]";

      } else return this.getUsage();

    } else { // Get basic info
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

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketCustomPayload custom = (SPacketCustomPayload) packet;
    if(custom.getChannelName().equals("REGISTER")) {

      // FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 24);

      PacketBuffer buff = custom.getBufferData();
      byte[] data = new byte[buff.readableBytes()];
      buff.readBytes(data);

      this.rcv_plugins = true;
      this.plugins = (new String(data, StandardCharsets.UTF_8)).split("\0");
    }
    return packet;
  }

  public void onDisconnect() {
    this.plugins = null;
    this.rcv_plugins = false;
    // FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 24);
  }
}
