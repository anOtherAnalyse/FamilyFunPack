package family_fun_pack.gui.components.actions;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.gui.components.ColorButton;
import family_fun_pack.gui.components.OnOffButton;
import family_fun_pack.modules.SearchModule;

@SideOnly(Side.CLIENT)
public class OnOffSearch implements OnOffAction {

  private SearchModule module;
  private int block_id;

  private OnOffButton tracer;
  private ColorButton color;

  public OnOffSearch(int block_id, SearchModule module, OnOffButton tracer, ColorButton color) {
    this.block_id = block_id;
    this.module = module;
    this.tracer = tracer;
    this.color = color;
  }

  public void toggle(boolean state) {
    this.tracer.enabled = state;
    this.color.enabled = state;
    module.setSearchState(this.block_id, state, this.tracer.getState(), this.color.getColor());
  }

}
