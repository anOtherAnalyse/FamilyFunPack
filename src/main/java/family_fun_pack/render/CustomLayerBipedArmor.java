package family_fun_pack.render;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import family_fun_pack.nbt.SpecialTagCompound;

/* Used to apply red enchant to unbrekable armor pieces */

@SideOnly(Side.CLIENT)
public class CustomLayerBipedArmor extends LayerBipedArmor {

  public static final float enchant_red = 0.95F;
  public static final float enchant_blue = 0.05F;
  public static final float enchant_green = 0.05F;

  private final RenderLivingBase<?> renderer_save;

  public CustomLayerBipedArmor(RenderLivingBase<?> entity_renderer) {
    super(entity_renderer);
    this.renderer_save = entity_renderer;
  }

  public void doRenderLayer(EntityLivingBase entity, float f1, float f2, float f3, float f4, float f5, float f6, float f7) {
    this.renderArmorLayer(entity, f1, f2, f3, f4, f5, f6, f7, EntityEquipmentSlot.CHEST);
    this.renderArmorLayer(entity, f1, f2, f3, f4, f5, f6, f7, EntityEquipmentSlot.LEGS);
    this.renderArmorLayer(entity, f1, f2, f3, f4, f5, f6, f7, EntityEquipmentSlot.FEET);
    this.renderArmorLayer(entity, f1, f2, f3, f4, f5, f6, f7, EntityEquipmentSlot.HEAD);
  }


  private void renderArmorLayer(EntityLivingBase entity, float fp1, float fp2, float fp3, float fp4, float fp5, float fp6, float fp7, EntityEquipmentSlot slot) {
    ItemStack itemstack = entity.getItemStackFromSlot(slot);

    if (itemstack.getItem() instanceof ItemArmor) {

      ItemArmor itemarmor = (ItemArmor)itemstack.getItem();

      if (itemarmor.getEquipmentSlot() == slot) {

        ModelBiped model = this.getModelFromSlot(slot);
        model = this.getArmorModelHook(entity, itemstack, slot, model);
        model.setModelAttributes(this.renderer_save.getMainModel());
        model.setLivingAnimations(entity, fp1, fp2, fp3);
        this.setModelSlotVisible(model, slot);
        this.renderer_save.bindTexture(this.getArmorResource((Entity)entity, itemstack, slot, null));

        if (itemarmor.hasOverlay(itemstack)) {
          int i = itemarmor.getColor(itemstack);
          float f = (i >> 16 & 0xFF) / 255.0F;
          float f1 = (i >> 8 & 0xFF) / 255.0F;
          float f2 = (i & 0xFF) / 255.0F;
          GlStateManager.color(1f * f, 1f * f1, 1f * f2, 1f);
          model.render((Entity)entity, fp1, fp2, fp4, fp5, fp6, fp7);
          this.renderer_save.bindTexture(this.getArmorResource((Entity)entity, itemstack, slot, "overlay"));
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
        model.render((Entity)entity, fp1, fp2, fp4, fp5, fp6, fp7);

        NBTTagCompound tag = itemstack.getTagCompound();
        if((itemstack.isItemStackDamageable() && itemstack.getItemDamage() > itemstack.getMaxDamage()) || (tag != null && (tag instanceof SpecialTagCompound || tag.getBoolean("Unbreakable")))) {
          CustomLayerBipedArmor.renderEnchantedGlint(this.renderer_save, entity, (ModelBase)model, fp1, fp2, fp3, fp4, fp5, fp6, fp7);
        } else if (itemstack.hasEffect()) {
          LayerArmorBase.renderEnchantedGlint(this.renderer_save, entity, (ModelBase)model, fp1, fp2, fp3, fp4, fp5, fp6, fp7);
        }
      }
    }
  }

  public static void renderEnchantedGlint(RenderLivingBase<?> render, EntityLivingBase entity, ModelBase model, float fp1, float fp2, float fp3, float fp4, float fp5, float fp6, float fp7) {
    float f = entity.ticksExisted + fp3;
    render.bindTexture(LayerArmorBase.ENCHANTED_ITEM_GLINT_RES);
    (Minecraft.getMinecraft()).entityRenderer.setupFogColor(true);
    GlStateManager.enableBlend();
    GlStateManager.depthFunc(514);
    GlStateManager.depthMask(false);
    GlStateManager.color(0.5F, 0.5F, 0.5F, 1.0F);

    for (int i = 0; i < 2; i++) {
      GlStateManager.disableLighting();
      GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
      GlStateManager.color(CustomLayerBipedArmor.enchant_red, CustomLayerBipedArmor.enchant_green, CustomLayerBipedArmor.enchant_blue, 1.0F);
      GlStateManager.matrixMode(5890);
      GlStateManager.loadIdentity();
      GlStateManager.scale(0.33333334F, 0.33333334F, 0.33333334F);
      GlStateManager.rotate(30.0F - i * 60.0F, 0.0F, 0.0F, 1.0F);
      GlStateManager.translate(0.0F, f * (0.001F + i * 0.003F) * 20.0F, 0.0F);
      GlStateManager.matrixMode(5888);
      model.render((Entity)entity, fp1, fp2, fp4, fp5, fp6, fp7);
      GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    GlStateManager.matrixMode(5890);
    GlStateManager.loadIdentity();
    GlStateManager.matrixMode(5888);
    GlStateManager.enableLighting();
    GlStateManager.depthMask(true);
    GlStateManager.depthFunc(515);
    GlStateManager.disableBlend();
    (Minecraft.getMinecraft()).entityRenderer.setupFogColor(false);
  }

}
