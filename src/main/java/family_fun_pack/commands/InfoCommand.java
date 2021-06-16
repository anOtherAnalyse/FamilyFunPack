package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;
import family_fun_pack.utils.ReflectUtils;

/* Get information about player, world, plugins, ... */

@OnlyIn(Dist.CLIENT)
public class InfoCommand extends Command implements PacketListener {

  private String[] plugins;
  private boolean rcv_plugins;

  public InfoCommand() {
    super("info");
    FamilyFunPack.getNetworkHandler().registerListener(PacketDirection.CLIENTBOUND, this, 23);
  }

  public String usage() {
    return this.getName() + " server | player | world | plugins";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getInstance();

    if(args.length > 1) {
      switch(args[1]) {
        case "plugins":
          if(! rcv_plugins) return "No information about listening plugins";
          if(plugins.length == 0) return "No listening plugins";
          return "Listening plugins: [" + String.join(", ", this.plugins) + "]";
        case "server":
          if(mc.player != null && mc.player.getServerBrand() != null) return "Server brand: " + mc.player.getServerBrand();
          return "Not on a server";
        case "player":
          if(mc.player != null) {
            if(mc.player.isPassenger()) return String.format("Player [%d] riding [%d]", mc.player.getId(), mc.player.getVehicle().getId());
            return String.format("Player [%d]", mc.player.getId());
          }
          break;
        case "world":
          {
            int chunkRadius = ReflectUtils.<Integer>getFieldValue(FamilyFunPack.getNetworkHandler().getNetHandler(), new String[] {"serverChunkRadius", "field_217287_m"});
            return String.format("Difficulty: %s%s, chunk radius: %d", mc.level.getLevelData().getDifficulty().toString(), mc.level.getLevelData().isHardcore() ? "[hardcore]" : "", chunkRadius);
          }
        default:
          return this.getUsage();
      }
      return null;
    }
    return this.getUsage();
  }

  public IPacket<?> packetReceived(PacketDirection direction, int id, IPacket<?> packet, ByteBuf in) {
    SCustomPayloadPlayPacket custom = (SCustomPayloadPlayPacket) packet;
    if(custom.getIdentifier().getPath().equals("register")) {

      PacketBuffer buff = custom.getData();
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
  }
}
