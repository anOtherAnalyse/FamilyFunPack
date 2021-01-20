package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;
import family_fun_pack.utils.WorldCapture;

/* World download */

@SideOnly(Side.CLIENT)
public class WorldDownloadCommand extends Command implements PacketListener {

  private WorldCapture capture;

  private Map<ChunkPos, List<Entity>> entities;
  private ReadWriteLock entities_lock;

  public WorldDownloadCommand() {
    super("download");
    this.capture = null;
  }

  public String usage() {
    return this.getName() + " start <capture_name> | stop";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();
    if(args.length > 1) {
      if(args[1].equals("start") && args.length > 2) {
        if(this.capture != null) return "A download is currently on";

        this.entities = new HashMap<ChunkPos, List<Entity>>();
        this.entities_lock = new ReentrantReadWriteLock();

        this.capture = new WorldCapture(args[2], mc.world.provider.getDimensionType(), new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ));

        MinecraftForge.EVENT_BUS.register(this);
        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 50);

        return "World download on, go walk around..";
      } else if(args[1].equals("stop")) {
        if(this.capture == null) return "No current download";

        String name = this.capture.getName();
        this.onDisconnect();

        return "World download saved under name \"" + name + "\"";
      }
    }
    return this.getUsage();
  }

  @SubscribeEvent
  public void onUnLoad(ChunkEvent.Unload event) {
    Chunk c = event.getChunk();
    this.capture(c);
  }

  // Save removed entity
  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketDestroyEntities destroy = (SPacketDestroyEntities) packet;

    Minecraft mc = Minecraft.getMinecraft();

    for(int entity_id : destroy.getEntityIDs()) {
      Entity e = mc.world.getEntityByID(entity_id);
      if(e != null && !(e instanceof EntityPlayer) && e.addedToChunk) {
        ChunkPos chunk = new ChunkPos(e.chunkCoordX, e.chunkCoordZ);

        if(mc.world.getChunkProvider().getLoadedChunk(chunk.x, chunk.z) == null) continue;

        this.entities_lock.writeLock().lock();
        if(this.entities == null) return packet;

        List<Entity> list = this.entities.get(chunk);
        if(list == null) {
          list = new LinkedList<Entity>();
          this.entities.put(chunk, list);
        }

        boolean exists = false;
        for(Entity i : list) {
          if(e.getUniqueID().equals(i.getUniqueID()) && e.getClass().equals(i.getClass())) { // abort
            exists = true;
            break;
          }
        }

        if(! exists) list.add(e);
        this.entities_lock.writeLock().unlock();
      }
    }

    return packet;
  }

  public void onDisconnect() {
    if(this.capture != null) {
      MinecraftForge.EVENT_BUS.unregister(this);
      FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 50);

      this.saveSurroundings();

      this.entities_lock.writeLock().lock();
      this.entities.clear();
      this.entities = null;
      this.entities_lock.writeLock().unlock();

      this.entities_lock = null;

      this.capture = null;
    }
  }

  private void saveSurroundings() {
    Minecraft mc = Minecraft.getMinecraft();

    int x = (int)mc.getRenderViewEntity().posX >> 4, z = (int)mc.getRenderViewEntity().posZ >> 4;
    for(int i = x - mc.gameSettings.renderDistanceChunks; i <= x + mc.gameSettings.renderDistanceChunks; i ++) {
      for(int j = z - mc.gameSettings.renderDistanceChunks; j <= z + mc.gameSettings.renderDistanceChunks; j ++) {
        Chunk c = mc.world.getChunkProvider().getLoadedChunk(i, j);
        if(c != null) this.capture(c);
      }
    }
  }

  private void capture(Chunk c) {
    ChunkPos position = new ChunkPos(c.x, c.z);

    // Add removed entities
    this.entities_lock.readLock().lock();
    List<Entity> list = this.entities.get(position);
    if(list != null) {
      for(Entity i : list) {

        // Check entity is not in chunk
        boolean added = false;
        for(ClassInheritanceMultiMap<Entity> slice : c.getEntityLists()) {
          for(Entity j : slice) {
            if(i.getUniqueID().equals(j.getUniqueID()) && i.getClass().equals(j.getClass())) {
              added = true;
              break;
            }
          }
        }

        if(! added) c.addEntity(i);
      }
    }
    this.entities_lock.readLock().unlock();

    // Reset lights & capture
    c.setLightPopulated(false);
    this.capture.captureChunk(c);

    // Remove removed entities
    this.entities_lock.readLock().lock();
    if(list != null) {
      for(Entity i : list) {
        c.removeEntity(i);
      }
    }
    this.entities.remove(position);
    this.entities_lock.readLock().unlock();
  }
}
