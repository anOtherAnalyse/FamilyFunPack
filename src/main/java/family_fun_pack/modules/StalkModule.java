package family_fun_pack.modules;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import java.lang.Thread;
import java.net.Proxy;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Stalk player, know when they connect, disconnect, speak in chat.. */

@SideOnly(Side.CLIENT)
public class StalkModule extends Module implements PacketListener {

  private static final TextFormatting ANNOUNCE_COLOR = TextFormatting.LIGHT_PURPLE;

  private Set<UUID> uuids;
  private ReadWriteLock uuids_lock;

  private Pattern chat_pattern;

  public GameProfileRepository repository;

  public StalkModule() {
    super("Stalk players", "See when given players connect/disconnect/speak");
    this.uuids = new HashSet<UUID>();
    this.uuids_lock = new ReentrantReadWriteLock();
    this.chat_pattern = Pattern.compile("^<([^>]+)>[\\s\\S]*$");

    YggdrasilAuthenticationService auth = new YggdrasilAuthenticationService(Proxy.NO_PROXY, null);
    this.repository = auth.createProfileRepository();
  }

  protected void enable() {
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 15, 46);

    NetHandlerPlayClient handler = this.getNetHandler();
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

  public void addUUID(UUID uuid) {
    this.uuids_lock.writeLock().lock();
    this.uuids.add(uuid);
    this.uuids_lock.writeLock().unlock();
  }

  public void addPlayer(String player) {
    Thread t = new LookupThread(player, this, LookupThread.Action.ADD);
    t.start();
  }

  public boolean delUUID(UUID uuid) {
    this.uuids_lock.writeLock().lock();
    boolean flag = this.uuids.remove(uuid);
    this.uuids_lock.writeLock().unlock();
    return flag;
  }

  public void delPlayer(String player) {
    Thread t = new LookupThread(player, this, LookupThread.Action.DEL);
    t.start();
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    NetHandlerPlayClient handler = this.getNetHandler();

    if(id == 46) { // SPacketPlayerListItem
      SPacketPlayerListItem list = (SPacketPlayerListItem) packet;

      if(list.getAction() == SPacketPlayerListItem.Action.UPDATE_LATENCY) return packet;

      for(SPacketPlayerListItem.AddPlayerData entry : list.getEntries()) {

        UUID uuid = entry.getProfile().getId();
        String name = entry.getProfile().getName();
        if(name == null) {
          NetworkPlayerInfo info = handler.getPlayerInfo(entry.getProfile().getId());
          if(info == null) name = uuid.toString();
          else name = info.getGameProfile().getName();
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
      String message = chat.getChatComponent().getFormattedText().replaceAll("§[0-9a-zA-Z]", "");
      Matcher match = this.chat_pattern.matcher(message);
      if(match.matches()) {
        String name = match.group(1);
        NetworkPlayerInfo info = handler.getPlayerInfo(name);
        if(info != null) {
          this.uuids_lock.readLock().lock();
          boolean flag = this.uuids.contains(info.getGameProfile().getId());
          this.uuids_lock.readLock().unlock();
          if(flag) {
            TextComponentString nmsg = new TextComponentString(chat.getChatComponent().getFormattedText().replace(String.format("<%s>", name), String.format("%s<%s>%s", StalkModule.ANNOUNCE_COLOR, name, TextFormatting.RESET)));
            packet = new SPacketChat(nmsg, chat.getType());
          }
        }
      }
    }

    return packet;
  }

  public void save(Configuration configuration) {
    super.save(configuration);
    this.uuids_lock.readLock().lock();
    String[] array = new String[this.uuids.size()];
    int i = 0;
    for(UUID uuid : this.uuids) {
      array[i] = uuid.toString();
      i ++;
    }
    this.uuids_lock.readLock().unlock();
    configuration.get(this.name, "uuids", new String[0]).set(array);
  }

  public void load(Configuration configuration) {
    String[] array = configuration.get(this.name, "uuids", new String[0]).getStringList();
    this.uuids_lock.writeLock().lock();
    for(String uuid : array) this.uuids.add(UUID.fromString(uuid));
    this.uuids_lock.writeLock().unlock();
    super.load(configuration);
  }

  private NetHandlerPlayClient getNetHandler() {
    INetHandler inet_handler = FamilyFunPack.getNetworkHandler().getNetHandler();
    if(inet_handler != null && inet_handler instanceof NetHandlerPlayClient) {
      return (NetHandlerPlayClient) inet_handler;
    }
    return null;
  }

  /* Profile lookup thread */
  private static class LookupThread extends Thread {

    public String player_name;
    public StalkModule module;
    public Action action;

    public LookupThread(String name, StalkModule module, Action action) {
      this.player_name = name;
      this.module = module;
      this.action = action;
    }

    public void run() {
      this.module.repository.findProfilesByNames(new String[] {this.player_name}, Agent.MINECRAFT, new LookupCallback(this));
    }

    public static enum Action {ADD, DEL};

    private static class LookupCallback implements ProfileLookupCallback {

      private LookupThread thread;

      public LookupCallback(LookupThread thread) {
        this.thread = thread;
      }

      public void onProfileLookupSucceeded(GameProfile profile) {
        Configuration configuration = FamilyFunPack.getModules().getConfiguration();
        switch(this.thread.action) {
          case ADD:
            this.thread.module.addUUID(profile.getId());
            FamilyFunPack.printMessage(profile.getName() + " added to stalking list");
            break;
          case DEL:
            if(this.thread.module.delUUID(profile.getId()))
              FamilyFunPack.printMessage(profile.getName() + " removed from stalking list");
            else
              FamilyFunPack.printMessage(profile.getName() + " is not in the stalking list");
            break;
        }
        this.thread.module.save(configuration);
        configuration.save();
      }

      public void onProfileLookupFailed(GameProfile profile, Exception exception) {
        FamilyFunPack.printMessage("Could not resolve player name \"" + this.thread.player_name + "\" (" + exception.getMessage() + ")");
      }
    }
  }
}
