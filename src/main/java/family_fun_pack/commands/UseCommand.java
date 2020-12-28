package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.HashMap;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.entities.EntityVoid;

/* Use entity / block */

/* .use [sneak|attack] <entity_id>
 * .use +1 -> register entity you are looking as number 1
 * .use [sneak|attack] #1 -> use entity registered as number 1
 * .use <block_x> <block_y> <block_z> */

@SideOnly(Side.CLIENT)
public class UseCommand extends Command {

  private Map<Integer, Integer> records;

  public UseCommand() {
    super("use");
    this.records = new HashMap<Integer, Integer>();
  }

  public String usage() {
    return this.getName() + " ([sneak|attack] <entity_id> | <block_x> <block_y> <block_z>)";
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
        boolean attack = false;
        if(args[i].equals("sneak")) {
          sneak = true;
          i += 1;
        } else if(args[i].equals("attack")) {
          attack = true;
          i += 1;
        }

        int id = 0;
        if(args[i].charAt(0) == '+' || args[i].charAt(0) == '#') {
          id = Integer.parseInt(args[i].substring(1));
        } else id = Integer.parseInt(args[i]);

        if(args[i].charAt(0) == '+') {
          RayTraceResult target_ray = mc.objectMouseOver;
          if(target_ray != null && target_ray.typeOfHit == RayTraceResult.Type.ENTITY) {
            Entity entity = target_ray.entityHit;
            this.records.put(id, entity.getEntityId());
            return "Entity " + Integer.toString(entity.getEntityId()) + " recorded as #" + Integer.toString(id);
          }
          return "Look at an entity";
        } else if(args[i].charAt(0) == '#') {
          Integer r = this.records.get(id);
          if(r == null) return "No such record";
          id = r.intValue();
        }

        if(sneak)
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketEntityAction(new EntityVoid(mc.world, mc.player.getEntityId()), CPacketEntityAction.Action.START_SNEAKING));

        if(attack)
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUseEntity(new EntityVoid(mc.world, id)));
        else FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUseEntity(new EntityVoid(mc.world, id), EnumHand.MAIN_HAND));

        return "Using entity " + Integer.toString(id);
      } catch(NumberFormatException e) {
        return "Entity id must be an integer";
      }
    }
    return "Usage: " + this.usage();
  }
}
