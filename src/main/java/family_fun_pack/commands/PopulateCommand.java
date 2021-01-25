package family_fun_pack.commands;

import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;
import java.lang.System;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Generate & populate given square of chunks */

@SideOnly(Side.CLIENT)
public class PopulateCommand extends Command implements PacketListener {

  private static final int RETRY_TIME = 1500;

  private int x, z, wx, wz;
  private int index = 0;

  private long last_send;

  private BlockPos[] window;
  private int completed;
  private ReadWriteLock window_lock;

  private boolean enabled;

  private int label_id;

  public PopulateCommand() {
    super("populate");
    this.window = new BlockPos[4];
    this.window_lock = new ReentrantReadWriteLock();
    this.enabled = false;
    this.label_id = -1;
  }

  public String usage() {
    return this.getName() + " <corner_x> <corner_z> <width_x> <width_z> | off";
  }

  public String execute(String[] args) {

    if(this.enabled) {
      if(args.length > 1 && args[1].equals("off")) {
        this.onDisconnect();
        return "Population was aborted";
      }
      return String.format("%d / %d chunks done", this.index, this.wx * this.wz);
    } else if(args.length > 4) {
      try {
        this.x = Integer.parseInt(args[1]);
        this.z = Integer.parseInt(args[2]);
        this.wx = Integer.parseInt(args[3]);
        this.wz = Integer.parseInt(args[4]);

        if(this.wx < 1 || this.wz < 1) return "Wrong width";

        this.enabled = true;
        this.index = 0;
        this.completed = 15;

        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 11);
        MinecraftForge.EVENT_BUS.register(this);

        return String.format("Starting population of %d chunks", this.wx * this.wz);
      } catch(NumberFormatException e) {
        return this.getUsage();
      }
    }
    return this.getUsage();
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {

    this.window_lock.readLock().lock();
    boolean completed = (this.completed == 15);
    this.window_lock.readLock().unlock();

    if(completed) {
      this.populateChunk();
    } else if(System.currentTimeMillis() - this.last_send >= PopulateCommand.RETRY_TIME) {
      this.sendRequests();
    }
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketBlockChange change = (SPacketBlockChange) packet;

    BlockPos position = change.getBlockPosition();

    this.window_lock.writeLock().lock();
    for(int i = 0; i < 4; i ++) {
      if(position.equals(this.window[i])) {
        this.completed |= (1 << i);

        this.window_lock.writeLock().unlock();
        return null;
      }
    }
    this.window_lock.writeLock().unlock();

    return packet;
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 11);
    MinecraftForge.EVENT_BUS.unregister(this);
    this.enabled = false;
    if(this.label_id >= 0) FamilyFunPack.getOverlay().removeLabel(this.label_id);
    this.label_id = -1;
  }

  public void populateChunk() {

    BlockPos position = this.indexToBlockPos(this.index);
    if(position == null) {
      this.onDisconnect();
      FamilyFunPack.printMessage(String.format("Population of %d chunks done", this.wx * this.wz));
    } else {

      this.window_lock.writeLock().lock();

      this.window[0] = position.add(16, 0, 24); // SOUTH-EAST
      this.window[1] = position.add(15, 0, 24); // SOUTH

      this.window[2] = position.add(16, 0, 8); // EAST
      this.window[3] = position.add(15, 0, 8); // TARGET

      this.window_lock.writeLock().unlock();

      this.sendRequests();

      this.index += 1;

      this.updateStatusLabel();
    }
  }

  public void sendRequests() {
    this.last_send = System.currentTimeMillis();

    this.window_lock.writeLock().lock();
    this.completed = 0;
    for(int i = 0; i < 4; i += 2) {
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(this.window[i], EnumFacing.WEST, EnumHand.MAIN_HAND, 0f, 0f, 0f));
    }
    this.window_lock.writeLock().unlock();
  }

  private BlockPos indexToBlockPos(int index) {
    if(index >= this.wx * this.wz) return null;
    return new BlockPos((this.x + (index / this.wz)) << 4, 0, (this.z + (index % this.wz)) << 4);
  }

  private void updateStatusLabel() {
    String label = String.format("Populating: %.2f%%", (float)(this.index - 1) * 100f / (float)(this.wx * this.wz));
    if(this.label_id >= 0) FamilyFunPack.getOverlay().modifyLabel(this.label_id, label);
    else this.label_id = FamilyFunPack.getOverlay().addLabel(label);
  }
}
