package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.gui.interfaces.PreviewGui;

@SideOnly(Side.CLIENT)
public class PeekCommand extends Command {

  private GuiScreen screen;

  public PeekCommand() {
    super("ground-peek");
    this.screen = null;
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();

    ItemStack stack = null;
    for(EntityItem e : mc.world.<EntityItem>getEntitiesWithinAABB(EntityItem.class, mc.player.getEntityBoundingBox().grow(8.0D, 3.0D, 8.0D))) {
      if(e.getItem() != null && e.getItem().getItem() instanceof ItemShulkerBox) {
        stack = e.getItem();
        break;
      }
    }
    if(stack == null) return "No shulker item on ground close to you";

    NBTTagCompound tag = stack.getTagCompound();
    if(tag != null && tag.hasKey("BlockEntityTag") && tag.getTagId("BlockEntityTag") == 10) {
      NBTTagCompound blockTag = tag.getCompoundTag("BlockEntityTag");
      if(blockTag.hasKey("Items") && blockTag.getTagId("Items") == 9) {
        this.screen = new PreviewGui(blockTag.getTagList("Items", 10), false);
        MinecraftForge.EVENT_BUS.register(this);
        return null;
      }
    }

    return "Shulker is empty, no data";
  }

  @SubscribeEvent
  public void onGuiOpened(GuiOpenEvent event) {
    if(event.getGui() == null) {
      event.setGui(this.screen);
      this.screen = null;
    }
    MinecraftForge.EVENT_BUS.unregister(this);
  }
}
