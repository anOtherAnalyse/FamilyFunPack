package family_fun_pack.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/* Utility class to create a world download */

public class WorldCapture {

  private static final Logger LOGGER = LogManager.getLogger();

  private IChunkLoader chunkLoader;
  private FakeWorld fakeWorld;
  private String name;

  public WorldCapture(String saveName, DimensionType dimension, BlockPos spawn) {
    Minecraft mc = Minecraft.getMinecraft();

    this.name = saveName;

    File saves_folder = new File("./saves");
    saves_folder.mkdirs();

    AnvilSaveHandler saveHandler = new AnvilSaveHandler(saves_folder, saveName, false, DataFixesManager.createFixer());
    WorldProvider provider = null;
    switch(dimension) {
      case OVERWORLD:
        provider = new WorldProviderSurface();
        break;
      case NETHER:
        provider = new WorldProviderHell();
        break;
      case THE_END:
        provider = new WorldProviderEnd();
        break;
    }

    WorldInfo info = new WorldInfo(new WorldSettings(42l, GameType.CREATIVE, false, false, WorldType.FLAT), saveName);
    info.setSpawn(spawn);
    info.setAllowCommands(true);
    info.setDifficulty(EnumDifficulty.PEACEFUL);
    info.setServerInitialized(true);

    saveHandler.saveWorldInfoWithPlayer(info, null);

    this.chunkLoader = saveHandler.getChunkLoader(provider);
    this.fakeWorld = new FakeWorld(saveHandler, info, provider);
  }

  public void captureChunk(Chunk chunk) {
    try {
      chunk.setLastSaveTime(this.fakeWorld.getTotalWorldTime());
      this.chunkLoader.saveChunk(this.fakeWorld, chunk);
    } catch (IOException e) {
      LOGGER.error("FFP: Couldn't save captured chunk", (Throwable)e);
    } catch (MinecraftException e) {
      LOGGER.error("FFP: Couldn't save captured chunk; already in use by another instance of Minecraft?", (Throwable)e);
    }

    this.chunkLoader.flush();
  }

  public FakeWorld getWorld() {
    return this.fakeWorld;
  }

  public String getName() {
    return this.name;
  }
}
