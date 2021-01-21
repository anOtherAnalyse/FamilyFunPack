package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.utils.WorldCapture;

/* World download */

@SideOnly(Side.CLIENT)
public class WorldDownloadCommand extends Command {

  private WorldCapture capture;

  public WorldDownloadCommand() {
    super("download");
    this.capture = null;
  }

  public String usage() {
    return this.getName() + " start <capture_name> | stop";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();
    if(args.length > 1) {
      if(args[1].equals("start") && args.length > 2) {
        if(this.capture != null) return "A download is currently on";

        this.capture = new WorldCapture(args[2], mc.world.provider.getDimensionType(), new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ));
        MinecraftForge.EVENT_BUS.register(this);

        return "World download on, go walk around..";
      } else if(args[1].equals("stop")) {
        if(this.capture == null) return "No current download";

        String name = this.capture.getName();
        this.onDisconnect();

        return "World download saved under name \"" + name + "\"";
      }
    }
    return this.getUsage();
  }

  @SubscribeEvent
  public void onUnLoad(ChunkEvent.Unload event) {
    Chunk c = event.getChunk();
    c.setLightPopulated(false);
    this.capture.captureChunk(c);
  }

  public void onDisconnect() {
    if(this.capture != null) {
      MinecraftForge.EVENT_BUS.unregister(this);
      this.saveSurroundings();
      this.capture = null;
    }
  }

  private void saveSurroundings() {
    Minecraft mc = Minecraft.getMinecraft();

    int x = (int)mc.getRenderViewEntity().posX >> 4, z = (int)mc.getRenderViewEntity().posZ >> 4;
    for(int i = x - mc.gameSettings.renderDistanceChunks; i <= x + mc.gameSettings.renderDistanceChunks; i ++) {
      for(int j = z - mc.gameSettings.renderDistanceChunks; j <= z + mc.gameSettings.renderDistanceChunks; j ++) {
        Chunk c = mc.world.getChunkProvider().getLoadedChunk(i, j);
        if(c != null) {
          c.setLightPopulated(false);
          this.capture.captureChunk(c);
        }
      }
    }
  }
}
