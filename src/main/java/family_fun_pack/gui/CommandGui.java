package family_fun_pack.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPig;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.passive.EntityPig;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.lang.Class;

import org.lwjgl.input.Keyboard;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.NoRenderPig;
import family_fun_pack.Tooltip;

@SideOnly(Side.CLIENT)
public class CommandGui extends GuiScreen {

  public final static int BACKGROUND_COLOR = (216 << 24);

  private static final int guiWidth = 148;
  private static final int guiHeight = 200;

  private int x, y, x_end, y_end;

  private ScrollGui send_packet_choice;

  private Tooltip tooltip;

  public CommandGui(Tooltip tooltip) {
    this.tooltip = tooltip;
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

    GuiLabel pig_label = new GuiLabel(this.fontRenderer, 4, this.x + 6, this.y + 24 + space * 4, CommandGui.guiWidth - 8, 16, 0xffeeeeee);
    pig_label.addLine("Pig POV");
    this.labelList.add(pig_label);

    // Add buttons
    OnOffButton on_invulnerable = new OnOffButton(0, this.x_end - 22, this.y + 28) {

      public void performAction() {
        FamilyFunPack.configuration.invulnerable = this.state;
        FamilyFunPack.configuration.currently_invulnerable = false;
        if(!this.state && FamilyFunPack.configuration.last_teleport_id != -1) {
          FamilyFunPack.sendPacket(new CPacketConfirmTeleport(FamilyFunPack.configuration.last_teleport_id));
          FamilyFunPack.configuration.last_teleport_id = -1;
        }
        FamilyFunPack.configuration.save();
      }

    };
    on_invulnerable.state = FamilyFunPack.configuration.invulnerable;
    this.buttonList.add(on_invulnerable);

    OnOffButton on_intercept = new OnOffButton(0, this.x_end - 22, this.y + space + 28) {
      public void performAction() {
        FamilyFunPack.configuration.block_player_packets = this.state;
        FamilyFunPack.configuration.save();
      }
    };
    on_intercept.state = FamilyFunPack.configuration.block_player_packets;
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
        CommandGui.this.mc.displayGuiScreen(new InfoItemGui(CommandGui.this.mc.player.inventoryContainer, CommandGui.this.tooltip));
      }
    };
    this.buttonList.add(open_info);

    OnOffButton on_pig_pov = new OnOffButton(0, this.x_end - 22, this.y + (space * 4) + 28) {

      private Minecraft mc = Minecraft.getMinecraft();

      public void performAction() {
        FamilyFunPack.configuration.pigPOV = this.state;
        if(this.state) {
          this.mc.player.eyeHeight = 0.6f;
          this.mc.getRenderManager().entityRenderMap.put(EntityPig.class, new NoRenderPig(this.mc.getRenderManager(), this.mc));
        } else {
          this.mc.player.eyeHeight = this.mc.player.getDefaultEyeHeight();
          this.mc.getRenderManager().entityRenderMap.put(EntityPig.class, new RenderPig(this.mc.getRenderManager()));
        }
      }
    };
    on_pig_pov.state = FamilyFunPack.configuration.pigPOV;
    this.buttonList.add(on_pig_pov);
  }

  // public void onGuiClosed() {}

  protected void actionPerformed(GuiButton button) throws IOException {
    if(button instanceof ActionButton) {
      ActionButton action = (ActionButton) button;
      action.changeState();
    }
  }

  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if(keyCode == Keyboard.KEY_ESCAPE || keyCode == this.tooltip.openGUIKey.getKeyCode()) {
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
