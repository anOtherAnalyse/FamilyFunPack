package family_fun_pack.gui.components.actions;

import net.minecraft.network.EnumPacketDirection;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.modules.PacketInterceptionModule;

@SideOnly(Side.CLIENT)
public class OnOffInterception implements OnOffAction {

  private final PacketInterceptionModule module;
  private final EnumPacketDirection direction;
  private final int id;

  public OnOffInterception(PacketInterceptionModule module, EnumPacketDirection direction, int id) {
    this.module = module;
    this.direction = direction;
    this.id = id;
  }

  public void toggle(boolean state) {
    if(state) this.module.addIntercept(this.direction, this.id);
    else this.module.removeIntercept(this.direction, this.id);
  }

}
