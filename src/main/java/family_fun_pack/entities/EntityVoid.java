package family_fun_pack.entities;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
public class EntityVoid extends Entity {

  public EntityVoid(World world, int id) {
    super(world);
    super.setEntityId(id);
  }

  protected void entityInit() {

  }

  protected void readEntityFromNBT(NBTTagCompound paramNBTTagCompound) {

  }

  protected void writeEntityToNBT(NBTTagCompound paramNBTTagCompound) {

  }
}
