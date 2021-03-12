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

    if(mc.player.dimension == 1) return "Why ?";

    WorldProvider provider = new WorldProviderSurface();

    WorldInfo info = new WorldInfo(new WorldSettings(-8076723744225505211l, GameType.SURVIVAL, true, false, WorldType.DEFAULT), "tmp");

    FakeWorld fake = new FakeWorld(null, info, provider);
    provider.setWorld(fake);

    MapGenStronghold generation = new MapGenStronghold();
    generation.generate(fake, 0, 0, null);

    BlockPos nearest = null;
    if(mc.player.dimension == 0) nearest = generation.getNearestStructurePos(fake, mc.player.getPosition(), false);
    else nearest = generation.getNearestStructurePos(fake, new BlockPos((int)mc.player.posX * 8, 70, (int)mc.player.posZ * 8), false);

    return String.format("Nearest stronghold around (%d, %d) overworld", nearest.getX(), nearest.getZ());
  }
}
