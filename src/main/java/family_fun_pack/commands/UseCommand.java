package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.entities.EntityVoid;

@SideOnly(Side.CLIENT)
public class UseCommand extends Command {

  public UseCommand() {
    super("use");
  }

  public String usage() {
    return this.getName() + " ([sneak] <entity_id> | <block_x> <block_y> <block_z>)";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();
    if(args.length > 3) {
      try {
        int x = Integer.parseInt(args[1]);
        int y = Integer.parseInt(args[2]);
        int z = Integer.parseInt(args[3]);
        Vec3d look = mc.player.getLookVec();
        CPacketPlayerTryUseItemOnBlock packet = new CPacketPlayerTryUseItemOnBlock(new BlockPos(x, y, z), EnumFacing.UP, EnumHand.MAIN_HAND, (float)look.x, (float)look.y, (float)look.z);
        FamilyFunPack.getNetworkHandler().sendPacket(packet);
        return "Using block (" + Integer.toString(x) + ", " + Integer.toString(y) + ", " + Integer.toString(z) + ")";
      } catch(NumberFormatException e) {
        return "Please specify integer coords";
      }
    } else if(args.length > 1) {
      try {
        int i = 1;
        boolean sneak = false;
        if(args[i].equals("sneak")) {
          sneak = true;
          i += 1;
        }
        int id = Integer.parseInt(args[i]);
        if(sneak)
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketEntityAction(new EntityVoid(mc.world, mc.player.getEntityId()), CPacketEntityAction.Action.START_SNEAKING));
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUseEntity(new EntityVoid(mc.world, id), EnumHand.MAIN_HAND));
        return "Using entity " + Integer.toString(id);
      } catch(NumberFormatException e) {
        return "Entity id must be an integer";
      }
    }
    return "Usage: " + this.usage();
  }
}
