package family_fun_pack.commands;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUpdateSign;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;
import java.lang.System;
import java.util.Random;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.network.PacketListener;

/* What is the block at (x, y, z) ? */

@SideOnly(Side.CLIENT)
public class BlockAtCommand extends Command implements PacketListener {

  private int chg_count;
  private boolean enabled;
  private Random random;

  public BlockAtCommand() {
    super("at");
    this.enabled = false;
    this.random = new Random(System.currentTimeMillis());
  }

  public String usage() {
    return this.getName() + " <x> <y> <z> | kill";
  }

  public String execute(String[] args) {
    Minecraft mc = Minecraft.getMinecraft();
    if(args.length > 3) {
      try {
        int x = Integer.parseInt(args[1]);
        int y = Integer.parseInt(args[2]);
        int z = Integer.parseInt(args[3]);

        if(this.enabled) return "Stop kill mode first";
        if(mc.player.getDistanceSq((double)x + 0.5D, (double)y + 0.5D, (double)z + 0.5D) < 64.0D) return "You should be able to see the block from here";

        this.chg_count = 0;
        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 11);
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(new BlockPos(x, y, z), EnumFacing.UP, EnumHand.MAIN_HAND, 0f, 0f, 0f));

      } catch(NumberFormatException e) {
        return this.getUsage();
      }
    } else if(args.length > 1 && args[1].equals("kill")) {
      this.enabled = !this.enabled;

      if(this.enabled) {
        MinecraftForge.EVENT_BUS.register(this);
      } else {
        MinecraftForge.EVENT_BUS.unregister(this);
      }

      return "Killing mode: " + (this.enabled ? "enabled" : "disabled");
    } else return this.getUsage();
    return null;
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    if(id == 11) { // SPacketBlockChange
      SPacketBlockChange change = (SPacketBlockChange) packet;

      if(++ this.chg_count > 1 && ! this.enabled) FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 11);

      BlockPos position = change.getBlockPosition();
      Block block = change.getBlockState().getBlock();
      String label = Block.REGISTRY.getNameForObject(block).getResourcePath().replace("_", " ").toLowerCase();
      FamilyFunPack.printMessage(String.format("Block at (%d, %d, %d) is [%s]", position.getX(), position.getY(), position.getZ(), label));

      if(block instanceof BlockStandingSign || block instanceof BlockWallSign) {
        FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 9);
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(position, EnumFacing.UP, EnumHand.MAIN_HAND, 0f, 0f, 0f)); // Be sure that chunk is loaded
        FamilyFunPack.getNetworkHandler().sendPacket(new CPacketUpdateSign(position, new ITextComponent[] {new TextComponentString(""), new TextComponentString(""), new TextComponentString(""), new TextComponentString("")}));
      }

    } else { // SPacketUpdateTileEntity
      SPacketUpdateTileEntity update = (SPacketUpdateTileEntity) packet;

      FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 9);

      if(update.getTileEntityType() == 9) {
        TileEntitySign tile = new TileEntitySign();
        tile.readFromNBT(update.getNbtCompound());
        for(int i = 0; i < 4; i ++) {
          FamilyFunPack.printMessage(String.format("#%d §8%s§r", i, tile.signText[i].getUnformattedText()));
        }
      }
    }
    return packet;
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    FamilyFunPack.getNetworkHandler().sendPacket(new CPacketPlayerTryUseItemOnBlock(new BlockPos(this.random.nextInt(30000000), this.random.nextInt(256), this.random.nextInt(30000000)), EnumFacing.SOUTH, EnumHand.MAIN_HAND, 0f, 0f, 0f));
  }

  public void onDisconnect() {
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 11);
    MinecraftForge.EVENT_BUS.unregister(this);
    this.enabled = false;
  }
}
