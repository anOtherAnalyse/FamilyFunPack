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

import family_fun_pack.FamilyFunPack;
import family_fun_pack.entities.EntityVoid;

/* Use entity / block */

/* .use [sneak|attack] [entity_id]
 * .use [<block_x> <block_y> <block_z>] */

@SideOnly(Side.CLIENT)
public class UseCommand extends Command {

  public UseCommand() {
    super("use");
  }

  public String usage() {
    return this.getName() + " ([sneak|attack] <entity_id> | <block_x> <block_y> <block_z>)";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();

    int x = 0, y = 0, z = 0, cursor = 1;
    boolean sneak = false, attack = false;
    Mode mode = null;

    /* Parse arguments */
    while(cursor < args.length) {
      if(args[cursor].equals("sneak")) {
        sneak = true;
        cursor += 1;
      } else if(args[cursor].equals("attack")) {
        attack = true;
        cursor += 1;
      } else break;
    }

    if(args.length - cursor >= 3) { // Block position
      try {
        x = Integer.parseInt(args[cursor++]);
        y = Integer.parseInt(args[cursor++]);
        z = Integer.parseInt(args[cursor]);
      } catch(NumberFormatException e) {
        return this.getUsage();
      }
      mode = Mode.BLOCK;
    } else if(args.length - cursor >= 1) { // Entity id
      try {
        x = Integer.parseInt(args[cursor]);
      } catch(NumberFormatException e) {
        return this.getUsage();
      }
      mode = Mode.ENTITY;
    } else { // Raytrace
      RayTraceResult target_ray = mc.objectMouseOver;
      if(target_ray == null) return "No target";

      if(target_ray.typeOfHit == RayTraceResult.Type.BLOCK) {
        BlockPos pos = target_ray.getBlockPos();
        x = pos.getX();
        y = pos.getY();
        z = pos.getZ();
        mode = Mode.BLOCK;
      } else if(target_ray.typeOfHit == RayTraceResult.Type.ENTITY) {
        x = target_ray.entityHit.getEntityId();
        mode = Mode.ENTITY;
      } else return "No target";
    }

    /* Execute command */

    String ret = null;

    if(sneak)
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketEntityAction(new EntityVoid(mc.world, mc.player.getEntityId()), CPacketEntityAction.Action.START_SNEAKING));

    switch(mode) {
      case BLOCK:
        {
          Vec3d look = mc.player.getLookVec();
          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(new BlockPos(x, y, z), EnumFacing.UP, EnumHand.MAIN_HAND, (float)look.x, (float)look.y, (float)look.z));
          ret = String.format("Using block (%d, %d, %d)", x, y, z);
        }
        break;
      case ENTITY:
        {
          if(attack) FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUseEntity(new EntityVoid(mc.world, x)));
          else FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUseEntity(new EntityVoid(mc.world, x), EnumHand.MAIN_HAND));
          ret = String.format("Using entity [%d]", x);
        }
        break;
    }

    if(sneak) // Stop sneaking, so we don't dismount on next update
      FamilyFunPack.getNetworkHandler().sendPacket(new CPacketEntityAction(new EntityVoid(mc.world, mc.player.getEntityId()), CPacketEntityAction.Action.STOP_SNEAKING));

    return ret;
  }

  public static enum Mode {BLOCK, ENTITY};
}
