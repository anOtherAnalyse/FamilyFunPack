package family_fun_pack.modules;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.INetHandler;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

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

@SideOnly(Side.CLIENT)
public abstract class AbstractPlayersRegister extends Module {

  public static final Pattern CHAT_PATTERN = Pattern.compile("^<([^>]+)>[\\s\\S]*$");
  public static GameProfileRepository REPOSITORY = null;

  public String list_name;

  public static GameProfile getSender(ITextComponent message) {
    String flat_message = message.getFormattedText().replaceAll("§[0-9a-zA-Z]", "");
    Matcher match = StalkModule.CHAT_PATTERN.matcher(flat_message);
    if(match.matches()) {
      String name = match.group(1);
      NetworkPlayerInfo info = AbstractPlayersRegister.getNetHandler().getPlayerInfo(name);
      if(info != null)
        return info.getGameProfile();
    }
    return null;
  }

  public static NetHandlerPlayClient getNetHandler() {
    INetHandler inet_handler = FamilyFunPack.getNetworkHandler().getNetHandler();
    if(inet_handler != null && inet_handler instanceof NetHandlerPlayClient) {
      return (NetHandlerPlayClient) inet_handler;
    }
    return null;
  }

  protected Set<UUID> uuids;
  protected ReadWriteLock uuids_lock;

  public AbstractPlayersRegister(String name, String desc, String list_name) {
    super(name, desc);
    this.list_name = list_name;
    this.uuids = new HashSet<UUID>();
    this.uuids_lock = new ReentrantReadWriteLock();

    if(AbstractPlayersRegister.REPOSITORY == null) {
      YggdrasilAuthenticationService auth = new YggdrasilAuthenticationService(Proxy.NO_PROXY, null);
      AbstractPlayersRegister.REPOSITORY = auth.createProfileRepository();
    }
  }

  public void addPlayer(String player) {
    Thread t = new LookupThread(player, this, LookupThread.Action.ADD);
    t.start();
  }

  public boolean addUUID(UUID uuid) {
    this.uuids_lock.writeLock().lock();
    boolean flag = this.uuids.add(uuid);
    this.uuids_lock.writeLock().unlock();
    return flag;
  }

  public void delPlayer(String player) {
    Thread t = new LookupThread(player, this, LookupThread.Action.DEL);
    t.start();
  }

  public boolean delUUID(UUID uuid) {
    this.uuids_lock.writeLock().lock();
    boolean flag = this.uuids.remove(uuid);
    this.uuids_lock.writeLock().unlock();
    return flag;
  }

  public void togglePlayer(String player) {
    Thread t = new LookupThread(player, this, LookupThread.Action.INV);
    t.start();
  }

  public boolean containsUUID(UUID uuid) {
    this.uuids_lock.readLock().lock();
    boolean flag = this.uuids.contains(uuid);
    this.uuids_lock.readLock().unlock();
    return flag;
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

  /* Profile lookup thread */
  private static class LookupThread extends Thread {

    public String player_name;
    public AbstractPlayersRegister module;
    public Action action;

    public LookupThread(String name, AbstractPlayersRegister module, Action action) {
      this.player_name = name;
      this.module = module;
      this.action = action;
    }

    public void run() {
      AbstractPlayersRegister.REPOSITORY.findProfilesByNames(new String[] {this.player_name}, Agent.MINECRAFT, new LookupCallback(this));
    }

    public static enum Action {ADD, DEL, INV};

    private static class LookupCallback implements ProfileLookupCallback {

      private LookupThread thread;

      public LookupCallback(LookupThread thread) {
        this.thread = thread;
      }

      public void onProfileLookupSucceeded(GameProfile profile) {
        Configuration configuration = FamilyFunPack.getModules().getConfiguration();

        Action action = this.thread.action;
        if(action == Action.INV) {
          if(this.thread.module.containsUUID(profile.getId())) action = Action.DEL;
          else action = Action.ADD;
        }

        switch(action) {
          case ADD:
            if(this.thread.module.addUUID(profile.getId()))
              FamilyFunPack.printMessage(String.format("%s added to %s list", profile.getName(), this.thread.module.list_name));
            else
              FamilyFunPack.printMessage(String.format("%s alread present in %s list", profile.getName(), this.thread.module.list_name));
            break;
          case DEL:
            if(this.thread.module.delUUID(profile.getId()))
              FamilyFunPack.printMessage(String.format("%s removed from %s list", profile.getName(), this.thread.module.list_name));
            else
              FamilyFunPack.printMessage(String.format("%s is not in %s list", profile.getName(), this.thread.module.list_name));
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
