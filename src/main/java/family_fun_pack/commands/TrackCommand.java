package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.interfaces.RadarInterface;
import family_fun_pack.network.PacketListener;

/* Brute-force search for player in given area - Paper only */

@SideOnly(Side.CLIENT)
public class TrackCommand extends Command implements PacketListener {

  // Scan mode parameters
  private static final int WINDOW_SIZE = 9;
  private static final int BURST_SIZE = 3;
  private static final long RETRY_TIME = 2000;

  // Track mode parameters
  private static final long REFRESH_TIME = 500;
  private static final int SAVED_TRACKED_POSITIONS = 16;
  private static final int LOGS_MAX_SIZE = 64;

  // Server dependent, have to be odd
  private static final int PLAYER_RENDER_DISTANCE = 7; // 9b render distance

  public ChunkPos corner;
  public int width_x, width_z;
  public int render_radius; // have to be odd
  public int current;

  private long last_sent;

  private int window;
  private ReadWriteLock window_lock;

  public LinkedList<ChunkPos> loaded;
  public ReadWriteLock loaded_lock;

  public List<String> logs;
  public ReadWriteLock logs_lock;

  private boolean enabled, received, target_lost;

  /* Operating mode */
  private Mode mode;
  public Mode effective_mode;
  private ReadWriteLock mode_lock;

  private ScanParameters save;

  private int label_id;

  public TrackCommand() {
    super("scan");
    this.window_lock = new ReentrantReadWriteLock();
    this.loaded_lock = new ReentrantReadWriteLock();
    this.logs_lock = new ReentrantReadWriteLock();
    this.mode_lock = new ReentrantReadWriteLock();
    this.loaded = new LinkedList<ChunkPos>();
    this.logs = new LinkedList<String>();
    this.enabled = false;
    this.label_id = -1;
  }

  public String usage() {
    return this.getName() + " [<center_x> <center_z>] <radius> [mode] | off";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();

    if(this.enabled) {
      if(args.length > 1 && args[1].equals("off")) {
        this.onDisconnect();
        return "Scan stopped";
      }
      MinecraftForge.EVENT_BUS.register(new GuiOpener(new RadarInterface(this)));
      return null;
    }

    if(args.length > 1) {

      int count = 0;
      int[] integers = new int[4];
      try {
        for(int i = 1; i < args.length && i <= 4; i ++) {
          integers[i - 1] = Integer.parseInt(args[i]);
          count ++;
        }
      } catch(NumberFormatException e) {}

      this.render_radius = TrackCommand.PLAYER_RENDER_DISTANCE; // Check 1 chunk every 7 chunks (9b render distance)

      int rx, rz, cx, cz;
      switch(count) {
        case 0: return this.getUsage();
        case 1: // Square scan centered on player
          cx = (int)mc.player.posX >> 4;
          cz = (int)mc.player.posZ >> 4;
          rx = integers[0];
          rz = integers[0];
          break;
        case 2: // Rectangle scan centered on player
          cx = (int)mc.player.posX >> 4;
          cz = (int)mc.player.posZ >> 4;
          rx = integers[0];
          rz = integers[1];
          break;
        case 3: // Square scan
          cx = integers[0];
          cz = integers[1];
          rx = integers[2];
          rz = integers[2];
          break;
        default: // Rectangle scan
          cx = integers[0];
          cz = integers[1];
          rx = integers[2];
          rz = integers[3];
          break;
      }

      try {
        this.mode = Mode.SCAN;
        if(args.length > ++count) {
          this.mode = Mode.valueOf(args[count].toUpperCase());
        }
        this.effective_mode = Mode.SCAN;
      } catch (IllegalArgumentException e) {
        return this.getUsage();
      }

      if(rx <= 0 || rz <= 0) return "Invalid radius";

      // Radius in chunks
      int rcx = (int)Math.ceil((double)rx / 16d) - (this.render_radius / 2);
      if(rcx < 0) rcx = 0;
      int rcz = (int)Math.ceil((double)rz / 16d) - (this.render_radius / 2);
      if(rcz < 0) rcz = 0;

      // Effective radius, in player render distance square
      int erx = (int)Math.ceil((double)rcx / (double)this.render_radius);
      int erz = (int)Math.ceil((double)rcz / (double)this.render_radius);

      this.width_x = (erx * 2) + 1;
      this.width_z = (erz * 2) + 1;
      this.corner = new ChunkPos(cx - (erx * this.render_radius), cz - (erz * this.render_radius));

      this.window = 0;
      this.current = 0;
      this.loaded.clear();
      this.logs.clear();
      this.enabled = true;
      this.target_lost = false;
      this.save = null;

      this.label_id = FamilyFunPack.getOverlay().addLabel("Scan: 0%");

      FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 11);
      MinecraftForge.EVENT_BUS.register(this);

      // Open gui
      MinecraftForge.EVENT_BUS.register(new GuiOpener(new RadarInterface(this)));

      return null;
    } else if(this.corner != null) { // Scan data exists, display it
      MinecraftForge.EVENT_BUS.register(new GuiOpener(new RadarInterface(this)));
      return null;
    }
    return this.getUsage();
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketBlockChange change = (SPacketBlockChange) packet;

