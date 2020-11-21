package family_fun_pack.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.gui.interfaces.AdvancedSearchGui;

@SideOnly(Side.CLIENT)
public class ArrowButton extends ActionButton {

  private boolean left;

  public ArrowButton(int x, int y, boolean left) {
    super(0, x, y, 8, 8, null);
    this.left = left;
  }

  public void drawButton(Minecraft client, int mouseX, int mouseY, float partialTicks) {
    if(this.enabled) {
      GlStateManager.enableAlpha();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

      GlStateManager.pushMatrix();
      GlStateManager.scale(0.5f, 0.5f, 0.5f);

      int x = (int)((float)this.x / 0.5f);
      int y = (int)((float)this.y / 0.5f);

      client.getTextureManager().bindTexture(SelectButton.NAVIGATION);
      Gui.drawModalRectWithCustomSizedTexture(x, y, (this.left ? 0 : 16), 0, 16, 16, 16 * 2, 16);

      GlStateManager.popMatrix();
    }
  }

  public void onClick(GuiScreen parent) {
    ((AdvancedSearchGui)parent).nextPreset((this.left ? -1 : 1));
  }

}
