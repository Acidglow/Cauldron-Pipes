package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ProtoChunk extends ChunkAccess {
    private static final Logger LOGGER = LogUtils.getLogger();
    private volatile @Nullable LevelLightEngine lightEngine;
    private volatile ChunkStatus status = ChunkStatus.EMPTY;
    private final List<CompoundTag> entities = Lists.newArrayList();
    private @Nullable CarvingMask carvingMask;
    private @Nullable BelowZeroRetrogen belowZeroRetrogen;
    private final ProtoChunkTicks<Block> blockTicks;
    private final ProtoChunkTicks<Fluid> fluidTicks;

    public ProtoChunk(
        ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, PalettedContainerFactory palettedContainerFactory, @Nullable BlendingData blendingData
    ) {
        this(chunkPos, upgradeData, null, new ProtoChunkTicks<>(), new ProtoChunkTicks<>(), levelHeightAccessor, palettedContainerFactory, blendingData);
    }

    public ProtoChunk(
        ChunkPos chunkPos,
        UpgradeData upgradeData,
        LevelChunkSection @Nullable [] sections,
        ProtoChunkTicks<Block> blockTicks,
        ProtoChunkTicks<Fluid> fluidTicks,
        LevelHeightAccessor levelHeightAccessor,
        PalettedContainerFactory palettedContainerFactory,
        @Nullable BlendingData blendingData
    ) {
        super(chunkPos, upgradeData, levelHeightAccessor, palettedContainerFactory, 0L, sections, blendingData);
        this.blockTicks = blockTicks;
        this.fluidTicks = fluidTicks;
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public ChunkAccess.PackedTicks getTicksForSerialization(long p_360967_) {
        return new ChunkAccess.PackedTicks(this.blockTicks.pack(p_360967_), this.fluidTicks.pack(p_360967_));
    }

    @Override
    public BlockState getBlockState(BlockPos p_63264_) {
        int i = p_63264_.getY();
        if (this.isOutsideBuildHeight(i)) {
            return Blocks.VOID_AIR.defaultBlockState();
        } else {
            LevelChunkSection levelchunksection = this.getSection(this.getSectionIndex(i));
            return levelchunksection.hasOnlyAir()
                ? Blocks.AIR.defaultBlockState()
                : levelchunksection.getBlockState(p_63264_.getX() & 15, i & 15, p_63264_.getZ() & 15);
        }
    }

    @Override
    public FluidState getFluidState(BlockPos p_63239_) {
        int i = p_63239_.getY();
        if (this.isOutsideBuildHeight(i)) {
            return Fluids.EMPTY.defaultFluidState();
        } else {
            LevelChunkSection levelchunksection = this.getSection(this.getSectionIndex(i));
            return levelchunksection.hasOnlyAir()
                ? Fluids.EMPTY.defaultFluidState()
                : levelchunksection.getFluidState(p_63239_.getX() & 15, i & 15, p_63239_.getZ() & 15);
        }
    }

    @Override
    public @Nullable BlockState setBlockState(BlockPos p_63217_, BlockState p_63218_, @Block.UpdateFlags int p_393955_) {
        int i = p_63217_.getX();
        int j = p_63217_.getY();
        int k = p_63217_.getZ();
        if (this.isOutsideBuildHeight(j)) {
            return Blocks.VOID_AIR.defaultBlockState();
        } else {
            int l = this.getSectionIndex(j);
            LevelChunkSection levelchunksection = this.getSection(l);
            boolean flag = levelchunksection.hasOnlyAir();
            if (flag && p_63218_.is(Blocks.AIR)) {
                return p_63218_;
            } else {
                int i1 = SectionPos.sectionRelative(i);
                int j1 = SectionPos.sectionRelative(j);
                int k1 = SectionPos.sectionRelative(k);
                BlockState blockstate = levelchunksection.setBlockState(i1, j1, k1, p_63218_);
                if (this.status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
                    boolean flag1 = levelchunksection.hasOnlyAir();
                    if (flag1 != flag) {
                        this.lightEngine.updateSectionStatus(p_63217_, flag1);
                    }

                    if (LightEngine.hasDifferentLightProperties(this, p_63217_, blockstate, p_63218_)) {
                        this.skyLightSources.update(this, i1, j, k1);
                        this.lightEngine.checkBlock(p_63217_);
                    }
                }

                EnumSet<Heightmap.Types> enumset1 = this.getPersistedStatus().heightmapsAfter();
                EnumSet<Heightmap.Types> enumset = null;

                for (Heightmap.Types heightmap$types : enumset1) {
                    Heightmap heightmap = this.heightmaps.get(heightmap$types);
                    if (heightmap == null) {
                        if (enumset == null) {
                            enumset = EnumSet.noneOf(Heightmap.Types.class);
                        }

                        enumset.add(heightmap$types);
                    }
                }

                if (enumset != null) {
                    Heightmap.primeHeightmaps(this, enumset);
                }

                for (Heightmap.Types heightmap$types1 : enumset1) {
                    this.heightmaps.get(heightmap$types1).update(i1, j, k1, p_63218_);
                }

                return blockstate;
            }
        }
    }

    @Override
    public void setBlockEntity(BlockEntity p_156488_) {
        this.pendingBlockEntities.remove(p_156488_.getBlockPos());
        this.blockEntities.put(p_156488_.getBlockPos(), p_156488_);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos p_63257_) {
        return this.blockEntities.get(p_63257_);
    }

    public Map<BlockPos, BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public void addEntity(CompoundTag tag) {
        this.entities.add(tag);
    }

    @Override
    public void addEntity(Entity p_63183_) {
        if (!p_63183_.isPassenger()) {
            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_63183_.problemPath(), LOGGER)) {
                TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_63183_.registryAccess());
                p_63183_.save(tagvalueoutput);
                this.addEntity(tagvalueoutput.buildResult());
            }
        }
    }

    @Override
    public void setStartForStructure(Structure p_223432_, StructureStart p_223433_) {
        BelowZeroRetrogen belowzeroretrogen = this.getBelowZeroRetrogen();
        if (belowzeroretrogen != null && p_223433_.isValid()) {
            BoundingBox boundingbox = p_223433_.getBoundingBox();
            LevelHeightAccessor levelheightaccessor = this.getHeightAccessorForGeneration();
            if (boundingbox.minY() < levelheightaccessor.getMinY() || boundingbox.maxY() > levelheightaccessor.getMaxY()) {
                return;
            }
        }

        super.setStartForStructure(p_223432_, p_223433_);
    }

    public List<CompoundTag> getEntities() {
        return this.entities;
    }

    @Override
    public ChunkStatus getPersistedStatus() {
        return this.status;
    }

    public void setPersistedStatus(ChunkStatus status) {
        this.status = status;
        if (this.belowZeroRetrogen != null && status.isOrAfter(this.belowZeroRetrogen.targetStatus())) {
            this.setBelowZeroRetrogen(null);
        }

        this.markUnsaved();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int p_204450_, int p_204451_, int p_204452_) {
        if (this.getHighestGeneratedStatus().isOrAfter(ChunkStatus.BIOMES)) {
            return super.getNoiseBiome(p_204450_, p_204451_, p_204452_);
        } else {
            throw new IllegalStateException("Asking for biomes before we have biomes");
        }
    }

    public static short packOffsetCoordinates(BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        int l = i & 15;
        int i1 = j & 15;
        int j1 = k & 15;
        return (short)(l | i1 << 4 | j1 << 8);
    }

    public static BlockPos unpackOffsetCoordinates(short packedPos, int yOffset, ChunkPos chunkPos) {
        int i = SectionPos.sectionToBlockCoord(chunkPos.x, packedPos & 15);
        int j = SectionPos.sectionToBlockCoord(yOffset, packedPos >>> 4 & 15);
        int k = SectionPos.sectionToBlockCoord(chunkPos.z, packedPos >>> 8 & 15);
        return new BlockPos(i, j, k);
    }

    @Override
    public void markPosForPostprocessing(BlockPos p_63266_) {
        if (!this.isOutsideBuildHeight(p_63266_)) {
            ChunkAccess.getOrCreateOffsetList(this.postProcessing, this.getSectionIndex(p_63266_.getY())).add(packOffsetCoordinates(p_63266_));
        }
    }

    @Override
    public void addPackedPostProcess(ShortList p_360490_, int p_63226_) {
        ChunkAccess.getOrCreateOffsetList(this.postProcessing, p_63226_).addAll(p_360490_);
    }

    public Map<BlockPos, CompoundTag> getBlockEntityNbts() {
        return Collections.unmodifiableMap(this.pendingBlockEntities);
    }

    @Override
    public @Nullable CompoundTag getBlockEntityNbtForSaving(BlockPos p_63275_, HolderLookup.Provider p_324473_) {
        BlockEntity blockentity = this.getBlockEntity(p_63275_);
        return blockentity != null ? blockentity.saveWithFullMetadata(p_324473_) : this.pendingBlockEntities.get(p_63275_);
    }

    @Override
    public void removeBlockEntity(BlockPos p_63262_) {
        this.blockEntities.remove(p_63262_);
        this.pendingBlockEntities.remove(p_63262_);
    }

    public @Nullable CarvingMask getCarvingMask() {
        return this.carvingMask;
    }

    public CarvingMask getOrCreateCarvingMask() {
        if (this.carvingMask == null) {
            this.carvingMask = new CarvingMask(this.getHeight(), this.getMinY());
        }

        return this.carvingMask;
    }

    public void setCarvingMask(CarvingMask carvingMask) {
        this.carvingMask = carvingMask;
    }

    public void setLightEngine(LevelLightEngine lightEngine) {
        this.lightEngine = lightEngine;
    }

    public void setBelowZeroRetrogen(@Nullable BelowZeroRetrogen belowZeroRetrogen) {
        this.belowZeroRetrogen = belowZeroRetrogen;
    }

    @Override
    public @Nullable BelowZeroRetrogen getBelowZeroRetrogen() {
        return this.belowZeroRetrogen;
    }

    private static <T> LevelChunkTicks<T> unpackTicks(ProtoChunkTicks<T> ticks) {
        return new LevelChunkTicks<>(ticks.scheduledTicks());
    }

    public LevelChunkTicks<Block> unpackBlockTicks() {
        return unpackTicks(this.blockTicks);
    }

    public LevelChunkTicks<Fluid> unpackFluidTicks() {
        return unpackTicks(this.fluidTicks);
    }

    @Override
    public LevelHeightAccessor getHeightAccessorForGeneration() {
        return (LevelHeightAccessor)(this.isUpgrading() ? BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR : this);
    }
}
