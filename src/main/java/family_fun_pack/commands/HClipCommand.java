package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.PacketInterceptionModule;

/* Horizontal client-side teleport */

@SideOnly(Side.CLIENT)
public class HClipCommand extends Command {

  public HClipCommand() {
    super("hclip");
  }

  public String usage() {
    return this.getName() + " <horizontal> [vertical]";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      Minecraft mc = Minecraft.getMinecraft();
      try {
        double weight = Double.parseDouble(args[1]);
        double vert = 0d;
        if(args.length > 2) vert = Double.parseDouble(args[2]);

        // Client side
        Vec3d direction = new Vec3d(Math.cos((mc.player.rotationYaw + 90f) * (float) (Math.PI / 180.0f)), 0, Math.sin((mc.player.rotationYaw + 90f) * (float) (Math.PI / 180.0f)));
        Entity target = mc.player.isRiding() ? mc.player.getRidingEntity() : mc.player;
        target.setPosition(target.posX + direction.x*weight, target.posY + vert, target.posZ + direction.z*weight);

        // Send move packet
        Packet packet = null;
        if(mc.player.isRiding()) packet = new CPacketVehicleMove(target);
        else packet = new CPacketPlayer.Position(target.posX, target.posY, target.posZ, true);

        PacketInterceptionModule intercept = (PacketInterceptionModule) FamilyFunPack.getModules().getByName("Packets interception");
        intercept.addException(EnumPacketDirection.SERVERBOUND, packet);

        FamilyFunPack.getNetworkHandler().sendPacket(packet);

        return String.format("Teleported you %s blocks forward", weight);
      } catch(NumberFormatException e) {
        return "This is not a real number";
      }
    }
    return "Specify a number";
  }
}
