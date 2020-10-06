package family_fun_pack.modules;

import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import java.util.Set;
import java.util.HashSet;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.NetworkHandler;
import family_fun_pack.network.PacketListener;

@SideOnly(Side.CLIENT)
public class PacketInterceptionModule extends Module implements PacketListener {

  private Set<Integer> inbound_block;
  private Set<Integer> outbound_block;

  public PacketInterceptionModule() {
    super("Packets interception", "Intercept network packets");
    this.inbound_block = new HashSet<Integer>();
    this.outbound_block = new HashSet<Integer>();
  }

  protected void enable() {
    NetworkHandler handler = FamilyFunPack.getNetworkHandler();
    for(Integer i : this.inbound_block) {
      handler.registerListener(EnumPacketDirection.CLIENTBOUND, this, i);
    }
    for(Integer i : this.outbound_block) {
      handler.registerListener(EnumPacketDirection.SERVERBOUND, this, i);
    }
    FamilyFunPack.getOverlay().addLabel("Interception: On");
  }

  protected void disable() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this);
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.SERVERBOUND, this);
    FamilyFunPack.getOverlay().removeLabel("Interception: On");
  }

  public void addIntercept(EnumPacketDirection direction, int id) {
    Set<Integer> selected = (direction == EnumPacketDirection.CLIENTBOUND ? this.inbound_block : this.outbound_block);
    selected.add(id);
    if(this.isEnabled()) {
      FamilyFunPack.getNetworkHandler().registerListener(direction, this, id);
    }
  }

  public void removeIntercept(EnumPacketDirection direction, int id) {
    Set<Integer> selected = (direction == EnumPacketDirection.CLIENTBOUND ? this.inbound_block : this.outbound_block);
    selected.remove(id);
    if(this.isEnabled()) {
      FamilyFunPack.getNetworkHandler().unregisterListener(direction, this, id);
    }
  }

  public boolean isFiltered(EnumPacketDirection direction, int id) {
    Set<Integer> selected = (direction == EnumPacketDirection.CLIENTBOUND ? this.inbound_block : this.outbound_block);
    return selected.contains(id);
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    return null;
  }
}
