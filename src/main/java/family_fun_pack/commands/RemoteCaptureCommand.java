package family_fun_pack.commands;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUpdateSign;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import java.lang.System;
import java.util.Iterator;
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

  private boolean getting_signs;
  private short retry_count;

  private int index;
  private int max_index;
  private int start_index;

  private int label_id;

  public RemoteCaptureCommand() {
    super("capture");
    this.window = new HashMap<BlockPos, Long>();
    this.window_lock = new ReentrantReadWriteLock();
    this.chunks = new LinkedList<ChunkPos>();
    this.current_lock = new ReentrantReadWriteLock();
    this.label_id = -1;
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

      String advance = String.format("Chunk [%d, %d] at %d / %d, %d chunks left", this.current.x, this.current.z, this.index, this.max_index, this.chunks.size() + 1);

      this.current_lock.readLock().unlock();
      return advance;
    }
    this.current_lock.readLock().unlock();

    int x, z, wx, wz;
    String name;
    RemoteCaptureCommand.Mode mode = RemoteCaptureCommand.Mode.FULL;

    if(args.length > 1 && args[1].equals("config")) {
      Configuration configuration = FamilyFunPack.getModules().getConfiguration();
    	configuration.load(); // re-load config to allow changes while playing
    	if(!configuration.hasCategory(this.getName())) {
         configuration.get(this.getName(), "target_x", 0);
         configuration.get(this.getName(), "target_z", 0);
         configuration.get(this.getName(), "width_x", 1);
         configuration.get(this.getName(), "width_z", 1);
         configuration.get(this.getName(), "name", "example");
         configuration.get(this.getName(), "mode", "half");
         configuration.save();
    	   return "Config for capture command was created";
    	}

    	x = configuration.get(this.getName(), "target_x", 0).getInt();
      z = configuration.get(this.getName(), "target_z", 0).getInt();
      wx = configuration.get(this.getName(), "width_x", 1).getInt();
      wz = configuration.get(this.getName(), "width_z", 1).getInt();
      name = configuration.get(this.getName(), "name", "example").getString();

      try {
        mode = RemoteCaptureCommand.Mode.valueOf(configuration.get(this.getName(), "mode", "half").getString().toUpperCase());
      } catch (IllegalArgumentException e) {
        return "Invalid capture mode was specified in configuration";
      }

    } else if(args.length > 5) {
      try {
        name = args[1];
        x = Integer.parseInt(args[2]);
        z = Integer.parseInt(args[3]);
        wx = Integer.parseInt(args[4]);
        wz = Integer.parseInt(args[5]);
        if(args.length > 6) mode = RemoteCaptureCommand.Mode.valueOf(args[6].toUpperCase());
      } catch(NumberFormatException e) {
        return this.getUsage();
      } catch (IllegalArgumentException e) {
        return this.getUsage();
      }
    } else return this.getUsage();

    if(wx <= 0 || wx > 12 || wz <= 0 || wz > 12) return "Width too big or invalid";

    switch(mode) {
      case FULL:
        this.max_index = 32768;
        this.start_index = 0;
        break;
      case HALF:
        this.max_index = 16384;
        this.start_index = 0;
        break;
      case SURFACE:
        this.max_index = 12800;
        this.start_index = 6400;
        break;
    }

    this.capture = new WorldCapture(name, mc.world.provider.getDimensionType(), new BlockPos(x << 4, 256, z << 4));
    this.chunks.clear();
    this.window.clear();
    this.getting_signs = false;
    this.retry_count = 2;
    this.index = this.start_index;

    for(int i = x; i < x + wx; i ++) {
      for(int j = z; j < z + wz; j ++) {
        this.chunks.add(new ChunkPos(i, j));
      }
    }

    this.current = this.getNextChunk();

    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 9, 11);
    MinecraftForge.EVENT_BUS.register(this);

    return "Starting to capture..";
	 }

  // Send request to get blocks states
  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {

    if(! FamilyFunPack.getNetworkHandler().isConnected()) return;

    long time = System.currentTimeMillis();
    int count = 0;

    if(this.getting_signs) { // Current phase is retrieving sign data

      boolean has_sign = false;

      this.current_lock.readLock().lock();
      this.window_lock.writeLock().lock();
      Iterator iterator = this.window.keySet().iterator();
      while(iterator.hasNext()) {
        BlockPos position = (BlockPos) iterator.next();
        Block block = this.current.getBlockState(position).getBlock();
        if(block instanceof BlockStandingSign || block instanceof BlockWallSign) {
          if(time - this.window.get(position).longValue() >= RemoteCaptureCommand.RE_SEND_TIME) {
            if(this.retry_count > 0) {
              FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(position, EnumFacing.UP, EnumHand.MAIN_HAND, 0f, 0f, 0f)); // Be sure that chunk is loaded
              FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUpdateSign(position, new ITextComponent[] {new TextComponentString(""), new TextComponentString(""), new TextComponentString(""), new TextComponentString("")}));
              this.window.put(position, time);
              this.retry_count -= 1;
            } else {
              iterator.remove();
              this.retry_count = 2;
              continue;
            }
          }
          has_sign = true;
          break;
        }
      }

      if(! has_sign) {
        this.getting_signs = false;
      }

      this.window_lock.writeLock().unlock();
      this.current_lock.readLock().unlock();
    } else { // Current phase is retrieving block data
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
  }

  // Receive blocks states
  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    if(id == 11) { // SPacketBlockChange
      SPacketBlockChange change = (SPacketBlockChange) packet;

      BlockPos position = change.getBlockPosition();

      this.current_lock.writeLock().lock();
      if(this.current != null && (position.getX() >> 4) == this.current.x && (position.getZ() >> 4) == this.current.z) {
        this.current.setBlockState(position, change.getBlockState());

        Block block = change.getBlockState().getBlock();
        this.window_lock.writeLock().lock();
        if(block instanceof BlockStandingSign || block instanceof BlockWallSign) {
          this.getting_signs = true;
          this.window.put(position, 0l);
        } else {
          this.window.remove(position);
        }
        this.window_lock.writeLock().unlock();

        this.updateStatusLabel();

        packet = null;
      }
      this.current_lock.writeLock().unlock();
    } else { // SPacketUpdateTileEntity
      SPacketUpdateTileEntity update = (SPacketUpdateTileEntity) packet;

      BlockPos position = update.getPos();

      this.current_lock.writeLock().lock();
      if(this.current != null && (position.getX() >> 4) == this.current.x && (position.getZ() >> 4) == this.current.z) {
        if(update.getTileEntityType() == 9) { // TileEntitySign
          TileEntity tile = this.current.getTileEntity(position, Chunk.EnumCreateEntityType.IMMEDIATE);
          if(tile instanceof TileEntitySign) {
            tile.readFromNBT(update.getNbtCompound());
            this.window_lock.writeLock().lock();
            this.window.remove(position);
            this.window_lock.writeLock().unlock();
            this.retry_count = 2;
          }
        }
        packet = null;
      }
      this.current_lock.writeLock().unlock();
    }
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
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 9, 11);
    MinecraftForge.EVENT_BUS.unregister(this);
    this.capture = null;
    this.chunks.clear();
    this.window.clear();
    this.index = 0;
    this.current = null;
    if(this.label_id >= 0) FamilyFunPack.getOverlay().removeLabel(this.label_id);
    this.label_id = -1;
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

  private void updateStatusLabel() {
    int chunks_count = this.capture.getRecordedCount();
    float counter = (float)(this.index - this.start_index) * 100f / (float)(this.max_index - this.start_index);
    String label = String.format("Capture [%d/%d], current %.2f%%", chunks_count, chunks_count + this.chunks.size() + 1, counter);

    if(this.label_id >= 0) FamilyFunPack.getOverlay().modifyLabel(this.label_id, label);
    else this.label_id = FamilyFunPack.getOverlay().addLabel(label);
  }

  public static enum Mode {FULL, HALF, SURFACE};
}
