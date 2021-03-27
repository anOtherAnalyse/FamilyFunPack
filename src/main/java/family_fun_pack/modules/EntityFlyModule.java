package family_fun_pack.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketInput;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraft.network.play.server.SPacketMoveVehicle;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Vanilla entity fly */

@SideOnly(Side.CLIENT)
public class EntityFlyModule extends Module implements PacketListener {

  public static Vec3d getDirectionFlat(float yaw) {
    yaw *= 0.017453292F;
    double x = MathHelper.cos(yaw);
    double z = MathHelper.sin(yaw);
    double len = MathHelper.sqrt(x * x + z * z);
    return new Vec3d(x/len, 0, z/len);
  }

  private Vec3d position;
  private int counter;

  private long timeout;

  public EntityFlyModule() {
    super("Entity fly", "Fly your ride");
  }

  protected void enable() {
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.SERVERBOUND, this, 16, 22);
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 41);
    MinecraftForge.EVENT_BUS.register(this);
  }

  protected void disable() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.SERVERBOUND, this, 16, 22);
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 41);
    MinecraftForge.EVENT_BUS.unregister(this);
    this.onDisconnect();
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    switch(id) {
      case 16: { // CPacketVehicleMove
        Minecraft mc = Minecraft.getMinecraft();

        if((! mc.player.isRiding()) || System.currentTimeMillis() < this.timeout) return null;

        if(this.position == null) {
          Entity ride = mc.player.getRidingEntity();
          this.position = new Vec3d(ride.posX, ride.posY, ride.posZ);
        }

        CPacketVehicleMove move = (CPacketVehicleMove) packet;
        if(--this.counter < 0) { // Do fly mechanics
          Entity ride = mc.player.getRidingEntity();
          Vec3d old = this.position;

          if(mc.gameSettings.keyBindJump.isKeyDown()) {
            this.position = this.position.addVector(0, 1d, 0);
          } else if(ride instanceof AbstractHorse && ((AbstractHorse) ride).isRearing()) {
            ((AbstractHorse) ride).setRearing(false);
          }

          if(mc.gameSettings.keyBindSneak.isKeyDown()) {
            this.position = this.position.addVector(0, -1d, 0);
          }

          if(mc.gameSettings.keyBindForward.isKeyDown()) {
            this.position = this.position.add(EntityFlyModule.getDirectionFlat(mc.player.rotationYaw + 90f));
          }

          if(mc.gameSettings.keyBindBack.isKeyDown()) {
            this.position = this.position.add(EntityFlyModule.getDirectionFlat(mc.player.rotationYaw + 270f));
          }

          if(mc.gameSettings.keyBindLeft.isKeyDown()) {
            this.position = this.position.add(EntityFlyModule.getDirectionFlat(mc.player.rotationYaw));
          }

          if(mc.gameSettings.keyBindRight.isKeyDown()) {
            this.position = this.position.add(EntityFlyModule.getDirectionFlat(mc.player.rotationYaw + 180f));
          }

          // Send updates
          if(! old.equals(this.position)) {
            this.counter = 1;

            if(this.position.y >= old.y) {
              this.counter += 1;
              ride.setPosition(this.position.x, this.position.y + 0.03125d, this.position.z);
              FamilyFunPack.getNetworkHandler().sendPacket(new CPacketVehicleMove(ride));
            }

            ride.setPosition(this.position.x, this.position.y, this.position.z);
            FamilyFunPack.getNetworkHandler().sendPacket(new CPacketVehicleMove(ride));
          } else {
            ride.setPosition(this.position.x, this.position.y, this.position.z);
          }

          mc.player.setPosition(this.position.x, this.position.y + ride.getMountedYOffset() + mc.player.getYOffset(), this.position.z);

          return null;
        }
      }
      break;
      case 22: {
        if(this.position != null) {
          CPacketInput input = (CPacketInput) packet;
          return new CPacketInput(input.getStrafeSpeed(), input.getForwardSpeed(), false, false);
        }
      }
      break;
      case 41: {
        SPacketMoveVehicle move = (SPacketMoveVehicle) packet;
        this.position = new Vec3d(move.getX(), move.getY(), move.getZ());
        this.counter = 0;
        this.timeout = System.currentTimeMillis() + 100; // Timeout before sending new move packets
      }
      break;
    }

    return packet;
  }

  @SubscribeEvent
  public void onMount(EntityMountEvent event) {
    if(event.getEntityMounting() instanceof EntityPlayerSP && event.isDismounting()) {
      this.onDisconnect();
    }
  }

  public void onDisconnect() {
    this.position = null;
    this.counter = 0;
  }
}
