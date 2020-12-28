package family_fun_pack.gui.interfaces;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.properties.IProperty;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.MainGui;
import family_fun_pack.gui.components.ArrowButton;
import family_fun_pack.gui.components.ColorButton;
import family_fun_pack.gui.components.GenericButton;
import family_fun_pack.gui.components.OnOffButton;
import family_fun_pack.gui.components.SelectButton;
import family_fun_pack.modules.SearchModule;

/* BlockStates selection for advanced search */

@SideOnly(Side.CLIENT)
public class AdvancedSearchGui extends RightPanel {

  private static final int guiWidth = 168;
  private static final int guiHeight = 200;

  // Interface dimensions
  private int x, y, x_end, y_end;

  // Currently displayed states
  private IBlockState display_state;
  private Block block;
  private TileEntity display_tile;

  // Presets management
  private int preset_count;
  private int current_preset;

  // Interface buttons
  private OnOffButton tracer;
  private ColorButton color;

  private ArrowButton left_arrow;
  private ArrowButton right_arrow;

  private GenericButton add;
  private GenericButton update;
  private GenericButton remove;

  public AdvancedSearchGui() {
    super();

    this.x = MainGui.guiWidth + 16;
    this.y = 12;
    this.x_end = AdvancedSearchGui.guiWidth + this.x;
    this.y_end = AdvancedSearchGui.guiHeight + this.y;

    this.buttonList.add(new GenericButton(0, this.x + 4, this.y + 4, "back") {
      public void onClick(GuiScreen parent) {
        ((AdvancedSearchGui) parent).close();
      }
    });

    this.display_tile = null;
    this.current_preset = 0;

    // Add buttons & labels
    this.left_arrow = new ArrowButton(this.x + (AdvancedSearchGui.guiWidth / 2 - 40), this.y + 7, true);
    this.buttonList.add(this.left_arrow);
    this.right_arrow = new ArrowButton(this.x + (AdvancedSearchGui.guiWidth / 2 + 32), this.y + 7, false);
    this.buttonList.add(this.right_arrow);

    int y = this.y_end - 32;
    GuiLabel label = new GuiLabel(this.fontRenderer, 0, this.x + 8, y, 100, 16, 0xffffffff);
    label.addLine("Tracer");
    this.labelList.add(label);
    this.tracer = new OnOffButton(0, this.x + 56, y + 4, null);
    this.buttonList.add(this.tracer);

    label = new GuiLabel(this.fontRenderer, 0, this.x + ((this.x_end - this.x) / 2), y, 100, 16, 0xffffffff);
    label.addLine("Color");
    this.labelList.add(label);
    this.color = new ColorButton(0, this.x_end - 42, y + 4, null);
    this.buttonList.add(this.color);

    this.add = new GenericButton(0, this.x_end - 26, this.y_end - 17, "Add") {
      public void onClick(GuiScreen parent) {
        ((AdvancedSearchGui)parent).addToSearch();
      }
    };
    this.buttonList.add(this.add);

    this.update = new GenericButton(0, this.x_end - 42, this.y_end - 17, "Update") {
      public void onClick(GuiScreen parent) {
        ((AdvancedSearchGui)parent).updateSearch();
      }
    };
    this.update.enabled = false;
    this.buttonList.add(this.update);

    this.remove = new GenericButton(0, this.x + 4, this.y_end - 17, "Remove") {
      public void onClick(GuiScreen parent) {
        ((AdvancedSearchGui)parent).removeFromSearch();
      }
    };
    this.remove.enabled = false;
    this.buttonList.add(this.remove);
  }

