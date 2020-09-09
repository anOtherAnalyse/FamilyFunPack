package family_fun_pack;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IReloadableResourceManager;

import family_fun_pack.render.CustomLayerBipedArmor;
import family_fun_pack.render.CustomLayerElytra;
import family_fun_pack.render.CustomRenderItem;

import java.lang.Class;
import java.lang.reflect.Field;
import java.lang.IllegalAccessException;
import java.lang.NoSuchFieldException;
import java.util.List;
import java.io.File;

@Mod(modid = FamilyFunPack.MODID, name = FamilyFunPack.NAME, version = FamilyFunPack.VERSION)
@SideOnly(Side.CLIENT)
public class FamilyFunPack
{
    public static final String MODID = "family_fun_pack";
    public static final String NAME = "Family Fun Pack";
    public static final String VERSION = "1.0";

    private static NetworkManager networkManager;
    public static Configuration configuration;
    private File confFile;

    public static void setNetworkManager(NetworkManager networkManager) {
      FamilyFunPack.networkManager = networkManager;
    }

    public static void sendPacket(Packet<?> packet) {
      if(FamilyFunPack.networkManager != null) {
        FamilyFunPack.networkManager.sendPacket(packet);
      }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
      this.confFile = event.getSuggestedConfigurationFile();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
      if(event.getSide() == Side.CLIENT) {
        FamilyFunPack.configuration = new Configuration(this.confFile);
        MinecraftForge.EVENT_BUS.register(new Tooltip());

        // Init armor renderers
        Minecraft client = Minecraft.getMinecraft();
        try {
          RenderManager renderManager = client.getRenderManager();
          RenderPlayer normal = renderManager.getSkinMap().get("default");
          RenderPlayer slim = renderManager.getSkinMap().get("slim");
          Class<RenderLivingBase> renderLivingClass = RenderLivingBase.class;
          Field layers_field = null;
          try {
            layers_field = renderLivingClass.getDeclaredField("field_177097_h");
          } catch (NoSuchFieldException e) {
            layers_field = renderLivingClass.getDeclaredField("layerRenderers");
          }
          layers_field.setAccessible(true);
          List<Object> layers_default = (List<Object>)layers_field.get(normal);
          List<Object> layers_slim = (List<Object>)layers_field.get(slim);

          // Set Armor renderers
          layers_default.set(0, new CustomLayerBipedArmor(normal));
          layers_slim.set(0, new CustomLayerBipedArmor(slim));

          // Set Elytra renderers
          layers_default.set(6, new CustomLayerElytra(normal));
          layers_slim.set(6, new CustomLayerElytra(slim));

        } catch (IllegalAccessException e) {
          throw new RuntimeException("FamilyFunPack error: " + e.getMessage());
        } catch (NoSuchFieldException e) {
          throw new RuntimeException("FamilyFunPack error: no such field " + e.getMessage() + " in class RenderLivingBase");
        }

        // Init item renderer
        try {
          Class<Minecraft> mcClass = Minecraft.class;
          Field renderItem_field = null;
          try {
            renderItem_field = mcClass.getDeclaredField("field_175621_X");
          } catch (NoSuchFieldException e) {
            renderItem_field = mcClass.getDeclaredField("renderItem");
          }
          renderItem_field.setAccessible(true);
          Field itemRenderer_field = null;
          try {
            itemRenderer_field = mcClass.getDeclaredField("field_175620_Y");
          } catch (NoSuchFieldException e) {
            itemRenderer_field = mcClass.getDeclaredField("itemRenderer");
          }
          itemRenderer_field.setAccessible(true);

          renderItem_field.set(client, new CustomRenderItem(client.getTextureManager(), client.getRenderItem().getItemModelMesher().getModelManager(),
            client.getItemColors()));
          ((IReloadableResourceManager)(client.getResourceManager())).registerReloadListener((IResourceManagerReloadListener) renderItem_field.get(client));

          itemRenderer_field.set(client, new ItemRenderer(client));

          // Update Item renderer in entity renderer (for first person item render)
          client.entityRenderer = new EntityRenderer(client, client.getResourceManager());
          ((IReloadableResourceManager)(client.getResourceManager())).registerReloadListener((IResourceManagerReloadListener) client.entityRenderer);

        } catch (IllegalAccessException e) {
          throw new RuntimeException("FamilyFunPack error: " + e.getMessage());
        } catch (NoSuchFieldException e) {
          throw new RuntimeException("FamilyFunPack error: no such field " + e.getMessage() + " in class Minecraft");
        }
      }
    }

}
