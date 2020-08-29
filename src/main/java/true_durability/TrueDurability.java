package true_durability;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

@Mod(modid = TrueDurability.MODID, name = TrueDurability.NAME, version = TrueDurability.VERSION)
@SideOnly(Side.CLIENT)
public class TrueDurability
{
    public static final String MODID = "true_durability";
    public static final String NAME = "True Durability";
    public static final String VERSION = "1.1";

    private static NetworkManager networkManager;
    public static Configuration configuration;

    public static void setNetworkManager(NetworkManager networkManager) {
      TrueDurability.networkManager = networkManager;
    }

    public static void sendPacket(Packet<?> packet) {
      if(TrueDurability.networkManager != null) {
        TrueDurability.networkManager.sendPacket(packet);
      }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {}

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
      if(event.getSide() == Side.CLIENT) {
        TrueDurability.configuration = new Configuration();
        MinecraftForge.EVENT_BUS.register(new Tooltip());
      }
    }

}
