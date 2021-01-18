package family_fun_pack.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketEntityTeleport;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;

import java.lang.Math;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Trace entities teleporting out of our render distance */

@SideOnly(Side.CLIENT)
public class TraceModule extends Module implements PacketListener {

  private static final Logger LOGGER = LogManager.getLogger();

  public TraceModule() {
    super("Trace teleports", "Print in chat coords of entity teleporting far away");
  }

  protected void enable() {
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 76);
  }

  protected void disable() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 76);
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketEntityTeleport tp = (SPacketEntityTeleport) packet;

    Minecraft mc = Minecraft.getMinecraft();

    if(Math.abs(mc.player.posX - tp.getX()) > 500d || Math.abs(mc.player.posZ - tp.getZ()) > 500d) {
      String name = "Unknown";
      Entity entity = mc.world.getEntityByID(tp.getEntityId());
      if(entity != null) name = entity.getClass().getSimpleName();

      double distance = Math.sqrt(Math.pow(mc.player.posX - tp.getX(), 2d) + Math.pow(mc.player.posZ - tp.getZ(), 2d));

      String warn = String.format("Entity [%s] teleported to [%.2f, %.2f, %.2f], %.2f blocks away", name, tp.getX(), tp.getY(), tp.getZ(), distance);
      FamilyFunPack.printMessage(warn);
      LOGGER.info("FFP - trace: " + warn);
    }

    return packet;
  }
}
