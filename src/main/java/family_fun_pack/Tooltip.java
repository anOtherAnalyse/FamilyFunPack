package family_fun_pack;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import org.lwjgl.input.Keyboard;

import java.util.List;

import family_fun_pack.gui.CommandGui;
import family_fun_pack.gui.OverlayGui;
import family_fun_pack.network.PacketListener;
import family_fun_pack.network.PacketIntercept;

@SideOnly(Side.CLIENT)
public class Tooltip {

  private boolean firstConnection;
  public KeyBinding openGUIKey;
  private KeyBinding intercept;

  private OverlayGui overlay;

  private Minecraft mc;

  public Tooltip() {
    this.firstConnection = true;
    this.openGUIKey = new KeyBinding("Open GUI", Keyboard.KEY_BACKSLASH, FamilyFunPack.NAME);
    this.intercept = new KeyBinding("Intercept network packets", Keyboard.KEY_C, FamilyFunPack.NAME);
    ClientRegistry.registerKeyBinding(this.openGUIKey);
    ClientRegistry.registerKeyBinding(this.intercept);
    this.mc = Minecraft.getMinecraft();
    this.overlay = new OverlayGui(this.mc.fontRenderer, FamilyFunPack.configuration);
  }

  @SubscribeEvent
  public void itemToolTip(ItemTooltipEvent event) {
    ItemStack stack = event.getItemStack();
    int max = stack.getMaxDamage();

    if(stack.isEmpty() || max <= 0) return;
    if(stack.hasTagCompound() && stack.getTagCompound().getBoolean("Unbreakable")) return;

    List<String> toolTip = event.getToolTip();

    int damage;
    NBTTagCompound tag = stack.getTagCompound();
    if(tag != null && tag instanceof SpecialTagCompound) {
      damage = ((SpecialTagCompound)tag).getTrueDamage();
    } else damage = stack.getItemDamage();

    long count = (long)max - (long)damage;

    TextFormatting color;
    if(damage < 0) color = TextFormatting.DARK_PURPLE;
    else if(damage > max) color = TextFormatting.DARK_RED;
    else color = TextFormatting.BLUE;

    toolTip.add("");
    toolTip.add(color.toString() + "Durability: " + Long.toString(count) + " [Max: " + Long.toString(max) + "]" + TextFormatting.RESET.toString());
  }

