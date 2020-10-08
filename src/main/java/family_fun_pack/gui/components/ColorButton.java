package family_fun_pack.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.modules.SearchModule;

@SideOnly(Side.CLIENT)
public class ColorButton extends ActionButton {

  private static final int[] COLORS = {0xffff0000, 0xffff9900, 0xffffff00, 0xffff6666, 0xff663300, 0xffff00ff, 0xff00ff99, 0xff00ff00, 0xff99ff99, 0xff0099cc, 0xff0066ff, 0xff6600ff, 0xff0000ff};

  private static final int BORDER = 0xff000000;

  private int index;
  private int block_id;
  private SearchModule module;

  public ColorButton(int id, int x, int y, int block_id, SearchModule module) {
    super(id, x, y, 16, 7, null);
    this.index = 0;
    this.block_id = block_id;
    this.module = module;
  }

  public ColorButton(int x, int y, int block_id, SearchModule module) {
    this(0, x, y, block_id, module);
  }

  public void reset() {
    this.index = 0;
  }

  public void setColor(int color) {
    for(int i = 0; i < ColorButton.COLORS.length; i ++) {
      if(ColorButton.COLORS[i] == color) {
        this.index = i;
        return;
      }
    }
  }

  public int getColor() {
    return ColorButton.COLORS[this.index];
  }

  public void drawButton(Minecraft client, int mouseX, int mouseY, float partialTicks) {
    int x_end = this.x + this.width;
    int y_end = this.y + this.height;

    this.drawRect(this.x, this.y, x_end, y_end, this.getColor());
    this.drawRect(this.x, this.y, x_end, this.y + 1, ColorButton.BORDER);
    this.drawRect(this.x, this.y, this.x + 1, y_end, ColorButton.BORDER);
    this.drawRect(this.x, y_end - 1, x_end, y_end, ColorButton.BORDER);
    this.drawRect(x_end - 1, this.y, x_end, y_end, ColorButton.BORDER);

    if(! this.enabled) this.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0x99333333);
  }

  public void onClick(Gui parent) {
    if(! this.enabled) return;
    this.index = (this.index + 1) % ColorButton.COLORS.length;
    this.module.setSearchSColor(this.block_id, this.getColor());
  }
}
