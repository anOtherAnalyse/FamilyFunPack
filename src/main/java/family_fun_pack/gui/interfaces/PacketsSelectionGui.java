package family_fun_pack.gui.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.client.renderer.GlStateManager;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.components.GenericButton;
import family_fun_pack.gui.components.OnOffButton;
import family_fun_pack.gui.components.ScrollBar;
import family_fun_pack.gui.components.actions.OnOffInterception;
import family_fun_pack.modules.PacketInterceptionModule;
import family_fun_pack.modules.Module;

/* GUI for selecting packets to be canceled */

@SideOnly(Side.CLIENT)
public class PacketsSelectionGui extends RightPanel {

  private static final int guiWidth = 148;
  private static final int guiHeight = 200;

  private static final int maxLabelsDisplayed = 16;

  private int x, y, x_end, y_end;

  private List<String> labels;
  private List<OnOffButton> enableList;

  private ScrollBar scroll;

  public EnumPacketDirection direction;

  public PacketsSelectionGui() {
    super();

    this.x = MainGui.guiWidth + 16;
    this.y = 12;
    this.x_end = PacketsSelectionGui.guiWidth + this.x;
    this.y_end = PacketsSelectionGui.guiHeight + this.y;

    this.direction = EnumPacketDirection.SERVERBOUND;
    this.labels = new ArrayList<String>();
    this.enableList = new ArrayList<OnOffButton>();

    GenericButton selection = new GenericButton(1, this.x + 2, this.y + 4, "Emitted") {
      public void onClick(GuiScreen parent) {
        if(this.displayString.equals("Emitted")) this.displayString = "Received";
        else this.displayString = "Emitted";
        this.width = this.fontRenderer.getStringWidth(this.displayString) + 4;
        this.x = PacketsSelectionGui.this.x + (PacketsSelectionGui.guiWidth / 2) - (this.width / 2);
        PacketsSelectionGui.this.switchDirection();
      }
    };
    selection.x = this.x + (PacketsSelectionGui.guiWidth / 2) - (selection.width / 2);
    this.buttonList.add(selection);
  }

  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    Gui.drawRect(this.x, this.y, this.x_end, this.y_end, MainGui.BACKGROUND_COLOR); // GUI background

    // borders
    Gui.drawRect(this.x, this.y, this.x_end, this.y + 2, 0xffbbbbbb);
    Gui.drawRect(this.x, this.y, this.x + 2, this.y_end, 0xffbbbbbb);
    Gui.drawRect(this.x_end - 2, this.y, this.x_end, this.y_end, 0xffbbbbbb);
    Gui.drawRect(this.x, this.y_end - 2, this.x_end, this.y_end, 0xffbbbbbb);

    // Update scroll
    if(this.scroll.clicked) {
      this.scroll.dragged(mouseX, mouseY);
    }

    super.drawScreen(mouseX, mouseY, partialTicks);

    // Draw labels
    GlStateManager.pushMatrix();
    float scale = 0.7f;
    GlStateManager.scale(scale, scale, scale);
    for(int i = this.scroll.current_scroll; (i - this.scroll.current_scroll) < PacketsSelectionGui.maxLabelsDisplayed & i < this.labels.size(); i ++) {
      int decal_y = (int)((float)(this.y + 20 + (i - this.scroll.current_scroll) * 11) / scale);
      int decal_x = (int)((float)(this.x + 4) / scale);
      this.drawString(this.fontRenderer, this.labels.get(i), decal_x, decal_y, 0xffbbbbbb);
      int border_decal_y = decal_y + (int)(8f / scale);
      Gui.drawRect(decal_x, border_decal_y, (int)(((float)this.x_end - 10f) / scale), border_decal_y + 1, 0xff111133); // Border at end of line
    }
    GlStateManager.popMatrix();

