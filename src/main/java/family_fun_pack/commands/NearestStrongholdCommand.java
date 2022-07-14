package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.utils.FakeWorld;

/* 9b9t - get nearest stronghold (based on 9b seed) */
/* 2b2t - get nearest stronghold (based on 2b know portal locations) */

@SideOnly(Side.CLIENT)
public class NearestStrongholdCommand extends Command {

  public NearestStrongholdCommand() {
    super("nearest");
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();

    //check if server is 2b2t.org using  Minecraft.getCurrentServerData()
    if (mc.getCurrentServerData().serverIP.equals("connect.2b2t.org")) {
      //get stronghold location nearest to player on 2b2t.org

      if (mc.player.dimension == 1) return "dont you feel stupid... dont you feel a little ashamed...";

      int[][] endPortalCoords = {{1888, -32}, {-560, 1504}, {2064, -4400}, {-4992, -512}, {2960, 4208}, {-3200, 4480}, {-5568, 608}, {-2496, 5296}};

      int closestX = endPortalCoords[0][0];
      int closestZ = endPortalCoords[0][1];
      int shortestDistance = (int) mc.player.getDistanceSq(endPortalCoords[0][0], 0, endPortalCoords[0][1]);
      for (int i = 1; i < endPortalCoords.length; i++) {
        int d = (int) mc.player.getDistanceSq(endPortalCoords[i][0], 0, endPortalCoords[i][1]);
        if (d < shortestDistance) {
          closestX = endPortalCoords[i][0];
          closestZ = endPortalCoords[i][1];
          shortestDistance = d;

        }
      }

      return String.format("Nearest stronghold around (%d, %d) overworld", closestX, closestZ);


    } else if (mc.getCurrentServerData().serverIP.equals("9b9t.org")) {


      if (mc.player.dimension == 1) return "Why ?";

      WorldProvider provider = new WorldProviderSurface();

      WorldInfo info = new WorldInfo(new WorldSettings(-8076723744225505211l, GameType.SURVIVAL, true, false, WorldType.DEFAULT), "tmp");

      FakeWorld fake = new FakeWorld(null, info, provider);
      provider.setWorld(fake);

      MapGenStronghold generation = new MapGenStronghold();
      generation.generate(fake, 0, 0, null);

      BlockPos nearest = null;
      if (mc.player.dimension == 0) nearest = generation.getNearestStructurePos(fake, mc.player.getPosition(), false);
      else
        nearest = generation.getNearestStructurePos(fake, new BlockPos((int) mc.player.posX * 8, 70, (int) mc.player.posZ * 8), false);

      return String.format("Nearest stronghold around (%d, %d) overworld", nearest.getX(), nearest.getZ());

    }
    return "you are not in 2b2t or 9b9t please join one to use this";
  }
}
