package family_fun_pack.modules;

import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.ProtocolType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.key.KeyManager;
import family_fun_pack.gui.MainGuiComponent;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.interfaces.PacketsInterceptionGui;
import family_fun_pack.network.NetworkHandler;
import family_fun_pack.network.PacketListener;

/* Cancel network packets */

@OnlyIn(Dist.CLIENT)
public class PacketInterceptionModule extends Module implements PacketListener {

  private Set<IPacket> exceptions;

  private int label_id;

  public PacketInterceptionModule() {
    super("packetCancel", "Packets interception");
    this.exceptions = new HashSet<IPacket>();
    KeyManager.registerKey("Packet canceller", -1, this);
    this.label_id = -1;
  }

  private Collection<Integer> getCanceledPackets(PacketDirection direction) {
    return this.<Collection<Integer>>getOrElse(direction.toString(), new ArrayList<Integer>());
  }

  protected void enable() {
    NetworkHandler handler = FamilyFunPack.getNetworkHandler();
    for(Integer i : this.getCanceledPackets(PacketDirection.CLIENTBOUND)) {
      handler.registerListener(PacketDirection.CLIENTBOUND, this, i);
    }
    for(Integer i : this.getCanceledPackets(PacketDirection.SERVERBOUND)) {
      handler.registerListener(PacketDirection.SERVERBOUND, this, i);
    }
    this.label_id = FamilyFunPack.getOverlay().addLabel("Packet canceller: On");
  }

  protected void disable() {
    FamilyFunPack.getNetworkHandler().unregisterListener(PacketDirection.CLIENTBOUND, this);
    FamilyFunPack.getNetworkHandler().unregisterListener(PacketDirection.SERVERBOUND, this);
    if(this.label_id >= 0) FamilyFunPack.getOverlay().removeLabel(this.label_id);
    this.label_id = -1;
  }

  // Add packet to be blocked
  public void addIntercept(PacketDirection direction, int id) {
    this.getCanceledPackets(direction).add(id);
    if(this.isEnabled()) FamilyFunPack.getNetworkHandler().registerListener(direction, this, id);
  }

  // remove packet from beeing blocked
  public void removeIntercept(PacketDirection direction, int id) {
    this.getCanceledPackets(direction).remove(id);
    if(this.isEnabled()) FamilyFunPack.getNetworkHandler().unregisterListener(direction, this, id);
  }

  // Is packet blocked
  public boolean isFiltered(PacketDirection direction, int id) {
    return this.getCanceledPackets(direction).contains(id);
  }

  /* Exception to pass the filter */
  public void addException(PacketDirection direction, IPacket<?> exception) {
    if(this.isEnabled()) {
      try {
        int id = ProtocolType.PLAY.getPacketId(direction, exception).intValue();

        if(this.isFiltered(direction, id)) {
          this.exceptions.add(exception);
        }
      } catch(Exception e) {}
    }
  }

  // Block packet
  public IPacket<?> packetReceived(PacketDirection direction, int id, IPacket<?> packet, ByteBuf in) {
    if(this.exceptions.remove(packet)) return packet;
    return null;
  }

  public void onDisconnect() {
    this.exceptions.clear();
  }

  // To be displayed in Main GUI, to access the packets selection GUI
  private class GuiComponent implements MainGuiComponent, Button.IPressable {

    private PacketInterceptionModule dependence;

    public GuiComponent(PacketInterceptionModule dependence) {
      this.dependence = dependence;
    }

    public String getLabel() {
      return "which packets ?";
    }

    public Widget getAction() {
      return new OpenGuiButton(0, 0, "select", this);
    }

    public MainGuiComponent getChild() {
      return null;
    }

    public void onPress(Button btn) {
      if(((OpenGuiButton) btn).isClicked()) {
        FamilyFunPack.getMainGui().setRightPanel(new PacketsInterceptionGui(this.dependence), ((OpenGuiButton) btn).getId());
      } else {
        FamilyFunPack.getMainGui().removeRightPanel();
      }
    }
  }

  // Bind open packets selection to this module in Main GUI
  public MainGuiComponent getChild() {
    return new GuiComponent(this);
  }
}
