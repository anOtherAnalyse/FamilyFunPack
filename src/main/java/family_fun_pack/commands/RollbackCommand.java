package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Set;
import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Rollback to previous teleportation */

@SideOnly(Side.CLIENT)
public class RollbackCommand extends Command implements PacketListener {

  private Vec3d position;
  private int teleport_id;

  public RollbackCommand() {
    super("rollback");
    this.teleport_id = -1;
  }

  public String usage() {
    return this.getName() + " [init]";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();

    if(args.length > 1) {
      if(args[1].equals("init")) { // Initiate a rollback
        this.position = new Vec3d(0d, 0d, 0d);
        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 47);
        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.SERVERBOUND, this, 0);
      } else return this.getUsage();
    } else if(this.teleport_id != -1) { // rollback

      mc.player.setPosition(this.position.x, this.position.y, this.position.z);
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketConfirmTeleport(this.teleport_id));

      this.onDisconnect();

      return String.format("Rollback to (%.2f, %.2f, %.2f)", this.position.x, this.position.y, this.position.z);
    } else return "No initialisation";

    return null;
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {

    if(id == 47) { // SPacketPlayerPosLook, rollback initialisation
      SPacketPlayerPosLook position = (SPacketPlayerPosLook) packet;

      Minecraft mc = Minecraft.getMinecraft();
      Set<SPacketPlayerPosLook.EnumFlags> flags = position.getFlags();

      double x = flags.contains(SPacketPlayerPosLook.EnumFlags.X) ? mc.player.posX + position.getX() : position.getX();
      double y = flags.contains(SPacketPlayerPosLook.EnumFlags.Y) ? mc.player.posY + position.getY() : position.getY();
      double z = flags.contains(SPacketPlayerPosLook.EnumFlags.Z) ? mc.player.posZ + position.getZ() : position.getZ();

      if(x != this.position.x || y != this.position.y || z != this.position.z) {
        this.position = new Vec3d(x, y, z);
        FamilyFunPack.printMessage(String.format("Rollback set to (%.2f, %.2f, %.2f)", this.position.x, this.position.y, this.position.z));
      }

      this.teleport_id = position.getTeleportId();
    } else return null; // CPacketConfirmTeleport to be blocked

    return packet;
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 47);
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.SERVERBOUND, this, 0);
    this.teleport_id = -1;
  }
}
