package family_fun_pack.gui.components.actions;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.modules.SearchModule;

@SideOnly(Side.CLIENT)
public class OnOffTracer implements OnOffAction {

  private SearchModule module;
  private int block_id;

  public OnOffTracer(int block_id, SearchModule module) {
    this.block_id = block_id;
    this.module = module;
  }

  public void toggle(boolean state) {
    module.setTracerState(this.block_id, state);
  }

}
