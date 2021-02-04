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

  private static final int WINDOW_SIZE = 9;
  private static final int BURST_SIZE = 3;
  private static final long RETRY_TIME = 2000;

  public ChunkPos corner;
  public int width_x, width_z;
  public int render_radius; // have to be odd
  public int current;

  private long last_sent;

  private int window;
  private ReadWriteLock window_lock;

  public List<ChunkPos> loaded;
  public ReadWriteLock loaded_lock;

  public List<String> logs;
  public ReadWriteLock logs_lock;

  public TrackCommand() {
    super("scan");
    this.window_lock = new ReentrantReadWriteLock();
    this.loaded_lock = new ReentrantReadWriteLock();
    this.logs_lock = new ReentrantReadWriteLock();
    this.corner = null;
  }

  public String usage() {
    return this.getName() + " <center_x> <center_z> <radius>";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();

    if(this.corner != null) {
      MinecraftForge.EVENT_BUS.register(new GuiOpener(new RadarInterface(this)));
      return null;
    }

    if(args.length > 3) {
      try {
        int x = Integer.parseInt(args[1]); // Chunk pos
        int z = Integer.parseInt(args[2]);
        int radius = Integer.parseInt(args[3]); // Radius in block

        if(radius <= 0) return "Invalid radius";

        this.render_radius = 7; // Check 1 chunk every 7 chunks (9b render distance)

        int radius_chunk = ((int)Math.ceil((double)radius / 16d)) - (this.render_radius / 2); // radius in chunk, minus the chunks within center render radius
        if(radius_chunk < 0) radius_chunk = 0;
        int effective_radius = (int)Math.ceil((double)radius_chunk / (double) this.render_radius); // radius in terms of areas to check (area of the size of a player render distance)

        this.width_x = (effective_radius * 2) + 1;
        this.width_z = (effective_radius * 2) + 1;
        this.corner = new ChunkPos(x - (effective_radius * this.render_radius), z - (effective_radius * this.render_radius));

        this.window = 0;
        this.current = 0;
        this.loaded = new LinkedList<ChunkPos>();
        this.logs = new LinkedList<String>();

        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 11);
        MinecraftForge.EVENT_BUS.register(this);

        // Open gui
        MinecraftForge.EVENT_BUS.register(new GuiOpener(new RadarInterface(this)));

        return null;
      } catch(NumberFormatException e) {
        return this.getUsage();
      }
    }
    return this.getUsage();
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketBlockChange change = (SPacketBlockChange) packet;

    BlockPos position = change.getBlockPosition();
    ChunkPos chunk = new ChunkPos(position.getX() >> 4, position.getZ() >> 4);
    if(position.getX() == 0 && position.getY() == 250 && position.getZ() == 0) {
      if(this.current >= this.width_x * this.width_z) this.onStop();
      this.window_lock.writeLock().lock();
      this.window = 0;
      this.window_lock.writeLock().unlock();
      packet = null;
    } else if(chunk.x >= this.corner.x && chunk.x < (this.corner.x + (this.width_x * this.render_radius)) && chunk.z >= this.corner.z && chunk.z < (this.corner.z + (this.width_z * this.render_radius))) {

      this.loaded_lock.writeLock().lock();
      this.loaded.add(chunk);
      this.loaded_lock.writeLock().unlock();

      this.logs_lock.writeLock().lock();
      this.logs.add(String.format("chunk %s[%d, %d]%s loaded", TextFormatting.BLUE, chunk.x, chunk.z, TextFormatting.WHITE));
      this.logs_lock.writeLock().unlock();

      packet = null;
    }

    return packet;
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    int count = 0;

    this.window_lock.writeLock().lock();

    // Tiemout
    if(this.window >= TrackCommand.WINDOW_SIZE && System.currentTimeMillis() - this.last_sent >= TrackCommand.RETRY_TIME) {
      this.window = 0;
      this.current -= (TrackCommand.WINDOW_SIZE - 1);
      this.logs_lock.writeLock().lock();
      this.logs.add(TextFormatting.YELLOW + "Request timed out");
      this.logs_lock.writeLock().unlock();
    }

    // Send requests
    while(count < TrackCommand.BURST_SIZE && this.window < TrackCommand.WINDOW_SIZE - 1) {
      BlockPos position = this.getNext();
      if(position != null) {
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, position, EnumFacing.UP));
        count ++;
        this.window ++;
      } else {
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, new BlockPos(0, 250, 0), EnumFacing.UP));
        this.last_sent = System.currentTimeMillis();
        this.window = TrackCommand.WINDOW_SIZE;
        break;
      }
    }

    // Send etalon
    if(count < TrackCommand.BURST_SIZE && this.window == TrackCommand.WINDOW_SIZE - 1) {
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, new BlockPos(0, 250, 0), EnumFacing.UP));
      this.last_sent = System.currentTimeMillis();
      this.window ++;
    }
    this.window_lock.writeLock().unlock();
  }

  public void onDisconnect() {
    this.onStop();
    this.corner = null;
    this.loaded = null;
    this.logs = null;
  }

  public void onStop() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 11);
    MinecraftForge.EVENT_BUS.unregister(this);
  }

  private BlockPos getNext() {
    if(this.current < this.width_x * this.width_z) {
      int x = (corner.x + ((this.current % this.width_x) * this.render_radius)) << 4;
      int z = (corner.z + ((this.current / this.width_x) * this.render_radius)) << 4;
      this.current ++;
      return new BlockPos(x, 0, z);
    }
    return null;
  }

  public ChunkPos getRelativePos(ChunkPos chunk) {
    int diff_x = chunk.x - this.corner.x + (this.render_radius / 2);
    int diff_z = chunk.z - this.corner.z + (this.render_radius / 2);
    return new ChunkPos(diff_x / this.render_radius, diff_z / this.render_radius);
  }

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
}
