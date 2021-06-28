package family_fun_pack.gui.interfaces;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.MainGuiComponent;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.GenericButton;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.components.ScrollBar;
import family_fun_pack.nbt.SpecialTagCompound;

/* Get information about items in our inventory (tags, preview shulker ...) */

@SideOnly(Side.CLIENT)
public class InfoItemGui extends RightPanel {

  private static int guiMaxWidth = 480;
  private static int guiHeight = 136;

  private int maxLines;

  private int x, y, x_end, y_end;

  public int current_slot;

  private String title;

  private List<String> tag;

  private ScrollBar scroll;
  private GenericButton previewOpen;

  public Container inventory;

  public InfoItemGui() {
    super();
    this.inventory = this.mc.player.inventoryContainer;
    this.current_slot = -1;

    this.x = MainGui.guiWidth + 16;
    this.y = (MainGui.guiHeight - InfoItemGui.guiHeight) / 2 + 12;
    this.x_end = this.width - 12;
    if(this.x_end - this.x > InfoItemGui.guiMaxWidth) this.x_end = this.x + InfoItemGui.guiMaxWidth;
    this.y_end = this.y + InfoItemGui.guiHeight;
    this.scroll = new ScrollBar(0, this.x_end - 8, this.y + 20, 0, this.y_end - 2);
    this.previewOpen = null;
    this.maxLines = (int) ((float) (InfoItemGui.guiHeight - 23) / ((float) this.fontRenderer.FONT_HEIGHT * 0.7f));
  }

  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    Gui.drawRect(this.x, this.y, this.x_end, this.y_end, MainGui.BACKGROUND_COLOR); // GUI background

    // borders
    Gui.drawRect(this.x, this.y, this.x_end, this.y + 1, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x, this.y, this.x + 1, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end - 1, this.y, this.x_end, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x, this.y_end - 1, this.x_end, this.y_end, MainGui.BORDER_COLOR);

    // items border
    int info_x = this.x + 4;
    int info_y = this.y + 4;
    int info_x_end = this.x + 84;
    int info_y_end = this.y + 132;
    Gui.drawRect(info_x, info_y, info_x_end, info_y_end, 0xff000000);
    for(int i = 0; i < 6; i ++) {
      Gui.drawRect(info_x - 1 + i*16, info_y, info_x + i*16, info_y_end, 0xffffffff);
    }
    for(int i = 0; i < 9; i ++) {
      Gui.drawRect(info_x, info_y - 1 + i*16, info_x_end, info_y + i*16, 0xffffffff);
    }

    // draw tag border
    info_x = this.x + 86;
    info_y = this.y + 18;
    info_x_end = this.x_end - 1;
    info_y_end = this.y_end - 1;
    Gui.drawRect(info_x, info_y, info_x_end, info_y + 1, 0xffffffff);
    Gui.drawRect(info_x, info_y, info_x + 1, info_y_end, 0xffffffff);

    // Draw preview open button
    if(this.previewOpen != null) this.previewOpen.drawButton(this.mc, mouseX, mouseY, partialTicks);

    // Draw tag
    if(this.current_slot != -1) {

      // Draw title
      int width = this.fontRenderer.getStringWidth(this.title);
      info_x += ((this.x_end - 2 - info_x) / 2 - (width) / 2);
      this.fontRenderer.drawStringWithShadow(this.title, info_x, this.y + 4, 0xffffffff);

      GlStateManager.pushMatrix();
      GlStateManager.scale(0.7f, 0.7f, 0.7f);
      info_x = (int)(((float)this.x + 89f) / 0.7f);

      int i = 0;
      for(String line : this.tag) {
        if(i >= this.scroll.current_scroll) {
          int index = i - this.scroll.current_scroll;
          if(index >= this.maxLines) break;

          info_y = (int) ((((float)this.y + 21f) / 0.7f) + index * this.fontRenderer.FONT_HEIGHT);
          this.drawString(this.fontRenderer, this.tag.get(i), info_x, info_y, 0xffffffff);
        }
        i ++;
      }

      GlStateManager.popMatrix();
    }

    // Update scroll
    if(this.scroll.clicked) {
      this.scroll.dragged(mouseX, mouseY);
    }

    // Draw scroll bar
    this.scroll.drawButton(this.mc, mouseX, mouseY, partialTicks);

