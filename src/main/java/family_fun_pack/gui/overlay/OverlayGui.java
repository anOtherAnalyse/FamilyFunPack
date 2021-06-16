package family_fun_pack.gui.overlay;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import family_fun_pack.gui.MainGui;

/* Ingame overlay, just labels of current states */

@OnlyIn(Dist.CLIENT)
public class OverlayGui extends AbstractGui {

  private static final int BORDER = 0xff000000;
  private static final float OVERLAY_SCALE = 0.7f;
  private static int counter = 0;

  private FontRenderer font;

  private Map<Integer, String> labels;
  private ReadWriteLock labels_lock;

  private int height;

  public OverlayGui() {
    this.font = Minecraft.getInstance().font;
    this.labels = new HashMap<Integer, String>();
    this.labels_lock = new ReentrantReadWriteLock();
    this.height = this.font.lineHeight + 4;
    this.setBlitOffset(1);
  }

  public void render(MatrixStack mStack) {
    int y = 4;
    this.labels_lock.readLock().lock();
    for(String l : this.labels.values()) {
      int width = this.font.width(l) + 4;
      int x_end = 4 + width;
      int y_end = y + this.height;

      RenderSystem.pushMatrix();
      RenderSystem.scalef(OverlayGui.OVERLAY_SCALE, OverlayGui.OVERLAY_SCALE, OverlayGui.OVERLAY_SCALE);

      AbstractGui.fill(mStack, 4, y, x_end, y_end, MainGui.BACKGROUND_COLOR);
      AbstractGui.fill(mStack, 4, y, x_end, y + 1, OverlayGui.BORDER);
      AbstractGui.fill(mStack, 4, y, 5, y_end, OverlayGui.BORDER);
      AbstractGui.fill(mStack, 4, y_end - 1, x_end, y_end, OverlayGui.BORDER);
      AbstractGui.fill(mStack, x_end - 1, y, x_end, y_end, OverlayGui.BORDER);
      this.font.draw(mStack, l, 6, y + 2, 0xffffffff);

      RenderSystem.popMatrix();

      y = y_end + 2;
    }
    this.labels_lock.readLock().unlock();
  }

  @SubscribeEvent
  public void drawOverlay(RenderGameOverlayEvent.Text event) {
    this.render(event.getMatrixStack());
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