  // When parent interface is linked (SearchSelectionGui)
  public void setParent(GuiScreen parent) {
    super.setParent(parent);
    this.block = ((SearchSelectionGui)this.parent).getCurrentBlock();
    this.display_state = this.block.getBlockState().getBaseState();

    int y = 28 + this.y;
    for(IProperty<?> p : this.block.getBlockState().getProperties()) {
      this.buttonList.add(new SelectButton(1, this.x_end - 40, y + 1, p));
      GuiLabel label = new GuiLabel(this.fontRenderer, 0, this.x + 8, y, 100, 16, 0xffffffff);
      label.addLine(p.getName());
      this.labelList.add(label);
      y += this.fontRenderer.FONT_HEIGHT + 4;
    }

    if(this.block.hasTileEntity(this.display_state)) {
      this.display_tile = ((SearchSelectionGui) this.parent).createTileEntity(this.display_state);
      if(this.display_tile == null) return;
      y = this.computeTileEntity(this.display_tile.getUpdateTag(), y, new LinkedList<String>());
    }

    this.preset_count = ((SearchModule) this.dependence).getAdvancedSearchListSize(this.block);
  }

  // Add labels in GUI for selecting wanted values in NBT fields
  private int computeTileEntity(NBTTagCompound tag, int y, List<String> path) {
    for(String key : tag.getKeySet()) {
      switch(tag.getTagId(key)) {
        case 1: // NBTTagByte
        case 2: // NBTTagShort
        case 3: // NBTTagInt
          if(key.length() <= 1) continue;
          path.add(key);
          this.buttonList.add(new SelectButton(2, this.x_end - 40, y + 1, path, (int)(tag.getShort(key))));
          path.remove(path.size() - 1);
          break;
        case 8: // NBTTagString
          String value = tag.getString(key);
          if(value.equals("minecraft:pig")) { // Special case for spawner, this is sketchy
            List<String> values = new LinkedList<String>();
            for(ResourceLocation i : EntityList.ENTITY_EGGS.keySet()) {
              values.add(i.toString());
            }
            int index = values.indexOf(value);
            if(index == -1) index = 0;
            path.add(key);
            this.buttonList.add(new SelectButton(2, this.x_end - 40, y + 1, path, values, index));
            path.remove(path.size() - 1);
          } else continue;
          break;
        case 10: // NBTTagCompound
          path.add(key);
          y = this.computeTileEntity(tag.getCompoundTag(key), y, path);
          path.remove(path.size() - 1);
          continue;
        default: continue;
      }
      GuiLabel label = new GuiLabel(this.fontRenderer, 0, this.x + 8, y, 100, 16, 0xffffffff);
      label.addLine(key);
      this.labelList.add(label);
      y += this.fontRenderer.FONT_HEIGHT + 4;
    }
    return y;
  }

  // Cycle value of given property of the current displayed state
  public void cycleProperty(IProperty<?> property) {
    this.display_state = this.display_state.cycleProperty(property);
    if(this.display_tile != null) this.display_tile.updateContainingBlockInfo();
  }

  // Set tag of currently displayed tileentity
  public void setTag(String[] path, Object value) {
    if(this.display_tile != null) {
      NBTTagCompound base = this.display_tile.getUpdateTag();
      NBTTagCompound tag = base;
      int i = 0;
      while(i < path.length - 1) {
        tag = tag.getCompoundTag(path[i]);
        i ++;
      }
      String key = path[path.length - 1];
      switch(tag.getTagId(key)) {
        case 1: tag.setByte(key, (byte)(((Integer)value).intValue() & 0xff)); break;
        case 2: tag.setShort(key, (short)((Integer)value).intValue()); break;
        case 3: tag.setInteger(key, ((Integer)value).intValue()); break;
        case 8: tag.setString(key, (String) value); break;
        default: return;
      }
      this.display_tile.readFromNBT(base);
    }
  }

  // Draw
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    // background
    Gui.drawRect(this.x, this.y, this.x_end, this.y_end, MainGui.BACKGROUND_COLOR);
    Gui.drawRect(this.x, this.y, this.x_end, this.y + 2, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x, this.y, this.x + 2, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end - 2, this.y, this.x_end, this.y_end, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x, this.y_end - 2, this.x_end, this.y_end, MainGui.BORDER_COLOR);

    // Title
    String label = null;
    if(this.current_preset == 0) label = "New preset";
    else label = "Preset " + Integer.toString(this.current_preset);
    this.drawString(this.fontRenderer, label, this.x + ((AdvancedSearchGui.guiWidth - this.fontRenderer.getStringWidth(label)) / 2), this.y + 6, 0xffeeeeee);

