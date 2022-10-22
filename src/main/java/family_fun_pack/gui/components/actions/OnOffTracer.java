package family_fun_pack.gui.components.actions;

import net.minecraft.block.Block;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.modules.SearchModule;

@SideOnly(Side.CLIENT)
public class OnOffTracer implements OnOffAction {

  private final SearchModule module;
  private final Block block;

  public OnOffTracer(Block block, SearchModule module) {
    this.block = block;
    this.module = module;
  }

  public void toggle(boolean state) {
    module.setTracerState(this.block, state);
  }

}
