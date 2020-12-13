package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.inventory.ContainerHorseChest;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.entities.EntityVoid;
import family_fun_pack.entities.GhostDonkey;
import family_fun_pack.network.PacketListener;

import family_fun_pack.modules.CommandsModule;

/* Open donkey inventory, usefull when there is a desync and the client is not aware of the mount */

@SideOnly(Side.CLIENT)
public class OpenDonkeyCommand extends Command implements PacketListener {

  public OpenDonkeyCommand() {
    super("open");
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 19);
    FamilyFunPack.getNetworkHandler().sendPacket(new CPacketEntityAction(new EntityVoid(Minecraft.getMinecraft().world, 0), CPacketEntityAction.Action.OPEN_INVENTORY));
    return null;
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 19);
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketOpenWindow open = (SPacketOpenWindow) packet;

    // Unregister in any case, don't keep listening
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 19);

    if("EntityHorse".equals(open.getGuiId())) {
      Minecraft mc = Minecraft.getMinecraft();

      int entity_id = open.getEntityId();
      Entity entity = mc.world.getEntityByID(entity_id);

      if(entity == null) { // desync between client & server, let's assume we are mounting a donkey
        GhostDonkey fake = new GhostDonkey(mc.world);
        // fake.setEntityId(entity_id); // performed in addEntityToWorld
        fake.setHorseSaddled(true);
        fake.setChested(true); // everything we need
        fake.setPosition(mc.player.posX, mc.player.posY, mc.player.posZ);

        // Spawn & ride
        mc.world.addEntityToWorld(entity_id, fake);
        mc.player.startRiding(fake, true);

        // Sync vehicle position
        ((CommandsModule)FamilyFunPack.getModules().getByName("FFP Commands")).getCommand("sync").execute(new String[0]);

        mc.player.openGuiHorseInventory(fake, new ContainerHorseChest(open.getWindowTitle().appendText(" [" + Integer.toString(entity_id) + "]"), open.getSlotCount()));
        mc.player.openContainer.windowId = open.getWindowId();
      } else if(entity instanceof AbstractHorse) {
        AbstractHorse horse = (AbstractHorse) entity;

        mc.player.openGuiHorseInventory(horse, new ContainerHorseChest(open.getWindowTitle(), open.getSlotCount()));
        mc.player.openContainer.windowId = open.getWindowId();
      } else FamilyFunPack.printMessage(TextFormatting.DARK_RED + "Error:" + TextFormatting.RESET + " Server gave us the inventory of an entity which is not an AbstractHorse");

      return null;
    }
    return packet;
  }
}
