package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class HClipCommand extends Command {

  public HClipCommand() {
    super("hclip");
  }

  public String usage() {
    return this.getName() + " <number>";
  }

  public String execute(String[] args) {
    if(args.length > 1) {
      Minecraft mc = Minecraft.getMinecraft();
      try {
        double weight = Double.parseDouble(args[1]);
        Vec3d direction = new Vec3d(Math.cos((mc.player.rotationYaw + 90f) * (float) (Math.PI / 180.0f)), 0, Math.sin((mc.player.rotationYaw + 90f) * (float) (Math.PI / 180.0f)));
        Entity target = mc.player.isRiding() ? mc.player.getRidingEntity() : mc.player;
        target.setPosition(target.posX + direction.x*weight, target.posY, target.posZ + direction.z*weight);
        return String.format("Teleported you %s blocks forward", weight);
      } catch(NumberFormatException e) {
        return "This is not a real number";
      }
    }
    return "Specify a number";
  }
}
