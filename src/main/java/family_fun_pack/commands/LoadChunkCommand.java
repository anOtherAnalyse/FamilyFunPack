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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import java.lang.System;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Keep a given chunk square loaded */

@SideOnly(Side.CLIENT)
public class LoadChunkCommand extends Command implements PacketListener {

  private static final int WINDOWS_SIZE = 9;
  private static final int BURST_SIZE = 3;
  private static final int RE_SEND_TIME = 2000;

  private Map<BlockPos, Long> window;
  private ReadWriteLock window_lock;

  private ChunkPos[] chunks;
  private int width;
  private int current;

  private int label_id;

  public LoadChunkCommand() {
    super("load");
    this.window_lock = new ReentrantReadWriteLock();
    this.window = new HashMap<BlockPos, Long>();
    this.label_id = -1;
  }

  public String usage() {
    return this.getName() + " <center_x> <center_y> [radius] | off";
  }

  public String execute(String[] args) {

    if(args.length > 1) {
      int x, z, radius;

      if(args[1].equals("off")) {
        this.onDisconnect();
	      return "Remote chunk loading is off";
      } else if(args[1].equals("config")) {

        Configuration configuration = FamilyFunPack.getModules().getConfiguration();
	    	configuration.load(); // re-load config to allow changes while playing
	    	if(!configuration.hasCategory(this.getName())) {
	           configuration.get(this.getName(), "target_x", 0);
	           configuration.get(this.getName(), "target_z", 0);
	           configuration.get(this.getName(), "radius", 2);
	           configuration.save();
	    	   return "Config for load command was created";
	    	}

        x = configuration.get(this.getName(), "target_x", 0).getInt();
        z = configuration.get(this.getName(), "target_z", 0).getInt();
        radius = configuration.get(this.getName(), "radius", 2).getInt();
      } else if(args.length > 2) {
        try {
          x = Integer.parseInt(args[1]);
	        z = Integer.parseInt(args[2]);

	        if(args.length > 3) radius = Integer.parseInt(args[3]);
          else radius = 2; // 2 allows the middle chunk to be updated (not lazy)
        } catch(NumberFormatException e) {
	        return this.getUsage();
	      }
      } else return this.getUsage();

      if(radius < 0) return "Invalid radius";

      if(this.chunks != null) return "Chunks are already kept loaded";

      this.width = (radius * 2) + 1;
      int index = 0;
      this.chunks = new ChunkPos[this.width * this.width];

      for(int i = x - radius; i <= x + radius; i ++) {
        for(int j = z - radius; j < z + radius; j ++) {
          this.chunks[index] = new ChunkPos(i, j);
          index += 1;
        }
      }

      for(int i = x - radius; i <= x + radius; i ++) {
        this.chunks[index] = new ChunkPos(i, z + radius);
        index += 1;
      }

      this.current = 0;
      this.window.clear();

      this.label_id = FamilyFunPack.getOverlay().addLabel("Remote loading: on");

      FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 11);
      MinecraftForge.EVENT_BUS.register(this);

      return String.format("Keeping loaded a square of radius %d around chunk [%d, %d], %d chunks", radius, x, z, this.width * this.width);
    }

    return this.getUsage();
	}

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {

    long time = System.currentTimeMillis();
    int count = 0;

    // Re-send request that were not fulfilled by server
    this.window_lock.writeLock().lock();
    for(BlockPos position : this.window.keySet()) {
      long sent_time = this.window.get(position).longValue();

      if(time - sent_time >= LoadChunkCommand.RE_SEND_TIME) {

        int z = position.getZ() >> 4;
        if(z == this.chunks[this.chunks.length - 1].z) {
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(position, EnumFacing.EAST, EnumHand.MAIN_HAND, 0f, 0f, 0f));
        } else {
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(position, EnumFacing.SOUTH, EnumHand.MAIN_HAND, 0f, 0f, 0f));
        }

        this.window.put(position, time);
        if(++count >= LoadChunkCommand.BURST_SIZE) {
          this.window_lock.writeLock().unlock();
          return;
        }
      }
    }
    this.window_lock.writeLock().unlock();

    this.window_lock.readLock().lock();
    int size = this.window.size();
    this.window_lock.readLock().unlock();

    // Send new requests
    while(count < LoadChunkCommand.BURST_SIZE && size < LoadChunkCommand.WINDOWS_SIZE) {

      ChunkPos chunk = this.chunks[this.current];
      BlockPos block = null;
      EnumFacing direction = null;

      if(this.current >= this.width * (this.width - 1)) {
        direction = EnumFacing.EAST;

        if(this.current == (this.width * this.width) - 1) {
          block = new BlockPos((chunk.x << 4) | 8, 0, (chunk.z << 4) | 8);
          this.current = 0;
        } else {
          block = new BlockPos((chunk.x << 4) | 15, 0, (chunk.z << 4) | 8);
          this.current += 2;
        }
      } else {
        block = new BlockPos((chunk.x << 4) | 8, 0, (chunk.z << 4) | 15);
        direction = EnumFacing.SOUTH;
        this.current += 2;
      }

      this.window_lock.writeLock().lock();
      this.window.put(block, time);
      this.window_lock.writeLock().unlock();

      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(block, direction, EnumHand.MAIN_HAND, 0f, 0f, 0f));

      count ++;
      size ++;
    }
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketBlockChange change = (SPacketBlockChange) packet;

    BlockPos position = change.getBlockPosition();

    this.window_lock.writeLock().lock();
    if(this.window.remove(position) != null) {
      this.window_lock.writeLock().unlock();
      return null;
    }
    this.window_lock.writeLock().unlock();

    return packet;
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 11);
    MinecraftForge.EVENT_BUS.unregister(this);

    this.chunks = null;

    this.window_lock.writeLock().lock();
    this.window.clear();
    this.window_lock.writeLock().unlock();

    if(this.label_id >= 0) FamilyFunPack.getOverlay().removeLabel(this.label_id);
    this.label_id = -1;
  }
}
