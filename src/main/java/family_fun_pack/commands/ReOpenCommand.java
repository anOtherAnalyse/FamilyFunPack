package family_fun_pack.commands;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.NoCloseModule;

/* Re-open closed inventory client-side */

@SideOnly(Side.CLIENT)
public class ReOpenCommand extends Command {

  public ReOpenCommand() {
    super("reopen");
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    NoCloseModule module = (NoCloseModule) FamilyFunPack.getModules().getByName("Silent close");
    GuiScreen screen = module.getOpenedContainer();
    if(screen == null) return "No container is currently opened";
    MinecraftForge.EVENT_BUS.register(new GuiOpener(screen));
    return null;
  }

  // Forge event listener used to open GUI
  private static class GuiOpener {

    private GuiScreen gui;

    public GuiOpener(GuiScreen gui) {
      this.gui = gui;
    }

    @SubscribeEvent
    public void onGuiOpened(GuiOpenEvent event) {
      if(event.getGui() == null) {
        event.setGui(this.gui);
      }
      MinecraftForge.EVENT_BUS.unregister(this);
    }
  }
}
