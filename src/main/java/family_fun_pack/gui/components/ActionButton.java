package family_fun_pack.gui.components;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public abstract class ActionButton extends GuiButton {

  public ActionButton(int id, int x, int y, int width, int height, String text) {
    super(id, x, y, width, height, text);
  }

  public abstract void onClick(GuiScreen parent);

}
