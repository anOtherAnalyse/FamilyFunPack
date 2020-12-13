package family_fun_pack.gui.interfaces;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

import org.lwjgl.input.Keyboard;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.components.GenericButton;
import family_fun_pack.nbt.SpecialTagCompound;

/* Shulker peek GUI */

@SideOnly(Side.CLIENT)
public class PreviewGui extends RightPanel {

  private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(FamilyFunPack.MODID, "textures/gui/preview.png");

  private static final int INNER_BORDER = PreviewGui.INNER_BORDER;

  private static int guiWidth = 176;
  private static int guiHeight = 80;

  private int x, y, x_end, y_end;

  private InventoryBasic inventory;
  private List<Slot> slots;

  private boolean hasParent;

  public PreviewGui(NBTTagList list) {
    this(list, true);
  }

  public PreviewGui(NBTTagList list, boolean hasParent) {
    this.hasParent = hasParent;

    this.inventory = new InventoryBasic(null, false, 27);
    this.slots = new LinkedList<Slot>();
    for(NBTBase i_base : list) {
      NBTTagCompound i = (NBTTagCompound) i_base;
      ItemStack stack = new ItemStack(i);
      if(!stack.isEmpty()) {
        int index = i.getByte("Slot") & 0xFF;
        if(i.getShort("Damage") < 0) {
          stack.setTagCompound(new SpecialTagCompound(stack.getTagCompound(), i.getShort("Damage")));
        }
        this.inventory.setInventorySlotContents(index, stack);
        this.slots.add(new Slot(this.inventory, index, 8 + (index % 9) * 18, 26 + (index / 9) * 18));
      }
    }

    if(! this.hasParent) {
      this.x = (this.width - PreviewGui.guiWidth) / 2;
      this.y = (this.height - PreviewGui.guiHeight) / 2;
    } else {
      this.x = (this.width - MainGui.guiWidth - 12 - PreviewGui.guiWidth) / 2 + MainGui.guiWidth + 12;
      this.y = (MainGui.guiHeight - PreviewGui.guiHeight) / 2 + 12;

      this.buttonList.add(new GenericButton(0, this.x + 3, this.y + 3, "Back") {
        public void onClick(GuiScreen parent) {
          ((PreviewGui) parent).close();
        }
      });
    }

    this.x_end = this.x + PreviewGui.guiWidth;
    this.y_end = this.y + PreviewGui.guiHeight;

    for(Slot s : this.slots) {
      s.xPos += this.x;
      s.yPos += this.y;
    }
  }

  public void drawScreen(int mouseX, int mouseY, float partialTicks) {

    if(this.hasParent) {
      // Gui background
      Gui.drawRect(this.x, this.y, this.x_end, this.y_end, MainGui.BACKGROUND_COLOR); // GUI background

      // borders
      Gui.drawRect(this.x, this.y, this.x_end, this.y + 1, PreviewGui.INNER_BORDER);
      Gui.drawRect(this.x, this.y, this.x + 1, this.y_end, PreviewGui.INNER_BORDER);
      Gui.drawRect(this.x_end - 1, this.y, this.x_end, this.y_end, PreviewGui.INNER_BORDER);
      Gui.drawRect(this.x, this.y_end - 1, this.x_end, this.y_end, PreviewGui.INNER_BORDER);
    }

    // items background
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    this.mc.getTextureManager().bindTexture(PreviewGui.GUI_TEXTURE);
    drawTexturedModalRect(this.x, this.y + 18, 0, 0, PreviewGui.guiWidth, PreviewGui.guiHeight);

    // Draw items
    RenderHelper.enableGUIStandardItemLighting();
    GlStateManager.enableDepth();
    for(Slot s : this.slots) {
      this.itemRender.renderItemAndEffectIntoGUI(this.mc.player, s.getStack(), s.xPos, s.yPos);
      this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, s.getStack(), s.xPos, s.yPos, null);
    }

    // Draw button
    super.drawScreen(mouseX, mouseY, partialTicks);

    // Draw tooltip
    if(mouseX >= this.x + 8 && mouseY >= this.y + 26) {
      int x = (mouseX - this.x - 8) / 18;
      int y = (mouseY - this.y - 26) / 18;
      if(x >= 0 && x < 9 && y >= 0 && y < 3) {
        ItemStack stack = this.inventory.getStackInSlot(y * 9 + x);
        if(! stack.isEmpty()) {
          this.renderToolTip(stack, mouseX, mouseY);
        }
      }
    }
  }

  public void keyTyped(char typedChar, int keyCode) throws IOException {
    if(keyCode == Keyboard.KEY_ESCAPE) {
      this.mc.displayGuiScreen(null);
      if (this.mc.currentScreen == null) {
        this.mc.setIngameFocus();
      }
    }
  }

  public void close() {
    this.transition((RightPanel) this.parent);
  }
}
