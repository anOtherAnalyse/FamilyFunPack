package family_fun_pack.render;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.nbt.NBTTagCompound;

import java.lang.Class;
import java.lang.NoSuchMethodException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalAccessException;

import family_fun_pack.nbt.SpecialTagCompound;

/* Used to apply red enchant to unbrekable items */

@SideOnly(Side.CLIENT)
public class CustomRenderItem extends RenderItem {

  private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

  private Method renderModel;

  private final TextureManager textureManagerSave;

  public CustomRenderItem(TextureManager textureManager, ModelManager modelManager, ItemColors itemColors) {
    super(textureManager, modelManager, itemColors);
    this.textureManagerSave = textureManager;
    Class<RenderItem> renderItem = RenderItem.class;
    try {
      this.renderModel = renderItem.getDeclaredMethod("func_191967_a", IBakedModel.class, int.class, ItemStack.class);
    } catch(NoSuchMethodException e) {
      try {
        this.renderModel = renderItem.getDeclaredMethod("renderModel", IBakedModel.class, int.class, ItemStack.class);
      } catch(NoSuchMethodException e2) {
        throw new RuntimeException("FamilyFunPack Error: no method renderModel in class RenderItem");
      }
    }
    this.renderModel.setAccessible(true);
  }

  public void renderItem(ItemStack stack, IBakedModel model) {
    if (!stack.isEmpty()) {

      GlStateManager.pushMatrix();
      GlStateManager.translate(-0.5F, -0.5F, -0.5F);

      if (model.isBuiltInRenderer()) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();
        stack.getItem().getTileEntityItemStackRenderer().renderByItem(stack);
      } else {
        try {
          this.renderModel.invoke(this, model, new Integer(-1), stack);
        } catch(IllegalAccessException e) {}
          catch(InvocationTargetException e) {}

        NBTTagCompound tag = stack.getTagCompound();
        if((stack.isItemStackDamageable() && stack.getItemDamage() > stack.getMaxDamage()) || (tag != null && (tag instanceof SpecialTagCompound || tag.getBoolean("Unbreakable")))) {
          this.renderEffect(model, 0xfffc0505);
        } else if (stack.hasEffect()) {
          this.renderEffect(model, -8372020);
        }
      }

      GlStateManager.popMatrix();
    }
  }

  private void renderEffect(IBakedModel model, int color) {
    GlStateManager.depthMask(false);
    GlStateManager.depthFunc(514);
    GlStateManager.disableLighting();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
    this.textureManagerSave.bindTexture(CustomRenderItem.RES_ITEM_GLINT);
    GlStateManager.matrixMode(5890);
    GlStateManager.pushMatrix();
    GlStateManager.scale(8.0F, 8.0F, 8.0F);
    float f = (float)(Minecraft.getSystemTime() % 3000L) / 3000.0F / 8.0F;
    GlStateManager.translate(f, 0.0F, 0.0F);
    GlStateManager.rotate(-50.0F, 0.0F, 0.0F, 1.0F);
    try {
      this.renderModel.invoke(this, model, new Integer(color), ItemStack.EMPTY);
    } catch(IllegalAccessException e) {}
      catch(InvocationTargetException e) {}
    GlStateManager.popMatrix();
    GlStateManager.pushMatrix();
    GlStateManager.scale(8.0F, 8.0F, 8.0F);
    float f1 = (float)(Minecraft.getSystemTime() % 4873L) / 4873.0F / 8.0F;
    GlStateManager.translate(-f1, 0.0F, 0.0F);
    GlStateManager.rotate(10.0F, 0.0F, 0.0F, 1.0F);
    try {
      this.renderModel.invoke(this, model, new Integer(color), ItemStack.EMPTY);
    } catch(IllegalAccessException e) {}
      catch(InvocationTargetException e) {}
    GlStateManager.popMatrix();
    GlStateManager.matrixMode(5888);
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    GlStateManager.enableLighting();
    GlStateManager.depthFunc(515);
    GlStateManager.depthMask(true);
    this.textureManagerSave.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
  }

}
