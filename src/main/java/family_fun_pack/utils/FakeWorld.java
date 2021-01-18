package family_fun_pack.utils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.block.BlockShulkerBox;
import net.minecraft.util.EnumFacing;

// Only one blockstate world, used for rendering tileentities in gui

@SideOnly(Side.CLIENT)
public class FakeWorld extends World {

  private IBlockState state;

  public FakeWorld(IBlockState state, WorldProvider provider) {
    super(null, null, provider, null, false);
    this.state = state;
  }

  public FakeWorld(ISaveHandler saveHandler, WorldInfo info, WorldProvider provider) {
    super(saveHandler, info, provider, null, false);
    this.state = Blocks.AIR.getDefaultState();
  }

  public IBlockState getBlockState(BlockPos position) {
      if(position.equals(BlockPos.ORIGIN)) return this.state;
      return Blocks.AIR.getDefaultState();
  }

  public boolean setBlockState(BlockPos position, IBlockState state, int updt) {
    this.state = state;
    return true;
  }

  public void notifyBlockUpdate(BlockPos pos, IBlockState oldState, IBlockState newState, int flags) { // Don't do anything

  }

  public long getTotalWorldTime() {
    if(this.worldInfo == null) return 0l;
    return super.getTotalWorldTime();
  }

  protected IChunkProvider createChunkProvider() {
    return null;
  }

  protected boolean isChunkLoaded(int paramInt1, int paramInt2, boolean paramBoolean) {
    return false;
  }

}
