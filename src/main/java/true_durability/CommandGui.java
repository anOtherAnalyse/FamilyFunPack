package true_durability;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;

import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class CommandGui extends GuiScreen {

  private static final int guiWidth = 148;
  private static final int guiHeight = 200;

  private int x, y, x_end, y_end;

  public CommandGui() {}

  public void initGui() {
    // Windows position
    this.x = (this.width / 4) - (CommandGui.guiWidth / 2);
    this.y = (this.height / 2) - (CommandGui.guiHeight / 2);
    this.x_end = CommandGui.guiWidth + x;
    this.y_end = CommandGui.guiHeight + y;

    // Add labels
    GuiLabel title = (new GuiLabel(this.fontRenderer, 0, this.x + 2, this.y + 3, CommandGui.guiWidth - 4, 16, 0xffeeeeee)).setCentered();
    title.addLine("Family Fun Pack");
    this.labelList.add(title);

    GuiLabel invulnerability = new GuiLabel(this.fontRenderer, 1, this.x + 6, this.y + 22, CommandGui.guiWidth - 8, 16, 0xffeeeeee);
    invulnerability.addLine("Invulnerability");
    this.labelList.add(invulnerability);

    // Add buttons
    OnOffButton on_invulnerable = new OnOffButton(0, this.x + 110, this.y + 26) {

      public void onChange() {
        TrueDurability.invulnerable = this.state;
      }

    };
    on_invulnerable.state = TrueDurability.invulnerable;
    this.buttonList.add(on_invulnerable);
  }

  public void onGuiClosed() {}

  protected void actionPerformed(GuiButton button) throws IOException {
    if(button instanceof OnOffButton) {
      OnOffButton on_off = (OnOffButton) button;
      on_off.inverseState();
    }
  }

  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if(keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACKSLASH) {
      Minecraft client = Minecraft.getMinecraft();
      client.displayGuiScreen(null);
      if (client.currentScreen == null) {
        client.setIngameFocus();
      }
    }
  }

  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    Gui.drawRect(this.x, this.y, this.x_end, this.y_end, 216 << 24); // GUI background

    // borders
    Gui.drawRect(this.x, this.y, this.x_end, this.y + 2, 0xffbbbbbb);
    Gui.drawRect(this.x, this.y, this.x + 2, this.y_end, 0xffbbbbbb);
    Gui.drawRect(this.x_end - 2, this.y, this.x_end, this.y_end, 0xffbbbbbb);
    Gui.drawRect(this.x, this.y_end - 2, this.x_end, this.y_end, 0xffbbbbbb);

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

	public boolean doesGuiPauseGame() {
	    return false;
	}

}
