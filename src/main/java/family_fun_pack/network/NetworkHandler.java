package family_fun_pack.network;

import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.NettyVarint21FrameEncoder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.IPacket;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedInEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import family_fun_pack.FamilyFunPack;

/* Handles network listeners */

@OnlyIn(Dist.CLIENT)
public class NetworkHandler {

  private boolean isConnected;
  private NetworkManager networkManager;

  private ReadWriteLock[] outbound_lock;
  private ReadWriteLock[] inbound_lock;
  private List<PacketListener>[] outbound_listeners;
  private List<PacketListener>[] inbound_listeners;

  public NetworkHandler() {
    this.isConnected = false;
    this.networkManager = null;
    this.outbound_listeners = new List[48];
    this.outbound_lock = new ReadWriteLock[48];

    for(int i = 0; i < 48; i ++) {
      this.outbound_lock[i] = new ReentrantReadWriteLock();
    }

    this.inbound_listeners = new List[92];
    this.inbound_lock = new ReadWriteLock[92];

    for(int i = 0; i < 92; i ++) {
      this.inbound_lock[i] = new ReentrantReadWriteLock();
    }
  }

  public IPacket<?> packetReceived(PacketDirection direction, int id, IPacket<?> packet, ByteBuf buf) {
    List<PacketListener> listeners;
    ReadWriteLock lock;

    if(direction == PacketDirection.CLIENTBOUND) {
      listeners = this.inbound_listeners[id];
      lock = this.inbound_lock[id];
    } else {
      listeners = this.outbound_listeners[id];
      lock = this.outbound_lock[id];
    }

    if(listeners != null) {
      int buff_start = 0;
      if(buf != null) buff_start = buf.readerIndex();

      lock.readLock().lock();
      int size = listeners.size(); // Get starting size, we assume that a listener can unregister itself & only itself
      lock.readLock().unlock();

      for(int i = 0; i < size; i ++) {
        lock.readLock().lock();
        PacketListener l = listeners.get(i - (size - listeners.size()));
        lock.readLock().unlock();

        if(buf != null) buf.readerIndex(buff_start);
        if((packet = l.packetReceived(direction, id, packet, buf)) == null) return null;
      }
    }

    return packet;
  }

  public void registerListener(PacketDirection direction, PacketListener listener, int ... ids) {
    List<PacketListener>[] listeners;
    ReadWriteLock[] locks;

    if(direction == PacketDirection.CLIENTBOUND) {
      listeners = this.inbound_listeners;
      locks = this.inbound_lock;
    } else {
      listeners = this.outbound_listeners;
      locks = this.outbound_lock;
    }

    for(int id : ids) {
      try {
        locks[id].writeLock().lock();

        if(listeners[id] == null) listeners[id] = new ArrayList<PacketListener>();
        if(! listeners[id].contains(listener)) { // Not twice
          listeners[id].add(listener);
        }
      } finally {
        locks[id].writeLock().unlock();
      }
    }
  }

  public void unregisterListener(PacketDirection direction, PacketListener listener) {
    List<PacketListener>[] listeners;
    ReadWriteLock[] locks;

    if(direction == PacketDirection.CLIENTBOUND) {
      listeners = this.inbound_listeners;
      locks = this.inbound_lock;
    } else {
      listeners = this.outbound_listeners;
      locks = this.outbound_lock;
    }

    for(int i = 0; i < listeners.length; i ++) {
      try {
        locks[i].writeLock().lock();
        if(listeners[i] != null) {
          listeners[i].remove(listener);
          if(listeners[i].size() == 0) listeners[i] = null;
        }
      } finally {
        locks[i].writeLock().unlock();
      }
    }
  }

  public void unregisterListener(PacketDirection direction, PacketListener listener, int ... ids) {
    List<PacketListener>[] listeners;
    ReadWriteLock[] locks;

    if(direction == PacketDirection.CLIENTBOUND) {
      listeners = this.inbound_listeners;
      locks = this.inbound_lock;
    } else {
      listeners = this.outbound_listeners;
      locks = this.outbound_lock;
    }

    for(int id : ids) {
      try {
        locks[id].writeLock().lock();
        if(listeners[id] != null) {
          listeners[id].remove(listener);
          if(listeners[id].size() == 0) listeners[id] = null;
        }
      } finally {
        locks[id].writeLock().unlock();
      }
    }
  }

  public void sendPacket(IPacket<?> packet) {
    if(this.networkManager != null) {
      this.networkManager.send(packet);
    }
  }

  public void disconnect() {
    if(this.networkManager != null) {
      this.networkManager.disconnect(new StringTextComponent("You have been successfully disconnected from server"));
    }
  }

  @SubscribeEvent
  public void onConnect(LoggedInEvent event) {
    if(! this.isConnected) {

      ChannelPipeline pipeline = event.getNetworkManager().channel().pipeline();

      try {
        // Install receive interception
        ChannelHandler old = pipeline.get("decoder");
        if(old != null && old instanceof NettyPacketDecoder) {
          InboundInterceptor spoof = new InboundInterceptor(this, PacketDirection.CLIENTBOUND);
          pipeline.replace("decoder", "decoder", spoof);
        }

        // Install send interception
        old = pipeline.get("encoder");
        if(old != null && old instanceof NettyPacketEncoder) {
          OutboundInterceptor spoof = new OutboundInterceptor(this, PacketDirection.SERVERBOUND);
          pipeline.replace("encoder", "encoder", spoof);
        }

        // Install special frame encoder
        old = pipeline.get("prepender");
        if(old != null && old instanceof NettyVarint21FrameEncoder) {
          OutboundFrameEncoder spoof = new OutboundFrameEncoder();
          pipeline.replace("prepender", "prepender", spoof);
        }

        // Record NetworkManager
        this.networkManager = event.getNetworkManager();
        this.isConnected = true;
      } catch (NoSuchElementException e) {}
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onDisconnect(LoggedOutEvent event) {
    this.isConnected = false;
    FamilyFunPack.getModules().onDisconnect();
    // FamilyFunPack.getMainGui().reset();
    this.networkManager = null;
  }

  public boolean isConnected() {
    return this.isConnected;
  }

  public ClientPlayNetHandler getNetHandler() {
    if(this.networkManager == null) return null;
    return (ClientPlayNetHandler) this.networkManager.getPacketListener();
  }
}
