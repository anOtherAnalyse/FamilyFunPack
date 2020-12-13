package family_fun_pack.render;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelElytra;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.nbt.NBTTagCompound;

import family_fun_pack.nbt.SpecialTagCompound;

/* Used to apply red enchant to unbrekable elytras */

@SideOnly(Side.CLIENT)
public class CustomLayerElytra implements LayerRenderer<EntityLivingBase> {

  private static final ResourceLocation TEXTURE_ELYTRA = new ResourceLocation("textures/entity/elytra.png");

  protected RenderLivingBase<?> renderPlayer;

  private final ModelElytra modelElytra = new ModelElytra();

  public CustomLayerElytra(RenderLivingBase<?> render) {
    this.renderPlayer = render;
  }

  public void doRenderLayer(EntityLivingBase entity, float f1, float f2, float f3, float f4, float f5, float f6, float f7) {
    ItemStack stack = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
    if (stack.getItem() != Items.ELYTRA) return;

    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

    if (entity instanceof AbstractClientPlayer) {
      AbstractClientPlayer player = (AbstractClientPlayer)entity;
      if (player.isPlayerInfoSet() && player.getLocationElytra() != null) {
        this.renderPlayer.bindTexture(player.getLocationElytra());
      } else if (player.hasPlayerInfo() && player.getLocationCape() != null && player.isWearing(EnumPlayerModelParts.CAPE)) {
        this.renderPlayer.bindTexture(player.getLocationCape());
      } else {
        this.renderPlayer.bindTexture(CustomLayerElytra.TEXTURE_ELYTRA);
      }
    } else {
      this.renderPlayer.bindTexture(CustomLayerElytra.TEXTURE_ELYTRA);
    }

    GlStateManager.pushMatrix();
    GlStateManager.translate(0.0F, 0.0F, 0.125F);

    this.modelElytra.setRotationAngles(f1, f2, f4, f5, f6, f7, (Entity)entity);
    this.modelElytra.render((Entity)entity, f1, f2, f4, f5, f6, f7);

    NBTTagCompound tag = stack.getTagCompound();
    if(stack.getItemDamage() > stack.getMaxDamage() || (tag != null && tag instanceof SpecialTagCompound)) {
      CustomLayerBipedArmor.renderEnchantedGlint(this.renderPlayer, entity, (ModelBase)this.modelElytra, f1, f2, f3, f4, f5, f6, f7);
    } else if (stack.isItemEnchanted()) {
      LayerArmorBase.renderEnchantedGlint(this.renderPlayer, entity, (ModelBase)this.modelElytra, f1, f2, f3, f4, f5, f6, f7);
    }

    GlStateManager.disableBlend();
    GlStateManager.popMatrix();
  }

  public boolean shouldCombineTextures() {
    return false;
  }

}
