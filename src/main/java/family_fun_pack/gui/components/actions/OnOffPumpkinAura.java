package family_fun_pack.gui.components.actions;

import family_fun_pack.modules.PumpkinAuraModule;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class OnOffPumpkinAura implements OnOffAction {

    private final PumpkinAuraModule module;

    private final int id;

    public OnOffPumpkinAura(final PumpkinAuraModule module, final int id) {
        this.module = module;
        this.id = id;
    }

    @Override
    public void toggle(boolean state) {
        switch (id) {
            case -1: {
                module.antiTotem = state;
                break;
            }
            case 0: {
                module.autoSwitch = state;
                break;
            }
            case 1: {
                module.sequential = state;
                break;
            }
        }
        module.save();
    }
}
