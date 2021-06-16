package family_fun_pack.gui.interfaces;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.LinkedList;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.components.GenericButton;

/* Shulker peek GUI */

@OnlyIn(Dist.CLIENT)
public class PreviewGui extends RightPanel implements Button.IPressable {

  private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(FamilyFunPack.MODID, "textures/gui/preview.png");

  private static final int guiWidth = 176;
  private static final int guiHeight = 80;

  private final Inventory inventory;
  private final List<Slot> slots;

  private final RightPanel parent;

  public PreviewGui(ListNBT list, RightPanel parent) {
    super(0, (MainGui.guiHeight - PreviewGui.guiHeight) / 2 + 12, 0, PreviewGui.guiHeight, new StringTextComponent("Shulker preview"));
    this.x = (this.width - MainGui.guiWidth - 12 - PreviewGui.guiWidth) / 2 + MainGui.guiWidth + 12;
    this.x_end = this.x + PreviewGui.guiWidth;

    this.inventory = new Inventory(27);
    this.slots = new LinkedList<Slot>();

    this.parent = parent;

    for(INBT tag : list) {
      ItemStack stack = ItemStack.of((CompoundNBT) tag);

      if(! stack.isEmpty()) {
        int index = ((CompoundNBT) tag).getByte("Slot") & 0xFF;
        this.inventory.setItem(index, stack);
        this.slots.add(new Slot(this.inventory, index, this.x + 8 + (index % 9) * 18, this.y + 26 + (index / 9) * 18));
      }
    }

    this.<GenericButton>addButton(new GenericButton(this.x + 3, this.y + 3, "Back", this));
  }

  public void render(MatrixStack mStack, int mouseX, int mouseY, float partialTicks) {
    // Background
    AbstractGui.fill(mStack, this.x, this.y, this.x_end, this.y_end, MainGui.BACKGROUND_COLOR);
    AbstractGui.fill(mStack, this.x, this.y, this.x_end, this.y + 1, MainGui.BORDER_COLOR);
    AbstractGui.fill(mStack, this.x, this.y, this.x + 1, this.y_end, MainGui.BORDER_COLOR);
    AbstractGui.fill(mStack, this.x_end - 1, this.y, this.x_end, this.y_end, MainGui.BORDER_COLOR);
    AbstractGui.fill(mStack, this.x, this.y_end - 1, this.x_end, this.y_end, MainGui.BORDER_COLOR);

    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    this.minecraft.getTextureManager().bind(PreviewGui.GUI_TEXTURE);
    this.blit(mStack, this.x, this.y + 18, 0, 0, PreviewGui.guiWidth, PreviewGui.guiHeight);

    // Items
    RenderSystem.enableDepthTest();
    for(Slot s : this.slots) {
      this.itemRenderer.renderAndDecorateItem(this.minecraft.player, s.getItem(), s.x, s.y);
      this.itemRenderer.renderGuiItemDecorations(this.minecraft.font, s.getItem(), s.x, s.y, null);
    }

    // Button
    super.render(mStack, mouseX, mouseY, partialTicks);

    // Draw tooltip
    if(mouseX >= this.x + 8 && mouseY >= this.y + 26) {
      int x = (mouseX - this.x - 8) / 18;
      int y = (mouseY - this.y - 26) / 18;
      if(x >= 0 && x < 9 && y >= 0 && y < 3) {
        ItemStack stack = this.inventory.getItem(y * 9 + x);
        if(! stack.isEmpty()) {
          this.renderTooltip(mStack, stack, mouseX, mouseY);
        }
      }
    }
  }

  public void onPress(Button btn) {
    if(this.parent != null) this.transition((RightPanel) this.parent);
  }
}
