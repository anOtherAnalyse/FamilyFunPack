package family_fun_pack.modules;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.gui.MainGuiComponent;
import family_fun_pack.gui.components.ActionButton;
import family_fun_pack.gui.components.OnOffButton;
import family_fun_pack.gui.components.OpenGuiButton;
import family_fun_pack.gui.components.SliderButton;
import family_fun_pack.gui.components.actions.NumberPumpkinAura;
import family_fun_pack.gui.components.actions.OnOffPumpkinAura;
import family_fun_pack.gui.interfaces.PumpkinAuraSettingsGui;
import family_fun_pack.network.PacketListener;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PumpkinAuraModule extends Module implements PacketListener {
    private final Minecraft mc = Minecraft.getMinecraft();
    public int placeRange;
    public int minDamage;
    public int maxDamage;
    public boolean autoSwitch;
    public boolean sequential;

    private BlockPos lastPos;

    public PumpkinAuraModule() {
        super("PumpkinAura", "Pumpkin PvP module for auscpvp.org/2b2t.org.au");
    }

    @Override
    protected void enable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void disable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        place();
    }

    private void place() {
        if (mc.world == null && mc.player == null) return;
        BlockPos pos = blockPosSupplier.get();
        if (autoSwitch) {
            int slot = -1;
            for (int i = 0; i < mc.player.inventory.mainInventory.size(); ++i)
            {
                if (!mc.player.inventory.mainInventory.get(i).isEmpty() && mc.player.inventory.mainInventory.get(i).getItem() == Item.getItemFromBlock(Blocks.PUMPKIN))
                {
                    slot = i;
                }
            }
            if (slot != -1) {
                mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
                mc.player.inventory.currentItem = slot;
                mc.playerController.updateController();
            }
        }
        if (mc.player.getHeldItemMainhand().getItem() == Item.getItemFromBlock(Blocks.PUMPKIN) && pos != null) {
            mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(
                    pos, EnumFacing.UP, EnumHand.MAIN_HAND, 0f, 0f, 0f
            ));
            lastPos = pos;
        }
    }

    @Override
    public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
        SPacketExplosion explosion = (SPacketExplosion) packet;

        if (explosion != null && sequential) {
            BlockPos pos = new BlockPos(explosion.getX(), explosion.getY(), explosion.getZ());
            if (pos.equals(this.lastPos)) {
                place();
            }
        }

        return packet;
    }

    public Supplier<BlockPos> blockPosSupplier = (() -> {
        List<EntityPlayer> players = mc.world.playerEntities.stream()
                .filter(player -> !player.equals(mc.player) && player.getDistance(mc.player) <= 12f && !(player.isDead || player.getHealth() <= 0))
                .collect(Collectors.toList());
        BlockPos placePos = null;
        float lastDamage = 0.5f;
        for (EntityPlayer player : players) {
            for (BlockPos pos : possiblePlacePositions(placeRange, true)) {
                float damage = calculateToastShitfuckerooPumpkin(pos.getX(), pos.getY() + 1, pos.getZ(), player);
                float selfDamage = calculateToastShitfuckerooPumpkin(pos.getX(), pos.getY() + 1, pos.getZ(), mc.player);
                if (damage > minDamage && damage > lastDamage && selfDamage < maxDamage) {
                    placePos = pos;
                    lastDamage = damage;
                }
            }
        }
        return placePos;
    });

    public List<BlockPos> possiblePlacePositions(final float placeRange, final boolean thirteen) {
        NonNullList<BlockPos> positions = NonNullList.create();
        positions.addAll(getSphere(getPlayerPos(mc.player), placeRange, (int) placeRange, false, true, 0).stream().filter(pos -> canPlacePumpkin(pos, thirteen)).collect(Collectors.toList()));
        return positions;
    }

    public boolean canPlacePumpkin(BlockPos blockPos, boolean oneDot15) {
        final BlockPos boost = blockPos.add(0, 1, 0);
        final BlockPos boost2 = blockPos.add(0, 2, 0);
        try {
            if (mc.world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK && mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN) {
                return false;
            }
            if ((mc.world.getBlockState(boost).getBlock() != Blocks.AIR || (mc.world.getBlockState(boost2).getBlock() != Blocks.AIR && !oneDot15))) {
                return false;
            }
            return mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost)).isEmpty() && mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost2)).isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    public List<BlockPos> getSphere(BlockPos pos, float r, int h, boolean hollow, boolean sphere, int plus_y) {
        ArrayList<BlockPos> circleblocks = new ArrayList<>();
        int cx = pos.getX();
        int cy = pos.getY();
        int cz = pos.getZ();
        int x = cx - (int) r;
        while ((float) x <= (float) cx + r) {
            int z = cz - (int) r;
            while ((float) z <= (float) cz + r) {
                int y = sphere ? cy - (int) r : cy;
                while (true) {
                    float f = y;
                    float f2 = sphere ? (float) cy + r : (float) (cy + h);
                    if (!(f < f2)) break;
                    double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
                    if (dist < (double) (r * r) && (!hollow || dist >= (double) ((r - 1.0f) * (r - 1.0f)))) {
                        BlockPos l = new BlockPos(x, y + plus_y, z);
                        circleblocks.add(l);
                    }
                    ++y;
                }
                ++z;
            }
            ++x;
        }
        return circleblocks;
    }

    public BlockPos getPlayerPos(EntityPlayer player) {
        return new BlockPos(Math.floor(player.posX), Math.floor(player.posY), Math.floor(player.posZ));
    }


    public float calculateToastShitfuckerooPumpkin(double posX, double posY, double posZ, Entity entity) {
        float doubleExplosionSize = 12.0f;
        Vec3d vec3d = new Vec3d(posX, posY, posZ);
        double blockDensity = 0.0;
        try {
            blockDensity = entity.world.getBlockDensity(vec3d, entity.getEntityBoundingBox());
        } catch (Exception exception) {
            // empty catch block
        }
        double v = (1.0 - entity.getDistance(posX, posY, posZ) / (double) doubleExplosionSize) * blockDensity;
        float damage = (int) ((v * v + v) / 2.0 * 7.0 * (double) doubleExplosionSize + 1.0);
        double final_ = 1.0;
        if (entity instanceof EntityLivingBase) {
            final_ = getBlastReduction((EntityLivingBase) entity, getDamageMultiplied(damage), new Explosion(
                    mc.world, null, posX, posY, posZ, 9f, false, true));
        }
        return (float) final_;
    }

    public float getBlastReduction(EntityLivingBase entity, float damageI, Explosion explosion) {
        float damage = damageI;
        if (entity instanceof EntityPlayer) {
            EntityPlayer ep = (EntityPlayer) entity;
            DamageSource ds = DamageSource.causeExplosionDamage(explosion);
            damage = CombatRules.getDamageAfterAbsorb(damage, (float) ep.getTotalArmorValue(), (float) ep.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
            int k = 0;
            try {
                k = EnchantmentHelper.getEnchantmentModifierDamage(ep.getArmorInventoryList(), ds);
            } catch (Exception exception) {
                // empty catch block
            }
            float f = MathHelper.clamp((float) k, 0.0f, 20.0f);
            damage *= 1.0f - f / 25.0f;
            if (entity.isPotionActive(MobEffects.RESISTANCE)) {
                damage -= damage / 4.0f;
            }
            damage = Math.max(damage, 0.0f);
            return damage;
        }
        damage = CombatRules.getDamageAfterAbsorb(damage, (float) entity.getTotalArmorValue(), (float) entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
        return damage;
    }

    public float getDamageMultiplied(float damage) {
        int diff = mc.world.getDifficulty().getDifficultyId();
        return damage * (diff == 0 ? 0.0f : (diff == 2 ? 1.0f : (diff == 1 ? 0.5f : 1.5f)));
    }
    @Override
    public void save(Configuration configuration) {
        configuration.get(this.getLabel(), "sequential", true).set(sequential);
        configuration.get(this.getLabel(), "placeRange", 5).set(placeRange);
        configuration.get(this.getLabel(), "minDamage", 6).set(minDamage);
        configuration.get(this.getLabel(), "maxDamage", 10).set(maxDamage);
        configuration.get(this.getLabel(), "autoSwitch", false).set(autoSwitch);
        super.save(configuration);
    }

    @Override
    public void load(Configuration configuration) {
        sequential = configuration.get(this.getLabel(), "sequential", true).getBoolean();
        placeRange = configuration.get(this.getLabel(), "placeRange", 5).getInt();
        minDamage = configuration.get(this.getLabel(), "minDamage", 6).getInt();
        maxDamage = configuration.get(this.getLabel(), "maxDamage", 10).getInt();
        autoSwitch = configuration.get(this.getLabel(), "autoSwitch", false).getBoolean();
        super.load(configuration);
    }

    public LinkedHashMap<String, ActionButton> getSettings() {
        load(FamilyFunPack.getModules().getConfiguration());
        LinkedHashMap<String, ActionButton> buttonMap = new LinkedHashMap<>();
        buttonMap.put("AutoSwitch", new OnOffButton(0, 0, 0, new OnOffPumpkinAura(this, 0)).setState(autoSwitch));
        buttonMap.put("Sequential", new OnOffButton(1, 0, 0, new OnOffPumpkinAura(this, 1)).setState(sequential));
        buttonMap.put("PlaceRange", new SliderButton(2, 0, 0, new NumberPumpkinAura(this, 2)).setNumber(placeRange));
        buttonMap.put("MinDamage", new SliderButton(3, 0, 0, new NumberPumpkinAura(this, 3)).setNumber(minDamage));
        buttonMap.put("MaxDamage", new SliderButton(4, 0, 0, new NumberPumpkinAura(this, 4)).setNumber(maxDamage));
        return buttonMap;
    }

    public void save() {
        save(FamilyFunPack.getModules().getConfiguration());
    }

    private static class SettingsGui implements MainGuiComponent {

        private final PumpkinAuraModule dependence;

        public SettingsGui(PumpkinAuraModule dependence) {
            this.dependence = dependence;
        }

        public String getLabel() {
            return "config issue?";
        }

        public ActionButton getAction() {
            return new OpenGuiButton(0, 0, "config", PumpkinAuraSettingsGui.class, this.dependence);
        }

        public MainGuiComponent getChild() {
            return null;
        }
    }

    @Override
    public MainGuiComponent getChild() {
        return new SettingsGui(this);
    }
}
