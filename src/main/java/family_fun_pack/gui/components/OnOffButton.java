package family_fun_pack.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.components.actions.OnOffAction;

@SideOnly(Side.CLIENT)
public class OnOffButton extends ActionButton {

  private static final ResourceLocation ON_OFF = new ResourceLocation(FamilyFunPack.MODID, "textures/gui/on_off.png");

  private boolean state;
  private OnOffAction action;

  public OnOffButton(int id, int x, int y, OnOffAction action) {
    super(id, x, y, 16, 7, null);
    this.state = false;
    this.action = action;
  }

  public OnOffButton(int x, int y, OnOffAction action) {
    this(0, x, y, action);
  }

  public void setState(boolean state) {
    this.state = state;
  }

  public boolean getState() {
    return this.state;
  }

  public void drawButton(Minecraft client, int mouseX, int mouseY, float partialTicks) {
		GlStateManager.enableAlpha();
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

    client.getTextureManager().bindTexture(OnOffButton.ON_OFF);
    int i = 0;
    if(! this.state) i = this.height;

    Gui.drawModalRectWithCustomSizedTexture(this.x, this.y, 0, i, this.width, this.height, this.width, this.height * 2);

    if(! this.enabled) this.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0x99333333);
  }

  public void onClick(GuiScreen parent) {
    if(! this.enabled) return;
    this.state = !this.state;
    if(this.action != null) this.action.toggle(this.state);
  }
}
