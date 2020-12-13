package family_fun_pack.gui.interfaces;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.tileentity.TileEntityEndPortal;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.lang.Class;
import org.lwjgl.input.Mouse;

import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.components.ColorButton;
import family_fun_pack.gui.components.OnOffButton;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.components.ScrollBar;
import family_fun_pack.gui.components.actions.OnOffSearch;
import family_fun_pack.gui.components.actions.OnOffTracer;
import family_fun_pack.modules.Module;
import family_fun_pack.modules.SearchModule;
import family_fun_pack.utils.FakeWorld;

/* Block selection GUI for search module */

@SideOnly(Side.CLIENT)
public class SearchSelectionGui extends RightPanel {

  private static final int INNER_BORDER = 0xffeeeeee;

  private static final int guiWidth = 268;
  private static final int guiHeight = 200;

  private static final int maxLabelsDisplayed = 10;

  private ScrollBar scroll;
  private GuiTextField selection;
  private String last_search;

  private int x, y, x_end, y_end;

  private List<Block> blocks;
  private List<OnOffButton> tracers;
  private List<OnOffButton> search;
  private List<ColorButton> colors;
  private List<OpenGuiButton> advanced;

  private FakeWorld world; // Used in tileentities rendering methods

  public SearchSelectionGui() {
    this.x = MainGui.guiWidth + 16;
    this.y = 12;
    this.x_end = SearchSelectionGui.guiWidth + this.x;
    this.y_end = SearchSelectionGui.guiHeight + this.y;

    this.blocks = new ArrayList<Block>();
    for(Block b : Block.REGISTRY) {
      this.blocks.add(b);
    }

    int max_scroll = this.blocks.size() - SearchSelectionGui.maxLabelsDisplayed;
    if(max_scroll < 0) max_scroll = 0;
    this.scroll = new ScrollBar(0, this.x_end - 10, this.y + 4, max_scroll, this.y_end - 4);
    this.buttonList.add(this.scroll);

    this.selection = new GuiTextField(0, this.fontRenderer, this.x + 4, this.y + 5, (int)((float)(this.x_end - 6 - this.scroll.width - this.x - 4) * 0.571f) - 6, 10);
    this.selection.setFocused(true);
    this.selection.setCanLoseFocus(false);
    this.selection.setMaxStringLength(256);
    this.advanced = new ArrayList<OpenGuiButton>(SearchSelectionGui.maxLabelsDisplayed);
    this.last_search = "";
    this.world = new FakeWorld(null, this.mc.world.provider);
  }

  // Depends on SearchModule
  public void dependsOn(Module dependence) {
    super.dependsOn(dependence);

    this.tracers = new ArrayList<OnOffButton>(this.blocks.size());
    this.search = new ArrayList<OnOffButton>(this.blocks.size());
    this.colors = new ArrayList<ColorButton>(this.blocks.size());

    int chart_width = this.x_end - 6 - this.scroll.width - this.x - 4;

    for(int i = 0; i < this.blocks.size(); i ++) {
      Block block = this.blocks.get(i);
      boolean search_state = ((SearchModule) this.dependence).getSearchState(block);

      OnOffButton tracer = new OnOffButton(i, 0, 0, new OnOffTracer(block, (SearchModule) this.dependence));
      tracer.x = ((((int)((float)chart_width * 0.143f)) - tracer.width) / 2) + (int)((float)chart_width * 0.714f) + this.x + 4;
      tracer.setState(((SearchModule) this.dependence).getTracerState(block));
      if(! search_state) tracer.enabled = false;

      ColorButton color = new ColorButton(0, 0, block, (SearchModule) this.dependence);
      color.x = ((((int)((float)chart_width * 0.143f)) - color.width) / 2) + (int)((float)chart_width * 0.857f) + this.x + 4;
      color.setColor(((SearchModule) this.dependence).getColor(block));
      if(! search_state) color.enabled = false;

      OnOffButton search = new OnOffButton(i, 0, 0, new OnOffSearch(block, (SearchModule) this.dependence, tracer, color));
      search.x = ((((int)((float)chart_width * 0.143f)) - search.width) / 2) + (int)((float)chart_width * 0.571f) + this.x + 4;
      search.setState(search_state);

      this.tracers.add(tracer);
      this.search.add(search);
      this.colors.add(color);
    }

    for(int i = 0; i < SearchSelectionGui.maxLabelsDisplayed; i ++) {
      this.advanced.add(new OpenGuiButton((int)((float)(this.x_end - 6 - this.scroll.width - this.x - 4) * 0.571f) - (int)((float)(this.fontRenderer.getStringWidth("More options")) * 0.6f) + this.x, 0, "More options", AdvancedSearchGui.class, this.dependence, 0.6f));
      this.buttonList.add(this.advanced.get(i));
    }
  }

