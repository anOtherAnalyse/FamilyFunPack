package family_fun_pack.commands;

import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketCustomSound;
import net.minecraft.network.play.server.SPacketEntityTeleport;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.network.play.server.SPacketSpawnGlobalEntity;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import java.lang.Class;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Dump received packets in chat */

@SideOnly(Side.CLIENT)
public class PacketDumpCommand extends Command implements PacketListener {

  private String last_packet;
  private int last_id;
  private int count;

  public PacketDumpCommand() {
    super("pckdump");
    this.count = 0;
    this.last_id = -1;
    this.last_packet = null;
  }

  public String usage() {
    return this.getName() + " <on|off>";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      if(args[1].equals("on")) {
        this.last_id = -1;
        this.last_packet = null;
        for(int i = 0; i < 80; i ++) {
          FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, i);
        }
        return "pckdump on";
      } else if(args[1].equals("client")) {
        this.last_id = -1;
        this.last_packet = null;
        for(int i = 0; i < 33; i ++) {
          FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.SERVERBOUND, this, i);
        }
        return "pckcltdump on";
      } else {
        FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this);
        FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.SERVERBOUND, this);
        return "pckdump off";
      }
    }
    return this.getUsage();
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {

    if(id != this.last_id) {
      if(this.last_packet != null) {
        FamilyFunPack.printMessage("Received packet [" + this.last_packet + "] x " + Integer.toString(this.count));
      }

      if(packet instanceof SPacketEntityTeleport) {
        SPacketEntityTeleport teleport = (SPacketEntityTeleport) packet;
        FamilyFunPack.printMessage(String.format("Received SPacketEntityTeleport for (%.2f, %.2f, %.2f) id: %d", teleport.getX(), teleport.getY(), teleport.getZ(), teleport.getEntityId()));
        this.last_packet = null;
        this.last_id = -1;
      } else if(packet instanceof SPacketCustomSound) {
        SPacketCustomSound sound = (SPacketCustomSound) packet;
        FamilyFunPack.printMessage(String.format("Received SPacketCustomSound at (%.2f, %.2f, %.2f)", sound.getX(), sound.getY(), sound.getZ()));
        this.last_packet = null;
        this.last_id = -1;
      } else if(packet instanceof SPacketExplosion) {
        SPacketExplosion expl = (SPacketExplosion) packet;
        FamilyFunPack.printMessage(String.format("Received SPacketExplosion at (%.2f, %.2f, %.2f)", expl.getX(), expl.getY(), expl.getZ()));
        this.last_packet = null;
        this.last_id = -1;
      } else if(packet instanceof SPacketSoundEffect) {
        SPacketSoundEffect sound = (SPacketSoundEffect) packet;
        FamilyFunPack.printMessage(String.format("Received SPacketSoundEffect at (%.2f, %.2f, %.2f)", sound.getX(), sound.getY(), sound.getZ()));
        this.last_packet = null;
        this.last_id = -1;
      } else if(packet instanceof SPacketSpawnGlobalEntity) {
        SPacketSpawnGlobalEntity glb = (SPacketSpawnGlobalEntity) packet;
        FamilyFunPack.printMessage(String.format("Received SPacketSpawnGlobalEntity at (%.2f, %.2f, %.2f)", glb.getX(), glb.getY(), glb.getZ()));
        this.last_packet = null;
        this.last_id = -1;
      } else {
        this.last_packet = packet.getClass().getSimpleName();
        this.last_id = id;
        this.count = 1;
      }
    } else this.count += 1;

    return packet;
  }
}
