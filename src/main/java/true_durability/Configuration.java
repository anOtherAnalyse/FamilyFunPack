package true_durability;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.network.EnumPacketDirection;

import java.util.Set;
import java.util.HashSet;

@SideOnly(Side.CLIENT)
public class Configuration {

  public boolean invulnerable;
  public boolean block_player_packets;
  public Set<Integer> inbound_block;
  public Set<Integer> outbound_block;

  public int last_teleport_id;
  public boolean currently_invulnerable;

  public Configuration() {
    this.invulnerable = false;
    this.block_player_packets = false;
    this.inbound_block = new HashSet<Integer>();
    this.outbound_block = new HashSet<Integer>();
    this.resetVolatileConf();
  }

  public void resetVolatileConf() {
    this.currently_invulnerable = false;
    this.last_teleport_id = -1;
  }

  public Set<Integer> getSet(EnumPacketDirection direction) {
    if(direction == EnumPacketDirection.SERVERBOUND) {
      return this.outbound_block;
    } else {
      return this.inbound_block;
    }
  }

}
