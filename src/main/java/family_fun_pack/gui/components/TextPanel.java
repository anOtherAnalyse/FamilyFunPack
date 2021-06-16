package family_fun_pack.gui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.LinkedList;

import family_fun_pack.gui.components.ScrollBar;

@OnlyIn(Dist.CLIENT)
public class TextPanel extends Screen {

  private final int x, y, x_end, y_end;

  private final ScrollBar scroll;

  private String label;
  private List<String> lines;
  private final int max_lines;

  public TextPanel(int x, int y, int width, int height) {
    super(StringTextComponent.EMPTY);

    this.x = x;
    this.y = y;
    this.x_end = x + width;
    this.y_end = y + height;

    Minecraft mc = Minecraft.getInstance();
    this.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());

    this.scroll = new ScrollBar(this.x_end - 8, this.y + 2, 0, this.y_end - 2);
    this.<ScrollBar>addWidget(this.scroll);

    this.max_lines = (int) ((float) (height - 4) / ((float) this.font.lineHeight * 0.7f));
  }

  public void render(MatrixStack mStack, int mouseX, int mouseY, float partialTicks) {
    // Background
    AbstractGui.fill(mStack, this.x, this.y, this.x_end, this.y_end, 0xff000000);
    AbstractGui.fill(mStack, this.x, this.y, this.x_end, this.y + 1, 0xffffffff);
    AbstractGui.fill(mStack, this.x, this.y, this.x + 1, this.y_end, 0xffffffff);
    AbstractGui.fill(mStack, this.x_end - 1, this.y, this.x_end, this.y_end, 0xffffffff);
    AbstractGui.fill(mStack, this.x, this.y_end - 1, this.x_end, this.y_end, 0xffffffff);

    if(this.lines != null) {
      // Title
      int x = this.x + (this.x_end - this.x - this.font.width(this.label)) / 2;
      AbstractGui.drawString(mStack, this.font, this.label, x, this.y - this.font.lineHeight - 2, 0xffffffff);

      // Lines
      RenderSystem.pushMatrix();

      RenderSystem.scalef(0.7f, 0.7f, 0.7f);
      x = (int) ((float) (this.x + 2) / 0.7f);

      int i = 0;
      for(String line : this.lines) {
        if(i >= this.scroll.current_scroll) {
          int index = i - this.scroll.current_scroll;
          if(index >= this.max_lines) break;

          int y = (int) ((float) (this.y + 2) / 0.7f) + (index * this.font.lineHeight);
          AbstractGui.drawString(mStack, this.font, this.lines.get(i), x, y, 0xffffffff);
        }
        i ++;
      }

      RenderSystem.popMatrix();
    }

    this.scroll.render(mStack, mouseX, mouseY, partialTicks);
  }

  public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    this.scroll.scroll(-1 * (int) (Math.abs(amount) / amount));
    return true;
  }

  public void setTitle(String title) {
    this.label = title;
  }

  public void setTag(String tag, String header) {
    this.lines = new LinkedList<String>();
    if(header != null) {
      this.lines.add(header);
      this.lines.add("");
    }

    // Divide tag into lines
    int line_width = (int) ((float) (this.x_end - this.x - 12) / 0.7f);

    StringBuilder next_line = new StringBuilder();

    int width = 0, shift = 0;
    boolean in_string = false;
    char[] arr = tag.toCharArray();
    for(int i = 0; i < arr.length; i ++) {
      int cw = this.font.width(Character.toString(arr[i]));

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
    if(next_line.length() > 0) this.lines.add(next_line.toString());

    if(this.lines.size() > this.max_lines) this.scroll.resetMaxScroll(this.lines.size() - this.max_lines);
    else this.scroll.resetMaxScroll(0);
  }

  private int newLine(StringBuilder next, int shift) {
    this.lines.add(next.toString());
    next.delete(0, next.length());
    if(shift <= 0) return 0;
    for(int i = 0; i < shift; i ++) next.append("  ");
    return this.font.width(next.toString());
  }

  public void resetTag() {
    this.lines = null;
    this.label = null;
    this.scroll.resetMaxScroll(0);
  }
}
