package family_fun_pack.modules;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketMultiBlockChange;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

import java.lang.Class;
import java.lang.IllegalAccessException;
import java.lang.NoSuchMethodException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.components.ColorButton;
import family_fun_pack.network.PacketListener;

@SideOnly(Side.CLIENT)
public class SearchModule extends Module implements PacketListener {

  // Thanks SalHack for the code
  public static void drawLine3D(float x, float y, float z, float x1, float y1, float z1, float thickness, int hex) {
    float red = (hex >> 16 & 0xFF) / 255.0F;
    float green = (hex >> 8 & 0xFF) / 255.0F;
    float blue = (hex & 0xFF) / 255.0F;
    float alpha = (hex >> 24 & 0xFF) / 255.0F;

    GlStateManager.pushMatrix();
    GlStateManager.disableTexture2D();
    GlStateManager.enableBlend();
    GlStateManager.disableAlpha();
    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
    GlStateManager.shadeModel(GL11.GL_SMOOTH);
    GL11.glLineWidth(thickness);
    GL11.glEnable(GL11.GL_LINE_SMOOTH);
    GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
    GlStateManager.disableDepth();
    GL11.glEnable(GL32.GL_DEPTH_CLAMP);
    final Tessellator tessellator = Tessellator.getInstance();
    final BufferBuilder bufferbuilder = tessellator.getBuffer();
    bufferbuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
    bufferbuilder.pos((double) x, (double) y, (double) z).color(red, green, blue, alpha).endVertex();
    bufferbuilder.pos((double) x1, (double) y1, (double) z1).color(red, green, blue, alpha).endVertex();
    tessellator.draw();
    GlStateManager.shadeModel(GL11.GL_FLAT);
    GL11.glDisable(GL11.GL_LINE_SMOOTH);
    GlStateManager.enableDepth();
    GL11.glDisable(GL32.GL_DEPTH_CLAMP);
    GlStateManager.disableBlend();
    GlStateManager.enableAlpha();
    GlStateManager.enableTexture2D();
    GlStateManager.popMatrix();
  }

  private Map<Block, Property> to_search;
  private Map<BlockPos, Property> targets;

  private List<ChunkPos> new_chunks;

  private ICamera camera;

  private Method transformMeth;

  public SearchModule() {
    super("Search", "Search for blocks");
    this.to_search = new HashMap<Block, Property>();
    this.targets = new HashMap<BlockPos, Property>();
    this.new_chunks = new ArrayList<ChunkPos>();
    this.camera = new Frustum();

    Class<EntityRenderer> entityRendererClass = EntityRenderer.class;
    try {
      this.transformMeth = entityRendererClass.getDeclaredMethod("func_78479_a", float.class, int.class);
    } catch(NoSuchMethodException e) {
      try {
        this.transformMeth = entityRendererClass.getDeclaredMethod("setupCameraTransform", float.class, int.class);
      } catch(NoSuchMethodException e2) {
        throw new RuntimeException("FamilyFunPack Error: no method setupCameraTransform in class EntityRenderer");
      }
    }
    this.transformMeth.setAccessible(true);

    FamilyFunPack.addModuleKey(0, this);
  }

