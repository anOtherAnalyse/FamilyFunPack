package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.network.play.server.SPacketSetPassengers;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Entity desync */

@SideOnly(Side.CLIENT)
public class VanishCommand extends Command implements PacketListener {

  private Entity ride;

  public VanishCommand() {
    super("vanish");
    this.ride = null;
  }

  public String usage() {
    return this.getName() + " <dismount | remount>";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      Minecraft mc = Minecraft.getMinecraft();
      String ret = null;
      switch(args[1]) {
        case "dismount":
          {
            if(mc.player.isRiding()) {
              this.ride = mc.player.getRidingEntity();
              mc.player.dismountRidingEntity();
              mc.world.removeEntity(this.ride);
              FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 50, 67);
              FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.SERVERBOUND, this, 12, 13, 14);
            } else ret = "You are not riding anything";
          }
          break;
        case "remount":
          {
            if(this.ride != null) {
              if(! mc.player.isRiding()) {
                this.ride.isDead = false;
                mc.world.spawnEntity(this.ride);
                mc.player.startRiding(this.ride, true);
                if(mc.player.isRiding())
                  ret = "Entity " + Integer.toString(this.ride.hashCode()) + " remounted";
                else
                  ret = "Could not remount";
              }
              this.onDisconnect();
            } else ret = "Nothing to remount";
          }
          break;
        default:
          ret = "Unknown argument " + args[1];
      }
      return ret;
    }
    return "dismount or remount ?";
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    if(direction == EnumPacketDirection.CLIENTBOUND) {
      if(id == 50) { // SPacketDestroyEntities
        SPacketDestroyEntities destroy = (SPacketDestroyEntities) packet;
        for(int i : destroy.getEntityIDs()) {
          if(i == this.ride.hashCode()) {
            FamilyFunPack.printMessage("Server destroyed our ride. We dismounted.");
            this.onDisconnect();
          }
        }
      } else { // SPacketSetPassengers
        Minecraft mc = Minecraft.getMinecraft();
        SPacketSetPassengers passengers = (SPacketSetPassengers) packet;
        boolean dismount = (passengers.getEntityId() == this.ride.hashCode());
        for(int i : passengers.getPassengerIds()) {
          if(i == mc.player.hashCode()) {
            dismount = !dismount;
            break;
          }
        }
        if(dismount) {
          FamilyFunPack.printMessage("Server dismounted you from your (vanished) ride.");
          this.ride.isDead = false;
          mc.world.spawnEntity(this.ride);
          this.onDisconnect();
        }
      }
    } else { // CPacketPlayer
      Minecraft mc = Minecraft.getMinecraft();
      if(! mc.player.isRiding()) {
        mc.player.onGround = true;
        this.ride.setPosition(mc.player.posX, mc.player.posY, mc.player.posZ);
        return new CPacketVehicleMove(this.ride);
      } else this.onDisconnect();
    }
    return packet;
  }

  public void onDisconnect() {
    if(this.ride != null) {
      this.ride = null;
      FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 50, 67);
      FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.SERVERBOUND, this, 12, 13, 14);
    }
  }

}
