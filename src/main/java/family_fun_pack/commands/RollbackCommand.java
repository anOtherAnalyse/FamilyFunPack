package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.EnumHand;
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
    return this.getName() + " [simple | double | tmp]";
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

      Packet<?>[] to_send = new Packet<?>[3];

      if(mode == Mode.SIMPLE) {
        to_send[0] = new CPacketConfirmTeleport(this.teleport_id); // Set position server-side
        to_send[1] = new CPacketPlayer.Rotation(mc.player.rotationYaw, mc.player.rotationPitch, true); // Refresh player chunk map
      } else if(mode == Mode.YEET) {

        if(mc.player.fishEntity != null) {
          Entity fish = mc.player.fishEntity.caughtEntity;

          if(fish != null && fish == mc.player.getRidingEntity()) {
            double d0 = this.position.x - mc.player.fishEntity.posX;
            double d1 = this.position.y - mc.player.fishEntity.posY;
            double d2 = this.position.z - mc.player.fishEntity.posZ;

            fish.motionX += d0 * 0.1D;
            fish.motionY += d1 * 0.1D;
            fish.motionZ += d2 * 0.1D;
          }
        }

        to_send[0] = new CPacketConfirmTeleport(this.teleport_id);
        to_send[1] = new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND); // fishing rod yeet
      } else {
        Entity ride = mc.player.getRidingEntity();
        if(ride == null) return "You are not riding anything";

        EntityVoid evoid = new EntityVoid(mc.world, 0);
        evoid.setPosition(ride.posX, ride.posY - 0.3d, ride.posZ);

        if(mode == Mode.DOUBLE) {
          to_send[0] = new CPacketConfirmTeleport(this.teleport_id);
          to_send[1] = new CPacketPlayer.Rotation(mc.player.rotationYaw, mc.player.rotationPitch, true);
        } else { // Tmp rollback
          to_send[0] = new CPacketPlayer.Rotation(mc.player.rotationYaw, mc.player.rotationPitch, true);
          to_send[1] = new CPacketPlayerTryUseItem(EnumHand.OFF_HAND); // 9b player chunk map refresh
        }

        to_send[2] = new CPacketVehicleMove(evoid);
      }

      /* Add exceptions to packet canceler */
      PacketInterceptionModule intercept = (PacketInterceptionModule) FamilyFunPack.getModules().getByName("Packets interception");
      for(int i = 0; i < to_send.length && to_send[i] != null; i ++) {
        intercept.addException(EnumPacketDirection.SERVERBOUND, to_send[i]);
      }

      // Set player position to rollback position (client-side)
      mc.player.setPosition(this.position.x, this.position.y, this.position.z);

      // Send everything in a row, hope it gets computed within the same tick
      for(int i = 0; i < to_send.length && to_send[i] != null; i ++) {
        FamilyFunPack.getNetworkHandler().sendPacket(to_send[i]);
      }

      if(mode != Mode.TMP) this.onDisconnect();

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

  private static enum Mode {SIMPLE, DOUBLE, TMP, YEET};
}
