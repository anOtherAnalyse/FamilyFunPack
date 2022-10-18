package family_fun_pack.gui.interfaces;

import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.ScrollBar;
import family_fun_pack.modules.Module;
import family_fun_pack.modules.PumpkinAuraModule;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PumpkinAuraSettingsGui extends RightPanel {

    private static final int guiWidth = 148;
    private static final int guiHeight = 200;

    private static final int maxLabelsDisplayed = 16;

    private final int x, y, x_end, y_end;
    private final List<String> labels;
    private final List<ActionButton> buttons;

    private ScrollBar scroll;

    public PumpkinAuraSettingsGui() {

        this.x = MainGui.guiWidth + 16;
        this.y = 12;
        this.x_end = PumpkinAuraSettingsGui.guiWidth + this.x;
        this.y_end = PumpkinAuraSettingsGui.guiHeight + this.y;

        this.labels = new ArrayList<>();
        this.buttons = new ArrayList<>();
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(this.x, this.y, this.x_end, this.y_end, MainGui.BACKGROUND_COLOR); // GUI background

        // borders
        Gui.drawRect(this.x, this.y, this.x_end, this.y + 2, 0xffbbbbbb);
        Gui.drawRect(this.x, this.y, this.x + 2, this.y_end, 0xffbbbbbb);
        Gui.drawRect(this.x_end - 2, this.y, this.x_end, this.y_end, 0xffbbbbbb);
        Gui.drawRect(this.x, this.y_end - 2, this.x_end, this.y_end, 0xffbbbbbb);

        // Update scroll
        if(this.scroll.clicked) {
            this.scroll.dragged(mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        // Draw labels
        GlStateManager.pushMatrix();
        float scale = 0.7f;
        GlStateManager.scale(scale, scale, scale);
        for (int i = this.scroll.current_scroll; (i - this.scroll.current_scroll) < PumpkinAuraSettingsGui.maxLabelsDisplayed & i < this.buttons.size(); i ++) {
            int decal_y = (int)((float)(this.y + 20 + (i - this.scroll.current_scroll) * 11) / scale);
            int decal_x = (int)((float)(this.x + 4) / scale);
            this.drawString(this.fontRenderer, this.labels.get(i), decal_x, decal_y, 0xffbbbbbb);
            int border_decal_y = decal_y + (int)(8f / scale);
            Gui.drawRect(decal_x, border_decal_y, (int)(((float)this.x_end - 10f) / scale), border_decal_y + 1, 0xff111133); // Border at end of line
        }
        GlStateManager.popMatrix();

        // Draw enable buttons
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        for(int i = this.scroll.current_scroll; (i - this.scroll.current_scroll) < PumpkinAuraSettingsGui.maxLabelsDisplayed & i < this.buttons.size(); i ++) {
            this.buttons.get(i).x = this.x_end - 44;
            this.buttons.get(i).y = this.y + 20 + (i - this.scroll.current_scroll) * 11;
            this.buttons.get(i).drawButton(this.mc, mouseX, mouseY, partialTicks);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if(mouseButton == 0) {
            for (int i = this.scroll.current_scroll; (i - this.scroll.current_scroll) < PumpkinAuraSettingsGui.maxLabelsDisplayed && i < this.buttons.size(); i ++) {
                this.buttons.get(i).onClick(this);
                this.buttons.get(i).mousePressed(this.mc, mouseX, mouseY);
                this.buttons.get(i).playPressSound(this.mc.getSoundHandler());
            }

            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if(state == 0) {
            for (int i = this.scroll.current_scroll; (i - this.scroll.current_scroll) < PumpkinAuraSettingsGui.maxLabelsDisplayed && i < this.buttons.size(); i ++) {
                this.buttons.get(i).mouseReleased(mouseX, mouseY);
            }

            this.scroll.mouseReleased(mouseX, mouseY);
        }
    }

    public void mouseWheel(int wheel) {
        this.scroll.scroll(wheel);
    }

    public void dependsOn(Module dependence) {
        super.dependsOn(dependence);
        if (dependence instanceof PumpkinAuraModule) {
            PumpkinAuraModule module = (PumpkinAuraModule) dependence;

            this.labels.clear();
            this.buttons.clear();

            for (Map.Entry<String, ActionButton> entry : module.getSettings().entrySet()) {
                this.labels.add(entry.getKey());
                this.buttons.add(entry.getValue());
            }

            int max_scroll = this.buttons.size() - PumpkinAuraSettingsGui.maxLabelsDisplayed;
            if(this.scroll != null) {
                this.scroll.resetMaxScroll(Math.max(max_scroll, 0));
            } else {
                this.scroll = new ScrollBar(0, this.x_end - 10, this.y + 4, Math.max(max_scroll, 0), this.y_end - 4);
                this.buttonList.add(this.scroll);
            }
        }
    }
}
