package family_fun_pack.render;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPig;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.client.Minecraft;

/* Render pig few inches lower, to be able to see where we are going with pig POV */

@SideOnly(Side.CLIENT)
public class NoRenderPig extends RenderPig {

  private Minecraft mc;

  public NoRenderPig(RenderManager manager, Minecraft mc) {
    super(manager);
    this.mc = mc;
  }

  public void doRender(EntityPig pig, double d0, double d1, double d2, float f1, float f2) {
    if(this.mc.player.getRidingEntity() == pig) d1 -= 0.5;
    super.doRender(pig, d0, d1, d2, f1, f2);
  }

}
