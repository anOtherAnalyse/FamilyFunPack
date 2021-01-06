package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.passive.AbstractChestHorse;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

/* Create entity and mount it, only for client side */
/* For example: use it while riding a llama to replace it by a horse */

@SideOnly(Side.CLIENT)
public class VoidMountCommand extends Command {

  public static void setSaddled(Entity entity) {
    if(entity instanceof AbstractHorse) {
      ((AbstractHorse) entity).setHorseSaddled(true);
      ((AbstractHorse) entity).setHorseTamed(true);

      if(entity instanceof AbstractChestHorse) {
        ((AbstractChestHorse) entity).setChested(true);

        if(entity instanceof EntityLlama) {
          ((EntityLlama) entity).getDataManager().set(new DataParameter(16, DataSerializers.VARINT), Integer.valueOf(3));
        }
      }
    }
  }

  private int last_id;

  public VoidMountCommand() {
    super("mount");
    this.last_id = 42;
  }

  public String usage() {
    return this.getName() + " <entity_type|null> [entity_id]";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      Minecraft mc = Minecraft.getMinecraft();

      if(args[1].equals("null")) {
        Entity entity = mc.player.getRidingEntity();
        if(entity == null) return "You are not riding anything";
        mc.player.dismountRidingEntity();
        mc.world.removeEntityFromWorld(entity.getEntityId());
        this.last_id = entity.getEntityId();
        return "Removed void mount";
      } else {

        int id = this.last_id;
        if(args.length > 2) {
          try {
            id = Integer.parseInt(args[2]);
          } catch(NumberFormatException e) {
            return "entity_id should be a number";
          }
        }

        if(mc.player.isRiding()) {
          if(args.length <= 2) id = mc.player.getRidingEntity().getEntityId();
          mc.player.dismountRidingEntity();
        }

        ResourceLocation resource = new ResourceLocation(args[1]);
        Entity entity = EntityList.createEntityByIDFromName(resource, mc.world);
        if(entity == null) return "Invalid entity class";

        VoidMountCommand.setSaddled(entity);

        entity.setPosition(mc.player.posX, mc.player.posY, mc.player.posZ);

        mc.world.addEntityToWorld(id, entity);
        mc.player.startRiding(entity, true);

        return "Mounted " + args[1];
      }
    }
    return this.getUsage();
  }
}
