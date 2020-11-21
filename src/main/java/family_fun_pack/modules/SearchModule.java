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

  private ReadWriteLock search_lock;
  private Map<Block, SearchOptions> to_search;

  private ReadWriteLock targets_lock;
  private Map<BlockPos, Property> targets;

  private List<ChunkPos> new_chunks;

  private ICamera camera;

  private Method transformMeth;

  public SearchModule() {
    super("Search", "Search for blocks");
    this.to_search = new HashMap<Block, SearchOptions>();
    this.search_lock = new ReentrantReadWriteLock();
    this.targets = new HashMap<BlockPos, Property>();
    this.targets_lock = new ReentrantReadWriteLock();
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
    this.new_chunks.clear();
  }

  /*
   * Configurations save & load
   */

  public void save(Configuration configuration) {
    for(Block b : Block.REGISTRY) {
      int id = Block.getIdFromBlock(b);

      this.search_lock.readLock().lock();
      SearchOptions opt = this.to_search.get(b);
      this.search_lock.readLock().unlock();

      if(opt == null) {
        configuration.get(this.name, "search_" + Integer.toString(id), false).set(false);
      } else {
        configuration.get(this.name, "search_" + Integer.toString(id), false).set(true);
        configuration.get(this.name, "tracer_" + Integer.toString(id), false).set(opt.default_property.tracer);
        configuration.get(this.name, "color_" + Integer.toString(id), ColorButton.DEFAULT_COLOR).set(opt.default_property.color);
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
        int color = configuration.get(this.name, "color_" + Integer.toString(id), ColorButton.DEFAULT_COLOR).getInt();

        this.search_lock.writeLock().lock();
        this.to_search.put(b, new SearchOptions(new Property(tracer, color)));
        this.search_lock.writeLock().unlock();
      }
    }
    super.load(configuration);
  }

  // Remove targets data from unloaded chunks
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
    if(this.new_chunks.size() > 0) {
      Minecraft mc = Minecraft.getMinecraft();
      if(mc.world == null) return;
      IChunkProvider provider = mc.world.getChunkProvider();
      if(provider == null) return;

      for(int i = 0; i < this.new_chunks.size(); i ++) {
        ChunkPos position = this.new_chunks.get(i);
        Chunk c = provider.getLoadedChunk(position.x, position.z);
        if(c != null) {
          this.searchChunk(c);
          this.new_chunks.remove(i);
          i --;
        }
      }
    }
  }

  /*
   * Network handler
   */

  public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
    if(id == 32) {
      SPacketChunkData chunk = (SPacketChunkData) packet;
      this.new_chunks.add(new ChunkPos(chunk.getChunkX(), chunk.getChunkZ()));
    } else if(id == 9) {
      SPacketUpdateTileEntity updt = (SPacketUpdateTileEntity) packet;

      Property p = this.isTargeted(Minecraft.getMinecraft().world.getBlockState(updt.getPos()), updt.getPos(), updt.getNbtCompound());

      this.targets_lock.writeLock().lock();
      if(p != null) this.targets.put(updt.getPos(), p);
      else this.targets.remove(updt.getPos());
      this.targets_lock.writeLock().unlock();

    } else if(id == 11) {
      SPacketBlockChange change = (SPacketBlockChange) packet;

      Property p = this.isTargeted(change.getBlockState(), change.getBlockPosition(), null);

      this.targets_lock.writeLock().lock();
      if(p != null) {
        this.targets.put(change.getBlockPosition(), p);
      } else this.targets.remove(change.getBlockPosition());
      this.targets_lock.writeLock().unlock();
    } else {
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
   * Render function
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
          RenderGlobal.renderFilledBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, red, blue, green, 0.5f);
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

  /*
   * Global Search functions, used to reset targets data for all loaded chunks in our field of view
  */

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

  /*
   * Basic search (per Block) setter / getter
   */

  public void setSearchState(int block_id, boolean state, boolean tracer, int color) {
    Block block = Block.getBlockById(block_id);

    this.search_lock.writeLock().lock();
    if(state) {
      this.to_search.put(block, new SearchOptions(new Property(tracer, color)));
    } else {
      SearchOptions opt = this.to_search.get(block);
      if(opt != null) {
        Property p = opt.default_property;
        FamilyFunPack.getModules().getConfiguration().get(this.name, "tracer_" + Integer.toString(block_id), false).set(p.tracer);
        FamilyFunPack.getModules().getConfiguration().get(this.name, "color_" + Integer.toString(block_id), ColorButton.DEFAULT_COLOR).set(p.color);
      }
      this.to_search.remove(block);
    }
    this.search_lock.writeLock().unlock();

    if(this.isEnabled()) this.resetTargets();
  }

  public void setTracerState(int block_id, boolean state) {
    Block block = Block.getBlockById(block_id);
    this.search_lock.readLock().lock();
    SearchOptions opt = this.to_search.get(block);
    this.search_lock.readLock().unlock();
    if(opt != null) {
      opt.default_property.tracer = state;
    }
  }

  public void setSearchColor(int block_id, int color) {
    Block block = Block.getBlockById(block_id);
    this.search_lock.readLock().lock();
    SearchOptions opt = this.to_search.get(block);
    this.search_lock.readLock().unlock();
    if(opt != null) {
      opt.default_property.color = color;
    }
  }

  public boolean getSearchState(int block_id) {
    Block block = Block.getBlockById(block_id);
    this.search_lock.readLock().lock();
    boolean ret = (this.to_search.get(block) != null);
    this.search_lock.readLock().unlock();
    return ret;
  }

  public boolean getTracerState(int block_id) {
    Block block = Block.getBlockById(block_id);
    this.search_lock.readLock().lock();
    SearchOptions opt = this.to_search.get(block);
    this.search_lock.readLock().unlock();
    if(opt != null) return opt.default_property.tracer;
    return FamilyFunPack.getModules().getConfiguration().get(this.name, "tracer_" + Integer.toString(block_id), false).getBoolean();
  }

  public int getColor(int block_id) {
    Block block = Block.getBlockById(block_id);
    this.search_lock.readLock().lock();
    SearchOptions opt = this.to_search.get(block);
    this.search_lock.readLock().unlock();
    if(opt != null) return opt.default_property.color;
    return FamilyFunPack.getModules().getConfiguration().get(this.name, "color_" + Integer.toString(block_id), ColorButton.DEFAULT_COLOR).getInt();
  }

  /*
   * Advanced searchs setter / getter
   */

   public void addAdvancedSearch(Block block, AdvancedSearch params) {
     this.search_lock.readLock().lock();
     SearchOptions opt = this.to_search.get(block);
     this.search_lock.readLock().unlock();

     if(opt == null) { // TODO: Load settings from file
       int id = Block.getIdFromBlock(block);
       opt = new SearchOptions(new Property(this.getTracerState(id), this.getColor(id)));
       this.search_lock.writeLock().lock();
       this.to_search.put(block, opt);
       this.search_lock.writeLock().unlock();
     }

     opt.addTarget(params);
     this.resetTargets();
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
     } // else TODO load from file - read length field

     return 0;
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
     } // else TODO load from file

     return null;
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

  public static class Property {
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

  private static class SearchOptions {

    public Property default_property;
    public List<AdvancedSearch> advanced_targets;
    public ReadWriteLock targets_locks;

    public SearchOptions(Property def) {
      this.default_property = def;
      this.advanced_targets = null;
      this.targets_locks = new ReentrantReadWriteLock();
    }

    public void addTarget(AdvancedSearch target) {
      this.targets_locks.writeLock().lock();
      if(this.advanced_targets == null) {
        this.advanced_targets = new LinkedList<AdvancedSearch>();
      }
      this.advanced_targets.add(target);
      this.targets_locks.writeLock().unlock();
    }

  }

  public static class AdvancedSearch {

    public Set<IBlockState> states;
    public NBTTagCompound tags;
    public Property property;

    public AdvancedSearch(Block block) {
      this.states = new HashSet<IBlockState>();
      for(IBlockState state : block.getBlockState().getValidStates()) {
        this.states.add(state);
      }
      this.tags = null;
      this.property = new Property(false, ColorButton.DEFAULT_COLOR);
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
}
