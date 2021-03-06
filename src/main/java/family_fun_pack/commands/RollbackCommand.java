package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Set;
import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.entities.EntityVoid;
import family_fun_pack.network.PacketListener;
import family_fun_pack.modules.PacketInterceptionModule;

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
    return this.getName() + " [simple | double]";
  }

  public String execute(String[] args) {

    if(this.teleport_id == -1) { // Init a rollback
      this.position = new Vec3d(0d, 0d, 0d);
      FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 47);
      FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.SERVERBOUND, this, 0);
    } else { // rollback
      Minecraft mc = Minecraft.getMinecraft();

      Mode mode = Mode.SIMPLE; // Type of rollback
      if(args.length > 1) {
        try {
          mode = Mode.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
          return this.getUsage();
        }
      }

      PacketInterceptionModule intercept = (PacketInterceptionModule) FamilyFunPack.getModules().getByName("Packets interception");
      Packet<?>[] to_send = new Packet<?>[3];

      // Set player position to rollback position
      mc.player.setPosition(this.position.x, this.position.y, this.position.z);
      to_send[0] = new CPacketConfirmTeleport(this.teleport_id); // Set position server-side
      to_send[1] = new CPacketPlayer.Rotation(mc.player.rotationYaw, mc.player.rotationPitch, true); // Refresh player chunk map

      if(mode == Mode.SIMPLE) {
        to_send[2] = null;
      } else {
        Entity ride = mc.player.getRidingEntity();
        if(ride == null) return "You are not riding anything";

        EntityVoid evoid = new EntityVoid(mc.world, 0);
        evoid.setPosition(ride.posX, ride.posY - 0.3d, ride.posZ);

        to_send[2] = new CPacketVehicleMove(evoid);
        intercept.addException(EnumPacketDirection.SERVERBOUND, to_send[2]);
      }

      intercept.addException(EnumPacketDirection.SERVERBOUND, to_send[1]); // Make it work with packet canceler

      // Send everything in a row
      for(int i = 0; i < 3; i ++) {
        if(to_send[i] != null) {
          FamilyFunPack.getNetworkHandler().sendPacket(to_send[i]);
        }
      }

      this.onDisconnect();

      return String.format("Rollback to (%.2f, %.2f, %.2f)", this.position.x, this.position.y, this.position.z);
    }

    return "Rollback initialized";
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

  private static enum Mode {SIMPLE, DOUBLE};
}
