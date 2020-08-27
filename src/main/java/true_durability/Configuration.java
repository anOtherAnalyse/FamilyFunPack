package true_durability;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class Configuration {

  public boolean invulnerable;
  public int last_teleport_id;

  public Configuration() {
    this.invulnerable = false;
    this.last_teleport_id = -1;
  }

}
