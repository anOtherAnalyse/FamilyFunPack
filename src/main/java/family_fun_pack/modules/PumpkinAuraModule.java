package family_fun_pack.modules;

import family_fun_pack.gui.MainGuiComponent;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.interfaces.PumpkinAuraSettingsGui;
import net.minecraftforge.common.config.Configuration;

public class PumpkinAuraModule extends Module {

    public PumpkinAuraModule() {
        super("PumpkinAura", "Pumpkin PvP module for auscpvp.org/2b2t.org.au");
    }

    @Override
    protected void enable() {

    }

    @Override
    protected void disable() {

    }

    @Override
    public void save(Configuration configuration) {
        super.save(configuration);
    }

    @Override
    public void load(Configuration configuration) {
        super.load(configuration);
    }

    private static class SettingsGui implements MainGuiComponent {

        private final PumpkinAuraModule dependence;

        public SettingsGui(PumpkinAuraModule dependence) {
            this.dependence = dependence;
        }

        public String getLabel() {
            return "> settings";
        }

        public ActionButton getAction() {
            return new OpenGuiButton(0, 0, "config", PumpkinAuraSettingsGui.class, this.dependence);
        }

        public MainGuiComponent getChild() {
            return null;
        }
    }

    @Override
    public MainGuiComponent getChild() {
        return new SettingsGui(this);
    }
}
