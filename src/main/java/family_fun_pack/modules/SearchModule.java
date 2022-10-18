package family_fun_pack.modules;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
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
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketMultiBlockChange;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import io.netty.buffer.ByteBuf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Class;
import java.lang.IllegalAccessException;
import java.lang.NoSuchMethodException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.MainGuiComponent;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.ColorButton;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.interfaces.SearchSelectionGui;
import family_fun_pack.network.PacketListener;

/* Block searching module */

@SideOnly(Side.CLIENT)
public class SearchModule extends Module implements PacketListener {

  // Draw tracer
  // Credits to SalHack for the code
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
    bufferbuilder.pos(x, y, z).color(red, green, blue, alpha).endVertex();
    bufferbuilder.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
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

  /* Map of Blocks to be searched */
  private final ReadWriteLock search_lock;
  private final Map<Block, SearchOptions> to_search;

  /* List of blocks that have been selected for highlighting */
  private final ReadWriteLock targets_lock;
  private final Map<BlockPos, Property> targets;

  /* New chunks to be search for targets */
  private final ReadWriteLock new_chunks_lock;
  private final List<ChunkPos> new_chunks;

  private final ICamera camera;

  private Method transformMeth;

  public SearchModule() {
    super("Search", "Search for blocks");
    this.to_search = new HashMap<Block, SearchOptions>();
    this.search_lock = new ReentrantReadWriteLock();
    this.targets = new HashMap<BlockPos, Property>();
    this.targets_lock = new ReentrantReadWriteLock();
    this.new_chunks = new ArrayList<ChunkPos>();
    this.new_chunks_lock = new ReentrantReadWriteLock();
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

  /*
   * Basic module actions
   */

  protected void enable() {
    MinecraftForge.EVENT_BUS.register(this);
    FamilyFunPack.getNetworkHandler().registerListener(EnumPacketDirection.CLIENTBOUND, this, 9, 11, 16, 32);
    this.resetTargets();
  }

  protected void disable() {
    MinecraftForge.EVENT_BUS.unregister(this);
    FamilyFunPack.getNetworkHandler().unregisterListener(EnumPacketDirection.CLIENTBOUND, this, 9, 11, 16, 32);
    this.onDisconnect();
  }

  public void onDisconnect() {
    this.targets_lock.writeLock().lock();
    this.targets.clear();
    this.targets_lock.writeLock().unlock();

    this.new_chunks_lock.writeLock().lock();
    this.new_chunks.clear();
    this.new_chunks_lock.writeLock().unlock();
  }

  /*
   * Configurations save & load
   */

   // save all configurations
  public void save(Configuration configuration) {
    for(Block block : Block.REGISTRY) {
      this.saveBlock(block, configuration);
    }
    super.save(configuration);
  }

  // save configuration for one block
  private void saveBlock(Block block, Configuration configuration) {
    int id = Block.getIdFromBlock(block);

    this.search_lock.readLock().lock();
    SearchOptions opt = this.to_search.get(block);
    this.search_lock.readLock().unlock();

    if(opt == null) {
      configuration.get(this.name, "search_" + id, false).set(false);
    } else {
      configuration.get(this.name, "search_" + id, false).set(true);
      configuration.get(this.name, "tracer_" + id, false).set(opt.default_property.tracer);
      configuration.get(this.name, "color_" + id, ColorButton.DEFAULT_COLOR).set(opt.default_property.color);

      int length = opt.getAdvancedSearchSize();
      opt.targets_locks.readLock().lock();
      configuration.get(this.name, "plength_" + id, 0).set(length);
      if(length > 0) {
        int i = 0;
        for(AdvancedSearch preset : opt.advanced_targets) {
          this.savePreset(configuration, preset, block, i);
          i ++;
        }
      }
      opt.targets_locks.readLock().unlock();
    }
  }

  // load all configurations
  public void load(Configuration configuration) {
    for(Block block : Block.REGISTRY) {
      this.loadBlock(block, configuration, false);
    }
    super.load(configuration);
  }

  // load configuration for one block
  private void loadBlock(Block block, Configuration configuration, boolean force) {
    int id = Block.getIdFromBlock(block);
    boolean search = configuration.get(this.name, "search_" + id, false).getBoolean();
    if(search || force) {
      boolean tracer = configuration.get(this.name, "tracer_" + id, false).getBoolean();
      int color = configuration.get(this.name, "color_" + id, ColorButton.DEFAULT_COLOR).getInt();

      SearchOptions opt = new SearchOptions(new Property(tracer, color));

      int length = configuration.get(this.name, "plength_" + id, 0).getInt();
      for(int i = 0; i < length; i ++) {
        opt.addTarget(this.loadPreset(configuration, block, i));
      }

      this.search_lock.writeLock().lock();
      this.to_search.put(block, opt);
      this.search_lock.writeLock().unlock();
    }
  }

  // load given preset configuration, for given block
  private AdvancedSearch loadPreset(Configuration configuration, Block block, int index) {
    AdvancedSearch out = new AdvancedSearch(block, false);
    String label = Block.getIdFromBlock(block) + "_" + index;

    // Get Block states
    int[] states = configuration.get(this.name, "states_" + label, new int[0]).getIntList();
    for(int i : states) {
      out.addState(Block.BLOCK_STATE_IDS.getByValue(i));
    }

    // Get tileentity
    boolean hasTile = configuration.get(this.name, "hasTile_" + label, false).getBoolean();
    if(hasTile) {
      byte[] buf = Base64.getDecoder().decode(configuration.get(this.name, "tile_" + label, "").getString());
      try {
        out.tags = CompressedStreamTools.read(new DataInputStream(new ByteArrayInputStream(buf)), new NBTSizeTracker(2097152L));
      } catch (IOException e) {}
    }

    // Color & tracer
    out.property.tracer = configuration.get(this.name, "tracer_" + label, false).getBoolean();
    out.property.color = configuration.get(this.name, "color_" + label, ColorButton.DEFAULT_COLOR).getInt();

    return out;
  }

  // save given preset configuration, for given block
  private void savePreset(Configuration configuration, AdvancedSearch preset, Block block, int index) {
    String label = Block.getIdFromBlock(block) + "_" + index;

    // Save block states
    int[] states = new int[preset.states.size()];
    int i = 0;
    for(IBlockState state : preset.states) {
      states[i] = Block.BLOCK_STATE_IDS.get(state);
      i ++;
    }
    configuration.get(this.name, "states_" + label, new int[0]).set(states);

    // Set tile entity
    boolean hasTile = (preset.tags != null);
    configuration.get(this.name, "hasTile_" + label, false).set(hasTile);
    if(hasTile) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try {
        CompressedStreamTools.write(preset.tags, new DataOutputStream(stream));
      } catch (IOException e) {}
      configuration.get(this.name, "tile_" + label, "").set(Base64.getEncoder().encodeToString(stream.toByteArray()));
    }

    // Color & tracer
    configuration.get(this.name, "tracer_" + label, false).set(preset.property.tracer);
    configuration.get(this.name, "color_" + label, ColorButton.DEFAULT_COLOR).set(preset.property.color);
  }