  protected void enable() {
    MinecraftForge.EVENT_BUS.register(this);
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 11, 16, 32);
    this.resetTargets();
  }

  protected void disable() {
    MinecraftForge.EVENT_BUS.unregister(this);
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 11, 16, 32);
    this.onDisconnect();
  }

  public void onDisconnect() {
    this.targets.clear();
    this.new_chunks.clear();
  }

  public void save(Configuration configuration) {
    for(Block b : Block.REGISTRY) {
      int id = Block.getIdFromBlock(b);
      Property p = this.to_search.get(b);
      if(p == null) {
        configuration.get(this.name, "search_" + Integer.toString(id), false).set(false);
      } else {
        configuration.get(this.name, "search_" + Integer.toString(id), false).set(true);
        configuration.get(this.name, "tracer_" + Integer.toString(id), false).set(p.tracer);
        configuration.get(this.name, "color_" + Integer.toString(id), ColorButton.COLORS[0]).set(p.color);
      }
    }
    super.save(configuration);
  }

  public void load(Configuration configuration) {
    for(Block b : Block.REGISTRY) {
      int id = Block.getIdFromBlock(b);
      boolean search = configuration.get(this.name, "search_" + Integer.toString(id), false).getBoolean();
      if(search) {
        boolean tracer = configuration.get(this.name, "tracer_" + Integer.toString(id), false).getBoolean();
        int color = configuration.get(this.name, "color_" + Integer.toString(id), ColorButton.COLORS[0]).getInt();
        this.to_search.put(b, new Property(tracer, color));
      }
    }
    super.load(configuration);
  }

  @SubscribeEvent
  public void onUnLoad(ChunkEvent.Unload event) {
    int x = event.getChunk().x, z = event.getChunk().z;
    Iterator<BlockPos> iterator = this.targets.keySet().iterator();
    while(iterator.hasNext()) {
      BlockPos position = iterator.next();
      if((position.getX() >> 4) == x && (position.getZ() >> 4) == z) {
        iterator.remove();
      }
    }
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if(this.new_chunks.size() > 0) {
      Minecraft mc = Minecraft.getMinecraft();
      if(mc.world == null) return;
      for(int i = 0; i < this.new_chunks.size(); i ++) {
        ChunkPos position = this.new_chunks.get(i);
        Chunk c = mc.world.getChunkProvider().getLoadedChunk(position.x, position.z);
        if(c != null) {
          this.searchChunk(c);
          this.new_chunks.remove(i);
          i --;
        }
      }
    }
  }

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    if(id == 32) {
      SPacketChunkData chunk = (SPacketChunkData) packet;
      this.new_chunks.add(new ChunkPos(chunk.getChunkX(), chunk.getChunkZ()));
    } else if(id == 11) {
      SPacketBlockChange change = (SPacketBlockChange) packet;
      Property p = this.to_search.get(change.getBlockState().getBlock());
      if(p != null) {
        this.targets.put(change.getBlockPosition(), p);
      } else this.targets.remove(change.getBlockPosition());
    } else {
      SPacketMultiBlockChange change = (SPacketMultiBlockChange) packet;
      for(SPacketMultiBlockChange.BlockUpdateData up : change.getChangedBlocks()) {
        Property p = this.to_search.get(up.getBlockState().getBlock());
        if(p != null) this.targets.put(up.getPos(), p);
        else this.targets.remove(up.getPos());
      }
    }
    return packet;
  }

  @SubscribeEvent
  public void onRender(RenderWorldLastEvent event) {
    Minecraft mc = Minecraft.getMinecraft();
    RenderManager renderManager = mc.getRenderManager();

    if (renderManager == null || renderManager.options == null)
            return;

    GlStateManager.disableTexture2D();
    GlStateManager.enableBlend();
    GlStateManager.disableAlpha();
    GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
    GlStateManager.shadeModel(GL11.GL_SMOOTH);
    GlStateManager.disableDepth();

    GlStateManager.pushMatrix();
    GlStateManager.depthMask(false);
    GL11.glEnable(GL11.GL_LINE_SMOOTH);
    GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
    GL11.glLineWidth(1.5f);

    List<Target> tracers = new LinkedList<Target>();

    try {

      for(BlockPos position : this.targets.keySet()) {
        AxisAlignedBB bb = mc.world.getBlockState(position).getSelectedBoundingBox(mc.world, position).grow(0.0020000000949949026D).offset(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ);
        
        Property p = this.targets.get(position);
        if(p.tracer) {
          tracers.add(new Target(position, p));
        }

        this.camera.setPosition(0d, 0d, 0d);
        if (camera.isBoundingBoxInFrustum(bb)) {

            float red = ((float)((p.color >> 16) & 255)) / 255f;
            float blue = ((float)((p.color >> 8) & 255)) / 255f;
            float green = ((float)(p.color & 255)) / 255f;

            RenderGlobal.drawBoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, red, blue, green, 0.65f);
            RenderGlobal.renderFilledBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, red, blue, green, 0.5f);
        }
      }

    } catch (ConcurrentModificationException e) {}

    GL11.glDisable(GL11.GL_LINE_SMOOTH);
    GlStateManager.depthMask(true);
    GlStateManager.popMatrix();

    final boolean bobbing = mc.gameSettings.viewBobbing;
    mc.gameSettings.viewBobbing = false;
    try {
      this.transformMeth.invoke(mc.entityRenderer, event.getPartialTicks(), 0);
    } catch(IllegalAccessException e) {}
      catch(InvocationTargetException e) {}

    for(Target t : tracers) {
      Vec3d pos = new Vec3d(t.position).addVector(0.5, 0.5, 0.5).subtract(renderManager.viewerPosX, renderManager.viewerPosY, renderManager.viewerPosZ);
      Vec3d forward = new Vec3d(0, 0, 1).rotatePitch(-(float) Math.toRadians(mc.getRenderViewEntity().rotationPitch)).rotateYaw(-(float) Math.toRadians(mc.getRenderViewEntity().rotationYaw));
      this.drawLine3D((float) forward.x, (float) forward.y + mc.getRenderViewEntity().getEyeHeight(), (float) forward.z, (float) pos.x, (float) pos.y, (float) pos.z, 0.85f, t.property.color);
    }

    mc.gameSettings.viewBobbing = bobbing;
    try {
      this.transformMeth.invoke(mc.entityRenderer, event.getPartialTicks(), 0);
    } catch(IllegalAccessException e) {}
      catch(InvocationTargetException e) {}

    GlStateManager.glLineWidth(1f);
    GlStateManager.shadeModel(GL11.GL_FLAT);
    GlStateManager.disableBlend();
    GlStateManager.enableAlpha();
    GlStateManager.enableTexture2D();
    GlStateManager.enableDepth();
    GlStateManager.enableCull();
  }

  public void resetTargets() {
    Minecraft mc = Minecraft.getMinecraft();
    if(mc.world == null) return;

    this.targets.clear();
    int x = (int)mc.getRenderViewEntity().posX >> 4, z = (int)mc.getRenderViewEntity().posZ >> 4;
    for(int i = x - mc.gameSettings.renderDistanceChunks; i <= x + mc.gameSettings.renderDistanceChunks; i ++) {
      for(int j = z - mc.gameSettings.renderDistanceChunks; j <= z + mc.gameSettings.renderDistanceChunks; j ++) {
        Chunk c = mc.world.getChunkProvider().getLoadedChunk(i, j);
        if(c != null) {
          this.searchChunk(c);
        }
      }
    }
  }

  private void searchChunk(Chunk chunk) {
    int m = -1;
    for(ExtendedBlockStorage storage : chunk.getBlockStorageArray()) {
      m ++;
      if(storage == null) continue;
      for(int i = 0; i < 16; i ++) {
        for(int j = 0; j < 16; j ++) {
          for(int k = 0; k < 16; k ++) {
            Property p = this.to_search.get(storage.get(i, j, k).getBlock());
            if(p != null) {
              BlockPos position = new BlockPos((chunk.x << 4) + i, (m << 4) + j, (chunk.z << 4) + k);
              this.targets.put(position, p);
            }
          }
        }
      }
    }
  }

  public void setSearchState(int block_id, boolean state, boolean tracer, int color) {
    Block block = Block.getBlockById(block_id);
    if(state) {
      this.to_search.put(block, new Property(tracer, color));
    } else {
      this.to_search.remove(block);
    }
    if(this.isEnabled()) this.resetTargets();
  }

  public void setTracerState(int block_id, boolean state) {
    Block block = Block.getBlockById(block_id);
    Property p = this.to_search.get(block);
    if(p != null) {
      p.tracer = state;
    }
  }

  public void setSearchSColor(int block_id, int color) {
    Block block = Block.getBlockById(block_id);
    Property p = this.to_search.get(block);
    if(p != null) {
      p.color = color;
    }
  }

  public boolean getSearchState(int block_id) {
    Block block = Block.getBlockById(block_id);
    return (this.to_search.get(block) != null);
  }

  public boolean getTracerState(int block_id) {
    Block block = Block.getBlockById(block_id);
    Property p = this.to_search.get(block);
    if(p == null) return false;
    return p.tracer;
  }

  public int getColor(int block_id) {
    Block block = Block.getBlockById(block_id);
    Property p = this.to_search.get(block);
    if(p == null) return ColorButton.COLORS[0];
    return p.color;
  }

  private static class Property {

    public boolean tracer;
    public int color;

    public Property(boolean tracer, int color) {
      this.tracer = tracer;
      this.color = color;
    }
  }

  private static class Target {
    public Property property;
    public BlockPos position;

    public Target(BlockPos position, Property property) {
      this.position = position;
      this.property = property;
    }
  }
}