  // Set to enabled search btn for given block
  public void enableSearchBtn(Block block) {
    int index = this.blocks.indexOf(block);
    this.search.get(index).setState(true);
  }

  // When advanced options clicked, get the Block for which it was asked
  public Block getCurrentBlock() {
    for(int i = 0; i < SearchSelectionGui.maxLabelsDisplayed; i ++) {
      OpenGuiButton btn = this.advanced.get(i);
      if(btn.isClicked()) {
        btn.resetState();
        return this.blocks.get(i + this.scroll.current_scroll);
      }
    }
    return null;
  }

  // Compute search bar input
  private void searchBlocks(String keyword) {
    this.blocks.clear();
    for(Block b : Block.REGISTRY) {
      String label = Block.REGISTRY.getNameForObject(b).getResourcePath().replace("_", " ").toLowerCase();
      if(label.contains(keyword)) this.blocks.add(b);
    }
    int max_scroll = this.blocks.size() - SearchSelectionGui.maxLabelsDisplayed;
    if(max_scroll < 0) max_scroll = 0;
    this.scroll.resetMaxScroll(max_scroll);
    this.dependsOn(this.dependence);
  }

  // Draw
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    // background
    Gui.drawRect(this.x, this.y, this.x_end, this.y_end, MainGui.BACKGROUND_COLOR);
    Gui.drawRect(this.x, this.y, this.x_end, this.y + 2, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x, this.y, this.x + 2, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end - 2, this.y, this.x_end, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x, this.y_end - 2, this.x_end, this.y_end, MainGui.BORDER_COLOR);

    // Update scroll
    if(this.scroll.clicked) {
      this.scroll.dragged(mouseX, mouseY);
    }

    // search bar
    this.selection.drawTextBox();

    // Vertical borders
    int chart_end = this.x_end - 6 - this.scroll.width;
    int chart_width = chart_end - this.x - 4;
    int decal_x;
    Gui.drawRect(this.x + 3, this.y + 16, this.x + 4, this.y + 192, SearchSelectionGui.INNER_BORDER);
    Gui.drawRect(this.x + 20, this.y + 16, this.x + 21, this.y + 192, SearchSelectionGui.INNER_BORDER);
    Gui.drawRect(chart_end - 1, this.y + 16, chart_end, this.y + 192, SearchSelectionGui.INNER_BORDER);
    decal_x = this.x + 4 + (int)((float)chart_width * 0.571f);
    Gui.drawRect(decal_x - 1, this.y + 16, decal_x, this.y + 192, SearchSelectionGui.INNER_BORDER);
    decal_x = this.x + 4 + (int)((float)chart_width * 0.714f);
    Gui.drawRect(decal_x - 1, this.y + 16, decal_x, this.y + 192, SearchSelectionGui.INNER_BORDER);
    decal_x = this.x + 4 + (int)((float)chart_width * 0.857f);
    Gui.drawRect(decal_x - 1, this.y + 16, decal_x, this.y + 192, SearchSelectionGui.INNER_BORDER);

