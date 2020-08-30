package true_durability.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.client.renderer.GlStateManager;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

import org.lwjgl.input.Keyboard;

import true_durability.TrueDurability;

@SideOnly(Side.CLIENT)
public class CommandGui extends GuiScreen {

  public final static int BACKGROUND_COLOR = (216 << 24);

  private static final int guiWidth = 148;
  private static final int guiHeight = 200;

  private int x, y, x_end, y_end;

  private ScrollGui send_packet_choice;

  public CommandGui() {
    this.send_packet_choice = null;
  }

  public void initGui() {
    // Windows position
    this.x = 12;
    this.y = 12;
    this.x_end = CommandGui.guiWidth + this.x;
    this.y_end = CommandGui.guiHeight + this.y;

    // Line y space
    int space = 18;

    // Add labels
    GuiLabel title = (new GuiLabel(this.fontRenderer, 0, this.x + 2, this.y + 4, CommandGui.guiWidth - 4, 16, 0xffeeeeee)).setCentered();
    title.addLine("Family Fun Pack");
    this.labelList.add(title);

    GuiLabel invulnerable_label = new GuiLabel(this.fontRenderer, 1, this.x + 6, this.y + 24, CommandGui.guiWidth - 8, 16, 0xffeeeeee);
    invulnerable_label.addLine("Portal invulnerability");
    this.labelList.add(invulnerable_label);

    GuiLabel packet_label = new GuiLabel(this.fontRenderer, 2, this.x + 6, this.y + 24 + space, CommandGui.guiWidth - 8, 16, 0xffeeeeee);
    packet_label.addLine("Packets interception");
    this.labelList.add(packet_label);

    GuiLabel packet_which = new GuiLabel(this.fontRenderer, 3, this.x + 6, this.y + 24 + space * 2, CommandGui.guiWidth - 8, 16, 0xffeeeeee);
    packet_which.addLine("Which packets ?");
    this.labelList.add(packet_which);

    GuiLabel info_items = new GuiLabel(this.fontRenderer, 4, this.x + 6, this.y + 24 + space * 3, CommandGui.guiWidth - 8, 16, 0xffeeeeee);
    info_items.addLine("Items tags");
    this.labelList.add(info_items);

    // Add buttons
    OnOffButton on_invulnerable = new OnOffButton(0, this.x_end - 22, this.y + 28) {

      public void performAction() {
        TrueDurability.configuration.invulnerable = this.state;
        TrueDurability.configuration.currently_invulnerable = false;
        if(!this.state && TrueDurability.configuration.last_teleport_id != -1) {
          TrueDurability.sendPacket(new CPacketConfirmTeleport(TrueDurability.configuration.last_teleport_id));
          TrueDurability.configuration.last_teleport_id = -1;
        }
      }

    };
    on_invulnerable.state = TrueDurability.configuration.invulnerable;
    this.buttonList.add(on_invulnerable);

    OnOffButton on_intercept = new OnOffButton(0, this.x_end - 22, this.y + space + 28) {
      public void performAction() {
        TrueDurability.configuration.block_player_packets = this.state;
      }
    };
    on_intercept.state = TrueDurability.configuration.block_player_packets;
    this.buttonList.add(on_intercept);

    OpenButton selection = new OpenButton(1, this.x + 96, this.y + (space * 2) + 24, this.fontRenderer, "select") {
      public void performAction() {
        if(this.clicked) {
          CommandGui.this.initSendPacketsSelection();
        } else {
          CommandGui.this.send_packet_choice = null;
        }
      }
    };
    this.buttonList.add(selection);

    OpenButton open_info = new OpenButton(2, this.x + 96, this.y + (space * 3) + 24, this.fontRenderer, "view") {
      public void performAction() {
        CommandGui.this.mc.displayGuiScreen(new InfoItemGui(CommandGui.this.mc.player.inventoryContainer));
      }
    };
    this.buttonList.add(open_info);
  }

  // public void onGuiClosed() {}

  protected void actionPerformed(GuiButton button) throws IOException {
    if(button instanceof ActionButton) {
      ActionButton action = (ActionButton) button;
      action.changeState();
    }
  }

  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if(keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACKSLASH) {
      this.mc.displayGuiScreen(null);
      if (this.mc.currentScreen == null) {
        this.mc.setIngameFocus();
      }
    }
  }

  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    Gui.drawRect(this.x, this.y, this.x_end, this.y_end, CommandGui.BACKGROUND_COLOR); // GUI background

    // borders
    Gui.drawRect(this.x, this.y, this.x_end, this.y + 2, 0xffbbbbbb);
    Gui.drawRect(this.x, this.y, this.x + 2, this.y_end, 0xffbbbbbb);
    Gui.drawRect(this.x_end - 2, this.y, this.x_end, this.y_end, 0xffbbbbbb);
    Gui.drawRect(this.x, this.y_end - 2, this.x_end, this.y_end, 0xffbbbbbb);

    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    super.drawScreen(mouseX, mouseY, partialTicks);

    // Draw packets intercept selection
    if(this.send_packet_choice != null) {
      this.send_packet_choice.drawScreen(mouseX, mouseY, partialTicks);
    }
  }

	public boolean doesGuiPauseGame() {
	    return true;
	}

  private void initSendPacketsSelection() {
    this.send_packet_choice = new ScrollGui(this.x_end + 4, this.y, this.fontRenderer);
  }

  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if(this.send_packet_choice != null) {
      this.send_packet_choice.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  protected void mouseReleased(int mouseX, int mouseY, int state) {
    super.mouseReleased(mouseX, mouseY, state);
    if(this.send_packet_choice != null) {
      this.send_packet_choice.mouseReleased(mouseX, mouseY, state);
    }
  }

}
