package family_fun_pack.gui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GenericButton extends Button {
  public static final int BACKGROUND = (216 << 24);
  public static final int COLOR = 0xffbbbbbb;

  public FontRenderer font;
  private float scale;

  private int id;

  // Colors
  protected int background;
  protected int text;

  public GenericButton(int x, int y, String text, Button.IPressable action) {
    this(x, y, text, action, 1f);
  }

  public GenericButton(int x, int y, String text, Button.IPressable action, float scale) {
    super(x, y, 0, 0, new StringTextComponent(text), action);
    this.font = Minecraft.getInstance().font;
    this.width = (int) ((float) this.font.width(text) * scale) + 4;
    this.height = (int) ((float) this.font.lineHeight * scale) + 4;
    this.scale = scale;
    this.background = GenericButton.BACKGROUND;
    this.text = GenericButton.COLOR;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getId() {
    return this.id;
  }

  public void renderButton(MatrixStack mStack, int mouseX, int mouseY, float partialTicks) {
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

    int x = (int) ((float) this.x / this.scale);
    int y = (int) ((float) this.y / this.scale);
    int x_end = x + (int) ((float) this.width / this.scale);
    int y_end = y + (int) ((float) this.height / this.scale);

    RenderSystem.pushMatrix();
    RenderSystem.scalef(this.scale, this.scale, this.scale);
    AbstractGui.fill(mStack, x, y, x_end, y_end, this.background);
    AbstractGui.fill(mStack, x, y, x_end, y + 1, this.text);
    AbstractGui.fill(mStack, x, y, x + 1, y_end, this.text);
    AbstractGui.fill(mStack, x, y_end - 1, x_end, y_end, this.text);
    AbstractGui.fill(mStack, x_end - 1, y, x_end, y_end, this.text);
    this.font.draw(mStack, this.getMessage(), (float) (x + 2), (float) (y + 2), this.text);
    // AbstractGui.drawString(mStack, this.font, this.getMessage(), x + 2, y + 2, this.text);
    RenderSystem.popMatrix();
  }
}