    this.left_arrow.enabled = (this.preset_count > 0);
    this.right_arrow.enabled = (this.preset_count > 0);

    // Current working block state
    ((SearchSelectionGui) this.parent).displayBlockFlat(this.x_end - 21, this.y + 5, this.display_state, this.display_tile);
    Gui.drawRect(this.x_end - 22, this.y + 4, this.x_end - 4, this.y + 5, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end - 22, this.y + 4, this.x_end - 21, this.y + 22, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end - 22, this.y + 21, this.x_end - 4, this.y + 22, MainGui.BORDER_COLOR);
    Gui.drawRect(this.x_end - 5, this.y + 4, this.x_end - 4, this.y + 22, MainGui.BORDER_COLOR);

    // Number of preset
    if(this.preset_count > 0) {
      label = Integer.toString(this.current_preset) + "/" + Integer.toString(this.preset_count);
      this.drawString(this.fontRenderer, label, this.x + ((AdvancedSearchGui.guiWidth - this.fontRenderer.getStringWidth(label)) / 2), this.y_end - 12, 0xffffffff);
    }

    // Draw buttons & Labels
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  // Close GUI
  public void close() {
    this.transition((RightPanel) this.parent);
  }

  /* Add, update & remove preset actions */

  private SearchModule.AdvancedSearch getAdvancedSearch() {
    SearchModule.AdvancedSearch search = new SearchModule.AdvancedSearch(this.block);
    boolean isEmpty = true;

    // Set up which states to search for
    for(GuiButton i : this.buttonList) {
      if(i instanceof SelectButton) {
        SelectButton select = (SelectButton) i;
        if(select.id == 1) {
          if(select.getIndex() != -1) {
            isEmpty = false;
            search.addProperty(select.getProperty(), this.display_state.getValue(select.getProperty()));
          }
        } else if(this.display_tile != null) {
          if(select.getIndex() != -1) {
            isEmpty = false;
            String[] path = select.getPath();
            NBTTagCompound tag = this.display_tile.getUpdateTag();
            int j = 0;
            while(j < path.length - 1) {
              tag = tag.getCompoundTag(path[j]);
              j ++;
            }
            String key = path[path.length - 1];
            switch(tag.getTagId(key)) {
              case 1:
                search.addTag(path, (byte)(select.getIndex() & 0xff));
                break;
              case 2:
                search.addTag(path, (short)select.getIndex());
                break;
              case 3:
                search.addTag(path, select.getIndex());
                break;
              case 8:
                search.addTag(path, select.getValue());
                break;
            }
          }
        }
      }
    }

    if(isEmpty) return null;

    // Set up tracer & color
    search.property.tracer = this.tracer.getState();
    search.property.color = this.color.getColor();

    return search;
  }

  public void addToSearch() {
    SearchModule.AdvancedSearch search = this.getAdvancedSearch();
    if(search == null) return;

    // Add to search
    ((SearchModule) this.dependence).addAdvancedSearch(this.block, search);

    // Reset gui
    this.reset();

    // Enable parent gui search btn
    ((SearchSelectionGui) this.parent).enableSearchBtn(this.block);
  }

  public void updateSearch() {
    if(this.current_preset > 0) {
      SearchModule.AdvancedSearch search = this.getAdvancedSearch();
      if(search == null) return;

      ((SearchModule) this.dependence).updateAdvancedSearch(this.block, this.current_preset - 1, search);
    }
  }

  public void removeFromSearch() {
    ((SearchModule) this.dependence).removeAdvancedSearch(this.block, this.current_preset - 1);
    this.preset_count -= 1;
    this.nextPreset(-1);
  }

  /* Reset gui state */

  public void reset() {
    this.color.reset();
    this.tracer.setState(false);
    for(GuiButton i : this.buttonList) {
      if(i instanceof SelectButton) {
        ((SelectButton) i).reset();
      }
    }
    this.display_state = this.block.getBlockState().getBaseState();
    if(this.display_tile != null) this.display_tile = ((SearchSelectionGui) this.parent).createTileEntity(this.display_state);
    this.preset_count = ((SearchModule) this.dependence).getAdvancedSearchListSize(this.block);
    this.current_preset = 0;
  }

