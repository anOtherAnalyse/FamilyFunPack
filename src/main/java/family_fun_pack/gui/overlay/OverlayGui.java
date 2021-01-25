package family_fun_pack.gui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import family_fun_pack.gui.MainGui;

/* In game overlay, just labels of current states */

@SideOnly(Side.CLIENT)
public class OverlayGui extends Gui {

  private static final int BORDER = 0xff000000;
  private static int counter = 0;

  private FontRenderer fontRenderer;

  private Map<Integer, String> labels;
  private ReadWriteLock labels_lock;

  private int height;

  public OverlayGui() {
    this.zLevel = 1;
    this.fontRenderer = Minecraft.getMinecraft().fontRenderer;
    this.labels = new HashMap<Integer, String>();
    this.labels_lock = new ReentrantReadWriteLock();
    this.height = this.fontRenderer.FONT_HEIGHT + 4;
  }

  public void drawOverlay() {
    int y = 4;
    this.labels_lock.readLock().lock();
    for(String l : this.labels.values()) {
      int width = this.fontRenderer.getStringWidth(l) + 4;
      int x_end = 4 + width;
      int y_end = y + this.height;

      this.drawRect(4, y, x_end, y_end, MainGui.BACKGROUND_COLOR);
      this.drawRect(4, y, x_end, y + 1, OverlayGui.BORDER);
      this.drawRect(4, y, 5, y_end, OverlayGui.BORDER);
      this.drawRect(4, y_end - 1, x_end, y_end, OverlayGui.BORDER);
      this.drawRect(x_end - 1, y, x_end, y_end, OverlayGui.BORDER);
      this.drawString(this.fontRenderer, l, 6, y + 2, 0xffffffff);

      y = y_end + 2;
    }
    this.labels_lock.readLock().unlock();
  }

  @SubscribeEvent
  public void drawOverlay(RenderGameOverlayEvent.Text event) {
    this.drawOverlay();
  }

  public int addLabel(String label) {
    OverlayGui.counter++;
    if(OverlayGui.counter < 0) OverlayGui.counter = 0;
    int id = OverlayGui.counter;
    this.labels_lock.writeLock().lock();
    this.labels.put(id, label);
    this.labels_lock.writeLock().unlock();
    return id;
  }

  public void removeLabel(int id) {
    this.labels_lock.writeLock().lock();
    this.labels.remove(id);
    this.labels_lock.writeLock().unlock();
  }

  public void modifyLabel(int id, String label) {
    this.labels_lock.writeLock().lock();
    this.labels.put(id, label);
    this.labels_lock.writeLock().unlock();
  }

}
