package family_fun_pack.modules;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Stalk player, know when they connect, disconnect, speak in chat.. */

@SideOnly(Side.CLIENT)
public class StalkModule extends AbstractPlayersRegister implements PacketListener {

  private static final TextFormatting ANNOUNCE_COLOR = TextFormatting.DARK_PURPLE;

  public StalkModule() {
    super("Stalk players", "See when given players connect/disconnect/speak", "stalk");
  }

  protected void enable() {
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 15, 46);

    NetHandlerPlayClient handler = AbstractPlayersRegister.getNetHandler();
    if(handler != null) {
      for(NetworkPlayerInfo info : handler.getPlayerInfoMap()) {
        this.uuids_lock.readLock().lock();
        if(this.uuids.contains(info.getGameProfile().getId())) {
          FamilyFunPack.printMessage(StalkModule.ANNOUNCE_COLOR + "Player " + info.getGameProfile().getName() + " is connected [" + info.getGameType().getName() + "]");
        }
        this.uuids_lock.readLock().unlock();
      }
    }
  }

  protected void disable() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 15, 46);
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {

    if(id == 46) { // SPacketPlayerListItem
      SPacketPlayerListItem list = (SPacketPlayerListItem) packet;

      if(list.getAction() == SPacketPlayerListItem.Action.UPDATE_LATENCY) return packet;

      NetHandlerPlayClient handler = AbstractPlayersRegister.getNetHandler();

      for(SPacketPlayerListItem.AddPlayerData entry : list.getEntries()) {

        UUID uuid = entry.getProfile().getId();
        String name = entry.getProfile().getName();
        if(name == null) {
          NetworkPlayerInfo info = handler.getPlayerInfo(entry.getProfile().getId());
          if(info == null) continue;
          name = info.getGameProfile().getName();
        }

        this.uuids_lock.readLock().lock();
        boolean flag = this.uuids.contains(uuid);
        this.uuids_lock.readLock().unlock();

        if(flag) {
          switch(list.getAction()) {
            case ADD_PLAYER:
              {
                String verb = null;
                if(handler.getPlayerInfoMap().isEmpty()) {
                  verb = " is connected";
                } else verb = " joined";
                if(entry.getDisplayName() == null)
                  FamilyFunPack.printMessage(StalkModule.ANNOUNCE_COLOR + "Player " + name + verb + " [" + entry.getGameMode().getName() + "]");
                else FamilyFunPack.printMessage(StalkModule.ANNOUNCE_COLOR + "Player " + name + verb + " under the name \"" + entry.getDisplayName().toString() + "\" [" + entry.getGameMode().getName() + "]");
              }
              break;
            case REMOVE_PLAYER:
              {
                FamilyFunPack.printMessage(StalkModule.ANNOUNCE_COLOR + "Player " + name + " disconnected");
              }
              break;
            case UPDATE_GAME_MODE:
              FamilyFunPack.printMessage(StalkModule.ANNOUNCE_COLOR + "Player " + name + " changed their game mode to " + entry.getGameMode().getName());
              break;
            case UPDATE_DISPLAY_NAME:
              if(entry.getDisplayName() == null)
                FamilyFunPack.printMessage(StalkModule.ANNOUNCE_COLOR + "Player " + name + " removed their custom display name");
              else FamilyFunPack.printMessage(StalkModule.ANNOUNCE_COLOR + "Player " + name + " changed their display name to \"" + entry.getDisplayName().toString() + "\"");
              break;
          }
        }
      }
    } else { // SPacketChat
      SPacketChat chat = (SPacketChat) packet;

      GameProfile sender = AbstractPlayersRegister.getSender(chat.getChatComponent());
      if(sender != null) {
        this.uuids_lock.readLock().lock();
        boolean flag = this.uuids.contains(sender.getId());
        this.uuids_lock.readLock().unlock();

        if(flag) {
          TextComponentString nmsg = new TextComponentString(chat.getChatComponent().getFormattedText().replace(String.format("<%s>", sender.getName()), String.format("%s<%s>%s", StalkModule.ANNOUNCE_COLOR, sender.getName(), TextFormatting.RESET)));
          packet = new SPacketChat(nmsg, chat.getType());
        }
      }
    }

    return packet;
  }
}
