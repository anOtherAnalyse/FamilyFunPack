package family_fun_pack.modules;

import family_fun_pack.gui.MainGuiComponent;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.OnOffButton;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.components.SliderButton;
import family_fun_pack.gui.components.actions.NumberPumpkinAura;
import family_fun_pack.gui.components.actions.OnOffPumpkinAura;
import family_fun_pack.gui.interfaces.PumpkinAuraSettingsGui;
import net.minecraftforge.common.config.Configuration;

import java.util.LinkedHashMap;

public class PumpkinAuraModule extends Module {

    private boolean break_;
    private int breakDelay;
    private boolean place;
    private int placeDelay;

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
        configuration.get(this.getLabel(), "break", false).set(break_);
        configuration.get(this.getLabel(), "breakDelay", 5).set(breakDelay);
        configuration.get(this.getLabel(), "place", false).set(place);
        configuration.get(this.getLabel(), "breakDelay", 5).set(placeDelay);
        super.save(configuration);
    }

    @Override
    public void load(Configuration configuration) {
        break_ = configuration.get(this.getLabel(), "break", false).getBoolean();
        breakDelay = configuration.get(this.getLabel(), "breakDelay", 5).getInt();
        place = configuration.get(this.getLabel(), "place", false).getBoolean();
        placeDelay = configuration.get(this.getLabel(), "breakDelay", 5).getInt();
        super.load(configuration);
    }

    public LinkedHashMap<String, ActionButton> getSettings() {
        LinkedHashMap<String, ActionButton> buttonMap = new LinkedHashMap<>();
        buttonMap.put("Place", new OnOffButton(1, 0, 0, new OnOffPumpkinAura(this, 1)));
        buttonMap.put("PlaceDelay", new SliderButton(2, 0, 0, new NumberPumpkinAura(this, 2)));
        buttonMap.put("Break", new OnOffButton(3, 0, 0, new OnOffPumpkinAura(this, 3)));
        buttonMap.put("BreakDelay", new SliderButton(4, 0, 0, new NumberPumpkinAura(this, 4)));
        return buttonMap;
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
