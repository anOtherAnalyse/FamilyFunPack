package family_fun_pack.gui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import family_fun_pack.FamilyFunPack;

@OnlyIn(Dist.CLIENT)
public class OnOffButton extends Button {

  private static final ResourceLocation ON_OFF = new ResourceLocation(FamilyFunPack.MODID, "textures/gui/on_off.png");

  private boolean state;
  private int metadata;

  public OnOffButton(int x, int y, Button.IPressable action) {
    super(x, y, 16, 7, StringTextComponent.EMPTY, action);
    this.state = false;
  }

  public void setMetadata(int data) {
    this.metadata = data;
  }

  public int getMetadata() {
    return this.metadata;
  }

  public void setState(boolean state) {
    this.state = state;
  }

  public boolean getState() {
    return this.state;
  }

  public void renderButton(MatrixStack mStack, int mouseX, int mouseY, float partialTicks) {
    Minecraft mc = Minecraft.getInstance();
    mc.getTextureManager().bind(OnOffButton.ON_OFF);
    RenderSystem.enableDepthTest();

    AbstractGui.blit(mStack, this.x, this.y, 0, this.state ? 0 : this.height, this.width, this.height, this.width, this.height * 2);

    if(! this.active) AbstractGui.fill(mStack, this.x, this.y, this.x + this.width, this.y + this.height, 0x99333333);
  }

  public void onPress() {
    this.state = !this.state;
    super.onPress();
  }
}
