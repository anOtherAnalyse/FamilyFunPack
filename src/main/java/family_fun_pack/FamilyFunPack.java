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

import family_fun_pack.render.CustomLayerBipedArmor;

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

        // Init renderers
        try {
          Minecraft client = Minecraft.getMinecraft();
          RenderManager renderManager = client.getRenderManager();
          RenderPlayer normal = renderManager.getSkinMap().get("default");
          RenderPlayer slim = renderManager.getSkinMap().get("slim");
          Class<RenderLivingBase> renderLivingClass = RenderLivingBase.class;
          Field layers_field = renderLivingClass.getDeclaredField("layerRenderers");
          layers_field.setAccessible(true);
          List<Object> layers_default = (List<Object>)layers_field.get(normal);
          List<Object> layers_slim = (List<Object>)layers_field.get(slim);
          layers_default.set(0, new CustomLayerBipedArmor(normal)); // Set our own renderer for default skin armor
          layers_slim.set(0, new CustomLayerBipedArmor(slim)); // Set our own renderer for slim skin armor
        } catch (IllegalAccessException e) {
          throw new RuntimeException("FamilyFunPack error: " + e.getMessage());
        } catch (NoSuchFieldException e) {
          throw new RuntimeException("FamilyFunPack error: " + e.getMessage());
        }
      }
    }

}
