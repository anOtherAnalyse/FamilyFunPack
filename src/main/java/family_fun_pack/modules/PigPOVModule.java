package family_fun_pack.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderPig;
import net.minecraft.entity.passive.EntityPig;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.render.NoRenderPig;

/* Lower player height, be able to see when travelling 1 by 1 tunnels on pig */

@SideOnly(Side.CLIENT)
public class PigPOVModule extends Module {

  public PigPOVModule() {
    super("Pig POV", "Pig Point Of View");
  }

  protected void enable() {
    Minecraft mc = Minecraft.getMinecraft();
    mc.player.eyeHeight = 0.6f;
    mc.getRenderManager().entityRenderMap.put(EntityPig.class, new NoRenderPig(mc.getRenderManager(), mc));
  }

  protected void disable() {
    Minecraft mc = Minecraft.getMinecraft();
    mc.player.eyeHeight = mc.player.getDefaultEyeHeight();
    mc.getRenderManager().entityRenderMap.put(EntityPig.class, new RenderPig(mc.getRenderManager()));
  }

  public void save(Configuration configuration) {}

  public void load(Configuration configuration) {}
}
