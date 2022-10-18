package family_fun_pack.gui.components.actions;

import family_fun_pack.modules.PumpkinAuraModule;
import family_fun_pack.modules.SearchModule;
import net.minecraft.block.Block;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class NumberPumpkinAura implements NumberAction {

  private final PumpkinAuraModule module;

  private final int id;

  public NumberPumpkinAura(PumpkinAuraModule module, int id) {
    this.module = module;
    this.id = id;
  }

  public void setNumber(int number) {
//    System.out.println(number);
//    this.module.setSearchColor(this.block, color);
  }

}
