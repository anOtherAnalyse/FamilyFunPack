package family_fun_pack;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;
import java.util.UUID;

import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.overlay.OverlayGui;
import family_fun_pack.key.KeyManager;
import family_fun_pack.network.NetworkHandler;
import family_fun_pack.modules.Modules;

@Mod(FamilyFunPack.MODID)
@OnlyIn(Dist.CLIENT)
public class FamilyFunPack {
    public static final String MODID = "family_fun_pack";
    public static final String NAME = "Family Fun Pack";

    private static Modules modules;
    private static MainGui mainGui;
    private static OverlayGui overlay;
    private static NetworkHandler networkHandler;

    private File confFile;

    public FamilyFunPack() {
      FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
    }

    /* Get NetworkHandler, for registering packets listeners */
    public static NetworkHandler getNetworkHandler() {
      return FamilyFunPack.networkHandler;
    }

    /* Get all Modules */
    public static Modules getModules() {
      return FamilyFunPack.modules;
    }

    /* Main GUI */
    public static MainGui getMainGui() {
      return FamilyFunPack.mainGui;
    }

    /* Overlay GUI */
    public static OverlayGui getOverlay() {
      return FamilyFunPack.overlay;
    }

    /* Print message in chat */
    public static void printMessage(String msg) {
      Minecraft.getInstance().gui.handleChat(ChatType.SYSTEM, new StringTextComponent(TextFormatting.BLUE + "[FFP] " + TextFormatting.RESET + msg), new UUID(0l, 0l));
    }

    private void init(FMLClientSetupEvent event) {
      this.confFile = new File("config", FamilyFunPack.MODID + ".toml");

      // Init overlay
      FamilyFunPack.overlay = new OverlayGui();

      // Init network handler
      FamilyFunPack.networkHandler = new NetworkHandler();

      // load modules configuration
      FamilyFunPack.modules = new Modules(this.confFile);

      // Init Main GUI
      FamilyFunPack.mainGui = new MainGui(FamilyFunPack.modules);
      KeyManager.addGuiKey(FamilyFunPack.mainGui);

      // register overlay
      MinecraftForge.EVENT_BUS.register(FamilyFunPack.overlay);

      // register network
      MinecraftForge.EVENT_BUS.register(FamilyFunPack.networkHandler);
    }
}