    // Draw items
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    RenderHelper.enableGUIStandardItemLighting();
    for(int i = 0; i < 8; i ++) {
      for(int j = 0; j < 5; j ++) {
        Slot slot = this.inventory.getSlot((i * 5) + j + 5);
        ItemStack stack = slot.getStack();
        int x = this.x + 4 + j * 16;
        int y = this.y + 4 + i * 16;
        if(! stack.isEmpty()) {
          GlStateManager.enableDepth();
          this.itemRender.renderItemAndEffectIntoGUI(this.mc.player, stack, x, y);
          this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, stack, x, y, null);
        }
      }
    }

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    if(mouseButton == 0) {

      if(this.scroll.mousePressed(this.mc, mouseX, mouseY)) return;
      else if(this.previewOpen != null && this.previewOpen.mousePressed(this.mc, mouseX, mouseY)) {
        this.previewOpen.onClick((GuiScreen) this);
        this.previewOpen.playPressSound(this.mc.getSoundHandler());
        return;
      }

      int x = (mouseX - 4 - this.x) / 16;
      int y = (mouseY - 4 - this.y) / 16;
      if(x >= 0 && x < 5 && y >= 0 && y < 8) { // Item slot clicked
        this.current_slot = (y * 5) + x + 5;
        ItemStack stack = this.inventory.getSlot(this.current_slot).getStack();
        if(!stack.isEmpty()) {
          this.title = stack.getDisplayName();

          NBTTagCompound tag = stack.getTagCompound();
          String tag_string;
          if(tag != null) {
            tag_string = tag.toString().replaceAll("§[0-9A-FK-Oa-fk-orR]", "");
            tag_string = tag_string.substring(1, tag_string.length() - 1);
          } else tag_string = "Stack has no tag";

          this.formatTag(String.format("Damage field: %s%d%s, Tag:", TextFormatting.BLUE, SpecialTagCompound.getStackDamage(stack), TextFormatting.RESET), tag_string);

          // If shulker tag, had option to preview shulker content
          if(tag != null && tag.hasKey("BlockEntityTag") && tag.getTagId("BlockEntityTag") == 10) {
            NBTTagCompound blockTag = tag.getCompoundTag("BlockEntityTag");
            if(blockTag.hasKey("Items") && blockTag.getTagId("Items") == 9) {
              this.previewOpen = new GenericButton(0, this.x_end - 46, this.y + 3, "Preview") {
                public void onClick(GuiScreen parent) {
                  PreviewGui preview = new PreviewGui(
                    InfoItemGui.this.inventory.getSlot(InfoItemGui.this.current_slot).getStack().getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Items", 10)
                  );
                  preview.setParent(parent);
                  InfoItemGui.this.transition(preview);
                }
              };
            } else this.previewOpen = null;
          } else this.previewOpen = null;

        } else {
          this.current_slot = -1;
          this.tag = null;
          this.title = null;
          this.previewOpen = null;
        }
        this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
    }
  }

  private void formatTag(String header, String tag) {
    this.tag = new LinkedList<String>();
    if(header != null) {
      this.tag.add(header);
      this.tag.add("");
    }

    // Divide tag into lines
    int line_width = (int) ((float) (this.x_end - this.x - 98) / 0.7f);

    StringBuilder next_line = new StringBuilder();

    int width = 0, shift = 0;
    boolean in_string = false;
    char[] arr = tag.toCharArray();
    for(int i = 0; i < arr.length; i ++) {
      int cw = this.fontRenderer.getStringWidth(Character.toString(arr[i]));

      if(width + cw >= line_width) {
        width = this.newLine(next_line, shift);
      }

      if(arr[i] == '}' && shift > 0 && !in_string) {
        shift --;
        width = this.newLine(next_line, shift);
      }

      next_line.append(arr[i]);
      width += cw;

      if(in_string) {
        if(arr[i] == '"' && (i <= 1 || arr[i-1] != '\\' || arr[i-2] != '\\')) in_string = false;
      } else {
        switch(arr[i]) {
          case '{':
            if(shift < 12) shift ++;
            width = this.newLine(next_line, shift);
            break;
          case '}':
            if(i + 1 < arr.length && arr[i + 1] != ',' && arr[i + 1] != '}' && arr[i + 1] != ']' && arr[i + 1] != '\'') width = this.newLine(next_line, shift);
            break;
          case ',':
            width = this.newLine(next_line, shift);
            break;
          case '"':
            in_string = true;
            break;
        }
      }
    }
    if(next_line.length() > 0) this.tag.add(next_line.toString());


    if(this.tag.size() > this.maxLines) this.scroll.resetMaxScroll(this.tag.size() - this.maxLines);
    else this.scroll.resetMaxScroll(0);
  }

  private int newLine(StringBuilder next, int shift) {
    this.tag.add(next.toString());
    next.delete(0, next.length());
    if(shift <= 0) return 0;
    for(int i = 0; i < shift; i ++) next.append("  ");
    return this.fontRenderer.getStringWidth(next.toString());
  }

  public void mouseReleased(int mouseX, int mouseY, int state) {
    if(state == 0) {
      this.scroll.mouseReleased(mouseX, mouseY);
    }
  }

  public void mouseWheel(int wheel) {
    this.scroll.scroll(wheel);
  }

  // To be displayed in Main GUI, to access the GUI
  private static class GuiComponent implements MainGuiComponent {

    public String getLabel() {
      return "Info items";
    }

    public ActionButton getAction() {
      return new OpenGuiButton(0, 0, "view", InfoItemGui.class, null);
    }

    public MainGuiComponent getChild() {
      return null;
    }
  }

  // Get Main Gui component to open GUI
  public static MainGuiComponent getMainGuiComponent() {
    return new GuiComponent();
  }

}
