package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

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
        Entity target = mc.player.isRiding() ? mc.player.getRidingEntity() : mc.player;
        target.setPosition(target.posX, target.posY + weight, target.posZ);
        return String.format("Teleported you %s blocks up", weight);
      } catch(NumberFormatException e) {
        return "This is not a real number";
      }
    }
    return "Specify a number";
  }
}
