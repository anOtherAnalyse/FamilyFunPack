package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraft.network.play.server.SPacketMoveVehicle;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.entities.EntityVoid;
import family_fun_pack.entities.GhostDonkey;
import family_fun_pack.network.PacketListener;

/* Remote ride for when your ride is not in your render distance */

@SideOnly(Side.CLIENT)
public class RemoteRideCommand extends Command implements PacketListener {

  private Vec3d remote_position;
  private Vec3d local_position;
  private Vec3d start_position;

  private boolean relY;

  public RemoteRideCommand() {
    super("remote");
    this.relY = false;
  }

  public String usage() {
    return this.getName() + " [peek | center | relY]";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();

    if(args.length > 1) {

      Entity ride = mc.player.getRidingEntity();
      if(ride == null) return "Remote riding was not set up";

      switch(args[1]) {
        case "peek":
          {
            EntityVoid remote = new EntityVoid(mc.world, 0);
            remote.setPosition(this.remote_position.x + 1000, this.remote_position.y + 1000, this.remote_position.z + 1000);
            FamilyFunPack.getNetworkHandler().sendPacket(new CPacketVehicleMove(remote));
          }
          break;
        case "center":
          {
            ride.setPosition(this.start_position.x, this.start_position.y, this.start_position.z);
            this.local_position = new Vec3d(this.start_position.x, this.start_position.y, this.start_position.z);
          }
          break;
        case "relY":
          this.relY = !this.relY;
          break;
        default: return this.getUsage();
      }

    } else {
      GhostDonkey fake = new GhostDonkey(mc.world);
      fake.setHorseSaddled(true);
      fake.setChested(true);
      fake.setPosition(mc.player.posX, mc.player.posY, mc.player.posZ);

      this.remote_position = new Vec3d(0d, 0d, 0d);
      this.local_position = new Vec3d(mc.player.posX, mc.player.posY, mc.player.posZ);
      this.start_position = new Vec3d(mc.player.posX, mc.player.posY, mc.player.posZ);

      mc.world.addEntityToWorld(-1, fake);
      mc.player.startRiding(fake, true);

      FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 41);
      FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.SERVERBOUND, this, 16);
      MinecraftForge.EVENT_BUS.register(this);
    }

    return null;
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    Minecraft mc = Minecraft.getMinecraft();
    Entity ride = mc.player.getRidingEntity();

    if(ride == null) {
      this.onDisconnect();
      return packet;
    }

    switch(id) {
      case 41: // SPacketMoveVehicle
        {
          SPacketMoveVehicle move = (SPacketMoveVehicle) packet;
          FamilyFunPack.printMessage(String.format("vehicle is at (%.2f, %.2f, %.2f)", move.getX(), move.getY(), move.getZ()));
          this.remote_position = new Vec3d(move.getX(), move.getY(), move.getZ());
          this.local_position = new Vec3d(ride.posX, ride.posY, ride.posZ);
        }
        break;
      case 16: // CPacketVehicleMove
        {
          CPacketVehicleMove move = (CPacketVehicleMove) packet;
          if(! this.relY)
            this.remote_position = new Vec3d(this.remote_position.x + (move.getX() - this.local_position.x), move.getY(), this.remote_position.z + (move.getZ() - this.local_position.z));
          else
            this.remote_position = new Vec3d(this.remote_position.x + (move.getX() - this.local_position.x), this.remote_position.y + (move.getY() - this.local_position.y), this.remote_position.z + (move.getZ() - this.local_position.z));

          this.local_position = new Vec3d(move.getX(), move.getY(), move.getZ());

          EntityVoid remote = new EntityVoid(mc.world, 0);
          remote.setPositionAndRotation(this.remote_position.x, this.remote_position.y, this.remote_position.z, move.getYaw(), move.getPitch());

          return new CPacketVehicleMove(remote);
        }
    }
    return null;
  }

  @SubscribeEvent
  public void onMount(EntityMountEvent event) { // Dismount cleanup
    if(event.getEntityMounting() instanceof EntityPlayerSP && event.getEntityBeingMounted().getEntityId() == -1 && (!event.isMounting())) {
      this.onDisconnect();
      Minecraft.getMinecraft().world.removeEntityFromWorld(-1);
    }
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 41);
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.SERVERBOUND, this, 16);
    MinecraftForge.EVENT_BUS.unregister(this);
    this.relY = false;
  }
}
