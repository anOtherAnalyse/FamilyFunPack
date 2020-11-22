package family_fun_pack.gui.components.actions;

import net.minecraft.block.Block;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.gui.components.ColorButton;
import family_fun_pack.gui.components.OnOffButton;
import family_fun_pack.modules.SearchModule;

@SideOnly(Side.CLIENT)
public class OnOffSearch implements OnOffAction {

  private SearchModule module;
  private Block block;

  private OnOffButton tracer;
  private ColorButton color;

  public OnOffSearch(Block block, SearchModule module, OnOffButton tracer, ColorButton color) {
    this.block = block;
    this.module = module;
    this.tracer = tracer;
    this.color = color;
  }

  public void toggle(boolean state) {
    this.tracer.enabled = state;
    this.color.enabled = state;
    module.setSearchState(this.block, state);
  }

}
