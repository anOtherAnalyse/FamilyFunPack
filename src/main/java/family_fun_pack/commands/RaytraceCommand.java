package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

/* Get entity id / position of entity / block we are staring at */

@SideOnly(Side.CLIENT)
public class RaytraceCommand extends Command {

  public RaytraceCommand() {
    super("raytrace");
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    RayTraceResult target_ray = Minecraft.getMinecraft().objectMouseOver;
    if(target_ray != null) {
      if(target_ray.typeOfHit == RayTraceResult.Type.BLOCK) {
        BlockPos pos = target_ray.getBlockPos();
        return String.format("Block at (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
      } else if(target_ray.typeOfHit == RayTraceResult.Type.ENTITY) {
        Entity entity = target_ray.entityHit;
        return String.format("Entity id is %d%s%s", entity.getEntityId(), (args.length > 1 && args[1].equals("+") ? " [" + entity.getUniqueID().toString() + "]" : ""), (entity.isRiding() ? String.format(" riding %d", entity.getRidingEntity().getEntityId()) : ""));
      }
      return "No target";
    }
    return null;
  }
}
