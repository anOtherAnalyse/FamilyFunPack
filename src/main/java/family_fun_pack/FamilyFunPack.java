package family_fun_pack;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.util.List;
import java.util.LinkedList;

import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.overlay.OverlayGui;
import family_fun_pack.key.KeyListener;
import family_fun_pack.modules.Module;
import family_fun_pack.modules.Modules;
import family_fun_pack.modules.interfaces.*;
import family_fun_pack.network.NetworkHandler;

@Mod(modid = FamilyFunPack.MODID, name = FamilyFunPack.NAME, version = FamilyFunPack.VERSION)
@SideOnly(Side.CLIENT)
public class FamilyFunPack
{
    public static final String MODID = "family_fun_pack";
    public static final String NAME = "Family Fun Pack";
    public static final String VERSION = "1.1.2";

    private static NetworkHandler networkHandler;
    private static Modules modules;
    private static OverlayGui overlay;
    private static KeyListener keyListener;
    private static MainGui mainGui;

    private File confFile;

    public static NetworkHandler getNetworkHandler() {
      return FamilyFunPack.networkHandler;
    }

    public static Modules getModules() {
      return FamilyFunPack.modules;
    }

    public static OverlayGui getOverlay() {
      return FamilyFunPack.overlay;
    }

    public static MainGui getMainGui() {
      return FamilyFunPack.mainGui;
    }

    public static void addModuleKey(int key, Module module) {
      FamilyFunPack.keyListener.addModuleKey(key, module);
    }

    public static void printMessage(String msg) {
      Minecraft.getMinecraft().ingameGUI.addChatMessage(ChatType.SYSTEM, new TextComponentString(TextFormatting.BLUE + "[FFP] " + TextFormatting.RESET + msg));
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
      this.confFile = event.getSuggestedConfigurationFile();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
      if(event.getSide() == Side.CLIENT) {

        // Init overlay
        FamilyFunPack.overlay = new OverlayGui();

        // Init network handler
        FamilyFunPack.networkHandler = new NetworkHandler();

        // Init key listener
        FamilyFunPack.keyListener = new KeyListener();

        // load modules configuration
        FamilyFunPack.modules = new Modules(this.confFile);

        // register overlay
        MinecraftForge.EVENT_BUS.register(FamilyFunPack.overlay);

        // register network
        MinecraftForge.EVENT_BUS.register(FamilyFunPack.networkHandler);

        // Init interfaces modules
        List<InterfaceModule> interfaces = new LinkedList<InterfaceModule>();
        interfaces.add(new PacketsSelectionModule().dependsOn(FamilyFunPack.modules.getByName("Packets interception")));
        interfaces.add(new InfoItemModule());
        interfaces.add(new SearchSelectionModule().dependsOn(FamilyFunPack.modules.getByName("Search")));

        // Init interface
        FamilyFunPack.mainGui = new MainGui(FamilyFunPack.modules, interfaces);
        FamilyFunPack.keyListener.setGui(FamilyFunPack.mainGui);

        // Register key listener
        MinecraftForge.EVENT_BUS.register(FamilyFunPack.keyListener);
      }
    }
}
