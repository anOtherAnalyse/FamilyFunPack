package family_fun_pack.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* Fast arrow hack */

@SideOnly(Side.CLIENT)
public class FastBowModule extends Module implements PacketListener {

  private static final int REPEAT_COUNT = 125;

  public FastBowModule() {
    super("Fast bow", "Arrow goes brrrr");
  }

  protected void enable() {
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.SERVERBOUND, this, 20);
  }

  protected void disable() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.SERVERBOUND, this, 20);
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    CPacketPlayerDigging dig = (CPacketPlayerDigging) packet;
    if(dig.getAction() == CPacketPlayerDigging.Action.RELEASE_USE_ITEM) {
      Minecraft mc = Minecraft.getMinecraft();

      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SPRINTING));
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.01d, mc.player.posZ, false));

      for(int i = 0; i < FastBowModule.REPEAT_COUNT; i ++) {
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY, mc.player.posZ, true));
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.01d, mc.player.posZ, false));
      }

      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayer.PositionRotation(mc.player.posX, mc.player.posY, mc.player.posZ, mc.player.rotationYaw, 90f, true));
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SPRINTING));
    }

    return packet;
  }
}
