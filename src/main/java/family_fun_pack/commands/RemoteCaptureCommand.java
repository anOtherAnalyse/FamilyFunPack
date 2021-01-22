package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import java.lang.System;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;
import family_fun_pack.utils.WorldCapture;

/* Remote world capture - slow */

@SideOnly(Side.CLIENT)
public class RemoteCaptureCommand extends Command implements PacketListener {

  private static final int WINDOWS_SIZE = 9;
  private static final int BURST_SIZE = 3;
  private static final int RE_SEND_TIME = 2000;

  private WorldCapture capture;

  private Map<BlockPos, Long> window;
  private ReadWriteLock window_lock;

  private List<ChunkPos> chunks;
  private Chunk current;
  private ReadWriteLock current_lock;

  private int index;
  private int max_index;
  private int start_index;

  public RemoteCaptureCommand() {
    super("capture");
    this.window = new HashMap<BlockPos, Long>();
    this.window_lock = new ReentrantReadWriteLock();
    this.chunks = new LinkedList<ChunkPos>();
    this.current_lock = new ReentrantReadWriteLock();
  }

  public String usage() {
    return this.getName() + " off | <save_name> <corner_x> <corner_z> <width_x> <width_z> [half | surface]";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();

    this.current_lock.readLock().lock();
    if(this.current != null) {

      if(args.length > 1 && args[1].equals("off")) {

        this.window_lock.writeLock().lock();
        this.onStop();
        this.window_lock.writeLock().unlock();

        this.current_lock.readLock().unlock();

        return "Capture was aborted";
      }

      this.current_lock.readLock().unlock();
      return String.format("Chunk [%d, %d] at %d / %d, %d chunks left", this.current.x, this.current.z, this.index, this.max_index, this.chunks.size() + 1);
    }
    this.current_lock.readLock().unlock();

    if(args.length > 4) {

      if(! FamilyFunPack.getNetworkHandler().isConnected()) return "This only works on servers";

      try {
        int x = Integer.parseInt(args[2]);
        int z = Integer.parseInt(args[3]);
        int wx = Integer.parseInt(args[4]);
        int wz = Integer.parseInt(args[5]);

        if(wx <= 0 || wx > 12 || wz <= 0 || wz > 12) return "Width too big or invalid";

        this.max_index = 32768;
        this.start_index = 0;
        if(args.length > 6) {
          if(args[6].equals("half")) this.max_index = 16384;
          else if(args[6].equals("surface")) {
            this.start_index = 6400;
            this.max_index = 12800;
          }
          else return this.getUsage();
        }

        this.capture = new WorldCapture(args[1], mc.world.provider.getDimensionType(), new BlockPos(x << 4, 256, z << 4));
        this.chunks.clear();
        this.window.clear();
        this.index = this.start_index;

        for(int i = x; i < x + wx; i ++) {
          for(int j = z; j < z + wz; j ++) {
            this.chunks.add(new ChunkPos(i, j));
          }
        }

        this.current = this.getNextChunk();

        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 11);
        MinecraftForge.EVENT_BUS.register(this);

        return "Starting to capture..";
      } catch(NumberFormatException e) {
        return this.getUsage();
      }
    }
    return this.getUsage();
  }

  // Send request to get blocks states
  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {

    if(! FamilyFunPack.getNetworkHandler().isConnected()) return;

    long time = System.currentTimeMillis();
    int count = 0;

    // Re-send request that were not fulfilled by server
    this.window_lock.writeLock().lock();
    for(BlockPos position : this.window.keySet()) {
      long sent_time = this.window.get(position).longValue();

      if(time - sent_time >= RemoteCaptureCommand.RE_SEND_TIME) {
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(position, EnumFacing.UP, EnumHand.MAIN_HAND, 0f, 0f, 0f));

        this.window.put(position, time);
        if(++count >= RemoteCaptureCommand.BURST_SIZE) {
          this.window_lock.writeLock().unlock();
          return;
        }
      }
    }
    this.window_lock.writeLock().unlock();

    this.window_lock.readLock().lock();
    int size = this.window.size();
    this.window_lock.readLock().unlock();

    // Chunk is full, record it
    if(size == 0 && this.index >= this.max_index) {

      this.current_lock.writeLock().lock();

      // Record
      this.capture.captureChunk(this.current);

      // Get next chunk
      this.current = this.getNextChunk();
      this.index = this.start_index;
      if(this.current == null) {
        FamilyFunPack.printMessage("Capture finished, saved under name: " + this.capture.getName());
        this.onStop();
      } else FamilyFunPack.printMessage(String.format("Chunk [%d, %d] captured, %d left", this.current.x, this.current.z, this.chunks.size() + 1));

      this.current_lock.writeLock().unlock();

      return;
    }

    // Send new requests
    while(count < RemoteCaptureCommand.BURST_SIZE && size < RemoteCaptureCommand.WINDOWS_SIZE && this.index < this.max_index) {
      BlockPos position = this.IndexToCoords(this.index).add(this.current.x << 4, 0, this.current.z << 4);

      this.window_lock.writeLock().lock();
      this.window.put(position, time);
      this.window_lock.writeLock().unlock();

      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(position, EnumFacing.UP, EnumHand.MAIN_HAND, 0f, 0f, 0f));

      this.index ++;
      size ++;
      count ++;
    }
  }

  // Receive blocks states
  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketBlockChange change = (SPacketBlockChange) packet;

    BlockPos position = change.getBlockPosition();

    this.current_lock.writeLock().lock();

    if(this.current != null && (position.getX() >> 4) == this.current.x && (position.getZ() >> 4) == this.current.z) {
      this.current.setBlockState(position, change.getBlockState());

      this.window_lock.writeLock().lock();
      this.window.remove(position);
      this.window_lock.writeLock().unlock();

      packet = null;
    }

    this.current_lock.writeLock().unlock();

    return packet;
  }

  public void onDisconnect() {
    this.window_lock.writeLock().lock();
    for(BlockPos position : this.window.keySet()) {
      int index = this.coordsToIndex(position);
      if(this.index > index) this.index = index;
    }
    this.window.clear();
    this.window_lock.writeLock().unlock();
  }

  public void onStop() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 11);
    MinecraftForge.EVENT_BUS.unregister(this);
    this.capture = null;
    this.chunks.clear();
    this.window.clear();
    this.index = 0;
    this.current = null;
  }

  public Chunk getNextChunk() {
    if(this.chunks.size() > 0) {
      ChunkPos next = this.chunks.remove(0);
      Chunk chunk = new Chunk(this.capture.getWorld(), next.x, next.z);
      this.capture.getWorld().setChunk(chunk);
      return chunk;
    }
    return null;
  }

  public BlockPos IndexToCoords(int index) {
    return new BlockPos((index & 15), (index >> 8) * 2, ((index >> 4) & 15));
  }

  public int coordsToIndex(BlockPos position) {
    return (position.getX() & 15) | ((position.getZ() & 15) << 4) | (position.getY() << 8);
  }
}
