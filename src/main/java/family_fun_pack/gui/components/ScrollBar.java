package family_fun_pack.gui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.StringTextComponent;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScrollBar extends Widget {

  static final int COLOR = 0xffbbbbbb;

  public int current_scroll;

  private int max_scroll;
  private int min_y;
  private int max_y;

  private double offset_y;

  public ScrollBar(int x, int y, int max_scroll, int max_y) {
    super(x, y, 6, 16, StringTextComponent.EMPTY);
    this.max_scroll = max_scroll;
    this.min_y = this.y;
    this.max_y = max_y - this.height;
    this.current_scroll = 0;
  }

  // Set max scroll, reset scroll bar at start
  public void resetMaxScroll(int max_scroll) {
    this.y = this.min_y;
    this.current_scroll = 0;
    this.max_scroll = max_scroll;
  }

  // Set max scroll, set current scroll at max scroll
  public void resetMaxScrollAndScroll(int max_scroll, boolean clicked) {
    if(clicked) {
      this.maxScrollUpdate(max_scroll);
    } else {
      this.y = this.max_y;
      this.max_scroll = max_scroll;
      this.current_scroll = max_scroll;
    }
  }

  // Set max scroll, keep current scroll position
  public void maxScrollUpdate(int max_scroll) {
    this.max_scroll = max_scroll;
    int old_y = this.y;
    if(max_scroll == 0) this.y = 0;
    else this.y = this.min_y + (int)(((float)this.current_scroll / (float)this.max_scroll) * (float)(this.max_y - this.min_y));
    this.offset_y -= (this.y - old_y);
  }

  public void scroll(int count) {
    this.current_scroll += count;
    if(this.current_scroll < 0) this.current_scroll = 0;
    else if(this.current_scroll > this.max_scroll) this.current_scroll = this.max_scroll;
    this.y = this.min_y + (int)(((float)this.current_scroll / (float)this.max_scroll) * (float)(this.max_y - this.min_y));
  }

  public void renderButton(MatrixStack mStack, int mouseX, int mouseY, float partialTicks) {
    AbstractGui.fill(mStack, this.x, this.y, this.x + this.width, this.y + this.height, ScrollBar.COLOR);
  }

  public void drag(double mouseX, double mouseY) {
    this.y = (int) (mouseY - this.offset_y);
    if(this.y < this.min_y) this.y = this.min_y;
    else if(this.y > this.max_y) this.y = this.max_y;

    this.current_scroll = (int)(((float)(this.y - this.min_y) / (float)(this.max_y - this.min_y)) * (float)this.max_scroll);
  }

  public void onClick(double mouseX, double mouseY) {
    this.offset_y = mouseY - (double) this.y;
  }

  protected void onDrag(double toX, double toY, double motionX, double motionY) {
    this.drag(toX, toY);
  }

  // No sound for you
  public void playDownSound(SoundHandler soundHandler) {}
}