    // Draw titles
    GlStateManager.pushMatrix();
    GlStateManager.scale(0.9f, 0.9f, 0.9f);
    String[] labels = {"Search", "Tracer", "Color"};
    int x_total = this.x + 10 + this.selection.width;
    int decal_y = (int)((float)(this.y + 8) / 0.9f);
    for(String i : labels) {
      int width = (int)((float)chart_width * 0.143f);
      int str_width = this.fontRenderer.getStringWidth(i);
      int x = x_total + ((width - str_width) / 2);
      decal_x = (int)((float)(x) / 0.9f);
      this.drawString(this.fontRenderer, i, decal_x, decal_y, 0xffffffff);
      x_total += width;
    }
    GlStateManager.popMatrix();

    // Draw chart
    int scroll_end = this.scroll.current_scroll + SearchSelectionGui.maxLabelsDisplayed > this.blocks.size() ? this.blocks.size() : this.scroll.current_scroll + SearchSelectionGui.maxLabelsDisplayed;
    int y = 20 + this.y;
    Gui.drawRect(this.x + 4, y - 1, chart_end, y, SearchSelectionGui.INNER_BORDER);
    int i;
    for(i = this.scroll.current_scroll; i < scroll_end; i ++) {

      Block block = this.blocks.get(i);

      // Draw block
      this.displayBlockFlat(this.x + 4, y, block);

      // Draw label
      GlStateManager.pushMatrix();
      GlStateManager.scale(0.7f, 0.7f, 0.7f);
      decal_y = (int)((float)y / 0.7f) + 4;
      decal_x = (int)((float)(this.x + 23) / 0.7f);
      String label = Block.REGISTRY.getNameForObject(block).getResourcePath().replace("_", " ");
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      this.drawString(this.fontRenderer, label, decal_x, decal_y, 0xffeeeeee);
      GlStateManager.popMatrix();

      // Draw buttons
      OnOffButton search = this.search.get(i);
      search.y = y + 4;
      search.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);

      OnOffButton tracer = this.tracers.get(i);
      ColorButton color = this.colors.get(i);

      int preset_count = ((SearchModule) this.dependence).getAdvancedSearchListSize(block);
      if(preset_count == 0) {
        tracer.visible = true;
        tracer.y = y + 4;
        tracer.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);

