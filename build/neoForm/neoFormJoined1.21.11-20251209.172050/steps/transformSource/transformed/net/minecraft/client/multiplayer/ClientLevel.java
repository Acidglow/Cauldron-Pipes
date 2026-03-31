package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.EndFlashState;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.sounds.DirectionalSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientLevel extends Level implements CacheSlot.Cleaner<ClientLevel> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Component DEFAULT_QUIT_MESSAGE = Component.translatable("multiplayer.status.quitting");
    private static final double FLUID_PARTICLE_SPAWN_OFFSET = 0.05;
    private static final int NORMAL_LIGHT_UPDATES_PER_FRAME = 10;
    private static final int LIGHT_UPDATE_QUEUE_SIZE_THRESHOLD = 1000;
    final EntityTickList tickingEntities = new EntityTickList();
    private final TransientEntitySectionManager<Entity> entityStorage = new TransientEntitySectionManager<>(Entity.class, new ClientLevel.EntityCallbacks());
    private final ClientPacketListener connection;
    private final LevelRenderer levelRenderer;
    private final LevelEventHandler levelEventHandler;
    private final ClientLevel.ClientLevelData clientLevelData;
    private final TickRateManager tickRateManager;
    private final @Nullable EndFlashState endFlashState;
    private final Minecraft minecraft = Minecraft.getInstance();
    final List<AbstractClientPlayer> players = Lists.newArrayList();
    final List<net.neoforged.neoforge.entity.PartEntity<?>> dragonParts = Lists.newArrayList();
    private final Map<MapId, MapItemSavedData> mapData = Maps.newHashMap();
    private int skyFlashTime;
    private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches = Util.make(
        new Object2ObjectArrayMap<>(3),
        p_406186_ -> {
            p_406186_.put(
                BiomeColors.GRASS_COLOR_RESOLVER, new BlockTintCache(p_194181_ -> this.calculateBlockTint(p_194181_, BiomeColors.GRASS_COLOR_RESOLVER))
            );
            p_406186_.put(
                BiomeColors.FOLIAGE_COLOR_RESOLVER, new BlockTintCache(p_194177_ -> this.calculateBlockTint(p_194177_, BiomeColors.FOLIAGE_COLOR_RESOLVER))
            );
            p_406186_.put(
                BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER,
                new BlockTintCache(p_406185_ -> this.calculateBlockTint(p_406185_, BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER))
            );
            p_406186_.put(
                BiomeColors.WATER_COLOR_RESOLVER, new BlockTintCache(p_194168_ -> this.calculateBlockTint(p_194168_, BiomeColors.WATER_COLOR_RESOLVER))
            );
            net.neoforged.neoforge.client.ColorResolverManager.registerBlockTintCaches(ClientLevel.this, p_406186_);
        }
    );
    private final ClientChunkCache chunkSource;
    private final Deque<Runnable> lightUpdateQueue = Queues.newArrayDeque();
    private int serverSimulationDistance;
    private final BlockStatePredictionHandler blockStatePredictionHandler = new BlockStatePredictionHandler();
    private final Set<BlockEntity> globallyRenderedBlockEntities = new ReferenceOpenHashSet<>();
    private final ClientExplosionTracker explosionTracker = new ClientExplosionTracker();
    private final WorldBorder worldBorder = new WorldBorder();
    private final EnvironmentAttributeSystem environmentAttributes;
    private final int seaLevel;
    private boolean tickDayTime;
    private static final Set<Item> MARKER_PARTICLE_ITEMS = Set.of(Items.BARRIER, Items.LIGHT);
    private final net.neoforged.neoforge.model.data.ModelDataManager modelDataManager = new net.neoforged.neoforge.model.data.ModelDataManager(this);

    public void handleBlockChangedAck(int sequence) {
        if (SharedConstants.DEBUG_BLOCK_BREAK) {
            LOGGER.debug("ACK {}", sequence);
        }

        this.blockStatePredictionHandler.endPredictionsUpTo(sequence, this);
    }

    @Override
    public void onBlockEntityAdded(BlockEntity p_427306_) {
        BlockEntityRenderer<BlockEntity, ?> blockentityrenderer = this.minecraft.getBlockEntityRenderDispatcher().getRenderer(p_427306_);
        if (blockentityrenderer != null && blockentityrenderer.shouldRenderOffScreen()) {
            this.globallyRenderedBlockEntities.add(p_427306_);
        }
    }

    public Set<BlockEntity> getGloballyRenderedBlockEntities() {
        return this.globallyRenderedBlockEntities;
    }

    public void setServerVerifiedBlockState(BlockPos pos, BlockState state, @Block.UpdateFlags int flags) {
        if (!this.blockStatePredictionHandler.updateKnownServerState(pos, state)) {
            super.setBlock(pos, state, flags, 512);
        }
    }

    public void syncBlockState(BlockPos pos, BlockState state, Vec3 playerPos) {
        BlockState blockstate = this.getBlockState(pos);
        if (blockstate != state) {
            this.setBlock(pos, state, 19);
            Player player = this.minecraft.player;
            if (this == player.level() && player.isColliding(pos, state)) {
                player.absSnapTo(playerPos.x, playerPos.y, playerPos.z);
            }
        }
    }

    BlockStatePredictionHandler getBlockStatePredictionHandler() {
        return this.blockStatePredictionHandler;
    }

    @Override
    public boolean setBlock(BlockPos p_233643_, BlockState p_233644_, @Block.UpdateFlags int p_233645_, int p_233646_) {
        if (this.blockStatePredictionHandler.isPredicting()) {
            // Neo: Record and store a snapshot in the prediction so that BE data can be restored if the break is denied.
            // Fixes MC-36093 and permits correct server-side only cancellation of block changes.
            var snapshot = net.neoforged.neoforge.common.util.BlockSnapshot.create(this.dimension(), this, p_233643_, p_233645_);

            BlockState blockstate = this.getBlockState(p_233643_);
            boolean flag = super.setBlock(p_233643_, p_233644_, p_233645_, p_233646_);
            if (flag) {
                this.blockStatePredictionHandler.retainKnownServerState(p_233643_, blockstate, this.minecraft.player);
                this.blockStatePredictionHandler.retainSnapshot(p_233643_, snapshot);
            }

            return flag;
        } else {
            return super.setBlock(p_233643_, p_233644_, p_233645_, p_233646_);
        }
    }

    public ClientLevel(
        ClientPacketListener connection,
        ClientLevel.ClientLevelData levelData,
        ResourceKey<Level> dimension,
        Holder<DimensionType> dimensionTypeRegistration,
        int viewDistance,
        int serverSimulationDistance,
        LevelRenderer levelRenderer,
        boolean isDebug,
        long biomeZoomSeed,
        int seaLevel
    ) {
        super(levelData, dimension, connection.registryAccess(), dimensionTypeRegistration, true, isDebug, biomeZoomSeed, 1000000);
        this.connection = connection;
        this.chunkSource = new ClientChunkCache(this, viewDistance);
        this.tickRateManager = new TickRateManager();
        this.clientLevelData = levelData;
        this.levelRenderer = levelRenderer;
        this.seaLevel = seaLevel;
        this.levelEventHandler = new LevelEventHandler(this.minecraft, this);
        this.endFlashState = dimensionTypeRegistration.value().hasEndFlashes() ? new EndFlashState() : null;
        this.setRespawnData(LevelData.RespawnData.of(dimension, new BlockPos(8, 64, 8), 0.0F, 0.0F));
        this.serverSimulationDistance = serverSimulationDistance;
        this.environmentAttributes = this.addEnvironmentAttributeLayers(EnvironmentAttributeSystem.builder()).build();
        this.updateSkyBrightness();
        if (this.canHaveWeather()) {
            this.prepareWeather();
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.LevelEvent.Load(this));
    }

    private EnvironmentAttributeSystem.Builder addEnvironmentAttributeLayers(EnvironmentAttributeSystem.Builder builder) {
        builder.addDefaultLayers(this);
        int i = ARGB.color(204, 204, 255);
        builder.addTimeBasedLayer(
            EnvironmentAttributes.SKY_COLOR, (p_465550_, p_465551_) -> this.getSkyFlashTime() > 0 ? ARGB.srgbLerp(0.22F, p_465550_, i) : p_465550_
        );
        builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_FACTOR, (p_465547_, p_465548_) -> this.getSkyFlashTime() > 0 ? 1.0F : p_465547_);
        return builder;
    }

    public void queueLightUpdate(Runnable task) {
        this.lightUpdateQueue.add(task);
    }

    public void pollLightUpdates() {
        int i = this.lightUpdateQueue.size();
        int j = i < 1000 ? Math.max(10, i / 10) : i;

        for (int k = 0; k < j; k++) {
            Runnable runnable = this.lightUpdateQueue.poll();
            if (runnable == null) {
                break;
            }

            runnable.run();
        }
    }

    public @Nullable EndFlashState endFlashState() {
        return this.endFlashState;
    }

    /**
     * Runs a single tick for the world
     */
    public void tick(BooleanSupplier hasTimeLeft) {
        this.updateSkyBrightness();
        if (this.tickRateManager().runsNormally()) {
            this.getWorldBorder().tick();
            this.tickTime();
        }

        if (this.skyFlashTime > 0) {
            this.setSkyFlashTime(this.skyFlashTime - 1);
        }

        if (this.endFlashState != null) {
            this.endFlashState.tick(this.getGameTime());
            if (this.endFlashState.flashStartedThisTick() && !(this.minecraft.screen instanceof WinScreen)) {
                this.minecraft
                    .getSoundManager()
                    .playDelayed(
                        new DirectionalSoundInstance(
                            SoundEvents.WEATHER_END_FLASH,
                            SoundSource.WEATHER,
                            this.random,
                            this.minecraft.gameRenderer.getMainCamera(),
                            this.endFlashState.getXAngle(),
                            this.endFlashState.getYAngle()
                        ),
                        30
                    );
            }
        }

        this.explosionTracker.tick(this);

        try (Zone zone = Profiler.get().zone("blocks")) {
            this.chunkSource.tick(hasTimeLeft, true);
        }

        JvmProfiler.INSTANCE.onClientTick(this.minecraft.getFps());
        this.environmentAttributes().invalidateTickCache();
    }

    private void tickTime() {
        this.clientLevelData.setGameTime(this.clientLevelData.getGameTime() + 1L);
        if (this.tickDayTime) {
            this.clientLevelData.setDayTime(this.clientLevelData.getDayTime() + advanceDaytime());
        }
    }

    public void setTimeFromServer(long gameTime, long dayTime, boolean tickDayTime) {
        this.clientLevelData.setGameTime(gameTime);
        this.clientLevelData.setDayTime(dayTime);
        this.tickDayTime = tickDayTime;
    }

    public Iterable<Entity> entitiesForRendering() {
        return this.getEntities().getAll();
    }

    public void tickEntities() {
        this.tickingEntities.forEach(p_308457_ -> {
            if (!p_308457_.isRemoved() && !p_308457_.isPassenger() && !this.tickRateManager.isEntityFrozen(p_308457_)) {
                this.guardEntityTick(this::tickNonPassenger, p_308457_);
            }
        });
    }

    public boolean isTickingEntity(Entity entity) {
        return this.tickingEntities.contains(entity);
    }

    @Override
    public boolean shouldTickDeath(Entity p_194185_) {
        return p_194185_.chunkPosition().getChessboardDistance(this.minecraft.player.chunkPosition()) <= this.serverSimulationDistance;
    }

    public void tickNonPassenger(Entity p_entity) {
        p_entity.setOldPosAndRot();
        p_entity.tickCount++;
        Profiler.get().push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(p_entity.getType()).toString());
        // Neo: Permit cancellation of Entity#tick via EntityTickEvent.Pre
        if (!net.neoforged.neoforge.event.EventHooks.fireEntityTickPre(p_entity).isCanceled()) {
            p_entity.tick();
            net.neoforged.neoforge.event.EventHooks.fireEntityTickPost(p_entity);
        }
        Profiler.get().pop();

        for (Entity entity : p_entity.getPassengers()) {
            this.tickPassenger(p_entity, entity);
        }
    }

    private void tickPassenger(Entity mount, Entity rider) {
        if (rider.isRemoved() || rider.getVehicle() != mount) {
            rider.stopRiding();
        } else if (rider instanceof Player || this.tickingEntities.contains(rider)) {
            rider.setOldPosAndRot();
            rider.tickCount++;
            rider.rideTick();

            for (Entity entity : rider.getPassengers()) {
                this.tickPassenger(rider, entity);
            }
        }
    }

    public void unload(LevelChunk chunk) {
        chunk.clearAllBlockEntities();
        this.chunkSource.getLightEngine().setLightEnabled(chunk.getPos(), false);
        this.entityStorage.stopTicking(chunk.getPos());
    }

    public void onChunkLoaded(ChunkPos chunkPos) {
        this.tintCaches.forEach((p_194154_, p_194155_) -> p_194155_.invalidateForChunk(chunkPos.x, chunkPos.z));
        this.entityStorage.startTicking(chunkPos);
    }

    public void onSectionBecomingNonEmpty(long sectionPos) {
        this.levelRenderer.onSectionBecomingNonEmpty(sectionPos);
    }

    public void clearTintCaches() {
        this.tintCaches.forEach((p_194157_, p_194158_) -> p_194158_.invalidateAll());
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        return true;
    }

    public int getEntityCount() {
        return this.entityStorage.count();
    }

    public void addEntity(Entity entity) {
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(entity, this)).isCanceled()) return;
        this.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        this.entityStorage.addEntity(entity);
        entity.onAddedToLevel();
    }

    public void removeEntity(int entityId, Entity.RemovalReason reason) {
        Entity entity = this.getEntities().get(entityId);
        if (entity != null) {
            entity.setRemoved(reason);
            entity.onClientRemoval();
        }
    }

    @Override
    public List<Entity> getPushableEntities(Entity p_393670_, AABB p_394410_) {
        LocalPlayer localplayer = this.minecraft.player;
        return localplayer != null
                && localplayer != p_393670_
                && localplayer.getBoundingBox().intersects(p_394410_)
                && EntitySelector.pushableBy(p_393670_).test(localplayer)
            ? List.of(localplayer)
            : List.of();
    }

    /**
     * Returns the Entity with the given ID, or null if it doesn't exist in this World.
     */
    @Override
    public @Nullable Entity getEntity(int id) {
        return this.getEntities().get(id);
    }

    public void disconnect(Component reason) {
        this.connection.getConnection().disconnect(reason);
    }

    public void animateTick(int posX, int posY, int posZ) {
        int i = 32;
        RandomSource randomsource = RandomSource.create();
        Block block = this.getMarkerParticleTarget();
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int j = 0; j < 667; j++) {
            this.doAnimateTick(posX, posY, posZ, 16, randomsource, block, blockpos$mutableblockpos);
            this.doAnimateTick(posX, posY, posZ, 32, randomsource, block, blockpos$mutableblockpos);
        }
    }

    private @Nullable Block getMarkerParticleTarget() {
        if (this.minecraft.gameMode.getPlayerMode() == GameType.CREATIVE) {
            ItemStack itemstack = this.minecraft.player.getMainHandItem();
            Item item = itemstack.getItem();
            if (MARKER_PARTICLE_ITEMS.contains(item) && item instanceof BlockItem blockitem) {
                return blockitem.getBlock();
            }
        }

        return null;
    }

    public void doAnimateTick(
        int posX, int posY, int posZ, int range, RandomSource random, @Nullable Block block, BlockPos.MutableBlockPos blockPos
    ) {
        int i = posX + this.random.nextInt(range) - this.random.nextInt(range);
        int j = posY + this.random.nextInt(range) - this.random.nextInt(range);
        int k = posZ + this.random.nextInt(range) - this.random.nextInt(range);
        blockPos.set(i, j, k);
        BlockState blockstate = this.getBlockState(blockPos);
        blockstate.getBlock().animateTick(blockstate, this, blockPos, random);
        FluidState fluidstate = this.getFluidState(blockPos);
        if (!fluidstate.isEmpty()) {
            fluidstate.animateTick(this, blockPos, random);
            ParticleOptions particleoptions = fluidstate.getDripParticle();
            if (particleoptions != null && this.random.nextInt(10) == 0) {
                boolean flag = blockstate.isFaceSturdy(this, blockPos, Direction.DOWN);
                BlockPos blockpos = blockPos.below();
                this.trySpawnDripParticles(blockpos, this.getBlockState(blockpos), particleoptions, flag);
            }
        }

        if (block == blockstate.getBlock()) {
            this.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, blockstate), i + 0.5, j + 0.5, k + 0.5, 0.0, 0.0, 0.0);
        }

        if (!blockstate.isCollisionShapeFullBlock(this, blockPos)) {
            for (AmbientParticle ambientparticle : this.environmentAttributes().getValue(EnvironmentAttributes.AMBIENT_PARTICLES, blockPos)) {
                if (ambientparticle.canSpawn(this.random)) {
                    this.addParticle(
                        ambientparticle.particle(),
                        blockPos.getX() + this.random.nextDouble(),
                        blockPos.getY() + this.random.nextDouble(),
                        blockPos.getZ() + this.random.nextDouble(),
                        0.0,
                        0.0,
                        0.0
                    );
                }
            }
        }
    }

    private void trySpawnDripParticles(BlockPos blockPos, BlockState blockState, ParticleOptions particleData, boolean shapeDownSolid) {
        if (blockState.getFluidState().isEmpty()) {
            VoxelShape voxelshape = blockState.getCollisionShape(this, blockPos);
            double d0 = voxelshape.max(Direction.Axis.Y);
            if (d0 < 1.0) {
                if (shapeDownSolid) {
                    this.spawnFluidParticle(
                        blockPos.getX(), blockPos.getX() + 1, blockPos.getZ(), blockPos.getZ() + 1, blockPos.getY() + 1 - 0.05, particleData
                    );
                }
            } else if (!blockState.is(BlockTags.IMPERMEABLE)) {
                double d1 = voxelshape.min(Direction.Axis.Y);
                if (d1 > 0.0) {
                    this.spawnParticle(blockPos, particleData, voxelshape, blockPos.getY() + d1 - 0.05);
                } else {
                    BlockPos blockpos = blockPos.below();
                    BlockState blockstate = this.getBlockState(blockpos);
                    VoxelShape voxelshape1 = blockstate.getCollisionShape(this, blockpos);
                    double d2 = voxelshape1.max(Direction.Axis.Y);
                    if (d2 < 1.0 && blockstate.getFluidState().isEmpty()) {
                        this.spawnParticle(blockPos, particleData, voxelshape, blockPos.getY() - 0.05);
                    }
                }
            }
        }
    }

    private void spawnParticle(BlockPos pos, ParticleOptions particleData, VoxelShape voxelShape, double y) {
        this.spawnFluidParticle(
            pos.getX() + voxelShape.min(Direction.Axis.X),
            pos.getX() + voxelShape.max(Direction.Axis.X),
            pos.getZ() + voxelShape.min(Direction.Axis.Z),
            pos.getZ() + voxelShape.max(Direction.Axis.Z),
            y,
            particleData
        );
    }

    private void spawnFluidParticle(double xStart, double xEnd, double zStart, double zEnd, double y, ParticleOptions particleData) {
        this.addParticle(
            particleData,
            Mth.lerp(this.random.nextDouble(), xStart, xEnd),
            y,
            Mth.lerp(this.random.nextDouble(), zStart, zEnd),
            0.0,
            0.0,
            0.0
        );
    }

    /**
     * Adds some basic stats of the world to the given crash report.
     */
    @Override
    public CrashReportCategory fillReportDetails(CrashReport report) {
        CrashReportCategory crashreportcategory = super.fillReportDetails(report);
        crashreportcategory.setDetail("Server brand", () -> this.minecraft.player.connection.serverBrand());
        crashreportcategory.setDetail(
            "Server type", () -> this.minecraft.getSingleplayerServer() == null ? "Non-integrated multiplayer server" : "Integrated singleplayer server"
        );
        crashreportcategory.setDetail("Tracked entity count", () -> String.valueOf(this.getEntityCount()));
        return crashreportcategory;
    }

    @Override
    public void playSeededSound(
        @Nullable Entity p_393665_,
        double p_263372_,
        double p_263404_,
        double p_263365_,
        Holder<SoundEvent> p_263335_,
        SoundSource p_263417_,
        float p_263416_,
        float p_263349_,
        long p_263408_
    ) {
        net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition event = net.neoforged.neoforge.event.EventHooks.onPlaySoundAtPosition(this, p_263372_, p_263404_, p_263365_, p_263335_, p_263417_, p_263416_, p_263349_);
        if (event.isCanceled() || event.getSound() == null) return;
        p_263335_ = event.getSound();
        p_263417_ = event.getSource();
        p_263416_ = event.getNewVolume();
        p_263349_ = event.getNewPitch();

        if (p_393665_ == this.minecraft.player) {
            this.playSound(p_263372_, p_263404_, p_263365_, p_263335_.value(), p_263417_, p_263416_, p_263349_, false, p_263408_);
        }
    }

    @Override
    public void playSeededSound(
        @Nullable Entity p_263536_, Entity p_394209_, Holder<SoundEvent> p_263518_, SoundSource p_263487_, float p_263538_, float p_263524_, long p_263509_
    ) {
        net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity event = net.neoforged.neoforge.event.EventHooks.onPlaySoundAtEntity(p_394209_, p_263518_, p_263487_, p_263538_, p_263524_);
        if (event.isCanceled() || event.getSound() == null) return;
        p_263518_ = event.getSound();
        p_263487_ = event.getSource();
        p_263538_ = event.getNewVolume();
        p_263524_ = event.getNewPitch();
        if (p_263536_ == this.minecraft.player) {
            this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(p_263518_.value(), p_263487_, p_263538_, p_263524_, p_394209_, p_263509_));
        }
    }

    @Override
    public void playLocalSound(Entity p_312749_, SoundEvent p_312635_, SoundSource p_312007_, float p_312298_, float p_312772_) {
        this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(p_312635_, p_312007_, p_312298_, p_312772_, p_312749_, this.random.nextLong()));
    }

    @Override
    public void playPlayerSound(SoundEvent p_401146_, SoundSource p_401176_, float p_401193_, float p_401050_) {
        if (this.minecraft.player != null) {
            this.minecraft
                .getSoundManager()
                .play(new EntityBoundSoundInstance(p_401146_, p_401176_, p_401193_, p_401050_, this.minecraft.player, this.random.nextLong()));
        }
    }

    @Override
    public void playLocalSound(
        double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean distanceDelay
    ) {
        this.playSound(x, y, z, sound, category, volume, pitch, distanceDelay, this.random.nextLong());
    }

    private void playSound(
        double x,
        double y,
        double z,
        SoundEvent soundEvent,
        SoundSource source,
        float volume,
        float pitch,
        boolean distanceDelay,
        long seed
    ) {
        double d0 = this.minecraft.gameRenderer.getMainCamera().position().distanceToSqr(x, y, z);
        SimpleSoundInstance simplesoundinstance = new SimpleSoundInstance(
            soundEvent, source, volume, pitch, RandomSource.create(seed), x, y, z
        );
        if (distanceDelay && d0 > 100.0) {
            double d1 = Math.sqrt(d0) / 40.0;
            this.minecraft.getSoundManager().playDelayed(simplesoundinstance, (int)(d1 * 20.0));
        } else {
            this.minecraft.getSoundManager().play(simplesoundinstance);
        }
    }

    @Override
    public void createFireworks(
        double p_104585_, double p_104586_, double p_104587_, double p_104588_, double p_104589_, double p_104590_, List<FireworkExplosion> p_330281_
    ) {
        if (p_330281_.isEmpty()) {
            for (int i = 0; i < this.random.nextInt(3) + 2; i++) {
                this.addParticle(
                    ParticleTypes.POOF, p_104585_, p_104586_, p_104587_, this.random.nextGaussian() * 0.05, 0.005, this.random.nextGaussian() * 0.05
                );
            }
        } else {
            this.minecraft
                .particleEngine
                .add(
                    new FireworkParticles.Starter(
                        this, p_104585_, p_104586_, p_104587_, p_104588_, p_104589_, p_104590_, this.minecraft.particleEngine, p_330281_
                    )
                );
        }
    }

    @Override
    public void sendPacketToServer(Packet<?> packet) {
        this.connection.send(packet);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.worldBorder;
    }

    @Override
    public RecipeAccess recipeAccess() {
        return this.connection.recipes();
    }

    @Override
    public TickRateManager tickRateManager() {
        return this.tickRateManager;
    }

    @Override
    public EnvironmentAttributeSystem environmentAttributes() {
        return this.environmentAttributes;
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    public ClientChunkCache getChunkSource() {
        return this.chunkSource;
    }

    @Override
    public @Nullable MapItemSavedData getMapData(MapId p_323873_) {
        return this.mapData.get(p_323873_);
    }

    public void overrideMapData(MapId mapId, MapItemSavedData mapData) {
        this.mapData.put(mapId, mapData);
    }

    @Override
    public Scoreboard getScoreboard() {
        return this.connection.scoreboard();
    }

    /**
     * Flags are as in setBlockState
     */
    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, @Block.UpdateFlags int flags) {
        this.levelRenderer.blockChanged(this, pos, oldState, newState, flags);
    }

    @Override
    public void setBlocksDirty(BlockPos blockPos, BlockState oldState, BlockState newState) {
        this.levelRenderer.setBlockDirty(blockPos, oldState, newState);
    }

    public void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ) {
        this.levelRenderer.setSectionDirtyWithNeighbors(sectionX, sectionY, sectionZ);
    }

    public void setSectionRangeDirty(int minY, int minX, int minZ, int maxY, int maxX, int maxZ) {
        this.levelRenderer.setSectionRangeDirty(minY, minX, minZ, maxY, maxX, maxZ);
    }

    @Override
    public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {
        this.levelRenderer.destroyBlockProgress(breakerId, pos, progress);
    }

    @Override
    public void globalLevelEvent(int id, BlockPos pos, int data) {
        this.levelEventHandler.globalLevelEvent(id, pos, data);
    }

    @Override
    public void levelEvent(@Nullable Entity p_394519_, int p_104655_, BlockPos p_104656_, int p_104657_) {
        try {
            this.levelEventHandler.levelEvent(p_104655_, p_104656_, p_104657_);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Playing level event");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Level event being played");
            crashreportcategory.setDetail("Block coordinates", CrashReportCategory.formatLocation(this, p_104656_));
            crashreportcategory.setDetail("Event source", p_394519_);
            crashreportcategory.setDetail("Event type", p_104655_);
            crashreportcategory.setDetail("Event data", p_104657_);
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public void addParticle(
        ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
    ) {
        this.doAddParticle(particleData, particleData.getType().getOverrideLimiter(), false, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @Override
    public void addParticle(
        ParticleOptions p_104714_,
        boolean p_104715_,
        boolean p_383197_,
        double p_104716_,
        double p_104717_,
        double p_104718_,
        double p_104719_,
        double p_104720_,
        double p_104721_
    ) {
        this.doAddParticle(
            p_104714_, p_104714_.getType().getOverrideLimiter() || p_104715_, p_383197_, p_104716_, p_104717_, p_104718_, p_104719_, p_104720_, p_104721_
        );
    }

    @Override
    public void addAlwaysVisibleParticle(
        ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
    ) {
        this.doAddParticle(particleData, false, true, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @Override
    public void addAlwaysVisibleParticle(
        ParticleOptions particleData,
        boolean ignoreRange,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed
    ) {
        this.doAddParticle(
            particleData, particleData.getType().getOverrideLimiter() || ignoreRange, true, x, y, z, xSpeed, ySpeed, zSpeed
        );
    }

    private void doAddParticle(
        ParticleOptions particle,
        boolean overrideLimiter,
        boolean alwaysShow,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed
    ) {
        try {
            Camera camera = this.minecraft.gameRenderer.getMainCamera();
            ParticleStatus particlestatus = this.calculateParticleLevel(alwaysShow);
            if (overrideLimiter) {
                this.minecraft.particleEngine.createParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
            } else if (!(camera.position().distanceToSqr(x, y, z) > 1024.0)) {
                if (particlestatus != ParticleStatus.MINIMAL) {
                    this.minecraft.particleEngine.createParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
                }
            }
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception while adding particle");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being added");
            crashreportcategory.setDetail("ID", BuiltInRegistries.PARTICLE_TYPE.getKey(particle.getType()));
            crashreportcategory.setDetail(
                "Parameters", () -> ParticleTypes.CODEC.encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), particle).toString()
            );
            crashreportcategory.setDetail("Position", () -> CrashReportCategory.formatLocation(this, x, y, z));
            throw new ReportedException(crashreport);
        }
    }

    private ParticleStatus calculateParticleLevel(boolean alwaysShow) {
        ParticleStatus particlestatus = this.minecraft.options.particles().get();
        if (alwaysShow && particlestatus == ParticleStatus.MINIMAL && this.random.nextInt(10) == 0) {
            particlestatus = ParticleStatus.DECREASED;
        }

        if (particlestatus == ParticleStatus.DECREASED && this.random.nextInt(3) == 0) {
            particlestatus = ParticleStatus.MINIMAL;
        }

        return particlestatus;
    }

    @Override
    public List<AbstractClientPlayer> players() {
        return this.players;
    }

    public List<net.neoforged.neoforge.entity.PartEntity<?>> dragonParts() {
        return this.dragonParts;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int p_205516_, int p_205517_, int p_205518_) {
        return this.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
    }

    private int getSkyFlashTime() {
        return this.minecraft.options.hideLightningFlash().get() ? 0 : this.skyFlashTime;
    }

    @Override
    public void setSkyFlashTime(int timeFlash) {
        this.skyFlashTime = timeFlash;
    }

    @Override
    public float getShade(Direction p_104703_, boolean p_104704_) {
        DimensionType.CardinalLightType dimensiontype$cardinallighttype = this.dimensionType().cardinalLightType();
        if (!p_104704_) {
            return dimensiontype$cardinallighttype == DimensionType.CardinalLightType.NETHER ? 0.9F : 1.0F;
        } else {
            return switch (p_104703_) {
                case DOWN -> dimensiontype$cardinallighttype == DimensionType.CardinalLightType.NETHER ? 0.9F : 0.5F;
                case UP -> dimensiontype$cardinallighttype == DimensionType.CardinalLightType.NETHER ? 0.9F : 1.0F;
                case NORTH, SOUTH -> 0.8F;
                case WEST, EAST -> 0.6F;
            };
        }
    }

    @Override
    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        BlockTintCache blocktintcache = this.tintCaches.get(colorResolver);
        return blocktintcache.getColor(blockPos);
    }

    public int calculateBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        int i = Minecraft.getInstance().options.biomeBlendRadius().get();
        if (i == 0) {
            return colorResolver.getColor(this.getBiome(blockPos).value(), blockPos.getX(), blockPos.getZ());
        } else {
            int j = (i * 2 + 1) * (i * 2 + 1);
            int k = 0;
            int l = 0;
            int i1 = 0;
            Cursor3D cursor3d = new Cursor3D(
                blockPos.getX() - i, blockPos.getY(), blockPos.getZ() - i, blockPos.getX() + i, blockPos.getY(), blockPos.getZ() + i
            );
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            while (cursor3d.advance()) {
                blockpos$mutableblockpos.set(cursor3d.nextX(), cursor3d.nextY(), cursor3d.nextZ());
                int j1 = colorResolver.getColor(this.getBiome(blockpos$mutableblockpos).value(), blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getZ());
                k += (j1 & 0xFF0000) >> 16;
                l += (j1 & 0xFF00) >> 8;
                i1 += j1 & 0xFF;
            }

            return (k / j & 0xFF) << 16 | (l / j & 0xFF) << 8 | i1 / j & 0xFF;
        }
    }

    @Override
    public void setRespawnData(LevelData.RespawnData p_451417_) {
        this.levelData.setSpawn(this.getWorldBorderAdjustedRespawnData(p_451417_));
    }

    @Override
    public LevelData.RespawnData getRespawnData() {
        return this.levelData.getRespawnData();
    }

    @Override
    public String toString() {
        return "ClientLevel";
    }

    public ClientLevel.ClientLevelData getLevelData() {
        return this.clientLevelData;
    }

    @Override
    public void gameEvent(Holder<GameEvent> p_316478_, Vec3 p_233640_, GameEvent.Context p_233641_) {
    }

    protected Map<MapId, MapItemSavedData> getAllMapData() {
        return ImmutableMap.copyOf(this.mapData);
    }

    protected void addMapData(Map<MapId, MapItemSavedData> map) {
        this.mapData.putAll(map);
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return this.entityStorage.getEntityGetter();
    }

    @Override
    public String gatherChunkSourceStats() {
        return "Chunks[C] W: " + this.chunkSource.gatherStats() + " E: " + this.entityStorage.gatherStats();
    }

    @Override
    public void addDestroyBlockEffect(BlockPos p_171667_, BlockState p_171668_) {
        if (!p_171668_.isAir() && !net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions.of(p_171668_).addDestroyEffects(p_171668_, this, p_171667_, this.minecraft.particleEngine)) {
            VoxelShape voxelshape = p_171668_.getShape(this, p_171667_);
            double d0 = 0.25;
            voxelshape.forAllBoxes(
                (p_445188_, p_445189_, p_445190_, p_445191_, p_445192_, p_445193_) -> {
                    double d1 = Math.min(1.0, p_445191_ - p_445188_);
                    double d2 = Math.min(1.0, p_445192_ - p_445189_);
                    double d3 = Math.min(1.0, p_445193_ - p_445190_);
                    int i = Math.max(2, Mth.ceil(d1 / 0.25));
                    int j = Math.max(2, Mth.ceil(d2 / 0.25));
                    int k = Math.max(2, Mth.ceil(d3 / 0.25));

                    for (int l = 0; l < i; l++) {
                        for (int i1 = 0; i1 < j; i1++) {
                            for (int j1 = 0; j1 < k; j1++) {
                                double d4 = (l + 0.5) / i;
                                double d5 = (i1 + 0.5) / j;
                                double d6 = (j1 + 0.5) / k;
                                double d7 = d4 * d1 + p_445188_;
                                double d8 = d5 * d2 + p_445189_;
                                double d9 = d6 * d3 + p_445190_;
                                this.minecraft
                                    .particleEngine
                                    .add(
                                        new TerrainParticle(
                                            this,
                                            p_171667_.getX() + d7,
                                            p_171667_.getY() + d8,
                                            p_171667_.getZ() + d9,
                                            d4 - 0.5,
                                            d5 - 0.5,
                                            d6 - 0.5,
                                            p_171668_,
                                            p_171667_
                                        ).updateSprite(p_171668_, p_171667_)
                                    );
                            }
                        }
                    }
                }
            );
        }
    }

    /**
     * @deprecated Neo: use {@link #addBreakingBlockEffect(BlockPos, Direction,
     *             net.minecraft.world.phys.HitResult)} instead
     */
    public void addBreakingBlockEffect(BlockPos pos, Direction direction) {
        this.addBreakingBlockEffect(pos, direction, null);
    }

    public void addBreakingBlockEffect(BlockPos pos, Direction direction, net.minecraft.world.phys.@Nullable HitResult hitResult) {
        BlockState blockstate = this.getBlockState(pos);
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE && !net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions.of(blockstate).addHitEffects(blockstate, this, hitResult, this.minecraft.particleEngine)) {
            int i = pos.getX();
            int j = pos.getY();
            int k = pos.getZ();
            float f = 0.1F;
            AABB aabb = blockstate.getShape(this, pos).bounds();
            double d0 = i + this.random.nextDouble() * (aabb.maxX - aabb.minX - 0.2F) + 0.1F + aabb.minX;
            double d1 = j + this.random.nextDouble() * (aabb.maxY - aabb.minY - 0.2F) + 0.1F + aabb.minY;
            double d2 = k + this.random.nextDouble() * (aabb.maxZ - aabb.minZ - 0.2F) + 0.1F + aabb.minZ;
            if (direction == Direction.DOWN) {
                d1 = j + aabb.minY - 0.1F;
            }

            if (direction == Direction.UP) {
                d1 = j + aabb.maxY + 0.1F;
            }

            if (direction == Direction.NORTH) {
                d2 = k + aabb.minZ - 0.1F;
            }

            if (direction == Direction.SOUTH) {
                d2 = k + aabb.maxZ + 0.1F;
            }

            if (direction == Direction.WEST) {
                d0 = i + aabb.minX - 0.1F;
            }

            if (direction == Direction.EAST) {
                d0 = i + aabb.maxX + 0.1F;
            }

            this.minecraft.particleEngine.add(new TerrainParticle(this, d0, d1, d2, 0.0, 0.0, 0.0, blockstate, pos).updateSprite(blockstate, pos).setPower(0.2F).scale(0.6F));
        }
    }

    public void setServerSimulationDistance(int serverSimulationDistance) {
        this.serverSimulationDistance = serverSimulationDistance;
    }

    public int getServerSimulationDistance() {
        return this.serverSimulationDistance;
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.connection.enabledFeatures();
    }

    @Override
    public PotionBrewing potionBrewing() {
        return this.connection.potionBrewing();
    }

    @Override
    public FuelValues fuelValues() {
        return this.connection.fuelValues();
    }

    @Override
    public void explode(
        @Nullable Entity p_363686_,
        @Nullable DamageSource p_365268_,
        @Nullable ExplosionDamageCalculator p_363795_,
        double p_360494_,
        double p_360591_,
        double p_364949_,
        float p_361233_,
        boolean p_364900_,
        Level.ExplosionInteraction p_365033_,
        ParticleOptions p_361889_,
        ParticleOptions p_361473_,
        WeightedList<ExplosionParticleInfo> p_437196_,
        Holder<SoundEvent> p_362359_
    ) {
    }

    @Override
    public int getSeaLevel() {
        return this.seaLevel;
    }

    @Override
    public int getClientLeafTintColor(BlockPos p_400248_) {
        return Minecraft.getInstance().getBlockColors().getColor(this.getBlockState(p_400248_), this, p_400248_, 0);
    }

    @Override
    public void registerForCleaning(CacheSlot<ClientLevel, ?> p_399472_) {
        this.connection.registerForCleaning(p_399472_);
    }

    public void trackExplosionEffects(Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> blockParticles) {
        this.explosionTracker.track(center, radius, blockCount, blockParticles);
    }

    @OnlyIn(Dist.CLIENT)
    public static class ClientLevelData implements WritableLevelData {
        private final boolean hardcore;
        private final boolean isFlat;
        private LevelData.RespawnData respawnData;
        private long gameTime;
        private long dayTime;
        private boolean raining;
        private Difficulty difficulty;
        private boolean difficultyLocked;

        public ClientLevelData(Difficulty difficulty, boolean hardcore, boolean isFlat) {
            this.difficulty = difficulty;
            this.hardcore = hardcore;
            this.isFlat = isFlat;
        }

        @Override
        public LevelData.RespawnData getRespawnData() {
            return this.respawnData;
        }

        @Override
        public long getGameTime() {
            return this.gameTime;
        }

        @Override
        public long getDayTime() {
            return this.dayTime;
        }

        public void setGameTime(long gameTime) {
            this.gameTime = gameTime;
        }

        public void setDayTime(long dayTime) {
            this.dayTime = dayTime;
        }

        @Override
        public void setSpawn(LevelData.RespawnData p_451388_) {
            this.respawnData = p_451388_;
        }

        @Override
        public boolean isThundering() {
            return false;
        }

        @Override
        public boolean isRaining() {
            return this.raining;
        }

        /**
         * Sets whether it is raining or not.
         */
        @Override
        public void setRaining(boolean isRaining) {
            this.raining = isRaining;
        }

        @Override
        public boolean isHardcore() {
            return this.hardcore;
        }

        @Override
        public Difficulty getDifficulty() {
            return this.difficulty;
        }

        @Override
        public boolean isDifficultyLocked() {
            return this.difficultyLocked;
        }

        @Override
        public void fillCrashReportCategory(CrashReportCategory p_171690_, LevelHeightAccessor p_171691_) {
            WritableLevelData.super.fillCrashReportCategory(p_171690_, p_171691_);
        }

        public void setDifficulty(Difficulty difficulty) {
            net.neoforged.neoforge.common.CommonHooks.onDifficultyChange(difficulty, this.difficulty);
            this.difficulty = difficulty;
        }

        public void setDifficultyLocked(boolean difficultyLocked) {
            this.difficultyLocked = difficultyLocked;
        }

        public double getHorizonHeight(LevelHeightAccessor level) {
            return this.isFlat ? level.getMinY() : 63.0;
        }

        public float voidDarknessOnsetRange() {
            return this.isFlat ? 1.0F : 32.0F;
        }
    }

    @OnlyIn(Dist.CLIENT)
    final class EntityCallbacks implements LevelCallback<Entity> {
        public void onCreated(Entity p_171696_) {
        }

        public void onDestroyed(Entity p_171700_) {
        }

        public void onTickingStart(Entity p_171704_) {
            ClientLevel.this.tickingEntities.add(p_171704_);
        }

        public void onTickingEnd(Entity p_171708_) {
            ClientLevel.this.tickingEntities.remove(p_171708_);
        }

        public void onTrackingStart(Entity p_171712_) {
            switch (p_171712_) {
                case AbstractClientPlayer abstractclientplayer:
                    ClientLevel.this.players.add(abstractclientplayer);
                    break;
                case EnderDragon enderdragon:
                    ClientLevel.this.dragonParts.addAll(Arrays.asList(enderdragon.getSubEntities()));
                    break;
                default:
                    if (p_171712_.isMultipartEntity()) {
                        ClientLevel.this.dragonParts.addAll(Arrays.asList(p_171712_.getParts()));
                    }
            }
        }

        public void onTrackingEnd(Entity p_171716_) {
            p_171716_.unRide();
            switch (p_171716_) {
                case AbstractClientPlayer abstractclientplayer:
                    ClientLevel.this.players.remove(abstractclientplayer);
                    break;
                case EnderDragon enderdragon:
                    ClientLevel.this.dragonParts.removeAll(Arrays.asList(enderdragon.getSubEntities()));
                    break;
                default:
                    if (p_171716_.isMultipartEntity()) {
                        ClientLevel.this.dragonParts.removeAll(Arrays.asList(p_171716_.getParts()));
                    }
            }

            p_171716_.onRemovedFromLevel();
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent(p_171716_, ClientLevel.this));
        }

        public void onSectionChange(Entity p_233660_) {
        }
    }

    @Override
    public net.neoforged.neoforge.model.data.ModelDataManager getModelDataManager() {
        return modelDataManager;
    }

    @Override
    public net.neoforged.neoforge.model.data.ModelData getModelData(BlockPos pos) {
        return modelDataManager.getAt(pos);
    }

    @Override
    public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
        // TODO 1.21.11: Make this extensible / use our dimension special effects replacement.
        var cardinalLightType = this.dimensionType().cardinalLightType();
        boolean constantAmbientLight = cardinalLightType == DimensionType.CardinalLightType.NETHER;

        if (!shade)
            return constantAmbientLight ? 0.9F : 1.0F;
        float yFactor = constantAmbientLight ? 0.9F : normalY < 0 ? 0.5F : 1.0F;
        return Math.min(normalX * normalX * 0.6F + normalY * normalY * yFactor + normalZ * normalZ * 0.8F, 1.0F);
    }

    // Neo: Variable day time code

    private float dayTimeFraction = 0.0f;
    private float dayTimePerTick = -1.0f;

    @org.jetbrains.annotations.ApiStatus.Internal
    public void setDayTimeFraction(float dayTimeFraction) {
        this.dayTimeFraction = dayTimeFraction;
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public float getDayTimeFraction() {
        return dayTimeFraction;
    }

    public float getDayTimePerTick() {
        return dayTimePerTick;
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public void setDayTimePerTick(float dayTimePerTick) {
        this.dayTimePerTick = dayTimePerTick;
    }
}
