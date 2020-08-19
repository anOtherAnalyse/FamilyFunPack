package true_durability;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = TrueDurability.MODID, name = TrueDurability.NAME, version = TrueDurability.VERSION)
public class TrueDurability
{
    public static final String MODID = "true_durability";
    public static final String NAME = "True Durability";
    public static final String VERSION = "1.0";

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {}

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
      if(event.getSide() == Side.CLIENT) {
        MinecraftForge.EVENT_BUS.register(new Tooltip());
      }
    }

}
