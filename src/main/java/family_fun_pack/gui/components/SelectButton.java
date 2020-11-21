package family_fun_pack.gui.components;

import net.minecraft.block.properties.IProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collection;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.interfaces.AdvancedSearchGui;

@SideOnly(Side.CLIENT)
public class SelectButton extends ActionButton {

  public static final ResourceLocation NAVIGATION = new ResourceLocation(FamilyFunPack.MODID, "textures/gui/navigation.png");

  private IProperty<?> property;
  private String[] values;
  private String[] path;
  private int index;
  private int base;
  private int cycle;

  public SelectButton(int id, int x, int y, IProperty<?> property) {
    super(id, x, y, 64, 16, null);
    this.property = property;
    Collection<?> values = this.property.getAllowedValues();
    this.values = new String[values.size()];
    this.base = 0;
    int j = 0;
    for(Object i : values) {
      this.values[j] = i.toString();
      j ++;
    }
    this.index = -1;
    this.cycle = 0;
    this.path = null;
  }

  public SelectButton(int x, int y, IProperty<?> property) {
    this(0, x, y, property);
  }

  public SelectButton(int id, int x, int y, Collection<String> tags, int current) {
    super(id, x, y, 64, 16, null);
    this.base = current;
    this.index = -1;
    this.property = null;
    this.path = new String[tags.size()];
    this.values = null;
    int j = 0;
    for(String i : tags) {
      this.path[j] = i;
      j ++;
    }
  }

  public SelectButton(int id, int x, int y, Collection<String> tags, Collection<String> values, int current) {
    this(id, x, y, tags, current);
    this.values = new String[values.size()];
    int j = 0;
    for(String i : values) {
      this.values[j] = i;
      j ++;
    }
  }

  public void drawButton(Minecraft client, int mouseX, int mouseY, float partialTicks) {
    GlStateManager.enableAlpha();
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

    GlStateManager.pushMatrix();
    GlStateManager.scale(0.5f, 0.5f, 0.5f);

    int x = (int)((float)this.x / 0.5f);
    int y = (int)((float)this.y / 0.5f);

    client.getTextureManager().bindTexture(SelectButton.NAVIGATION);
    Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16 * 2, 16);
    Gui.drawModalRectWithCustomSizedTexture(x + this.width - 16, y, 16, 0, 16, 16, 16 * 2, 16);
    Gui.drawRect(x + 16, y, x + this.width - 16, y + 1, 0xffffffff);
    Gui.drawRect(x + 16, y + 15, x + this.width - 16, y + 16, 0xffffffff);

    String label = null;
    if(this.index == -1) label = "any";
    else if(this.values != null) label = this.values[this.index];
    else label = Integer.toString(this.index);
    int str_width = client.fontRenderer.getStringWidth(label);
    while(str_width > this.width - 32) {
      this.width += 16;
      this.x -= 8;
      x = (int)((float)this.x / 0.5f);
    }
    x = x + 16 + ((this.width - 32 - str_width) / 2);
    this.drawString(client.fontRenderer, label, x, y + 4, 0xffffffff);

    GlStateManager.popMatrix();
  }

  public void onClick(GuiScreen parent) {
    if(this.cycle > 0) {
      if(this.property != null) {
        while(this.cycle > 0) {
          ((AdvancedSearchGui) parent).cycleProperty(this.property);
          this.cycle --;
        }
      } else {
        int index = (this.index == -1 ? this.base : this.index);
        if(this.values == null) {
          ((AdvancedSearchGui) parent).setTag(this.path, (Object)Integer.valueOf(index));
        } else {
          ((AdvancedSearchGui) parent).setTag(this.path, (Object)(this.values[index]));
        }
      }
    }
  }

  public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
    if(mouseX >= this.x && mouseY >= this.y && mouseX < (this.x + this.width / 2) && mouseY < (this.y + this.height / 2)) {
      int length = (this.values == null ? 256 : this.values.length);
      if(mouseX < this.x + 8) {
        if(this.index == -1) this.index = ((this.base - 1) + length) % length;
        else if(this.index == this.base) this.index = -1;
        else this.index = ((this.index - 1) + length) % length;

        if(this.index == -1) this.cycle = 0;
        else this.cycle = length - 1;

        return true;
      } else if(mouseX >= this.x + (this.width / 2) - 8) {
        if(this.index == -1) this.index = this.base;
        else if(this.index == (((this.base - 1) + length) % length)) this.index = -1;
        else this.index = (this.index + 1) % length;

        if(this.index == this.base) this.cycle = 0;
        else this.cycle = 1;

        return true;
      }
    }
    return false;
  }

  /* Getters */

  public int getIndex() {
    return this.index;
  }

  public IProperty<?> getProperty() {
    return this.property;
  }

  public String getValue() {
    if(this.index == -1 || this.values == null) return null;
    return this.values[this.index];
  }

  public String[] getPath() {
    return this.path;
  }

  /* Setters */

  public void reset() {
    this.index = -1;
  }

  public void setValue(String value) {
    if(this.values != null) {
      int j = 0;
      for(String i : this.values) {
        if(i.equals(value)) {
          this.index = j;
          return;
        }
        j ++;
      }
      this.index = -1;
    }
  }

  public void setValue(int value) {
    this.index = value;
  }

}
