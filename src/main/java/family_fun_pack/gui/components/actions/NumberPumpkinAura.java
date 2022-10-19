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
    switch (id) {
      case 2: {
        module.placeRange = number;
        break;
      }
      case 3: {
        module.minDamage = number;
        break;
      }
      case 4: {
        module.maxDamage = number;
        break;
      }
      case 6: {
        module.renderColor = number;
        break;
      }
    }
    module.save();
  }

  public void setIndex(int index) {
    switch (id) {
      case 2: {
        module.placeRange_index = index;
        break;
      }
      case 3: {
        module.minDamage_index = index;
        break;
      }
      case 4: {
        module.maxDamage_index = index;
        break;
      }
    }
    module.save();
  }
}