  /* Switch between presets */

  public void nextPreset(int direction) {
    this.current_preset = ((this.current_preset + direction) + (this.preset_count + 1)) % (this.preset_count + 1);
    if(this.current_preset == 0) { // New preset
      this.reset();
      this.add.enabled = true;
      this.update.enabled = false;
      this.remove.enabled = false;
    } else { // Update old preset
      this.readPreset(((SearchModule) this.dependence).getAdvancedSearch(this.block, this.current_preset - 1));
      this.add.enabled = false;
      this.update.enabled = true;
      this.remove.enabled = true;
    }
  }

  private SelectButton getButtonForProperty(IProperty<?> p) {
    for(GuiButton i : this.buttonList) {
      if(i instanceof SelectButton) {
        if(p.equals(((SelectButton)i).getProperty())) return (SelectButton)i;
      }
    }
    return null;
  }

  private SelectButton getButtonForPath(List<String> path) {
    for(GuiButton i : this.buttonList) {
      if(i instanceof SelectButton) {
        String[] current = ((SelectButton)i).getPath();
        if(current != null && current.length == path.size()) {
          int k = 0;
          for(String j : path) {
            if(!j.equals(current[k])) break;
            k ++;
          }
          if(k == current.length) return (SelectButton) i;
        }
      }
    }
    return null;
  }

  private void readPresetTag(NBTTagCompound tag, List<String> path) {
    for(String key : tag.getKeySet()) {
      path.add(key);
      if(tag.getTagId(key) == 10) {
        this.readPresetTag(tag.getCompoundTag(key), path);
      } else {
        SelectButton btn = this.getButtonForPath(path);
        if(tag.getTagId(key) == 8) {
          btn.setValue(tag.getString(key));
        } else {
          btn.setValue(tag.getInteger(key));
        }
      }
      path.remove(path.size() - 1);
    }
  }

  private void readPreset(SearchModule.AdvancedSearch preset) {
    int nb_prop = this.block.getDefaultState().getPropertyKeys().size();

    // Set IProperty select buttons
    if(nb_prop > 0) {
      boolean[] isSet = new boolean[nb_prop];
      for(IBlockState state : preset.states) {
        int i = 0;
        for(IProperty<?> p : state.getPropertyKeys()) {
          SelectButton btn = this.getButtonForProperty(p);
          if(!isSet[i]) {
            btn.setValue(state.getValue(p).toString());
            isSet[i] = true;
          } else if(btn.getIndex() != -1 && !btn.getValue().equals(state.getValue(p).toString())) {
            btn.reset();
          }
          i ++;
        }
      }
    }

    // Reset all tileentity tag select buttons
    for(GuiButton i : this.buttonList) {
      if(i instanceof SelectButton && i.id == 2) ((SelectButton)i).reset();
    }

    // Set tileentity tag select buttons
    if(preset.tags != null) {
      this.readPresetTag(preset.tags, new LinkedList<String>());
    }

    // Set display state & tile
    this.display_state = this.block.getBlockState().getBaseState();
    if(this.display_tile != null) this.display_tile = ((SearchSelectionGui) this.parent).createTileEntity(this.display_state);
    for(GuiButton i : this.buttonList) {
      if(i instanceof SelectButton) {

        if(((SelectButton)i).getIndex() == -1) continue;

        if(i.id == 1) {
          for(int j = 0; j < ((SelectButton)i).getIndex(); j ++) {
            this.display_state = this.display_state.cycleProperty(((SelectButton)i).getProperty());
          }
        } else {
          if(((SelectButton)i).getValue() == null) {
            this.setTag(((SelectButton)i).getPath(), Integer.valueOf(((SelectButton)i).getIndex()));
          } else {
            this.setTag(((SelectButton)i).getPath(), ((SelectButton)i).getValue());
          }
        }
      }
    }

    // Set color & tracer
    this.color.setColor(preset.property.color);
    this.tracer.setState(preset.property.tracer);
  }
}
