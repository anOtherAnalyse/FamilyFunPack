package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPacketUpdateSign;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.StringBuilder;

import family_fun_pack.FamilyFunPack;

/* Edit a sign, read a sign, hide data in a sign... */

@SideOnly(Side.CLIENT)
public class EditSignCommand extends Command {

  public EditSignCommand() {
    super("sign");
  }

  public String usage() {
    return this.getName() + " <text>";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();
    RayTraceResult target_ray = mc.objectMouseOver;
    if(target_ray != null) {
      if(target_ray.typeOfHit == RayTraceResult.Type.BLOCK) {
        BlockPos pos = target_ray.getBlockPos();

        if(args.length > 1) { // Edit sign
          TextComponentString[] str = new TextComponentString[4];
          StringBuilder build = new StringBuilder();
          int i = 0;
          for(int j = 1; j < args.length; j ++) {
            if(args[j].equals("+")) {
              str[i] = new TextComponentString(build.toString());
              if(++i >= 4) break;
              build = new StringBuilder();
            } else {
              if(build.length() > 0) build.append(' ');
              build.append(args[j]);
            }
          }

          for(; i < 4; i ++) {
            str[i] = new TextComponentString(build.toString());
            build = new StringBuilder();
          }

          FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUpdateSign(pos, str));
        } else { // Read sign
          TileEntity tile = mc.world.getTileEntity(pos);
          if(tile instanceof TileEntitySign) {
            TileEntitySign sign = (TileEntitySign) tile;
            for(int i = 0; i < 4; i ++) {
              FamilyFunPack.printMessage(String.format("#%d §8%s§r", i, sign.signText[i].getUnformattedText()));
            }
          } else return "No sign data";
        }
        return null;
      }
    }
    return "Look at a sign";
  }
}
