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
import family_fun_pack.utils.Timer;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.Explosion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PumpkinAuraModule extends Module implements PacketListener {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final HashMap<EntityPlayer, PopCounter> popMap = new HashMap<>();
    public int placeRange;
    public int minDamage;
    public int maxDamage;
    public boolean autoSwitch;
    public boolean sequential;
    public boolean antiTotem;
    public boolean ignoreTerrain;
    private BlockPos lastPos;
    private BlockPos renderPos;

    public PumpkinAuraModule() {
        super("Pumpkin Aura", "Pumpkin PvP module for auscpvp.org/2b2t.org.au");
        FamilyFunPack.addModuleKey(0, this);
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
        if (mc.world == null && mc.player == null) return;
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
        place(false);
    }

    private void place(boolean sequential) {
        if (mc.world == null && mc.player == null) return;
        BlockPos pos = blockPosSupplier.get();
        handlePlacing(pos, sequential);
    }

    private void handlePlacing(BlockPos pos, boolean sequential) {
        final EnumHand hand = mc.player.getHeldItemMainhand().getItem() == Item.getItemFromBlock(Blocks.PUMPKIN) ? EnumHand.MAIN_HAND
                : mc.player.getHeldItemOffhand().getItem() == Item.getItemFromBlock(Blocks.PUMPKIN) ? EnumHand.OFF_HAND : null;
        if (hand != null && pos != null) {
            mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(
                    pos, EnumFacing.UP, hand, 0f, 0f, 0f
            ));

            if (!sequential) {
                lastPos = pos;
            }
        }
    }

    public boolean isDoublePoppable(EntityPlayer player, float damage) {
        if (antiTotem && this.popMap.get(player) != null) {
            final PopCounter popCounter = this.popMap.get(player);
            final float playerHealth = player.getHealth() + player.getAbsorptionAmount();
            return popCounter.timer.passed(500L) && playerHealth - damage <= 0.0F;
        }

        return true;
    }

    @Override
    public Packet<?> packetReceived(EnumPacketDirection direction, int id, Packet<?> packet, ByteBuf in) {
        if (mc.world == null) return packet;

        if (packet instanceof SPacketExplosion && sequential) {
            SPacketExplosion explosion = (SPacketExplosion) packet;
            BlockPos pos = new BlockPos(explosion.getX(), explosion.getY(), explosion.getZ());
            if (pos.equals(this.lastPos)) {
                place(true);
            }
        }

        if (packet instanceof SPacketEntityStatus) {
            SPacketEntityStatus totemPop = (SPacketEntityStatus) packet;
            Entity entity = totemPop.getEntity(mc.world);
            if (entity instanceof EntityPlayer) {
                this.popMap.computeIfAbsent((EntityPlayer) entity, v -> new PopCounter()).pop();
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
                if (damage > minDamage && damage > lastDamage && selfDamage < maxDamage && isDoublePoppable(player, damage)) {
                    placePos = pos;
                    lastDamage = damage;
                }
            }
        }
        renderPos = placePos;
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
        float doubleExplosionSize = 8.0f;
        Vec3d vec3d = new Vec3d(posX, posY, posZ);
        double blockDensity = 0.0;
        try {
            blockDensity = this.getBlockDensity(vec3d, entity.getEntityBoundingBox(), ignoreTerrain);
        } catch (Exception exception) {
            // empty catch block
        }
        double v = (1.0 - entity.getDistance(posX, posY, posZ) / (double) doubleExplosionSize) * blockDensity;
        float damage = (int) ((v * v + v) / 2.0 * 7.0 * (double) doubleExplosionSize + 1.0);
        double final_ = 1.0;
        if (entity instanceof EntityLivingBase) {
            final_ = getBlastReduction((EntityLivingBase) entity, getDamageMultiplied(damage), new Explosion(
                    mc.world, null, posX, posY, posZ, 8f, true, true));
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

    public float getBlockDensity(Vec3d vec, AxisAlignedBB bb, boolean terrain)
    {
        double d0 = 1.0D / ((bb.maxX - bb.minX) * 2.0D + 1.0D);
        double d1 = 1.0D / ((bb.maxY - bb.minY) * 2.0D + 1.0D);
        double d2 = 1.0D / ((bb.maxZ - bb.minZ) * 2.0D + 1.0D);
        double d3 = (1.0D - Math.floor(1.0D / d0) * d0) / 2.0D;
        double d4 = (1.0D - Math.floor(1.0D / d2) * d2) / 2.0D;

        if (d0 >= 0.0D && d1 >= 0.0D && d2 >= 0.0D)
        {
            int j2 = 0;
            int k2 = 0;

            for (float f = 0.0F; f <= 1.0F; f = (float)((double)f + d0))
            {
                for (float f1 = 0.0F; f1 <= 1.0F; f1 = (float)((double)f1 + d1))
                {
                    for (float f2 = 0.0F; f2 <= 1.0F; f2 = (float)((double)f2 + d2))
                    {
                        double d5 = bb.minX + (bb.maxX - bb.minX) * (double)f;
                        double d6 = bb.minY + (bb.maxY - bb.minY) * (double)f1;
                        double d7 = bb.minZ + (bb.maxZ - bb.minZ) * (double)f2;


                        if (this.rayTraceBlocks(new Vec3d(d5 + d3, d6, d7 + d4), vec, terrain) == null)
                        {
                            ++j2;
                        }

                        ++k2;
                    }
                }
            }

            return (float) j2 / (float)k2;
        }
        else
        {
            return 0.0F;
        }
    }

    @Nullable
    public RayTraceResult rayTraceBlocks(Vec3d start, Vec3d end, boolean terrain)
    {
        return this.rayTraceBlocks(start, end, false, false, false,
                (b, p) -> !(terrain && b.getExplosionResistance(mc.player) < 100 && p.distanceSq(end.x, end.y, end.z) <= 64.0));
    }

    @Nullable
    public RayTraceResult rayTraceBlocks(Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, BiPredicate<Block, BlockPos> blockChecker)
    {
        if (!Double.isNaN(vec31.x) && !Double.isNaN(vec31.y) && !Double.isNaN(vec31.z))
        {
            if (!Double.isNaN(vec32.x) && !Double.isNaN(vec32.y) && !Double.isNaN(vec32.z))
            {
                int i = MathHelper.floor(vec32.x);
                int j = MathHelper.floor(vec32.y);
                int k = MathHelper.floor(vec32.z);
                int l = MathHelper.floor(vec31.x);
                int i1 = MathHelper.floor(vec31.y);
                int j1 = MathHelper.floor(vec31.z);
                BlockPos blockpos = new BlockPos(l, i1, j1);
                IBlockState iblockstate = mc.world.getBlockState(blockpos);
                Block block = iblockstate.getBlock();

                if ((!ignoreBlockWithoutBoundingBox || iblockstate.getCollisionBoundingBox(mc.world, blockpos) != Block.NULL_AABB)
                        && block.canCollideCheck(iblockstate, stopOnLiquid) && (blockChecker == null || blockChecker.test(block, blockpos)))
                {
                    RayTraceResult raytraceresult = iblockstate.collisionRayTrace(mc.world, blockpos, vec31, vec32);

                    if (raytraceresult != null)
                    {
                        return raytraceresult;
                    }
                }

                RayTraceResult raytraceresult2 = null;
                int k1 = 200;

                while (k1-- >= 0)
                {
                    if (Double.isNaN(vec31.x) || Double.isNaN(vec31.y) || Double.isNaN(vec31.z))
                    {
                        return null;
                    }

                    if (l == i && i1 == j && j1 == k)
                    {
                        return returnLastUncollidableBlock ? raytraceresult2 : null;
                    }

                    boolean flag2 = true;
                    boolean flag = true;
                    boolean flag1 = true;
                    double d0 = 999.0D;
                    double d1 = 999.0D;
                    double d2 = 999.0D;

                    if (i > l)
                    {
                        d0 = (double)l + 1.0D;
                    }
                    else if (i < l)
                    {
                        d0 = (double)l + 0.0D;
                    }
                    else
                    {
                        flag2 = false;
                    }

                    if (j > i1)
                    {
                        d1 = (double)i1 + 1.0D;
                    }
                    else if (j < i1)
                    {
                        d1 = (double)i1 + 0.0D;
                    }
                    else
                    {
                        flag = false;
                    }

                    if (k > j1)
                    {
                        d2 = (double)j1 + 1.0D;
                    }
                    else if (k < j1)
                    {
                        d2 = (double)j1 + 0.0D;
                    }
                    else
                    {
                        flag1 = false;
                    }

                    double d3 = 999.0D;
                    double d4 = 999.0D;
                    double d5 = 999.0D;
                    double d6 = vec32.x - vec31.x;
                    double d7 = vec32.y - vec31.y;
                    double d8 = vec32.z - vec31.z;

                    if (flag2)
                    {
                        d3 = (d0 - vec31.x) / d6;
                    }

                    if (flag)
                    {
                        d4 = (d1 - vec31.y) / d7;
                    }

                    if (flag1)
                    {
                        d5 = (d2 - vec31.z) / d8;
                    }

                    if (d3 == -0.0D)
                    {
                        d3 = -1.0E-4D;
                    }

                    if (d4 == -0.0D)
                    {
                        d4 = -1.0E-4D;
                    }

                    if (d5 == -0.0D)
                    {
                        d5 = -1.0E-4D;
                    }

                    EnumFacing enumfacing;

                    if (d3 < d4 && d3 < d5)
                    {
                        enumfacing = i > l ? EnumFacing.WEST : EnumFacing.EAST;
                        vec31 = new Vec3d(d0, vec31.y + d7 * d3, vec31.z + d8 * d3);
                    }
                    else if (d4 < d5)
                    {
                        enumfacing = j > i1 ? EnumFacing.DOWN : EnumFacing.UP;
                        vec31 = new Vec3d(vec31.x + d6 * d4, d1, vec31.z + d8 * d4);
                    }
                    else
                    {
                        enumfacing = k > j1 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                        vec31 = new Vec3d(vec31.x + d6 * d5, vec31.y + d7 * d5, d2);
                    }

                    l = MathHelper.floor(vec31.x) - (enumfacing == EnumFacing.EAST ? 1 : 0);
                    i1 = MathHelper.floor(vec31.y) - (enumfacing == EnumFacing.UP ? 1 : 0);
                    j1 = MathHelper.floor(vec31.z) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
                    blockpos = new BlockPos(l, i1, j1);
                    IBlockState iblockstate1 = mc.world.getBlockState(blockpos);
                    Block block1 = iblockstate1.getBlock();

                    if (!ignoreBlockWithoutBoundingBox || iblockstate1.getMaterial() == Material.PORTAL || iblockstate1.getCollisionBoundingBox(mc.world, blockpos) != Block.NULL_AABB)
                    {
                        if (block1.canCollideCheck(iblockstate1, stopOnLiquid) && (blockChecker == null || blockChecker.test(block1, blockpos)))
                        {
                            RayTraceResult raytraceresult1 = iblockstate1.collisionRayTrace(mc.world, blockpos, vec31, vec32);

                            if (raytraceresult1 != null)
                            {
                                return raytraceresult1;
                            }
                        }
                        else
                        {
                            raytraceresult2 = new RayTraceResult(RayTraceResult.Type.MISS, vec31, enumfacing, blockpos);
                        }
                    }
                }

                return returnLastUncollidableBlock ? raytraceresult2 : null;
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    public void save(Configuration configuration) {
        configuration.get(this.getLabel(), "ignoreTerrain", true).set(ignoreTerrain);
        configuration.get(this.getLabel(), "antiTotem", true).set(antiTotem);
        configuration.get(this.getLabel(), "autoSwitch", false).set(autoSwitch);
        configuration.get(this.getLabel(), "sequential", true).set(sequential);
        configuration.get(this.getLabel(), "placeRange", 5).set(placeRange);
        configuration.get(this.getLabel(), "minDamage", 6).set(minDamage);
        configuration.get(this.getLabel(), "maxDamage", 10).set(maxDamage);
        super.save(configuration);
    }

    @Override
    public void load(Configuration configuration) {
        ignoreTerrain = configuration.get(this.getLabel(), "ignoreTerrain", true).getBoolean();
        antiTotem = configuration.get(this.getLabel(), "antiTotem", true).getBoolean();
        autoSwitch = configuration.get(this.getLabel(), "autoSwitch", false).getBoolean();
        sequential = configuration.get(this.getLabel(), "sequential", true).getBoolean();
        placeRange = configuration.get(this.getLabel(), "placeRange", 5).getInt();
        minDamage = configuration.get(this.getLabel(), "minDamage", 6).getInt();
        maxDamage = configuration.get(this.getLabel(), "maxDamage", 10).getInt();
        super.load(configuration);
    }

    public LinkedHashMap<String, ActionButton> getSettings() {
        load(FamilyFunPack.getModules().getConfiguration());
        LinkedHashMap<String, ActionButton> buttonMap = new LinkedHashMap<>();
        buttonMap.put("IgnoreTerrain", new OnOffButton(-2, 0, 0, new OnOffPumpkinAura(this, -2)).setState(ignoreTerrain));
        buttonMap.put("AntiTotem", new OnOffButton(-1, 0, 0, new OnOffPumpkinAura(this, -1)).setState(antiTotem));
        buttonMap.put("AutoSwitch", new OnOffButton(0, 0, 0, new OnOffPumpkinAura(this, 0)).setState(autoSwitch));
        buttonMap.put("Sequential", new OnOffButton(1, 0, 0, new OnOffPumpkinAura(this, 1)).setState(sequential));
        buttonMap.put("PlaceRange", new SliderButton(2, 0, 0, new NumberPumpkinAura(this, 2)).setValue(placeRange).setMin(1).setMax(6));
        buttonMap.put("MinDamage", new SliderButton(4, 0, 0, new NumberPumpkinAura(this, 4)).setValue(minDamage).setMin(0).setMax(36));
        buttonMap.put("MaxDamage", new SliderButton(5, 0, 0, new NumberPumpkinAura(this, 5)).setValue(maxDamage).setMin(0).setMax(36));
        return buttonMap;
    }

    public void save() {
        save(FamilyFunPack.getModules().getConfiguration());
    }

    private static class PopCounter {
        private final Timer timer = new Timer();
        private int pops;

        public int getPops() {
            return pops;
        }

        public void pop() {
            timer.reset();
            pops++;
        }

        public void reset() {
            pops = 0;
        }

        public long lastPop() {
            return timer.getTime();
        }
    }

    private static class SettingsGui implements MainGuiComponent {

        private final PumpkinAuraModule dependence;

        public SettingsGui(PumpkinAuraModule dependence) {
            this.dependence = dependence;
        }

        public String getLabel() {
            return "config issue ?";
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