    BlockPos position = change.getBlockPosition();
    ChunkPos chunk = new ChunkPos(position.getX() >> 4, position.getZ() >> 4);

    this.mode_lock.writeLock().lock();
    if(this.effective_mode == Mode.TRACK) { // Track player

      this.loaded_lock.readLock().lock();
      ChunkPos latest = this.loaded.getLast(); // Latest position
      this.loaded_lock.readLock().unlock();

      if(position.getY() == -63) { // etalon received
        if(! this.received) { // Go to scan mode
          this.window_lock.writeLock().lock();
          this.window = 0;
          this.window_lock.writeLock().unlock();

          this.render_radius = 2;

          this.effective_mode = Mode.TRACK_SCAN;
          this.current = 0;
          this.width_x = 17;
          this.width_z = 17;
          this.corner = new ChunkPos(latest.x - 16, latest.z - 16);
        }
        packet = null;
      } else if(position.getY() == -31 && latest.x == chunk.x && latest.z == chunk.z) {
        this.received = true;
        packet = null;
      }
    } else { // Area scan
      if(position.getY() == -64) {

        if(this.effective_mode == Mode.SCAN) {
          float advancement = (float)this.current * 100f / (float)(this.width_x * this.width_z);
          FamilyFunPack.getOverlay().modifyLabel(this.label_id, String.format("Scan: %.2f%%", advancement));
        }

        if(this.current >= this.width_x * this.width_z) {
          if(this.effective_mode == Mode.TRACK_SCAN) {
            this.loaded_lock.readLock().lock();
            ChunkPos latest = this.loaded.getLast(); // Latest position
            this.loaded_lock.readLock().unlock();

            if(! this.target_lost) {
              this.addLog(String.format("%sTarget lost around chunk [%d, %d]%s", TextFormatting.RED, latest.x, latest.z, TextFormatting.WHITE));
              FamilyFunPack.getOverlay().modifyLabel(this.label_id, String.format("%sTarget lost", TextFormatting.RED));
            }

            this.target_lost = true;
            this.current = 0;
          } else this.onStop();
        }

        this.window_lock.writeLock().lock();
        this.window = 0;
        this.window_lock.writeLock().unlock();
        packet = null;
      } else if(position.getY() == -32 && chunk.x >= this.corner.x && chunk.x < (this.corner.x + (this.width_x * this.render_radius)) && chunk.z >= this.corner.z && chunk.z < (this.corner.z + (this.width_z * this.render_radius))) {

        Minecraft mc = Minecraft.getMinecraft();
        boolean player_chunk = this.inArea(new ChunkPos((int)mc.player.posX >> 4, (int)mc.player.posZ >> 4), chunk, (TrackCommand.PLAYER_RENDER_DISTANCE / 2)); // Is the chunk inside our render distance

        this.loaded_lock.writeLock().lock();
        if(this.loaded != null) this.loaded.add(chunk);
        this.loaded_lock.writeLock().unlock();

        if(this.mode == Mode.SCAN || (this.effective_mode == Mode.SCAN && (this.inArea(new ChunkPos(0, 0), chunk, 16) || player_chunk))) {

          this.addLog(String.format("chunk %s[%d, %d]%s loaded", TextFormatting.BLUE, chunk.x, chunk.z, TextFormatting.WHITE));

        } else if(! player_chunk) { // Avoid our position

          if(this.effective_mode == Mode.SCAN) { // Prepare transition from scan to track
            // Save scanning config, to be able to go back to scan later
            this.save = new ScanParameters(this.corner, this.width_x, this.width_z, this.render_radius, this.current);

            // Save scanned loaded nodes
            this.loaded_lock.writeLock().lock();
            this.save.loaded = this.loaded;
            this.loaded = new LinkedList<ChunkPos>();
            this.loaded.add(chunk);
            this.loaded_lock.writeLock().unlock();
          }

          if(this.target_lost) {
            this.addLog(String.format("%sTarget is back!%s", TextFormatting.GREEN, TextFormatting.WHITE));
            this.target_lost = false;
          }

          this.addLog(String.format("Target around chunk %s[%d, %d]%s", TextFormatting.BLUE, chunk.x, chunk.z, TextFormatting.WHITE));
          FamilyFunPack.getOverlay().modifyLabel(this.label_id, String.format("Target around %s[%d, %d]", TextFormatting.BLUE, chunk.x << 4, chunk.z << 4));

          this.loaded_lock.writeLock().lock();
          if(this.loaded.size() > TrackCommand.SAVED_TRACKED_POSITIONS) {
            this.loaded.remove(0);
          }
          this.loaded_lock.writeLock().unlock();

          this.effective_mode = Mode.TRACK;
          this.last_sent = 0l;
        }

        packet = null;
      }
    }
    this.mode_lock.writeLock().unlock();

