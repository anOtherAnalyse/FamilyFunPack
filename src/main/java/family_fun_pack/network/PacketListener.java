package family_fun_pack.network;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.channel.ChannelHandlerContext;
import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.play.server.*;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.lang.Math;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.SpecialTagCompound;

@SideOnly(Side.CLIENT)
public class PacketListener extends NettyPacketDecoder {

  private final EnumPacketDirection direction;
  private boolean isPlay;

  public PacketListener(EnumPacketDirection direction) {
    super(direction);
    this.direction = direction; // let's save it twice
    this.isPlay = false;
  }

  protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) throws IOException, InstantiationException, IllegalAccessException, Exception {
    if (in.readableBytes() != 0) {

      int start_index = in.readerIndex(); // Mark start index
      super.decode(context, in, out); // Computer packet

      if(!this.isPlay) { // don't go fetch the attr every time)
        EnumConnectionState state = (EnumConnectionState)(context.channel().attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get());
        this.isPlay = (state == EnumConnectionState.PLAY);
      }

      if(this.isPlay && out.size() > 0) {
        Packet packet = (Packet)out.get(0);

        int id = ((EnumConnectionState)context.channel().attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get()).getPacketId(this.direction, packet);

        switch(id) {
          /*
          case 18:
            FamilyFunPack.printMessage("Server GUI close received");
            break;
          */
          case 14: // SPacketTabComplete
            {
              if(FamilyFunPack.configuration.player_completion) {
                FamilyFunPack.configuration.player_completion = false;
                SPacketTabComplete completion = (SPacketTabComplete) packet;
                NetHandlerPlayClient handler = (NetHandlerPlayClient)(FamilyFunPack.getNetHandler());
                if(handler == null) break;
                Minecraft client = Minecraft.getMinecraft();
                boolean add = true;
                for(String name : completion.getMatches()) {
                  if(handler.getPlayerInfo(name) == null) {
                    FamilyFunPack.printMessage("Addtional player: " + name);
                      add = false;
                  }
                }
                if(add) {
                  FamilyFunPack.printMessage("No additional players over " + Integer.toString(completion.getMatches().length) + " players");
                } else {
                  int total_tab = handler.getPlayerInfoMap().size();
                  FamilyFunPack.printMessage("Board contains [" + Integer.toString(total_tab) + "/" + Integer.toString(completion.getMatches().length) + "] players");
                }
              } else if(FamilyFunPack.configuration.commands_completion) {
                FamilyFunPack.configuration.commands_completion = false;
                SPacketTabComplete completion = (SPacketTabComplete) packet;
                Minecraft client = Minecraft.getMinecraft();
                FamilyFunPack.printMessage("Available commands:");
                for(String cmd : completion.getMatches()) {
                  FamilyFunPack.printMessage(cmd);
                }
              }
            }
            break;
          case 20: // Windows items
            {
              SPacketWindowItems packet_window = (SPacketWindowItems) packet;
              int end_index = in.readerIndex();

              PacketBuffer buf = new PacketBuffer(in);
              buf.readerIndex(start_index + 4);
              for(ItemStack i : packet_window.getItemStacks()) {
                if(buf.readShort() >= 0) {
                  buf.readerIndex(buf.readerIndex() + 1);
                  short true_damage = buf.readShort();
                  if(true_damage < 0) {
                    i.setTagCompound(new SpecialTagCompound(buf.readCompoundTag(), (int)true_damage));
                  } else buf.readCompoundTag();
                }
              }

              in.readerIndex(end_index);
            }
            break;
          case 22: // Set slot packet
            {
              SPacketSetSlot packet_slot = (SPacketSetSlot) packet;
              int end_index = in.readerIndex();

              // Read real item durability
              PacketBuffer buf = new PacketBuffer(in);
              buf.readerIndex(start_index + 4);
              if(buf.readShort() >= 0) {
                buf.readerIndex(buf.readerIndex() + 1);
                short real_damage = buf.readShort();
                if(real_damage < 0) { // We want to save this value
                  ItemStack stack = packet_slot.getStack();
                  stack.setTagCompound(new SpecialTagCompound(stack.getTagCompound(), (int)real_damage));
                }
              }

              in.readerIndex(end_index);
            }
            break;
          case 47: // SPacketPlayerPosLook
            {
              if(FamilyFunPack.configuration.currently_invulnerable) {
                if(! Minecraft.getMinecraft().player.isRiding()) {
                  SPacketPlayerPosLook old = (SPacketPlayerPosLook) packet;
                  Set<SPacketPlayerPosLook.EnumFlags> flags = old.getFlags();
                  flags.add(SPacketPlayerPosLook.EnumFlags.Y_ROT);
                  flags.add(SPacketPlayerPosLook.EnumFlags.X_ROT);

                  FamilyFunPack.configuration.last_teleport_id = old.getTeleportId();

                  SPacketPlayerPosLook spoof = new SPacketPlayerPosLook(old.getX(), old.getY(), old.getZ(), 0, 0, flags, old.getTeleportId());
                  out.set(0, spoof);
                } else out.clear();
              }
            }
            break;
          case 53: // SPacketRespawn
            {
              // Know when to activate invulnerability
              if(FamilyFunPack.configuration.invulnerable && !FamilyFunPack.configuration.currently_invulnerable) {
                SPacketRespawn respawn = (SPacketRespawn) packet;
                FamilyFunPack.configuration.currently_invulnerable = (respawn.getDimensionID() != Minecraft.getMinecraft().player.dimension);
              }
            }
            break;
          case 50: // SPacketDestroyEntities
            {
              if(FamilyFunPack.configuration.ride != null) {
                SPacketDestroyEntities destroy = (SPacketDestroyEntities) packet;
                for(int i : destroy.getEntityIDs()) {
                  if(i == FamilyFunPack.configuration.ride.hashCode()) {
                    FamilyFunPack.printMessage("Server destroyed our ride. Better dismount.");
                  }
                }
              }
            }
            break;
          case 63: // SPacketEntityEquipment
            {
              SPacketEntityEquipment equipment = (SPacketEntityEquipment) packet;
              int end_index = in.readerIndex();

              PacketBuffer buf = new PacketBuffer(in);
              buf.readerIndex(start_index + 3 + (int)Math.floor(Math.log((double)equipment.getEntityID()) / Math.log(128d)));
              if(buf.readShort() >= 0) {
                buf.readerIndex(buf.readerIndex() + 1);
                short real_damage = buf.readShort();
                if(real_damage < 0) { // We want to save this value
                  ItemStack stack = equipment.getItemStack();
                  stack.setTagCompound(new SpecialTagCompound(stack.getTagCompound(), (int)real_damage));
                }
              }

              in.readerIndex(end_index);
            }
            break;
          case 67: // SPacketSetPassengers
            {
              if(FamilyFunPack.configuration.ride != null) {
                SPacketSetPassengers passengers = (SPacketSetPassengers) packet;
                if(passengers.getEntityId() == FamilyFunPack.configuration.ride.hashCode()) {
                  boolean dismount = true;
                  for(int i : passengers.getPassengerIds()) {
                    if(i == Minecraft.getMinecraft().player.hashCode()) {
                      dismount = false;
                      break;
                    }
                  }
                  if(dismount) {
                    FamilyFunPack.printMessage("Server dismounted you from your (vanished) ride.");
                    Minecraft.getMinecraft().world.spawnEntity(FamilyFunPack.configuration.ride);
                    FamilyFunPack.configuration.ride = null;
                  }
                }
              }
            }
            break;
          case 76: // SPacketEntityTeleport
            {
              SPacketEntityTeleport teleport = (SPacketEntityTeleport) packet;
              Minecraft mc = Minecraft.getMinecraft();
              double x = mc.player.posX - teleport.getX();
              double z = mc.player.posZ - teleport.getZ();
              double distance = (x * x) + (z * z);
              if(distance >= 1000000d) {
                String name = Integer.toString(teleport.getEntityId());
                Entity entity = mc.world.getEntityByID(teleport.getEntityId());
                if(entity != null && entity instanceof EntityPlayer) name = " player " + ((EntityPlayer)entity).getName();
                FamilyFunPack.printMessage("\u00A7cEntity " + name + " teleported to (" + String.format("%.2f", teleport.getX()) + ", " + String.format("%.2f", teleport.getY()) + ", " + String.format("%.2f", teleport.getZ()) + ")");
              }
            }
            break;
        }

        // packets interception
        if(FamilyFunPack.configuration.block_player_packets && FamilyFunPack.configuration.inbound_block.contains(id)) {
          out.clear();
        }
      }
    }
  }

}