  @SubscribeEvent
  public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
    if(this.firstConnection) {

      ChannelPipeline pipeline = event.getManager().channel().pipeline();

      try {
        // Install receive interception
        ChannelHandler old = pipeline.get("decoder");
        if(old != null && old instanceof NettyPacketDecoder) {
          PacketListener spoof = new PacketListener(EnumPacketDirection.CLIENTBOUND);
          pipeline.replace("decoder", "decoder", spoof);
          this.firstConnection = false;
        }

        // Install send interception
        old = pipeline.get("encoder");
        if(old != null && old instanceof NettyPacketEncoder) {
          PacketIntercept intercept = new PacketIntercept(EnumPacketDirection.SERVERBOUND);
          pipeline.replace("encoder", "encoder", intercept);
          this.firstConnection = false;
        }

        // Record NetworkManager
        FamilyFunPack.setNetworkManager(event.getManager());
      } catch (java.util.NoSuchElementException e) {}
    }
  }

  @SubscribeEvent
	public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
    this.firstConnection = true;
    FamilyFunPack.configuration.resetVolatileConf();
    FamilyFunPack.setNetworkManager(null);
  }

  @SubscribeEvent
  public void onKey(KeyInputEvent event) {
    if(this.openGUIKey.isPressed()) {
      if(! (this.mc.currentScreen instanceof CommandGui)) {
        this.mc.displayGuiScreen(new CommandGui(this));
      }
    } else if(this.intercept.isPressed()) {
      FamilyFunPack.configuration.block_player_packets = ! FamilyFunPack.configuration.block_player_packets;
      FamilyFunPack.configuration.save();
    }
  }

  @SubscribeEvent
  public void drawOverlay(RenderGameOverlayEvent.Text event) {
    this.overlay.drawOverlay();
  }

  @SubscribeEvent
  public void onChat(ClientChatEvent event) { // handle commands
    String message = event.getMessage();
    if(message.startsWith("/")) {
      String[] cmd = message.substring(1).split("[ ]+");
      if(cmd.length <= 0) return;
      switch(cmd[0]) {
        case "diff": // players list diff
          {
            FamilyFunPack.configuration.player_completion = true;
            FamilyFunPack.sendPacket(new CPacketTabComplete("", null, false));
            event.setCanceled(true);
            this.mc.ingameGUI.getChatGUI().addToSentMessages("diff");
          }
          break;
        case "commands": // available commands
          {
            RayTraceResult target_ray = this.mc.objectMouseOver;
            BlockPos target = null;
            boolean has_target = false;
            if(target_ray != null && target_ray.typeOfHit == RayTraceResult.Type.BLOCK) {
              target = target_ray.getBlockPos();
              has_target = true;
            }
            FamilyFunPack.configuration.commands_completion = true;
            FamilyFunPack.sendPacket(new CPacketTabComplete("/", target, has_target));
            event.setCanceled(true);
            this.mc.ingameGUI.getChatGUI().addToSentMessages("commands");
          }
          break;
        case "vanish": // vanish riding entity
          {
            if(cmd.length > 1) {
              switch(cmd[1]) {
                case "dismount":
                  {
                    if(this.mc.player.isRiding()) {
                      FamilyFunPack.configuration.ride = this.mc.player.getRidingEntity();
                      this.mc.player.dismountRidingEntity();
                      this.mc.world.removeEntity(FamilyFunPack.configuration.ride);
                    } else FamilyFunPack.printMessage("You are not riding anything");
                  }
                  break;
                case "remount":
                  {
                    if(FamilyFunPack.configuration.ride != null) {
                      if(! this.mc.player.isRiding()) {
                        FamilyFunPack.configuration.ride.isDead = false;
                        this.mc.world.spawnEntity(FamilyFunPack.configuration.ride);
                        this.mc.player.startRiding(FamilyFunPack.configuration.ride, true);
                        if(this.mc.player.isRiding())
                          FamilyFunPack.printMessage("Entity " + Integer.toString(FamilyFunPack.configuration.ride.hashCode()) + " remounted");
                        else FamilyFunPack.printMessage("Could not remount");
                      }
                      FamilyFunPack.configuration.ride = null;
                    } else FamilyFunPack.printMessage("Nothing to remount");
                  }
                  break;
                default:
                  FamilyFunPack.printMessage("Unknown argument " + cmd[1]);
              }
            } else FamilyFunPack.printMessage("dismount or remount ?");
            event.setCanceled(true);
            this.mc.ingameGUI.getChatGUI().addToSentMessages(message);
          }
          break;
        case "hclip": // salhack hclip copy
          {
            if(cmd.length > 1) {
              try {
                double weight = Double.parseDouble(cmd[1]);
                Vec3d direction = new Vec3d(Math.cos((this.mc.player.rotationYaw + 90f) * (float) (Math.PI / 180.0f)), 0, Math.sin((this.mc.player.rotationYaw + 90f) * (float) (Math.PI / 180.0f)));
                Entity target = this.mc.player.isRiding() ? this.mc.player.getRidingEntity() : this.mc.player;
                target.setPosition(this.mc.player.posX + direction.x*weight, this.mc.player.posY, this.mc.player.posZ + direction.z*weight);
                FamilyFunPack.printMessage(String.format("Teleported you %s blocks forward", weight));
              } catch(NumberFormatException e) {
                FamilyFunPack.printMessage("This is not a real number");
              }
            } else FamilyFunPack.printMessage("Specify a number");
            event.setCanceled(true);
            this.mc.ingameGUI.getChatGUI().addToSentMessages(message);
          }
          break;
        case "vclip":
          {
            if(cmd.length > 1) {
              try {
                double weight = Double.parseDouble(cmd[1]);
                Entity target = this.mc.player.isRiding() ? this.mc.player.getRidingEntity() : this.mc.player;
                target.setPosition(this.mc.player.posX, this.mc.player.posY + weight, this.mc.player.posZ);
                FamilyFunPack.printMessage(String.format("Teleported you %s blocks up", weight));
              } catch(NumberFormatException e) {
                FamilyFunPack.printMessage("This is not a real number");
              }
            } else FamilyFunPack.printMessage("Specify a number");
            event.setCanceled(true);
            this.mc.ingameGUI.getChatGUI().addToSentMessages(message);
          }
          break;
      }
    }
  }

}
