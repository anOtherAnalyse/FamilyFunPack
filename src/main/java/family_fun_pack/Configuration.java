package family_fun_pack;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.network.EnumPacketDirection;
import net.minecraftforge.common.config.Property;

import java.util.Set;
import java.util.HashSet;
import java.io.File;

@SideOnly(Side.CLIENT)
public class Configuration {

  public boolean invulnerable;
  public boolean block_player_packets;
  public Set<Integer> inbound_block;
  public Set<Integer> outbound_block;

  public int last_teleport_id;
  public boolean currently_invulnerable;

  private net.minecraftforge.common.config.Configuration conf;

  public Configuration(File save) {
    this.invulnerable = false;
    this.block_player_packets = false;
    this.inbound_block = new HashSet<Integer>();
    this.outbound_block = new HashSet<Integer>();
    this.resetVolatileConf();
    this.conf = new net.minecraftforge.common.config.Configuration(save);
    this.load();
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

  public void load() {
    Property invulnerable = this.conf.get("ALL", "Invulnerability", false);
    this.invulnerable = invulnerable.getBoolean();
    Property intercept = this.conf.get("ALL", "Intercept", false);
    this.block_player_packets = intercept.getBoolean();
    Property inbound = this.conf.get("ALL", "Inbound", new int[0]);
    for(int id : inbound.getIntList()) {
      this.inbound_block.add(id);
    }
    Property outbound = this.conf.get("ALL", "Outbound", new int[0]);
    for(int id : outbound.getIntList()) {
      this.outbound_block.add(id);
    }
  }

  public int[] convertArray(Integer[] in) {
    int[] out = new int[in.length];
    int j = 0;
    for(int i : in) {
      out[j] = i;
      j ++;
    }
    return out;
  }

  public void save() {
    Property invulnerable = this.conf.get("ALL", "Invulnerability", false);
    invulnerable.set(this.invulnerable);
    Property intercept = this.conf.get("ALL", "Intercept", false);
    intercept.set(this.block_player_packets);
    Property inbound = this.conf.get("ALL", "Inbound", new int[0]);
    inbound.set(this.convertArray(this.inbound_block.toArray(new Integer[0])));
    Property outbound = this.conf.get("ALL", "Outbound", new int[0]);
    outbound.set(this.convertArray(this.outbound_block.toArray(new Integer[0])));
    this.conf.save();
  }

}
