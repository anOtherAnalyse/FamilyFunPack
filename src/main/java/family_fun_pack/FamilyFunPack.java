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
      }
    }

}
