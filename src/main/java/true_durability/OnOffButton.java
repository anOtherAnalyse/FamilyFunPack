package true_durability;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class OnOffButton extends GuiButton {

  private static final ResourceLocation on_off = new ResourceLocation(TrueDurability.MODID, "textures/gui/on_off.png");

  public boolean state;

  public OnOffButton(int id, int x, int y) {
    super(id, x, y, 16, 7, null);
    this.state = false;
  }

  public void drawButton(Minecraft client, int mouseX, int mouseY, float partialTicks) {
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.disableLighting();
		GlStateManager.enableAlpha();
		GlStateManager.enableBlend();

    client.getTextureManager().bindTexture(OnOffButton.on_off);
    int i = 0;
    if(! this.state) i = this.height;

    Gui.drawModalRectWithCustomSizedTexture(this.x, this.y, 0, i, this.width, this.height, this.width, this.height * 2);
  }

  public void inverseState() {
    this.state = !this.state;
    this.onChange();
  }

  public void onChange() {}
}
