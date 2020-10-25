package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.inventory.ContainerHorseChest;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.entities.EntityVoid;
import family_fun_pack.network.PacketListener;

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
      EntityDonkey fake = new EntityDonkey(mc.world); // Let's assume it's a donkey
      fake.setEntityId(open.getEntityId());
      fake.setHorseSaddled(true);
      fake.setChested(true);
      mc.player.openGuiHorseInventory(fake, new ContainerHorseChest(open.getWindowTitle(), open.getSlotCount()));
      mc.player.openContainer.windowId = open.getWindowId();
      return null;
    }
    return packet;
  }
}
