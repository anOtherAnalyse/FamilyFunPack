package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class VoidMountCommand extends Command {

  public VoidMountCommand() {
    super("mount");
  }

  public String usage() {
    return this.getName() + " <entity_type|null>";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      Minecraft mc = Minecraft.getMinecraft();

      if(args[1].equals("null")) {
        Entity entity = mc.player.getRidingEntity();
        if(entity == null) return "You are not riding anything";
        mc.player.dismountRidingEntity();
        mc.world.removeEntityFromWorld(entity.getEntityId());
        return "Removed void mount";
      } else {
        ResourceLocation resource = new ResourceLocation(args[1]);
        Entity entity = EntityList.createEntityByIDFromName(resource, mc.world);
        if(entity == null) return "Invalid entity class";

        entity.setPosition(mc.player.posX, mc.player.posY, mc.player.posZ);
        mc.world.addEntityToWorld(42, entity);
        mc.player.startRiding(entity, true);

        return "Mounted " + args[1];
      }
    }
    return this.usage();
  }
}
