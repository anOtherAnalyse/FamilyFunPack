package family_fun_pack.gui.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import org.lwjgl.input.Keyboard;

import java.io.IOException;

import family_fun_pack.commands.TrackCommand;
import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.GenericButton;
import family_fun_pack.gui.components.ScrollBar;

@SideOnly(Side.CLIENT)
public class RadarInterface extends GuiScreen {

  private static void drawRect(double left, double top, double right, double bottom, float red, float green, float blue) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferbuilder = tessellator.getBuffer();
    GlStateManager.enableBlend();
    GlStateManager.disableTexture2D();
    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    GlStateManager.color(red, green, blue, 1f);
    bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
    bufferbuilder.pos(left, bottom, 0.0D).endVertex();
    bufferbuilder.pos(right, bottom, 0.0D).endVertex();
    bufferbuilder.pos(right, top, 0.0D).endVertex();
    bufferbuilder.pos(left, top, 0.0D).endVertex();
    tessellator.draw();
    GlStateManager.enableTexture2D();
    GlStateManager.disableBlend();
  }

  public TrackCommand backend;

  private ScrollBar scroll;
  private int last_logs_size;

  private GenericButton back_btn;

  private int x, y, x_end, y_end;
  private int gui_width;

  private int center_x, center_z;

  public RadarInterface(TrackCommand backend) {
    this.backend = backend;
    this.last_logs_size = 0;
  }

  public void initGui() {

    int width = this.height - 8;

    this.x = (this.width - (width + 180)) / 2;
    if(this.x < 0) this.x = 0;
    this.y = 4;
    this.y_end = y + width;
    this.x_end = x + width + 32;

    this.center_x = (this.backend.corner.x + (this.backend.width_x / 2) * this.backend.render_radius);
    this.center_z = (this.backend.corner.z + (this.backend.width_z / 2) * this.backend.render_radius);

    GuiLabel log = new GuiLabel(this.fontRenderer, 0, this.x_end + 6, this.y + 3, 138, 16, 0xffffffff);
    log.addLine("Logs");
    log.setCentered();
    this.labelList.add(log);

    this.scroll = new ScrollBar(0, this.x_end + 138, this.y + 19, 0, this.y_end - 4);
    this.buttonList.add(this.scroll);

    this.back_btn = new GenericButton(0, this.x_end - 73, this.y + 4, "Back to scan") {
      public void onClick(GuiScreen parent) {
        ((RadarInterface) parent).backend.backToScan();
      }
    };
    this.back_btn.enabled = false;
    this.buttonList.add(this.back_btn);
  }

  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    TrackCommand.Mode mode = this.backend.effective_mode;

    Gui.drawRect(this.x, this.y, this.x_end, this.y_end, MainGui.BACKGROUND_COLOR); // GUI background

    // borders
    Gui.drawRect(this.x, this.y, this.x_end, this.y + 2, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x, this.y, this.x + 2, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end - 2, this.y, this.x_end, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x, this.y_end - 2, this.x_end, this.y_end, MainGui.BORDER_COLOR);

    // Log panel
    Gui.drawRect(this.x_end + 4, this.y, this.x_end + 148, this.y_end, MainGui.BACKGROUND_COLOR);
    Gui.drawRect(this.x_end + 4, this.y, this.x_end + 148, this.y + 2, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end + 4, this.y, this.x_end + 6, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end + 146, this.y, this.x_end + 148, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end + 4, this.y_end - 2, this.x_end + 148, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end + 4, this.y + 16, this.x_end + 148, this.y + 17, MainGui.BORDER_COLOR);

    // Draw logs
    GlStateManager.pushMatrix();
    float scale = 0.6f;
    GlStateManager.scale(scale, scale, scale);
    this.backend.logs_lock.readLock().lock();
    if(this.last_logs_size != this.backend.logs.size()) { // Log was added
      this.last_logs_size = this.backend.logs.size();
      int max_scroll = this.last_logs_size - 28;
      if(max_scroll > 0) this.scroll.resetMaxScrollAndScroll(max_scroll);
    }
    int i = 0;
    for(String str : this.backend.logs) {
      if(i >= this.scroll.current_scroll) {
        if(i - this.scroll.current_scroll >= 28) break;
        int y = (int)((float)((i - this.scroll.current_scroll) * 8 + this.y + 21) / scale);
        this.drawString(this.fontRenderer, str, (int)((float)(this.x_end + 8) / scale), y, 0xffffffff);
      }
      i ++;
    }
    this.backend.logs_lock.readLock().unlock();
    GlStateManager.popMatrix();

    // Center radar
    Gui.drawRect(this.x + 40, this.y + 24, this.x_end - 40, this.y_end - 24, 0xff000000);
    Gui.drawRect(this.x + 40, this.y + 24, this.x_end - 40, this.y + 25, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x + 40, this.y + 24, this.x + 41, this.y_end - 24, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end - 41, this.y + 24, this.x_end - 40, this.y_end - 24, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x + 40, this.y_end - 25, this.x_end - 40, this.y_end - 24, MainGui.BORDER_COLOR);

    int middle_x = (this.x_end - this.x) / 2;
    int middle_y = (this.y_end - this.y) / 2;
    Gui.drawRect(this.x + 38, this.y + middle_y, this.x + 40, this.y + middle_y + 1, 0xffffffff);
    Gui.drawRect(this.x_end - 40, this.y + middle_y, this.x_end - 38, this.y + middle_y + 1, 0xffffffff);
    Gui.drawRect(this.x + middle_x, this.y + 22, this.x + middle_x + 1, this.y + 24, 0xffffffff);
    Gui.drawRect(this.x + middle_x, this.y_end - 24, this.x + middle_x + 1, this.y_end - 22, 0xffffffff);

    int state = this.backend.current;

    GlStateManager.pushMatrix();
    scale = 0.7f;
    GlStateManager.scale(scale, scale, scale);
    this.drawString(this.fontRenderer, "N [-z]", (int)((float)(this.x + middle_x - 9) / scale), (int)((float)(this.y + 16) / scale), 0xffffffff);
    this.drawString(this.fontRenderer, "S [+z]", (int)((float)(this.x + middle_x - 9) / scale), (int)((float)(this.y_end - 19) / scale), 0xffffffff);
    this.drawString(this.fontRenderer, "W [-x]", (int)((float)(this.x + 16) / scale), (int)((float)(this.y + middle_y - 2) / scale), 0xffffffff);
    this.drawString(this.fontRenderer, "E [+x]", (int)((float)(this.x_end - 34) / scale), (int)((float)(this.y + middle_y - 2) / scale), 0xffffffff);

    if(mode == TrackCommand.Mode.SCAN) { // Scanning info
      String coord = String.format("Center chunk: [%d, %d]", this.center_x, this.center_z);
      float coord_width = (float)this.fontRenderer.getStringWidth(coord);
      this.drawString(this.fontRenderer, coord, (int)(((float)(this.x_end - 4) / scale) - coord_width), (int)((float)(this.y + 4) / scale), 0xffffffff);

      float percent = ((float)state / (float)(this.backend.width_x * this.backend.width_z)) * 100f;
      this.drawString(this.fontRenderer, String.format("Scanned: %.2f%%", percent), (int)((float)(this.x + 4) / scale), (int)((float)(this.y + 4) / scale), 0xffffffff);

      this.drawString(this.fontRenderer, String.format("Dimensions: [%d x %d] chunks", this.backend.width_x * this.backend.render_radius, this.backend.width_z * this.backend.render_radius), (int)((float)(this.x + 4) / scale), (int)((float)(this.y_end - 8) / scale), 0xffffffff);

      this.back_btn.enabled = false;
    } else { // Tracking info
      this.backend.loaded_lock.readLock().lock();
      ChunkPos last = this.backend.loaded.getLast();
      this.backend.loaded_lock.readLock().unlock();

      this.drawString(this.fontRenderer, "Tracking player..", (int)((float)(this.x + 4) / scale), (int)((float)(this.y + 4) / scale), 0xffcc0000);
      this.drawString(this.fontRenderer, String.format("Latest chunk: [%d, %d]", last.x, last.z), (int)((float)(this.x + 4) / scale), (int)((float)(this.y_end - 8) / scale), 0xffffffff);

      String coord = String.format("Around: [%d, %d]", last.x << 4, last.z << 4);
      float coord_width = (float)this.fontRenderer.getStringWidth(coord);
      this.drawString(this.fontRenderer, coord, (int)(((float)(this.x_end - 4) / scale) - coord_width), (int)((float)(this.y_end - 8) / scale), 0xffffffff);

      this.back_btn.enabled = true;
    }

    GlStateManager.popMatrix();

    float start_x, start_z;
    int width = (this.y_end - this.y - 50);

    if(mode == TrackCommand.Mode.SCAN) { // Draw scan advancement
      if(this.backend.width_x >= this.backend.width_z) {
        scale = (float)width / (float)this.backend.width_x;
        start_x = (float)(this.x + 41) / scale;
        start_z = ((float)(this.y + 25) / scale) + ((float)(this.backend.width_x - this.backend.width_z) / 2);
      } else {
        scale = (float)width / (float)this.backend.width_z;
        start_x = ((float)(this.x + 41) / scale) + ((float)(this.backend.width_z - this.backend.width_x) / 2);
        start_z = (float)(this.y + 25) / scale;
      }

      int adv_z = state / this.backend.width_x;
      int adv_x = state % this.backend.width_x;

      GlStateManager.pushMatrix();
      GlStateManager.scale(scale, scale, scale);

      if(adv_z > 0) RadarInterface.drawRect(start_x, start_z, start_x + this.backend.width_x, start_z + adv_z, 0f, 0f, 0.6f);
      if(adv_x > 0) RadarInterface.drawRect(start_x, start_z + adv_z, start_x + adv_x, start_z + adv_z + 1, 0f, 0f, 0.6f);

      this.backend.loaded_lock.readLock().lock();
      for(ChunkPos chunk : this.backend.loaded) {
        ChunkPos position = this.backend.getRelativePos(chunk);
        RadarInterface.drawRect(start_x + (float)position.x, start_z + (float)position.z, start_x + (float)position.x + 1f, start_z + (float)position.z + 1f, 0.8f, 0f, 0f);
      }
      this.backend.loaded_lock.readLock().unlock();

      GlStateManager.popMatrix();

      // Draw center axis
      Gui.drawRect(this.x + middle_x, this.y + middle_y - 4, this.x + middle_x + 1, this.y + middle_y + 5, MainGui.BORDER_COLOR);
      Gui.drawRect(this.x + middle_x - 4, this.y + middle_y, this.x + middle_x + 5, this.y + middle_y + 1, MainGui.BORDER_COLOR);

    } else { // Draw player moves

      GlStateManager.pushMatrix();
      scale = (float)width / 101f;
      GlStateManager.scale(scale, scale, scale);

      start_x = (float)(this.x + 41) / scale;
      start_z = (float)(this.y + 25) / scale;

      this.backend.loaded_lock.readLock().lock();
      ChunkPos last = this.backend.loaded.getLast();
      for(ChunkPos chunk : this.backend.loaded) {
        int x = 50 + (chunk.x - last.x);
        int z = 50 + (chunk.z - last.z);

        if(x >= 0 && x < 101 && z >= 0 && z < 101) {
          float color = (x == 50 && z == 50 ? 1f : 0.4f);
          RadarInterface.drawRect(start_x + (float)x, start_z + (float)z, start_x + (float)x + 1f, start_z + (float)z + 1f, color, 0f, 0f);
        }
      }
      this.backend.loaded_lock.readLock().unlock();

      // Draw our position
      Minecraft mc = Minecraft.getMinecraft();
      ChunkPos us = new ChunkPos((int)mc.player.posX >> 4, (int)mc.player.posZ >> 4);
      int x = 50 + (us.x - last.x);
      int z = 50 + (us.z - last.z);
      if(z < 0) z = -1;
      else if(z > 100) z = 101;
      if(x < 0) x = -1;
      else if(x > 100) x = 101;
      RadarInterface.drawRect(start_x + (float)x, start_z + (float)z, start_x + (float)x + 1f, start_z + (float)z + 1f, 0f, 0.8f, 0f);
      GlStateManager.popMatrix();
    }

    // Update scroll
    if(this.scroll.clicked) {
      this.scroll.dragged(mouseX, mouseY);
    }

    // Draw buttons & labels
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  protected void actionPerformed(GuiButton btn) throws IOException {
    if(btn instanceof ActionButton) {
      ((ActionButton) btn).onClick(this);
    }
  }

  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if(keyCode == Keyboard.KEY_ESCAPE) {
      this.mc.displayGuiScreen(null);
      if (this.mc.currentScreen == null) {
        this.mc.setIngameFocus();
      }
    }
  }

  public boolean doesGuiPauseGame() {
	  return false;
	}
}
