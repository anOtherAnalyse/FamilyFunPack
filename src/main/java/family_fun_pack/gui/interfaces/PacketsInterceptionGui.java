package family_fun_pack.gui.interfaces;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.ProtocolType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.ArrayList;

import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.components.GenericButton;
import family_fun_pack.gui.components.OnOffButton;
import family_fun_pack.gui.components.ScrollBar;
import family_fun_pack.modules.PacketInterceptionModule;

/* GUI for selecting packets to be canceled */

@OnlyIn(Dist.CLIENT)
public class PacketsInterceptionGui extends RightPanel implements Button.IPressable {

  private static final int guiWidth = 168;
  private static final int guiHeight = 200;

  private static final int maxLabelsDisplayed = 16;

  private List<String> labels;
  private List<OnOffButton> selection;

  private ScrollBar scroll;

  private PacketDirection direction;

  private PacketInterceptionModule module;

  public PacketsInterceptionGui(PacketInterceptionModule module) {
    super(MainGui.guiWidth + 16, 12, PacketsInterceptionGui.guiWidth, PacketsInterceptionGui.guiHeight, new StringTextComponent("Packet canceller selection"));

    this.module = module;
    this.direction = PacketDirection.SERVERBOUND;

    // Labels & buttons
    this.labels = new ArrayList<String>();
    this.selection = new ArrayList<OnOffButton>();

    // Switch packets selection button
    GenericButton selection = new GenericButton(this.x + 2, this.y + 4, "ServerBound", this);
    selection.x = this.x + (PacketsInterceptionGui.guiWidth / 2) - (selection.getWidth() / 2);
    this.<GenericButton>addButton(selection);

    // Scroll bar
    this.scroll = new ScrollBar(this.x_end - 10, this.y + 4, 0, this.y_end - 4);
    this.<ScrollBar>addButton(this.scroll);

    // Init selection list
    this.initPacketsList();
  }

  public void onPress(Button btn) {
    if(btn instanceof OnOffButton) {
      if(((OnOffButton) btn).getState()) {
        this.module.addIntercept(this.direction, ((OnOffButton) btn).getMetadata());
      } else {
        this.module.removeIntercept(this.direction, ((OnOffButton) btn).getMetadata());
      }
    } else {
      if(this.direction == PacketDirection.SERVERBOUND) {
        btn.setMessage(new StringTextComponent("ClientBound"));
        btn.setWidth(this.font.width("ClientBound") + 4);
      } else {
        btn.setMessage(new StringTextComponent("ServerBound"));
        btn.setWidth(this.font.width("ServerBound") + 4);
      }
      btn.x = this.x + (PacketsInterceptionGui.guiWidth / 2) - (btn.getWidth() / 2);
      this.switchDirection();
    }
  }

  public void render(MatrixStack mStack, int mouseX, int mouseY, float partialTicks) {
    // Background
    AbstractGui.fill(mStack, this.x, this.y, this.x_end, this.y_end, MainGui.BACKGROUND_COLOR);
    AbstractGui.fill(mStack, this.x, this.y, this.x_end, this.y + 2, MainGui.BORDER_COLOR);
    AbstractGui.fill(mStack, this.x, this.y, this.x + 2, this.y_end, MainGui.BORDER_COLOR);
    AbstractGui.fill(mStack, this.x_end - 2, this.y, this.x_end, this.y_end, MainGui.BORDER_COLOR);
    AbstractGui.fill(mStack, this.x, this.y_end - 2, this.x_end, this.y_end, MainGui.BORDER_COLOR);

    // List selection button & scroll bar
    super.render(mStack, mouseX, mouseY, partialTicks);

    // Draw selection list
    for(int i = this.scroll.current_scroll, index = 0; index < PacketsInterceptionGui.maxLabelsDisplayed && i < this.labels.size(); index = (++i - this.scroll.current_scroll)) {

      RenderSystem.pushMatrix();
      RenderSystem.scalef(0.7f, 0.7f, 0.7f);

      // Labels
      int decal_y = (int)((float)(this.y + 20 + index * 11) / 0.7f);
      int decal_x = (int)((float)(this.x + 4) / 0.7f);
      AbstractGui.drawString(mStack, this.font, this.labels.get(i), decal_x, decal_y, 0xffbbbbbb);

      // Border
      int border_decal_y = decal_y + (int)(8f / 0.7f);
      AbstractGui.fill(mStack, decal_x, border_decal_y, (int)(((float)this.x_end - 10f) / 0.7f), border_decal_y + 1, 0xff111133);

      RenderSystem.popMatrix();

      // Buttons
      OnOffButton current = this.selection.get(i);
      current.x = this.x_end - 28;
      current.y = this.y + 20 + index * 11;
      current.renderButton(mStack, mouseX, mouseY, partialTicks);
    }
  }

  public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
    if(mouseButton == 0) {
      for(int i = this.scroll.current_scroll, index = 0; index < PacketsInterceptionGui.maxLabelsDisplayed && i < this.selection.size(); index = (++i - this.scroll.current_scroll)) {
        if(this.selection.get(i).mouseClicked(mouseX, mouseY, mouseButton)) return true;
      }
    }
    return super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    this.scroll.scroll(-1 * (int) (Math.abs(amount) / amount));
    return true;
  }

  public void switchDirection() {
    if(this.direction == PacketDirection.SERVERBOUND) this.direction = PacketDirection.CLIENTBOUND;
    else this.direction = PacketDirection.SERVERBOUND;
    this.initPacketsList();
  }

  public void initPacketsList() {
    // Init labels
    this.labels.clear();
    int size = this.direction == PacketDirection.SERVERBOUND ? 48 : 92;
    for(int i = 0; i < size; i ++) {
      String label = ProtocolType.PLAY.createPacket(this.direction, i).getClass().getSimpleName();
      if(i >= 18 && i <= 20) label = "CPlayerPacket." + label;
      this.labels.add(label);
    }

    // Init on/off buttons
    this.selection.clear();
    for(int i = 0; i < this.labels.size(); i ++) {
      OnOffButton btn = new OnOffButton(0, 0, this);
      btn.setState(this.module.isFiltered(this.direction, i));
      btn.setMetadata(i);
      this.selection.add(btn);
    }

    // Set max scroll
    int max_scroll = this.labels.size() - PacketsInterceptionGui.maxLabelsDisplayed;
    this.scroll.resetMaxScroll(max_scroll > 0 ? max_scroll : 0);
  }
}
