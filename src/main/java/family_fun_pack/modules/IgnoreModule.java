package family_fun_pack.modules;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Client-side player ignore - persistent, not like 9b current /ignore */

@SideOnly(Side.CLIENT)
public class IgnoreModule extends AbstractPlayersRegister implements PacketListener {

  public IgnoreModule() {
    super("Ignore players", "Client-side /ignore", "ignore");
  }

  protected void enable() {
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 15);
  }

  protected void disable() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 15);
  }

  public boolean addUUID(UUID uuid) {
    this.toggle(true); // Enable module when at least one player is to be ignored
    return super.addUUID(uuid);
  }

  public boolean delUUID(UUID uuid) {
    boolean flag = super.delUUID(uuid);
    this.uuids_lock.readLock().lock();
    if(this.uuids.size() == 0) this.toggle(false); // Disable module when empty
    this.uuids_lock.readLock().unlock();
    return flag;
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    SPacketChat chat = (SPacketChat) packet;

    GameProfile sender = StalkModule.getSender(chat.getChatComponent());
    if(sender != null) {
      this.uuids_lock.readLock().lock();
      boolean flag = this.uuids.contains(sender.getId());
      this.uuids_lock.readLock().unlock();

      if(flag) return null;
    }

    return packet;
  }

  public boolean displayInGui() {
    return false;
  }
}
