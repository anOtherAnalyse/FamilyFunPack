package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.PacketInterceptionModule;

/* Vertical client-side teleport */

@SideOnly(Side.CLIENT)
public class VClipCommand extends Command {

  public VClipCommand() {
    super("vclip");
  }

  public String usage() {
    return this.getName() + " <number>";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      Minecraft mc = Minecraft.getMinecraft();
      try {
        double weight = Double.parseDouble(args[1]);

        // Client side
        Entity target = mc.player.isRiding() ? mc.player.getRidingEntity() : mc.player;
        target.setPosition(target.posX, target.posY + weight, target.posZ);

        // Send move packet
        Packet packet = null;
        if(mc.player.isRiding()) packet = new CPacketVehicleMove(target);
        else packet = new CPacketPlayer.Position(target.posX, target.posY, target.posZ, true);

        PacketInterceptionModule intercept = (PacketInterceptionModule) FamilyFunPack.getModules().getByName("Packets interception");
        intercept.addException(EnumPacketDirection.SERVERBOUND, packet);

        FamilyFunPack.getNetworkHandler().sendPacket(packet);

        return String.format("Teleported you %s blocks up", weight);
      } catch(NumberFormatException e) {
        return "This is not a real number";
      }
    }
    return "Specify a number";
  }
}
