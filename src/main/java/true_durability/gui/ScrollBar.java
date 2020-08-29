package true_durability.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class ScrollBar extends GuiButton {

  public int current_scroll;
  public boolean clicked;

  private int max_scroll;
  private int min_y;
  private int max_y;

  private int offset_y;

  public ScrollBar(int id, int x, int y, int max_scroll, int max_y) {
    super(id, x, y, 6, 16, null);
    this.max_scroll = max_scroll;
    this.min_y = this.y;
    this.max_y = max_y - this.height;
    this.current_scroll = 0;
    this.clicked = false;
  }

  public void resetMaxScroll(int max_scroll) {
    this.y = this.min_y;
    this.current_scroll = 0;
    this.max_scroll = max_scroll;
    this.clicked = false;
  }

  public void drawButton(Minecraft client, int mouseX, int mouseY, float partialTicks) {
    this.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xffbbbbbb);
  }

  public void dragged(int mouseX, int mouseY) {
    this.y = mouseY - this.offset_y;
    if(this.y < this.min_y) this.y = this.min_y;
    else if(this.y > this.max_y) this.y = this.max_y;

    this.current_scroll = (int)(((float)(this.y - this.min_y) / (float)(this.max_y - this.min_y)) * (float)this.max_scroll);
  }

  public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
    if(super.mousePressed(mc, mouseX, mouseY)) {
      this.clicked = true;
      this.offset_y = mouseY - this.y;
    }
    return false;
  }

  public void mouseReleased(int mouseX, int mouseY) {
    this.clicked = false;
  }
}
