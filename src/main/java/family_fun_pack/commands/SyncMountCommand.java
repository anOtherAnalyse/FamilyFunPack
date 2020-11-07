package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraft.network.play.server.SPacketMoveVehicle;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.entities.EntityVoid;
import family_fun_pack.entities.GhostDonkey;
import family_fun_pack.network.PacketListener;

@SideOnly(Side.CLIENT)
public class SyncMountCommand extends Command implements PacketListener {

  public SyncMountCommand() {
    super("sync");
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();
    Entity ride = new EntityVoid(mc.world, 0);
    ride.setPosition(mc.player.posX + 1000d, mc.player.posY, mc.player.posZ + 1000d);
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 41);
    FamilyFunPack.getNetworkHandler().sendPacket(new CPacketVehicleMove(ride));
    return null;
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 41);
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 41);

    SPacketMoveVehicle move = (SPacketMoveVehicle) packet;

    FamilyFunPack.printMessage(String.format("Vehicle sync -> (%.2f, %.2f, %.2f)", move.getX(), move.getY(), move.getZ()));
    return packet;
  }
}
