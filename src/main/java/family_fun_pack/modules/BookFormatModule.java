package family_fun_pack.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.Class;
import java.lang.IllegalAccessException;
import java.lang.NoSuchFieldException;
import java.lang.reflect.Field;

import family_fun_pack.gui.interfaces.BookEditingGui;

/* Special book editing gui allowing text formatting */

@SideOnly(Side.CLIENT)
public class BookFormatModule extends Module {

  private Field book_edited;
  private Field book_signed;

  public BookFormatModule() {
    super("Book format", "Special book editing gui");

    // Get access to book field of GuiScreenBook
    Class<GuiScreenBook> screenBook = GuiScreenBook.class;
    try {
      this.book_edited = screenBook.getDeclaredField("field_146474_h");
    } catch (NoSuchFieldException e) {
      try {
        this.book_edited = screenBook.getDeclaredField("book");
      } catch (NoSuchFieldException e2) {
        throw new RuntimeException("FamilyFunPack error: no such field " + e2.getMessage() + " in class GuiScreenBook");
      }
    }
    this.book_edited.setAccessible(true);

    // Get access to bookIsUnsigned field of GuiScreenBook
    try {
      this.book_signed = screenBook.getDeclaredField("field_146475_i");
    } catch (NoSuchFieldException e) {
      try {
        this.book_signed = screenBook.getDeclaredField("bookIsUnsigned");
      } catch (NoSuchFieldException e2) {
        throw new RuntimeException("FamilyFunPack error: no such field " + e2.getMessage() + " in class GuiScreenBook");
      }
    }
    this.book_signed.setAccessible(true);
  }

  protected void enable() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  protected void disable() {
    MinecraftForge.EVENT_BUS.unregister(this);
  }

  public boolean displayInGui() {
    return false;
  }

  @SubscribeEvent
  public void onGuiOpened(GuiOpenEvent event) {
    if(event.getGui() instanceof GuiScreenBook) {
      GuiScreenBook gui = (GuiScreenBook) event.getGui();

      if(gui instanceof BookEditingGui) return;

      try {
        if((boolean) this.book_signed.get(gui)) { // Book is editable
          ItemStack book = (ItemStack) this.book_edited.get(gui);
          event.setGui(new BookEditingGui(Minecraft.getMinecraft().player, book));
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException("FamilyFunPack error: " + e.getMessage());
      }
    }
  }

  public boolean defaultState() {
    return true;
  }
}
