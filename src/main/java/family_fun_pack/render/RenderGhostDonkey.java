package family_fun_pack.render;

import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.client.renderer.entity.RenderAbstractHorse;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;

/* Render ghost donkey with its special (yet shitty) skin */

@SideOnly(Side.CLIENT)
public class RenderGhostDonkey extends RenderAbstractHorse {

  private static final ResourceLocation texture = new ResourceLocation(FamilyFunPack.MODID, "textures/entity/horse/ghost_donkey.png");

  public RenderGhostDonkey(RenderManager manager) {
    super(manager, 0.87F);
  }

  protected ResourceLocation getEntityTexture(AbstractHorse p_getEntityTexture_1_) {
    return RenderGhostDonkey.texture;
  }
}
