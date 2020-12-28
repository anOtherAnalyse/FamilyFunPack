package family_fun_pack.gui.components.actions;

import net.minecraft.block.Block;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.modules.SearchModule;

@SideOnly(Side.CLIENT)
public class ColorSearch implements ColorAction {

  private SearchModule module;

  private Block block;

  public ColorSearch(SearchModule module, Block block) {
    this.module = module;
    this.block = block;
  }

  public void setColor(int color) {
    this.module.setSearchColor(this.block, color);
  }

}
