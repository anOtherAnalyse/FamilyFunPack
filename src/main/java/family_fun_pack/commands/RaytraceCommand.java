package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

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
        return String.format("Entity id is %d", entity.getEntityId());
      }
      return "No target";
    }
    return null;
  }
}
