package family_fun_pack.gui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.gui.GuiUtils;

@OnlyIn(Dist.CLIENT)
public class ItemButton extends Button {

  private static final int HOVER_COLOR = 0xff000040;

  private final ItemStack stack;

  public ItemButton(int x, int y, ItemStack stack, Button.IPressable onPress) {
    super(x, y, 16, 16, StringTextComponent.EMPTY, onPress);
    this.stack = stack;
  }

  public void renderButton(MatrixStack mStack, int mouseX, int mouseY, float partialTicks) {
    if(! this.stack.isEmpty()) {
      Minecraft mc = Minecraft.getInstance();
      ItemRenderer itemRenderer = mc.getItemRenderer();

      if(this.isHovered()) {
        AbstractGui.fill(mStack, this.x, this.y, this.x + this.width, this.y + this.height, ItemButton.HOVER_COLOR);
      }

      RenderSystem.enableDepthTest();
      itemRenderer.renderAndDecorateItem(mc.player, this.stack, this.x, this.y);
      itemRenderer.renderGuiItemDecorations(mc.font, this.stack, this.x, this.y, null);

      if(this.isHovered()) {
        GuiUtils.preItemToolTip(this.stack);
        GuiUtils.drawHoveringText(mStack, this.stack.getTooltipLines(mc.player, mc.options.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL), mouseX, mouseY, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), -1, mc.font);
        GuiUtils.postItemToolTip();
      }
    }
  }

  public ItemStack getStack() {
    return this.stack;
  }

  public void onPress() {
    if(this.onPress != null) this.onPress.onPress(this);
  }
}
