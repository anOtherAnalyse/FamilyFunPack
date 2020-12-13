package family_fun_pack.entities;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.render.RenderGhostDonkey;

/* Ghost donkey, will appear when using .open command while mounting an entity that does not exist client-side
 * For example after mounting an entity from unloaded chunk, and then using .open to open its inventory
*/

@SideOnly(Side.CLIENT)
public class GhostDonkey extends EntityDonkey {

  static {
    Minecraft.getMinecraft().getRenderManager().entityRenderMap.put(GhostDonkey.class, new RenderGhostDonkey(Minecraft.getMinecraft().getRenderManager()));
  }

  public GhostDonkey(World world) {
    super(world);
  }

  public void onLivingUpdate() {
    super.onLivingUpdate();
    if(!this.isBeingRidden()) Minecraft.getMinecraft().world.removeEntityFromWorld(this.getEntityId());
  }

  // On dismount
  protected void removePassenger(Entity player) {
    super.removePassenger(player);
    Minecraft.getMinecraft().world.removeEntityFromWorld(this.getEntityId());
  }
}
