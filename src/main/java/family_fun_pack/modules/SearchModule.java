package family_fun_pack.modules;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class SearchModule extends Module {

  public SearchModule() {
    super("Search", "Search for blocks");
  }

  protected void enable() {

  }

  protected void disable() {

  }

  public void setSearchState(int block_id, boolean state) {

  }

  public void setTracerState(int block_id, boolean state) {

  }

  public void setSearchSColor(int block_id, int color) {

  }

}