    return packet;
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {

    Minecraft mc = Minecraft.getMinecraft();
    BlockPos etalon = new BlockPos((int)mc.player.posX, -64, (int)mc.player.posZ);

    if(this.effective_mode == Mode.TRACK) { // follow a player
      long time = System.currentTimeMillis();
      if(time - this.last_sent >= TrackCommand.REFRESH_TIME) {
        this.last_sent = time;
        this.loaded_lock.readLock().lock();
        ChunkPos chunk = this.loaded.getLast();
        this.loaded_lock.readLock().unlock();
        this.received = false;
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, new BlockPos(chunk.x << 4, -31, chunk.z << 4), EnumFacing.UP));
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, etalon.add(0, 1, 0), EnumFacing.UP)); // etalon
      }
    } else { // Scan an area
      int count = 0;

      this.window_lock.writeLock().lock();

      // Tiemout
      if(this.window >= TrackCommand.WINDOW_SIZE && System.currentTimeMillis() - this.last_sent >= TrackCommand.RETRY_TIME) {
        this.window = 0;
        this.current -= (TrackCommand.WINDOW_SIZE - 1);
        this.addLog(TextFormatting.YELLOW + "Request timed out");
      }

      // Send requests
      while(count < TrackCommand.BURST_SIZE && this.window < TrackCommand.WINDOW_SIZE - 1) {
        BlockPos position = this.getNext();
        if(position != null) {
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, position, EnumFacing.UP));
          count ++;
          this.window ++;
        } else {
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, etalon, EnumFacing.UP));
          this.last_sent = System.currentTimeMillis();
          this.window = TrackCommand.WINDOW_SIZE;
          break;
        }
      }

      // Send etalon
      if(count < TrackCommand.BURST_SIZE && this.window == TrackCommand.WINDOW_SIZE - 1) {
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, etalon, EnumFacing.UP));
        this.last_sent = System.currentTimeMillis();
        this.window ++;
      }
      this.window_lock.writeLock().unlock();
    }
  }

  public void onDisconnect() {
    this.onStop();
    this.loaded_lock.writeLock().lock();
    this.loaded.clear();
    this.corner = null;
    this.loaded_lock.writeLock().unlock();

    this.logs_lock.writeLock().lock();
    this.logs.clear();
    this.logs_lock.writeLock().unlock();
  }

  // Scan stopped
  public void onStop() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 11);
    MinecraftForge.EVENT_BUS.unregister(this);
    this.enabled = false;
    this.save = null;
    if(this.label_id >= 0) FamilyFunPack.getOverlay().removeLabel(this.label_id);
  }

  // Next blockPos to request
  private BlockPos getNext() {
    if(this.current < this.width_x * this.width_z) {
      int x = (corner.x + ((this.current % this.width_x) * this.render_radius)) << 4;
      int z = (corner.z + ((this.current / this.width_x) * this.render_radius)) << 4;
      this.current ++;
      return new BlockPos(x, -32, z);
    }
    return null;
  }

  // Get chunk relative position to corner chunk
  public ChunkPos getRelativePos(ChunkPos chunk) {
    int diff_x = chunk.x - this.corner.x + (this.render_radius / 2);
    int diff_z = chunk.z - this.corner.z + (this.render_radius / 2);
    return new ChunkPos(diff_x / this.render_radius, diff_z / this.render_radius);
  }

  // Is a chunk in given area
  private boolean inArea(ChunkPos center, ChunkPos target, int radius) {
    int x = center.x - target.x;
    int z = center.z - target.z;
    return (x <= radius && x >= -radius && z <= radius && z >= -radius);
  }

  // Add line to logs
  private void addLog(String log) {
    this.logs_lock.writeLock().lock();
    if(this.logs != null) {
      this.logs.add(log);
      if(this.logs.size() > TrackCommand.LOGS_MAX_SIZE) this.logs.remove(0);
    }
    this.logs_lock.writeLock().unlock();
  }

  // Go back to scanning mode
  public void backToScan() {
    if(this.effective_mode != Mode.SCAN) {
      this.mode_lock.writeLock().lock();

      this.window_lock.writeLock().lock();
      this.window = 0;
      this.window_lock.writeLock().unlock();

      this.save.loadConfig(this);

      this.effective_mode = Mode.SCAN;

      this.mode_lock.writeLock().unlock();
    }
  }

  // Operating mode
  public static enum Mode {SCAN, TRACK, TRACK_SCAN};

  // Forge event listener used to open radar GUI
  private static class GuiOpener {

    private RadarInterface gui;

    public GuiOpener(RadarInterface gui) {
      this.gui = gui;
    }

    @SubscribeEvent
    public void onGuiOpened(GuiOpenEvent event) {
      if(event.getGui() == null) {
        event.setGui(this.gui);
      }
      MinecraftForge.EVENT_BUS.unregister(this);
    }
  }

  // Save of a scan config
  private static class ScanParameters {
    public ChunkPos corner;
    public int width_x, width_z;
    public int render_radius;
    public int current;
    public LinkedList<ChunkPos> loaded;

    public ScanParameters(ChunkPos corner, int width_x, int width_z, int render_radius, int current) {
      this.corner = corner;
      this.width_x = width_x;
      this.width_z = width_z;
      this.render_radius = render_radius;
      this.current = current;
    }

    public void loadConfig(TrackCommand parent) {
      parent.corner = this.corner;
      parent.width_x = this.width_x;
      parent.width_z = this.width_z;
      parent.render_radius = this.render_radius;
      parent.current = this.current;

      parent.loaded_lock.writeLock().lock();
      parent.loaded = this.loaded;
      parent.loaded_lock.writeLock().unlock();
    }
  }
}
