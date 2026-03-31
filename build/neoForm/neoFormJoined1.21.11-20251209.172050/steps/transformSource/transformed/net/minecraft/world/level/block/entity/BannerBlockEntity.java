package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BannerBlockEntity extends BlockEntity implements Nameable {
    public static final int MAX_PATTERNS = 6;
    private static final String TAG_PATTERNS = "patterns";
    private static final Component DEFAULT_NAME = Component.translatable("block.minecraft.banner");
    private @Nullable Component name;
    private final DyeColor baseColor;
    /**
     * A list of all patterns stored on this banner.
     */
    private BannerPatternLayers patterns = BannerPatternLayers.EMPTY;

    public BannerBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, ((AbstractBannerBlock)blockState.getBlock()).getColor());
    }

    public BannerBlockEntity(BlockPos pos, BlockState blockState, DyeColor baseColor) {
        super(BlockEntityType.BANNER, pos, blockState);
        this.baseColor = baseColor;
    }

    @Override
    public Component getName() {
        return this.name != null ? this.name : DEFAULT_NAME;
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.name;
    }

    @Override
    protected void saveAdditional(ValueOutput p_422307_) {
        super.saveAdditional(p_422307_);
        if (!this.patterns.equals(BannerPatternLayers.EMPTY)) {
            p_422307_.store("patterns", BannerPatternLayers.CODEC, this.patterns);
        }

        p_422307_.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
    }

    @Override
    protected void loadAdditional(ValueInput p_421624_) {
        super.loadAdditional(p_421624_);
        this.name = parseCustomNameSafe(p_421624_, "CustomName");
        this.patterns = p_421624_.read("patterns", BannerPatternLayers.CODEC).orElse(BannerPatternLayers.EMPTY);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_324478_) {
        return this.saveWithoutMetadata(p_324478_);
    }

    public BannerPatternLayers getPatterns() {
        return this.patterns;
    }

    public ItemStack getItem() {
        ItemStack itemstack = new ItemStack(BannerBlock.byColor(this.baseColor));
        itemstack.applyComponents(this.collectComponents());
        return itemstack;
    }

    public DyeColor getBaseColor() {
        return this.baseColor;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_397928_) {
        super.applyImplicitComponents(p_397928_);
        this.patterns = p_397928_.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        this.name = p_397928_.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_338762_) {
        super.collectImplicitComponents(p_338762_);
        p_338762_.set(DataComponents.BANNER_PATTERNS, this.patterns);
        p_338762_.set(DataComponents.CUSTOM_NAME, this.name);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput p_422302_) {
        p_422302_.discard("patterns");
        p_422302_.discard("CustomName");
    }
}
