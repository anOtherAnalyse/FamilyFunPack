package family_fun_pack.modules;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import java.util.Set;
import java.util.HashSet;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.MainGuiComponent;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.interfaces.PacketsSelectionGui;
import family_fun_pack.network.NetworkHandler;
import family_fun_pack.network.PacketListener;

/* Block network packets */

@SideOnly(Side.CLIENT)
public class PacketInterceptionModule extends Module implements PacketListener {

  private Set<Integer> inbound_block;
  private Set<Integer> outbound_block;

  private Set<Packet> exceptions;

  private int label_id;

  public PacketInterceptionModule() {
    super("Packets interception", "Intercept network packets");
    this.inbound_block = new HashSet<Integer>();
    this.outbound_block = new HashSet<Integer>();
    this.exceptions = new HashSet<Packet>();
    FamilyFunPack.addModuleKey(0, this);
    this.label_id = -1;
  }

  protected void enable() {
    NetworkHandler handler = FamilyFunPack.getNetworkHandler();
    for(Integer i : this.inbound_block) {
      handler.registerListener(EnumPacketDirection.CLIENTBOUND, this, i);
    }
    for(Integer i : this.outbound_block) {
      handler.registerListener(EnumPacketDirection.SERVERBOUND, this, i);
    }
    this.label_id = FamilyFunPack.getOverlay().addLabel("Interception: On");
  }

  protected void disable() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this);
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.SERVERBOUND, this);
    if(this.label_id >= 0) FamilyFunPack.getOverlay().removeLabel(this.label_id);
    this.label_id = -1;
  }

  // reinventing the wheel
  public static int[] convertArray(Integer[] in) {
    int[] out = new int[in.length];
    for(int j = 0; j < in.length; j ++) {
      out[j] = in[j];
    }
    return out;
  }

  public void save(Configuration configuration) {
    configuration.get(this.getLabel(), "inbound_filter", new int[0]).set(PacketInterceptionModule.convertArray(this.inbound_block.toArray(new Integer[0])));
    configuration.get(this.getLabel(), "outbound_filter", new int[0]).set(PacketInterceptionModule.convertArray(this.outbound_block.toArray(new Integer[0])));
    super.save(configuration);
  }

  public void load(Configuration configuration) {
    for(int id : configuration.get(this.getLabel(), "inbound_filter", new int[0]).getIntList()) {
      this.inbound_block.add(id);
    }
    for(int id : configuration.get(this.getLabel(), "outbound_filter", new int[0]).getIntList()) {
      this.outbound_block.add(id);
    }
    super.load(configuration);
  }

  // Add packet to be blocked
  public void addIntercept(EnumPacketDirection direction, int id) {
    Set<Integer> selected = (direction == EnumPacketDirection.CLIENTBOUND ? this.inbound_block : this.outbound_block);
    selected.add(id);
    if(this.isEnabled()) {
      FamilyFunPack.getNetworkHandler().registerListener(direction, this, id);
    }
  }

  // remove packet from beeing blocked
  public void removeIntercept(EnumPacketDirection direction, int id) {
    Set<Integer> selected = (direction == EnumPacketDirection.CLIENTBOUND ? this.inbound_block : this.outbound_block);
    selected.remove(id);
    if(this.isEnabled()) {
      FamilyFunPack.getNetworkHandler().unregisterListener(direction, this, id);
    }
  }

  // Is packet blocked
  public boolean isFiltered(EnumPacketDirection direction, int id) {
    Set<Integer> selected = (direction == EnumPacketDirection.CLIENTBOUND ? this.inbound_block : this.outbound_block);
    return selected.contains(id);
  }

  /* Exception to pass the filter */
  public void addException(EnumPacketDirection direction, Packet<?> exception) {
    if(this.isEnabled()) {
      try {
        int id = EnumConnectionState.getById(0).getPacketId(direction, exception).intValue();

        if(this.isFiltered(direction, id)) {
          this.exceptions.add(exception);
        }
      } catch(Exception e) {}
    }
  }

  // Block packet
  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    if(this.exceptions.remove(packet)) {
      return packet;
    }
    return null;
  }

  public void onDisconnect() {
    this.exceptions.clear();
  }

  // To be displayed in Main GUI, to access the packets selection GUI
  private class GuiComponent implements MainGuiComponent {

    private PacketInterceptionModule dependence;

    public GuiComponent(PacketInterceptionModule dependence) {
      this.dependence = dependence;
    }

    public String getLabel() {
      return "which packets ?";
    }

    public ActionButton getAction() {
      return new OpenGuiButton(0, 0, "select", PacketsSelectionGui.class, this.dependence);
    }

    public MainGuiComponent getChild() {
      return null;
    }
  }

  // Bind open packets selection to this module in Main GUI
  public MainGuiComponent getChild() {
    return new GuiComponent(this);
  }
}
