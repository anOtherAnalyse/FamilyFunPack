package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractChestHorse;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.network.EnumPacketDirection;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.CommandsModule;
import family_fun_pack.modules.PacketInterceptionModule;

// rollback dupe automation
// faster way of doing this https://www.youtube.com/watch?v=o3aMoQTOZGs

@SideOnly(Side.CLIENT)
public class RollbackDupeCommand extends Command {

  private boolean on;

  public RollbackDupeCommand() {
    super("rdupe");
  }

  public String usage() {
    return this.getName() + " [reset]";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();

    if(args.length > 1 && args[1].equals("reset")) {
      this.on = false;
    } else {
      PacketInterceptionModule intercept = (PacketInterceptionModule) FamilyFunPack.getModules().getByClass(PacketInterceptionModule.class);
      CommandsModule cmd = (CommandsModule) FamilyFunPack.getModules().getByClass(CommandsModule.class);

      if(! on) { // init

        List<AbstractHorse> horses = mc.player.world.getEntitiesWithinAABB(AbstractHorse.class, mc.player.getEntityBoundingBox().grow(6.0D, 2.0D, 6.0D));
        if(horses.size() == 0) return "where's your ride ?";

        Entity ride = horses.get(0);

        cmd.handleCommand("rollback");

        intercept.addIntercept(EnumPacketDirection.SERVERBOUND, 12);
        intercept.addIntercept(EnumPacketDirection.SERVERBOUND, 13);
        intercept.addIntercept(EnumPacketDirection.SERVERBOUND, 14);
        intercept.addIntercept(EnumPacketDirection.SERVERBOUND, 15);
        intercept.removeIntercept(EnumPacketDirection.SERVERBOUND, 16);
        intercept.toggle(true);

        cmd.handleCommand(String.format("use %d", ride.getEntityId()));

        this.on = true;
      } else { // exec

        List<AbstractChestHorse> donkeys = mc.player.world.getEntitiesWithinAABB(AbstractChestHorse.class, mc.player.getEntityBoundingBox().grow(6.0D, 2.0D, 6.0D));

        Entity ddonkey = null;
        for(AbstractChestHorse c : donkeys) {
          if(c != mc.player.getRidingEntity()) {
            ddonkey = c;
            break;
          }
        }

        if(ddonkey == null) return "where's donkey ?";

        cmd.handleCommand(String.format("use sneak %d", ddonkey.getEntityId()));

        intercept.addIntercept(EnumPacketDirection.SERVERBOUND, 16);

        cmd.handleCommand("rollback double");

        this.on = false;
      }
    }

    return null;
  }
}