  /*
  * Targets management
  */

  // Remove targets located on unload chunks
  @SubscribeEvent
  public void onUnLoad(ChunkEvent.Unload event) {
    int x = event.getChunk().x, z = event.getChunk().z;

    this.targets_lock.writeLock().lock();
    Iterator<BlockPos> iterator = this.targets.keySet().iterator();
    while(iterator.hasNext()) {
      BlockPos position = iterator.next();
      if((position.getX() >> 4) == x && (position.getZ() >> 4) == z) {
        iterator.remove();
      }
    }
    this.targets_lock.writeLock().unlock();
  }

  // Explore new chunks for new targets
  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    this.new_chunks_lock.readLock().lock();
    int size = this.new_chunks.size();
    this.new_chunks_lock.readLock().unlock();

    if(size == 0) return;

    Minecraft mc = Minecraft.getMinecraft();
    if(mc.world == null) return;
    IChunkProvider provider = mc.world.getChunkProvider();
    if(provider == null) return;

    this.new_chunks_lock.writeLock().lock();
    Iterator iterator = this.new_chunks.iterator();
    while(iterator.hasNext()) {
      ChunkPos position = (ChunkPos) iterator.next();
      Chunk c = provider.getLoadedChunk(position.x, position.z);
      if(c != null) {
        this.searchChunk(c);
        iterator.remove();
      }
    }
    this.new_chunks_lock.writeLock().unlock();
  }

  /*
   * Network handler
   */

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    if(id == 32) { // New chunk
      SPacketChunkData chunk = (SPacketChunkData) packet;
      this.new_chunks_lock.writeLock().lock();
      this.new_chunks.add(new ChunkPos(chunk.getChunkX(), chunk.getChunkZ()));
      this.new_chunks_lock.writeLock().unlock();
    } else if(id == 9) { // New tile entity
      SPacketUpdateTileEntity updt = (SPacketUpdateTileEntity) packet;

      Property p = this.isTargeted(Minecraft.getMinecraft().world.getBlockState(updt.getPos()), updt.getPos(), updt.getNbtCompound());

      this.targets_lock.writeLock().lock();
      if(p != null) this.targets.put(updt.getPos(), p);
      else this.targets.remove(updt.getPos());
      this.targets_lock.writeLock().unlock();

    } else if(id == 11) { // New Block
      SPacketBlockChange change = (SPacketBlockChange) packet;

      Property p = this.isTargeted(change.getBlockState(), change.getBlockPosition(), null);

      this.targets_lock.writeLock().lock();
      if(p != null) {
        this.targets.put(change.getBlockPosition(), p);
      } else this.targets.remove(change.getBlockPosition());
      this.targets_lock.writeLock().unlock();
    } else { // New Blocks
      SPacketMultiBlockChange change = (SPacketMultiBlockChange) packet;
      for(SPacketMultiBlockChange.BlockUpdateData up : change.getChangedBlocks()) {

        Property p = this.isTargeted(up.getBlockState(), up.getPos(), null);

        this.targets_lock.writeLock().lock();
        if(p != null) this.targets.put(up.getPos(), p);
        else this.targets.remove(up.getPos());
        this.targets_lock.writeLock().unlock();
      }
    }
    return packet;
  }

  /*
   * Render function, highlight targets with right color + Draw tracers
  */

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

    this.targets_lock.readLock().lock();
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
          RenderGlobal.renderFilledBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, red, blue, green, 0.25f);
      }
    }
    this.targets_lock.readLock().unlock();

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
      drawLine3D((float) forward.x, (float) forward.y + mc.getRenderViewEntity().getEyeHeight(), (float) forward.z, (float) pos.x, (float) pos.y, (float) pos.z, 0.85f, t.property.color);
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

  /*
   * Global Search functions, used to reset targets data for all loaded chunks in our field of view
  */

  // Clear all targets, renew targets for every chunks in render distance
  public void resetTargets() {
    Minecraft mc = Minecraft.getMinecraft();
    if(mc.world == null) return;

    this.targets_lock.writeLock().lock();
    this.targets.clear();
    this.targets_lock.writeLock().unlock();

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

  // Search a chunk for targets
  private void searchChunk(Chunk chunk) {
    int m = -1;
    for(ExtendedBlockStorage storage : chunk.getBlockStorageArray()) {
      m ++;
      if(storage == null) continue;
      for(int i = 0; i < 16; i ++) {
        for(int j = 0; j < 16; j ++) {
          for(int k = 0; k < 16; k ++) {
            BlockPos position = new BlockPos((chunk.x << 4) + i, (m << 4) + j, (chunk.z << 4) + k);
            Property p =  this.isTargeted(storage.get(i, j, k), position, null);

            if(p != null) {
              this.targets_lock.writeLock().lock();
              this.targets.put(position, p);
              this.targets_lock.writeLock().unlock();
            }
          }
        }
      }
    }
  }

  // Remove targets that were registered with given property
  public void ClearFromTargets(Property property) {
    this.targets_lock.readLock().lock();
    Iterator i = this.targets.keySet().iterator();
    while(i.hasNext()) {
      BlockPos position = (BlockPos)i.next();
      if(this.targets.get(position) == property) i.remove();
    }
    this.targets_lock.readLock().unlock();
  }

  /*
   * Basic search (per Block) setter / getter
   */

  public void setSearchState(Block block, boolean state) {
    if(state) {
      // Load configuration
      this.loadBlock(block, FamilyFunPack.getModules().getConfiguration(), true);

      // Reset targets
      if(this.isEnabled()) this.resetTargets();
    } else {
      // Save configuration
      this.saveBlock(block, FamilyFunPack.getModules().getConfiguration());

      // Remove from search list
      this.search_lock.writeLock().lock();
      SearchOptions opt = this.to_search.remove(block);
      this.search_lock.writeLock().unlock();

      // Reset targets
      if(this.isEnabled()) {
        if(opt.advanced_targets != null) {
          opt.targets_locks.readLock().lock();
          for(AdvancedSearch preset : opt.advanced_targets) {
            this.ClearFromTargets(preset.property);
          }
          opt.targets_locks.readLock().unlock();
        } else this.ClearFromTargets(opt.default_property);
      }
    }

  }

  public void setTracerState(Block block, boolean state) {
    this.search_lock.readLock().lock();
    SearchOptions opt = this.to_search.get(block);
    this.search_lock.readLock().unlock();
    if(opt != null) {
      opt.default_property.tracer = state;
    }
  }

  public void setSearchColor(Block block, int color) {
    this.search_lock.readLock().lock();
    SearchOptions opt = this.to_search.get(block);
    this.search_lock.readLock().unlock();
    if(opt != null) {
      opt.default_property.color = color;
    }
  }

  public boolean getSearchState(Block block) {
    this.search_lock.readLock().lock();
    boolean ret = (this.to_search.get(block) != null);
    this.search_lock.readLock().unlock();
    return ret;
  }

  public boolean getTracerState(Block block) {
    this.search_lock.readLock().lock();
    SearchOptions opt = this.to_search.get(block);
    this.search_lock.readLock().unlock();
    if(opt != null) return opt.default_property.tracer;
    return FamilyFunPack.getModules().getConfiguration().get(this.name, "tracer_" + Block.getIdFromBlock(block), false).getBoolean();
  }

  public int getColor(Block block) {
    this.search_lock.readLock().lock();
    SearchOptions opt = this.to_search.get(block);
    this.search_lock.readLock().unlock();
    if(opt != null) return opt.default_property.color;
    return FamilyFunPack.getModules().getConfiguration().get(this.name, "color_" + Block.getIdFromBlock(block), ColorButton.DEFAULT_COLOR).getInt();
  }

  /*
   * Advanced searchs setter / getter
   */

   public void addAdvancedSearch(Block block, AdvancedSearch params) {
     this.search_lock.readLock().lock();
     SearchOptions opt = this.to_search.get(block);
     this.search_lock.readLock().unlock();

     if(opt == null) {
       this.setSearchState(block, true); // Enable & load search data

       this.search_lock.readLock().lock();
       opt = this.to_search.get(block);
       this.search_lock.readLock().unlock();
     }

     opt.addTarget(params);
     if(this.isEnabled()) this.resetTargets();
   }

   public void updateAdvancedSearch(Block block, int index, AdvancedSearch new_preset) {
       this.search_lock.readLock().lock();
       SearchOptions opt = this.to_search.get(block);
       this.search_lock.readLock().unlock();

       if(opt != null) {
         opt.updateTarget(index, new_preset);
         if(this.isEnabled()) this.resetTargets();
       } else {
         this.savePreset(FamilyFunPack.getModules().getConfiguration(), new_preset, block, index);
       }
   }

   public int getAdvancedSearchListSize(Block block) {
     this.search_lock.readLock().lock();
     SearchOptions opt = this.to_search.get(block);
     this.search_lock.readLock().unlock();

     if(opt != null) {
       int size = 0;
       opt.targets_locks.readLock().lock();
       if(opt.advanced_targets != null) size = opt.advanced_targets.size();
       opt.targets_locks.readLock().unlock();
       return size;
     }

     return FamilyFunPack.getModules().getConfiguration().get(this.name, "plength_" + Block.getIdFromBlock(block), 0).getInt();
   }

   public AdvancedSearch getAdvancedSearch(Block block, int index) {
     this.search_lock.readLock().lock();
     SearchOptions opt = this.to_search.get(block);
     this.search_lock.readLock().unlock();

     if(opt != null) {
       AdvancedSearch out = null;
       opt.targets_locks.readLock().lock();
       if(index >= 0 && index < opt.advanced_targets.size()) out = opt.advanced_targets.get(index);
       opt.targets_locks.readLock().unlock();
       return out;
     }

     return this.loadPreset(FamilyFunPack.getModules().getConfiguration(), block, index);
   }

   public void removeAdvancedSearch(Block block, int index) {
     this.search_lock.readLock().lock();
     SearchOptions opt = this.to_search.get(block);
     this.search_lock.readLock().unlock();

     Configuration config = FamilyFunPack.getModules().getConfiguration();
     int length = this.getAdvancedSearchListSize(block);

     if(opt != null) {
       opt.targets_locks.writeLock().lock();
       AdvancedSearch preset = opt.removeTarget(index);
       opt.targets_locks.writeLock().unlock();
       if(this.isEnabled()) {
         this.ClearFromTargets(preset.property);
       }
     } else {
       int i = index + 1;
       while(i < length) { // shift presets
         AdvancedSearch next = this.loadPreset(config, block, i);
         this.savePreset(config, next, block, i - 1);
         i ++;
       }

       // Update presets list length
       config.get(this.name, "plength_" + Block.getIdFromBlock(block), 0).set(length - 1);
     }

     // Erase last preset from config structure
     ConfigCategory cat = config.getCategory(this.name);
     String label = Block.getIdFromBlock(block) + "_" + (length - 1);
     cat.remove("states_" + label);
     cat.remove("hasTile_" + label);
     cat.remove("tile_" + label);
     cat.remove("tracer_" + label);
     cat.remove("color_" + label);
   }

  /*
  * Data structures management functions
  */

  // Does tag contains sub ?
  public boolean containsTag(NBTTagCompound tag, NBTTagCompound sub) {
    for(String key : sub.getKeySet()) {
      if(tag.getTagId(key) != sub.getTagId(key)) return false;
      if(sub.getTagId(key) == 10) { // NBTTagCompound
        if(! this.containsTag(tag.getCompoundTag(key), sub.getCompoundTag(key))) return false;
      } else {
        if(! tag.getTag(key).equals(sub.getTag(key))) return false;
      }
    }
    return true;
  }

  // Which color to use for highlighting given blockstate, null if none
  public Property isTargeted(IBlockState state, BlockPos position, NBTTagCompound updt_tag) {
    this.search_lock.readLock().lock();
    SearchOptions options = this.to_search.get(state.getBlock());
    this.search_lock.readLock().unlock();
    if(options == null) return null;
    if(options.advanced_targets == null) return options.default_property;
    options.targets_locks.readLock().lock();
    for(AdvancedSearch i : options.advanced_targets) {
      if(i.states.contains(state)) {
        if(i.tags != null) {
          if(updt_tag == null) {
            TileEntity tile = Minecraft.getMinecraft().world.getTileEntity(position);
            if(tile == null || !(this.containsTag(tile.getUpdateTag(), i.tags))) continue;
          } else if(! (this.containsTag(updt_tag, i.tags))) continue;
        }
        options.targets_locks.readLock().unlock();
        return i.property;
      }
    }
    options.targets_locks.readLock().unlock();
    return null;
  }

  /*
  * Sub-Classes used in data structures
  */

  // A highlight property (color + tracer enabled ?)
  public static class Property {
    public boolean tracer;
    public int color;

    public Property(boolean tracer, int color) {
      this.tracer = tracer;
      this.color = color;
    }
  }

  // A target to be highlighted, located at position
  private static class Target {
    public Property property;
    public BlockPos position;

    public Target(BlockPos position, Property property) {
      this.position = position;
      this.property = property;
    }
  }

  // Search options for a block: default search property and eventually presets overwrite
  private static class SearchOptions {

    public Property default_property;
    public List<AdvancedSearch> advanced_targets;
    public ReadWriteLock targets_locks;

    public SearchOptions(Property def) {
      this.default_property = def;
      this.advanced_targets = null;
      this.targets_locks = new ReentrantReadWriteLock();
    }

    public int getAdvancedSearchSize() {
      int out = 0;
      this.targets_locks.readLock().lock();
      if(this.advanced_targets != null) {
        out = this.advanced_targets.size();
      }
      this.targets_locks.readLock().unlock();
      return out;
    }

    public void addTarget(AdvancedSearch target) {
      this.targets_locks.writeLock().lock();
      if(this.advanced_targets == null) {
        this.advanced_targets = new LinkedList<AdvancedSearch>();
      }
      this.advanced_targets.add(target);
      this.targets_locks.writeLock().unlock();
    }

    public void updateTarget(int index, AdvancedSearch target) {
      this.targets_locks.writeLock().lock();
      AdvancedSearch old = this.advanced_targets.get(index);
      old.states = target.states;
      old.tags = target.tags;
      old.property.tracer = target.property.tracer;
      old.property.color = target.property.color;
      this.targets_locks.writeLock().unlock();
    }

    public AdvancedSearch removeTarget(int index) {
      AdvancedSearch out = null;
      this.targets_locks.writeLock().lock();
      if(this.advanced_targets != null) {
        out = this.advanced_targets.remove(index);
        if(this.advanced_targets.size() == 0) this.advanced_targets = null;
      }
      this.targets_locks.writeLock().unlock();
      return out;
    }

  }

  // Advanced search preset: which block states to search for, which tileentity tags, which property to apply
  public static class AdvancedSearch {

    public Set<IBlockState> states;
    public NBTTagCompound tags;
    public Property property;

    public AdvancedSearch(Block block, boolean pre_filled) {
      this.states = new HashSet<IBlockState>();
      if(pre_filled) {
        for(IBlockState state : block.getBlockState().getValidStates()) {
          this.states.add(state);
        }
      }
      this.tags = null;
      this.property = new Property(false, ColorButton.DEFAULT_COLOR);
    }

    public AdvancedSearch(Block block) {
      this(block, true);
    }

    public void addProperty(IProperty<?> property, Comparable<?> value) {
      Iterator iterator = this.states.iterator();
      while(iterator.hasNext()) {
        IBlockState state = (IBlockState)iterator.next();
        if(! state.getValue(property).equals(value)) {
          iterator.remove();
        }
      }
    }

    public void addState(IBlockState state) {
      this.states.add(state);
    }

    private NBTTagCompound addTagCompound(String path, NBTTagCompound base) {
      NBTTagCompound tag = new NBTTagCompound();
      base.setTag(path, tag);
      return tag;
    }

    private NBTTagCompound pathLoop(String[] path) {
      if(this.tags == null) this.tags = new NBTTagCompound();
      NBTTagCompound tag = this.tags;
      for(int i = 0; i < path.length - 1; i ++) {
        tag = this.addTagCompound(path[i], tag);
      }
      return tag;
    }

    public void addTag(String[] path, int value) {
      NBTTagCompound base = this.pathLoop(path);
      base.setInteger(path[path.length - 1], value);
    }

    public void addTag(String[] path, short value) {
      NBTTagCompound base = this.pathLoop(path);
      base.setShort(path[path.length - 1], value);
    }

    public void addTag(String[] path, byte value) {
      NBTTagCompound base = this.pathLoop(path);
      base.setByte(path[path.length - 1], value);
    }

    public void addTag(String[] path, String value) {
      NBTTagCompound base = this.pathLoop(path);
      base.setString(path[path.length - 1], value);
    }
  }

  /*
   * Link with search selection GUI
   */

   // To be displayed in Main GUI, to access the search selection GUI
   private class GuiComponent implements MainGuiComponent {

     private final SearchModule dependence;

     public GuiComponent(SearchModule dependence) {
       this.dependence = dependence;
     }

     public String getLabel() {
       return "which blocks ?";
     }

     public ActionButton getAction() {
       return new OpenGuiButton(0, 0, "blocks", SearchSelectionGui.class, this.dependence);
     }

     public MainGuiComponent getChild() {
       return null;
     }
   }

   // Search selection GUI is child of this module in Main GUI
   public MainGuiComponent getChild() {
     return new GuiComponent(this);
   }
}