        color.visible = true;
        color.y = y + 4;
        color.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);
      } else {
        tracer.visible = false;
        color.visible = false;

        // Draw number of advanced presets
        decal_x = this.x + 4 + (int)((float)chart_width * 0.857f);
        Gui.drawRect(decal_x - 1, y, decal_x, y + 16, 0xff000000);

        GlStateManager.pushMatrix();
        GlStateManager.scale(0.60f, 0.60f, 0.60f);

        if(preset_count == 1) label = "Registered preset: 1";
        else label = "Registered presets: " + Integer.toString(preset_count);
        this.drawString(this.fontRenderer, label, (int)((this.x + 4 + (int)((float)chart_width * 0.714f) + (((int)((float)chart_width * 0.286f) - (int)(this.fontRenderer.getStringWidth(label) * 0.60f)) / 2)) / 0.60f), (int)((y + 5) / 0.60f), 0xffffffff);

        GlStateManager.popMatrix();
      }

      // Draw border
      Gui.drawRect(this.x + 4, y + 16, chart_end, y + 17, SearchSelectionGui.INNER_BORDER);
      y += 17;

      // Draw more options button
      OpenGuiButton options = this.advanced.get(i - this.scroll.current_scroll);
      if(this.hasSpecialStates(this.blocks.get(i))) {
        options.y = y - 9;
        options.visible = true;
      } else options.visible = false;
    }

    while(i < this.scroll.current_scroll + SearchSelectionGui.maxLabelsDisplayed) {
      this.advanced.get(i - this.scroll.current_scroll).visible = false;
      i ++;
    }

    // Buttons
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  // Does given block have multiple possible states
  public boolean hasSpecialStates(Block b) {
    if(b.getBlockState().getValidStates().size() > 1) return true;
    if(b.hasTileEntity(null)) {
      return ((!(b.createTileEntity(this.mc.world, b.getDefaultState()) instanceof TileEntityLockableLoot))
         && b != Blocks.POWERED_COMPARATOR && b != Blocks.UNPOWERED_COMPARATOR && b != Blocks.ENCHANTING_TABLE
         && b != Blocks.DAYLIGHT_DETECTOR && b != Blocks.DAYLIGHT_DETECTOR_INVERTED && b != Blocks.ENDER_CHEST
         && b != Blocks.END_PORTAL && b != Blocks.END_GATEWAY && b != Blocks.NOTEBLOCK && b != Blocks.STANDING_SIGN
         && b != Blocks.WALL_SIGN);
    }
    return false;
  }

  // Display block face at given coords in GUI
  public void displayBlockFlat(int x, int y, Block block) {
    IBlockState state = block.getDefaultState();
    try {
      state = state.withProperty(BlockHorizontal.FACING, EnumFacing.SOUTH);
    } catch(IllegalArgumentException e) {}
    try {
      state = state.withProperty(BlockDirectional.FACING, EnumFacing.SOUTH);
    } catch(IllegalArgumentException e) {}

    // Better than default state rendering
    if(block instanceof BlockWall) {
      state = state.withProperty(BlockWall.UP, Boolean.valueOf(true));
    } else if(block == Blocks.BED) {
      state = state.withProperty(BlockHorizontal.FACING, EnumFacing.NORTH);
    }

    this.displayBlockFlat(x, y, state);
  }

  // Display block state at given coords in GUI
  public void displayBlockFlat(int x, int y, IBlockState state) {
    this.displayBlockFlat(x, y, state, null);
  }

  // Display block state with tileentity at given coords in GUI
  public void displayBlockFlat(int x, int y, IBlockState state, TileEntity tile) {
    if(state.getBlock() == Blocks.AIR) return;

    ItemStack stack = new ItemStack(state.getBlock());

    IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);
    if(model == null || model == this.mc.getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getMissingModel()) {
      //model = mc.getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getModel(new ModelResourceLocation(Block.REGISTRY.getNameForObject(block).getResourcePath(), "inventory"));
      model = this.itemRender.getItemModelWithOverrides(stack, null, this.mc.player);
    }

    GlStateManager.pushMatrix();
    this.mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    this.mc.renderEngine.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
    GlStateManager.enableRescaleNormal();
    GlStateManager.enableAlpha();
    GlStateManager.alphaFunc(516, 0.1F);
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

    RenderHelper.enableGUIStandardItemLighting();

    GlStateManager.translate(x, y, 150f);
    GlStateManager.translate(8.0F, 8.0F, 0f);
    GlStateManager.scale(1.0F, -1.0F, 1.0F);
    GlStateManager.scale(16.0F, 16.0F, 16.0F);

    GlStateManager.disableLighting();

    GlStateManager.pushMatrix();
    GlStateManager.translate(-0.5F, -0.5F, -0.5F);

    if(model == null || model.isBuiltInRenderer() || model == this.mc.getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getMissingModel()) {
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableRescaleNormal();
      if(tile == null) tile = this.createTileEntity(state);
      //stack.getItem().getTileEntityItemStackRenderer().renderByItem(stack);
      if(tile != null) this.renderByTileEntity(tile, state);
    } else {
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();
      bufferbuilder.begin(7, DefaultVertexFormats.ITEM);
      for (EnumFacing enumfacing : EnumFacing.values())
      {
        this.itemRender.renderQuads(bufferbuilder, model.getQuads(state, enumfacing, 4242L), -1, stack);
      }
      this.itemRender.renderQuads(bufferbuilder, model.getQuads(state, null, 4242L), -1, stack);
      tessellator.draw();
    }

    GlStateManager.popMatrix();

    GlStateManager.disableAlpha();
    GlStateManager.disableRescaleNormal();
    GlStateManager.disableLighting();
    GlStateManager.popMatrix();
    mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    mc.renderEngine.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
  }

  // Special render for tileentties
  private void renderByTileEntity(TileEntity tile, IBlockState state) {
    // Use custom world to set blockstate, used by tileentities for rendering
    this.world.setBlockState(null, state, 0);
    World save = TileEntityRendererDispatcher.instance.world;
    TileEntityRendererDispatcher.instance.world = this.world; // for Shulker box rendering

    if(tile instanceof TileEntityBanner || tile instanceof TileEntityEnderChest || tile instanceof TileEntityBed
      || tile instanceof TileEntityChest || tile instanceof TileEntityShulkerBox) {
      TileEntityRendererDispatcher.instance.render(tile, 0d, 0d, 0d, 0f, 1f);
    } else if(tile instanceof TileEntitySkull) {
      if (TileEntitySkullRenderer.instance != null) {
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        TileEntitySkullRenderer.instance.renderSkull(0f, 0f, 0f, (EnumFacing)(state.getValue(BlockDirectional.FACING)), (((TileEntitySkull) tile).getSkullRotation() * 360) / 16.0F, ((TileEntitySkull)tile).getSkullType(), this.mc.getSession().getProfile(), -1, 0f);
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
      }
    } else {
      TileEntitySpecialRenderer<TileEntity> renderer = null;
      if(tile instanceof TileEntityEndGateway) renderer = TileEntityRendererDispatcher.instance.getRenderer(TileEntityEndPortal.class); // Avoid glitchy gateway beam rendering
      else renderer = TileEntityRendererDispatcher.instance.getRenderer((Class)tile.getClass());
      if(renderer != null) renderer.render(tile, 0d, 0d, 0d, 0f, -1, 0f);
    }

    TileEntityRendererDispatcher.instance.world = save;
  }

  // Create tileentity for blockstate, associate it to fake world
  public TileEntity createTileEntity(IBlockState state) {
    TileEntity tile = state.getBlock().createTileEntity(this.mc.world, state);
    if(tile == null) return null;
    tile.setWorld(this.world);
    return tile;
  }

  // Mouse clicked handling
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    if(mouseButton == 0) {
      for(int i = this.scroll.current_scroll; (i - this.scroll.current_scroll) < SearchSelectionGui.maxLabelsDisplayed && i < this.blocks.size(); i ++) {
        OnOffButton search = this.search.get(i);
        if(search.mousePressed(this.mc, mouseX, mouseY)) {
          search.onClick((GuiScreen) this);
          search.playPressSound(this.mc.getSoundHandler());
          return;
        }

        OnOffButton tracer = this.tracers.get(i);
        if(tracer.mousePressed(this.mc, mouseX, mouseY)) {
          tracer.onClick((GuiScreen) this);
          tracer.playPressSound(this.mc.getSoundHandler());
          return;
        }

        this.colors.get(i).mousePressed(this.mc, mouseX, mouseY);
      }
      this.selection.mouseClicked(mouseX, mouseY, mouseButton);
      super.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  // Mouse released handling
  public void mouseReleased(int mouseX, int mouseY, int state) {
    if(state == 0) {
      for(int i = this.scroll.current_scroll; (i - this.scroll.current_scroll) < SearchSelectionGui.maxLabelsDisplayed && i < this.blocks.size(); i ++) {
        this.colors.get(i).mouseReleased(mouseX, mouseY);
      }
      super.mouseReleased(mouseX, mouseY, state);
    }
  }

  // Update search bar every tick (make it blink)
  public void updateScreen() { // every tick
    this.selection.updateCursorCounter();
  }

  // On key typed
  public void keyTyped(char keyChar, int keyCode) throws IOException {
    this.selection.textboxKeyTyped(keyChar, keyCode);
    String keyword = this.selection.getText().trim().toLowerCase();
    if(! this.last_search.equals(keyword)) {
      this.last_search = keyword;
      this.searchBlocks(keyword);
    }
  }
}
