package family_fun_pack.gui.interfaces;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.Class;
import java.lang.IllegalAccessException;
import java.lang.NoSuchFieldException;
import java.lang.NoSuchMethodException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.IOException;

import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.FormatButton;

/* Edit book gui with formats selection */

@SideOnly(Side.CLIENT)
public class BookEditingGui extends GuiScreenBook {

  private int x, y;

  private Field getting_signed;
  private Field book_title;
  private Method insert;

  public BookEditingGui(EntityPlayer player, ItemStack book) {
    super(player, book, true);

    // Get access to bookGettingSigned field
    Class<GuiScreenBook> screenBook = GuiScreenBook.class;
    try {
      this.getting_signed = screenBook.getDeclaredField("field_146480_s");
    } catch (NoSuchFieldException e) {
      try {
        this.getting_signed = screenBook.getDeclaredField("bookGettingSigned");
      } catch (NoSuchFieldException e2) {
        throw new RuntimeException("FamilyFunPack error: no such field " + e2.getMessage() + " in class GuiScreenBook");
      }
    }
    this.getting_signed.setAccessible(true);

    // Get access to bookTitle field
    try {
      this.book_title = screenBook.getDeclaredField("field_146482_z");
    } catch (NoSuchFieldException e) {
      try {
        this.book_title = screenBook.getDeclaredField("bookTitle");
      } catch (NoSuchFieldException e2) {
        throw new RuntimeException("FamilyFunPack error: no such field " + e2.getMessage() + " in class GuiScreenBook");
      }
    }
    this.book_title.setAccessible(true);

    // Get access to pageInsertIntoCurrent method
    try {
      this.insert = screenBook.getDeclaredMethod("func_146459_b", String.class);
    } catch(NoSuchMethodException e) {
      try {
        this.insert = screenBook.getDeclaredMethod("pageInsertIntoCurrent", String.class);
      } catch(NoSuchMethodException e2) {
        throw new RuntimeException("FamilyFunPack Error: no method pageInsertIntoCurrent in class GuiScreenBook");
      }
    }
    this.insert.setAccessible(true);
  }

  public void initGui() {
    super.initGui();

    this.x = ((this.width - 192) / 2) - 58;
    this.y = 9;

    // Title
    GuiLabel title = (new GuiLabel(this.fontRenderer, 0, this.x + 2, this.y + 4, 60, 16, 0xffffffff)).setCentered();
    title.addLine("Formatting");
    this.labelList.add(title);

    // Color formats
    for(int i = 0; i < 16; i ++) {
      this.buttonList.add(new FormatButton(i, this.x + 16 + (i / 8) * 24, this.y + 21 + (i % 8) * 8, this.fontRenderer, TextFormatting.fromColorIndex(i)));
    }

    // Special formats
    int i = this.y + 28 + 8 * 8;
    for(String format : TextFormatting.getValidValues(false, true)) {
      this.buttonList.add(new FormatButton(0, this.x + 4, i, this.fontRenderer, TextFormatting.getValueByName(format)));
      i += 12;
    }
  }

  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    // Draw background
    Gui.drawRect(this.x, this.y, this.x + 64, this.y + 168, MainGui.BACKGROUND_COLOR);
    Gui.drawRect(this.x, this.y, this.x + 64, this.y + 1, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x, this.y, this.x + 1, this.y + 168, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x, this.y + 167, this.x + 64, this.y + 168, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x + 63, this.y, this.x + 64, this.y + 168, MainGui.BORDER_COLOR);

    // Draw labels
    GlStateManager.pushMatrix();
    float scale = 0.7f;
    GlStateManager.scale(scale, scale, scale);

    int i = this.y + 29 + 8 * 8;
    for(String format : TextFormatting.getValidValues(false, true)) {
      TextFormatting f = TextFormatting.getValueByName(format);
      this.drawString(this.fontRenderer, f.toString() + format, (int)((float)(this.x + 14) / scale), (int)((float)i / scale), 0xffffffff);
      i += 12;
    }

    GlStateManager.popMatrix();

    // draw book gui + buttons
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  protected void keyTyped(char character, int code) throws IOException {
    try {
      boolean signing = (boolean) this.getting_signed.get(this);

      if(signing) { // Modify signing behavior to enable 32 chars title
        String title = (String) this.book_title.get(this);
        super.keyTyped(character, code);
        if(code != 14 && code != 28 && code != 156 && title.length() < 32) {
          this.book_title.set(this, title + Character.toString(character));
        }
      } else super.keyTyped(character, code);

    } catch (IllegalAccessException e) {
      throw new RuntimeException("FFP error: " + e.getMessage());
    }
  }

  public void appendFormat(String format) {
    try {
      boolean signing = (boolean) this.getting_signed.get(this);
      if(signing) { // title
        String title = (String) this.book_title.get(this);
        if(format.length() + title.length() <= 32) {
          this.book_title.set(this, title + format);
        }
      } else { // book
        this.insert.invoke(this, format);
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException("FFP error: " + e.getMessage());
    } catch (InvocationTargetException e) {
      throw new RuntimeException("FFP error: " + e.getMessage());
    }
  }

  protected void actionPerformed(GuiButton button) throws IOException {
    if(button instanceof ActionButton) {
      ((ActionButton) button).onClick(this);
    } else super.actionPerformed(button);
  }

}
