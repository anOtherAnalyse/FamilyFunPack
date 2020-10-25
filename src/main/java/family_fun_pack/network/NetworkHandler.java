package family_fun_pack.network;

import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

import java.util.List;
import java.util.LinkedList;

import family_fun_pack.FamilyFunPack;

@SideOnly(Side.CLIENT)
public class NetworkHandler {

  private boolean isConnected;
  private NetworkManager networkManager;

  private List<PacketListener>[] outbound_listeners;
  private List<PacketListener>[] inbound_listeners;

  public NetworkHandler() {
    this.isConnected = false;
    this.networkManager = null;
    this.outbound_listeners = new List[33];
    this.inbound_listeners = new List[80];
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf buf) {
    List<PacketListener>[] listeners = (direction == EnumPacketDirection.CLIENTBOUND ? this.inbound_listeners : this.outbound_listeners);
    if(listeners[id] == null) return packet;
    try {
      for(PacketListener listener : listeners[id]) {
        if((packet = listener.packetReceived(direction, id, packet, buf)) == null) return null;
      }
    } catch (java.util.ConcurrentModificationException e) { // TO FIX with mutex

    }
    return packet;
  }

  public void registerListener(EnumPacketDirection direction, PacketListener listener, int ... ids) {
    List<PacketListener>[] listeners = (direction == EnumPacketDirection.CLIENTBOUND ? this.inbound_listeners : this.outbound_listeners);
    for(int id : ids) {
      if(listeners[id] == null) listeners[id] = new LinkedList<PacketListener>();
      listeners[id].add(listener);
    }
  }

  public void unregisterListener(EnumPacketDirection direction, PacketListener listener) {
    List<PacketListener>[] listeners = (direction == EnumPacketDirection.CLIENTBOUND ? this.inbound_listeners : this.outbound_listeners);
    for(int i = 0; i < listeners.length; i ++) {
      if(listeners[i] == null) continue;
      listeners[i].remove(listener);
      if(listeners[i].size() == 0) listeners[i] = null;
    }
  }

  public void unregisterListener(EnumPacketDirection direction, PacketListener listener, int ... ids) {
    List<PacketListener>[] listeners = (direction == EnumPacketDirection.CLIENTBOUND ? this.inbound_listeners : this.outbound_listeners);
    for(int id : ids) {
      if(listeners[id] == null) return;
      listeners[id].remove(listener);
      if(listeners[id].size() == 0) listeners[id] = null;
    }
  }

  public void sendPacket(Packet<?> packet) {
    if(this.networkManager != null) {
      this.networkManager.sendPacket(packet);
    }
  }

  public INetHandler getNetHandler() {
    if(this.networkManager != null) {
      return this.networkManager.getNetHandler();
    }
    return null;
  }

  public void disconnect() {
    if(this.networkManager != null) {
      this.networkManager.closeChannel(new TextComponentString("You have been successfully disconnected from server"));
    }
  }

  @SubscribeEvent
  public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
    if(!this.isConnected) {

      ChannelPipeline pipeline = event.getManager().channel().pipeline();

      try {
        // Install receive interception
        ChannelHandler old = pipeline.get("decoder");
        if(old != null && old instanceof NettyPacketDecoder) {
          InboundInterceptor spoof = new InboundInterceptor(this, EnumPacketDirection.CLIENTBOUND);
          pipeline.replace("decoder", "decoder", spoof);
        }

        // Install send interception
        old = pipeline.get("encoder");
        if(old != null && old instanceof NettyPacketEncoder) {
          OutboundInterceptor spoof = new OutboundInterceptor(this, EnumPacketDirection.SERVERBOUND);
          pipeline.replace("encoder", "encoder", spoof);
        }

        // Record NetworkManager
        this.networkManager = event.getManager();
        this.isConnected = true;
      } catch (java.util.NoSuchElementException e) {}
    }
  }

  @SubscribeEvent
	public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
    this.isConnected = false;
    FamilyFunPack.getModules().onDisconnect();
    this.networkManager = null;
  }

}
