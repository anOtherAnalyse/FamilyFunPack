package family_fun_pack.gui;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;

import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.Tooltip;

@SideOnly(Side.CLIENT)
public class PreviewGui extends GuiScreen {

  private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(FamilyFunPack.MODID, "textures/gui/preview.png");
  private static int WIDTH = 176;
  private static int HEIGHT = 80;

  private int x, y, x_end, y_end;

  private InventoryBasic inventory;
  private GuiScreen previous;
  private Tooltip tooltip;
  private List<Slot> slots;

  public PreviewGui(NBTTagList list, GuiScreen previous, Tooltip tooltip) {
    this.inventory = new InventoryBasic(null, false, 27);
    this.slots = new LinkedList<Slot>();
    for(NBTBase i_base : list) {
      NBTTagCompound i = (NBTTagCompound) i_base;
      ItemStack stack = new ItemStack(i);
      if(!stack.isEmpty()) {
        int index = i.getByte("Slot") & 0xFF;
        this.inventory.setInventorySlotContents(index, stack);
        this.slots.add(new Slot(this.inventory, index, 8 + (index % 9) * 18, 26 + (index / 9) * 18));
      }
    }
    this.tooltip = tooltip;
    this.previous = previous;
  }

  public void initGui() {
    this.x = (this.width / 2) - (PreviewGui.WIDTH / 2);
    this.y = (this.height / 2) - (PreviewGui.HEIGHT / 2);
    this.x_end = this.x + PreviewGui.WIDTH;
    this.y_end = this.y + PreviewGui.HEIGHT;
    for(Slot s : this.slots) {
      s.xPos += this.x;
      s.yPos += this.y;
    }
    this.addButton(new OpenButton(0, this.x + 3, this.y + 3, this.fontRenderer, "Back") {
      public void performAction() {
        PreviewGui.this.mc.displayGuiScreen(PreviewGui.this.previous);
      }
    });
  }

  protected void actionPerformed(GuiButton button) throws IOException {
    ((OpenButton) button).performAction();
  }

  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    // Gui background
    Gui.drawRect(this.x, this.y, this.x_end, this.y_end, CommandGui.BACKGROUND_COLOR); // GUI background

    // borders
    Gui.drawRect(this.x, this.y, this.x_end, this.y + 1, 0xffbbbbbb);
    Gui.drawRect(this.x, this.y, this.x + 1, this.y_end, 0xffbbbbbb);
    Gui.drawRect(this.x_end - 1, this.y, this.x_end, this.y_end, 0xffbbbbbb);
    Gui.drawRect(this.x, this.y_end - 1, this.x_end, this.y_end, 0xffbbbbbb);

    // items background
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    this.mc.getTextureManager().bindTexture(PreviewGui.GUI_TEXTURE);
    drawTexturedModalRect(this.x, this.y + 18, 0, 0, PreviewGui.WIDTH, PreviewGui.HEIGHT);

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

  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if(keyCode == Keyboard.KEY_ESCAPE) {
      this.mc.displayGuiScreen(this.previous);
    } else if(keyCode == this.tooltip.openGUIKey.getKeyCode()) {
      this.mc.displayGuiScreen(null);
      if (this.mc.currentScreen == null) {
        this.mc.setIngameFocus();
      }
    }
  }

  public boolean doesGuiPauseGame() {
	   return false;
	}

}
