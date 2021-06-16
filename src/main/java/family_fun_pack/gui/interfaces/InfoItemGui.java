package family_fun_pack.gui.interfaces;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Iterator;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.MainGuiComponent;
import family_fun_pack.gui.components.ItemButton;
import family_fun_pack.gui.components.GenericButton;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.components.TextPanel;
import family_fun_pack.utils.ReflectUtils;

/* Get information about items in our inventory (tags, preview shulker ...) */

@OnlyIn(Dist.CLIENT)
public class InfoItemGui extends RightPanel implements Button.IPressable {

  private static final int guiMaxWidth = 480;
  private static final int guiHeight = 143;

  private TextPanel info;
  private GenericButton openPreview;

  public InfoItemGui() {
    super(MainGui.guiWidth + 16, (MainGui.guiHeight - InfoItemGui.guiHeight) / 2 + 12, 0, InfoItemGui.guiHeight, new StringTextComponent("Inventory information"));

    if(this.x_end - this.x > InfoItemGui.guiMaxWidth) this.x_end = this.x + InfoItemGui.guiMaxWidth;
    else this.x_end = this.width - 12;

    this.initItems(this.minecraft.player.inventoryMenu);

    this.info = this.<TextPanel>addWidget(new TextPanel(this.x + 91, this.y + 18, this.x_end - this.x - 91, this.y_end - this.y - 18));
  }

  private void initItems(PlayerContainer inventory) {
    for(int i = 0; i < 8; i ++) {
      for(int j = 0; j < 5; j ++) {
        ItemStack stack = inventory.getSlot((i * 5) + j + 5).getItem();

        int x = this.x + 4 + j * 17;
        int y = this.y + 4 + i * 17;
        this.<ItemButton>addButton(new ItemButton(x, y, stack, this));
      }
    }
  }

  public void onReopen() {
    // Delete previous item buttons
    Iterator iterator = this.buttons.iterator();
    while(iterator.hasNext()) {
      Widget btn = (Widget) iterator.next();
      this.children.remove(btn);
      iterator.remove();
    }

    // Reset buttons
    this.initItems(this.minecraft.player.inventoryMenu);

    // Reset open preview button
    this.resetPreviewButton();

    // Reset text panel
    this.info.resetTag();
  }

  public void render(MatrixStack mStack, int mouseX, int mouseY, float partialTicks) {
    // Background
    AbstractGui.fill(mStack, this.x, this.y, this.x_end, this.y_end, MainGui.BACKGROUND_COLOR);
    AbstractGui.fill(mStack, this.x, this.y, this.x_end, this.y + 1, MainGui.BORDER_COLOR);
    AbstractGui.fill(mStack, this.x, this.y, this.x + 1, this.y_end, MainGui.BORDER_COLOR);
    AbstractGui.fill(mStack, this.x_end - 1, this.y, this.x_end, this.y_end, MainGui.BORDER_COLOR);
    AbstractGui.fill(mStack, this.x, this.y_end - 1, this.x_end, this.y_end, MainGui.BORDER_COLOR);

    // items grid
    int info_x = this.x + 4;
    int info_y = this.y + 4;
    int info_x_end = this.x + 88;
    int info_y_end = this.y + 139;

    AbstractGui.fill(mStack, info_x, info_y, info_x_end, info_y_end, 0xff000000);
    for(int i = 0; i < 6; i ++) AbstractGui.fill(mStack, info_x - 1 + i * 17, info_y, info_x + i * 17, info_y_end, 0xffffffff);
    for(int i = 0; i < 9; i ++) AbstractGui.fill(mStack, info_x, info_y - 1 + i * 17, info_x_end, info_y + i * 17, 0xffffffff);

    // Item info
    this.info.render(mStack, mouseX, mouseY, partialTicks);

    // Buttons
    super.render(mStack, mouseX, mouseY, partialTicks);
  }

  public void onPress(Button btn) {
    if(btn instanceof ItemButton) {
      ItemStack stack = ((ItemButton) btn).getStack();

      // Reset open preview button
      this.resetPreviewButton();

      if(! stack.isEmpty()) {

        // Set up info panel
        this.info.setTitle(stack.getDisplayName().getString());

        Item item = ReflectUtils.<Item>getFieldValue(stack, new String[] {"item"});
        String header = null;
        if(item != null) header = TextFormatting.DARK_PURPLE + item.toString() + TextFormatting.RESET;

        CompoundNBT tag = stack.getTag();
        if(tag != null) {
          String str = tag.toString().replaceAll("§[0-9A-FK-Oa-fk-orR]", "");
          this.info.setTag(str.substring(1, str.length() - 1), header);
        } else {
          this.info.setTag("No compound tag", header);
        }

        // Shulker preview
        if(tag != null && tag.contains("BlockEntityTag") && tag.getTagType("BlockEntityTag") == 10) {
          CompoundNBT blockTag = tag.getCompound("BlockEntityTag");

          if(blockTag.contains("Items") && blockTag.getTagType("Items") == 9) {
            int index = this.buttons.indexOf(btn);
            if(index >= 0) {
              this.openPreview = this.<GenericButton>addButton(new GenericButton(this.x_end - 28, this.y + 2, "Preview", this, 0.6f));
              this.openPreview.setId(index);
            }
          }
        }
      } else this.info.resetTag();
    } else if(btn instanceof GenericButton) {
      Widget widget = this.buttons.get(((GenericButton) btn).getId());
      if(widget != null && widget instanceof ItemButton) {
        ItemButton item = (ItemButton) widget;
        this.transition(new PreviewGui(item.getStack().getTagElement("BlockEntityTag").getList("Items", 10), this));
      }
    }
  }

  private void resetPreviewButton() {
    if(this.openPreview != null) {
      this.children.remove(this.openPreview);
      this.buttons.remove(this.openPreview);
      this.openPreview = null;
    }
  }

  // To be displayed in Main GUI, to access the GUI
  private static class GuiComponent implements MainGuiComponent, Button.IPressable {

    public String getLabel() {
      return "Info items";
    }

    public Widget getAction() {
      return new OpenGuiButton(0, 0, "view", this);
    }

    public MainGuiComponent getChild() {
      return null;
    }

    public void onPress(Button btn) {
      if(((OpenGuiButton) btn).isClicked()) {
        FamilyFunPack.getMainGui().setRightPanel(new InfoItemGui(), ((OpenGuiButton) btn).getId());
      } else {
        FamilyFunPack.getMainGui().removeRightPanel();
      }
    }
  }

  // Get Main Gui component to open GUI
  public static MainGuiComponent getMainGuiComponent() {
    return new GuiComponent();
  }
}