    // Draw enable buttons
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    for(int i = this.scroll.current_scroll; (i - this.scroll.current_scroll) < PacketsSelectionGui.maxLabelsDisplayed & i < this.enableList.size(); i ++) {
      OnOffButton current = this.enableList.get(i);
      current.x = this.x_end - 28;
      current.y = this.y + 20 + (i - this.scroll.current_scroll) * 11;
      current.drawButton(this.mc, mouseX, mouseY, partialTicks);
    }
  }

  public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    if(mouseButton == 0) {
      for(int i = this.scroll.current_scroll; (i - this.scroll.current_scroll) < PacketsSelectionGui.maxLabelsDisplayed && i < this.enableList.size(); i ++) {
        OnOffButton current = this.enableList.get(i);
        if(current.mousePressed(this.mc, mouseX, mouseY)) {
          current.onClick(this);
          current.playPressSound(this.mc.getSoundHandler());
          return;
        }
      }
      super.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  public void mouseReleased(int mouseX, int mouseY, int state) {
    if(state == 0) {
      this.scroll.mouseReleased(mouseX, mouseY);
    }
  }

  public void mouseWheel(int wheel) {
    this.scroll.scroll(wheel);
  }

  public void switchDirection() {
    if(this.direction == EnumPacketDirection.SERVERBOUND) this.direction = EnumPacketDirection.CLIENTBOUND;
    else this.direction = EnumPacketDirection.SERVERBOUND;
    this.initPacketsList();
  }

  public void dependsOn(Module dependence) {
    super.dependsOn(dependence);
    this.initPacketsList();
  }

  public void initPacketsList() {
    this.labels.clear();
    if(this.direction == EnumPacketDirection.SERVERBOUND) {
      this.labels.add("CPacketConfirmTeleport");
      this.labels.add("CPacketTabComplete");
      this.labels.add("CPacketChatMessage");
      this.labels.add("CPacketClientStatus");
      this.labels.add("CPacketClientSettings");
      this.labels.add("CPacketConfirmTransaction");
      this.labels.add("CPacketEnchantItem");
      this.labels.add("CPacketClickWindow");
      this.labels.add("CPacketCloseWindow");
      this.labels.add("CPacketCustomPayload");
      this.labels.add("CPacketUseEntity");
      this.labels.add("CPacketKeepAlive");
      this.labels.add("CPacketPlayer");
      this.labels.add("CPacketPlayer.Position");
      this.labels.add("CPacketPlayer.PositionRotation");
      this.labels.add("CPacketPlayer.Rotation");
      this.labels.add("CPacketVehicleMove");
      this.labels.add("CPacketSteerBoat");
      this.labels.add("CPacketPlaceRecipe");
      this.labels.add("CPacketPlayerAbilities");
      this.labels.add("CPacketPlayerDigging");
      this.labels.add("CPacketEntityAction");
      this.labels.add("CPacketInput");
      this.labels.add("CPacketRecipeInfo");
      this.labels.add("CPacketResourcePackStatus");
      this.labels.add("CPacketSeenAdvancements");
      this.labels.add("CPacketHeldItemChange");
      this.labels.add("CPacketCreativeInventoryAction");
      this.labels.add("CPacketUpdateSign");
      this.labels.add("CPacketAnimation");
      this.labels.add("CPacketSpectate");
      this.labels.add("CPacketPlayerTryUseItemOnBlock");
      this.labels.add("CPacketPlayerTryUseItem");
    } else {
      this.labels.add("SPacketSpawnObject");
      this.labels.add("SPacketSpawnExperienceOrb");
      this.labels.add("SPacketSpawnGlobalEntity");
      this.labels.add("SPacketSpawnMob");
      this.labels.add("SPacketSpawnPainting");
      this.labels.add("SPacketSpawnPlayer");
      this.labels.add("SPacketAnimation");
      this.labels.add("SPacketStatistics");
      this.labels.add("SPacketBlockBreakAnim");
      this.labels.add("SPacketUpdateTileEntity");
      this.labels.add("SPacketBlockAction");
      this.labels.add("SPacketBlockChange");
      this.labels.add("SPacketUpdateBossInfo");
      this.labels.add("SPacketServerDifficulty");
      this.labels.add("SPacketTabComplete");
      this.labels.add("SPacketChat");
      this.labels.add("SPacketMultiBlockChange");
      this.labels.add("SPacketConfirmTransaction");
      this.labels.add("SPacketCloseWindow");
      this.labels.add("SPacketOpenWindow");
      this.labels.add("SPacketWindowItems");
      this.labels.add("SPacketWindowProperty");
      this.labels.add("SPacketSetSlot");
      this.labels.add("SPacketCooldown");
      this.labels.add("SPacketCustomPayload");
      this.labels.add("SPacketCustomSound");
      this.labels.add("SPacketDisconnect");
      this.labels.add("SPacketEntityStatus");
      this.labels.add("SPacketExplosion");
      this.labels.add("SPacketUnloadChunk");
      this.labels.add("SPacketChangeGameState");
      this.labels.add("SPacketKeepAlive");
      this.labels.add("SPacketChunkData");
      this.labels.add("SPacketEffect");
      this.labels.add("SPacketParticles");
      this.labels.add("SPacketJoinGame");
      this.labels.add("SPacketMaps");
      this.labels.add("SPacketEntity");
      this.labels.add("SPacketEntity.EntityRelMove");
      this.labels.add("SPacketEntity.EntityLookMove");
      this.labels.add("SPacketEntity.EntityLook");
      this.labels.add("SPacketMoveVehicle");
      this.labels.add("SPacketSignEditorOpen");
      this.labels.add("SPacketPlaceGhostRecipe");
      this.labels.add("SPacketPlayerAbilities");
      this.labels.add("SPacketCombatEvent");
      this.labels.add("SPacketPlayerListItem");
      this.labels.add("SPacketPlayerPosLook");
      this.labels.add("SPacketUseBed");
      this.labels.add("SPacketRecipeBook");
      this.labels.add("SPacketDestroyEntities");
      this.labels.add("SPacketRemoveEntityEffect");
      this.labels.add("SPacketResourcePackSend");
      this.labels.add("SPacketRespawn");
      this.labels.add("SPacketEntityHeadLook");
      this.labels.add("SPacketSelectAdvancementsTab");
      this.labels.add("SPacketWorldBorder");
      this.labels.add("SPacketCamera");
      this.labels.add("SPacketHeldItemChange");
      this.labels.add("SPacketDisplayObjective");
      this.labels.add("SPacketEntityMetadata");
      this.labels.add("SPacketEntityAttach");
      this.labels.add("SPacketEntityVelocity");
      this.labels.add("SPacketEntityEquipment");
      this.labels.add("SPacketSetExperience");
      this.labels.add("SPacketUpdateHealth");
      this.labels.add("SPacketScoreboardObjective");
      this.labels.add("SPacketSetPassengers");
      this.labels.add("SPacketTeams");
      this.labels.add("SPacketUpdateScore");
      this.labels.add("SPacketSpawnPosition");
      this.labels.add("SPacketTimeUpdate");
      this.labels.add("SPacketTitle");
      this.labels.add("SPacketSoundEffect");
      this.labels.add("SPacketPlayerListHeaderFooter");
      this.labels.add("SPacketCollectItem");
      this.labels.add("SPacketEntityTeleport");
      this.labels.add("SPacketAdvancementInfo");
      this.labels.add("SPacketEntityProperties");
      this.labels.add("SPacketEntityEffect");
    }

    // reset / sed Enable buttons list
    this.enableList.clear();
    for(int i = 0; i < this.labels.size(); i ++) {
      OnOffButton enable_btn = new OnOffButton(i, 0, 0, new OnOffInterception((PacketInterceptionModule) this.dependence, this.direction, i));
      enable_btn.setState(((PacketInterceptionModule) this.dependence).isFiltered(this.direction, i));
      this.enableList.add(enable_btn);
    }

    // reset / set scroll bar
    int max_scroll = this.labels.size() - PacketsSelectionGui.maxLabelsDisplayed;
    if(this.scroll != null) {
      this.scroll.resetMaxScroll(max_scroll > 0 ? max_scroll : 0);
    } else {
      this.scroll = new ScrollBar(0, this.x_end - 10, this.y + 4, max_scroll > 0 ? max_scroll : 0, this.y_end - 4);
      this.buttonList.add(this.scroll);
    }
  }

}
