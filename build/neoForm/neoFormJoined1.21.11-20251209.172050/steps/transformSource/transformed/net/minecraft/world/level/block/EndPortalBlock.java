package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class EndPortalBlock extends BaseEntityBlock implements Portal {
    public static final MapCodec<EndPortalBlock> CODEC = simpleCodec(EndPortalBlock::new);
    private static final VoxelShape SHAPE = Block.column(16.0, 6.0, 12.0);

    @Override
    public MapCodec<EndPortalBlock> codec() {
        return CODEC;
    }

    public EndPortalBlock(BlockBehaviour.Properties p_53017_) {
        super(p_53017_);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153196_, BlockState p_153197_) {
        return new TheEndPortalBlockEntity(p_153196_, p_153197_);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState p_371319_, BlockGetter p_399758_, BlockPos p_371244_, Entity p_399952_) {
        return p_371319_.getShape(p_399758_, p_371244_);
    }

    @Override
    protected void entityInside(BlockState p_53025_, Level p_53026_, BlockPos p_53027_, Entity p_53028_, InsideBlockEffectApplier p_405056_, boolean p_451764_) {
        if (p_53028_.canUsePortal(false)) {
            if (!p_53026_.isClientSide() && p_53026_.dimension() == Level.END && p_53028_ instanceof ServerPlayer serverplayer && !serverplayer.seenCredits) {
                serverplayer.showEndCredits();
            } else {
                p_53028_.setAsInsidePortal(this, p_53027_);
            }
        }
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel p_351032_, Entity p_350274_, BlockPos p_350589_) {
        LevelData.RespawnData leveldata$respawndata = p_351032_.getRespawnData();
        ResourceKey<Level> resourcekey = p_351032_.dimension();
        boolean flag = resourcekey == Level.END;
        ResourceKey<Level> resourcekey1 = flag ? leveldata$respawndata.dimension() : Level.END;
        BlockPos blockpos = flag ? leveldata$respawndata.pos() : ServerLevel.END_SPAWN_POINT;
        ServerLevel serverlevel = p_351032_.getServer().getLevel(resourcekey1);
        if (serverlevel == null) {
            return null;
        } else {
            Vec3 vec3 = blockpos.getBottomCenter();
            float f;
            float f1;
            Set<Relative> set;
            if (!flag) {
                EndPlatformFeature.createEndPlatform(serverlevel, BlockPos.containing(vec3).below(), true);
                f = Direction.WEST.toYRot();
                f1 = 0.0F;
                set = Relative.union(Relative.DELTA, Set.of(Relative.X_ROT));
                if (p_350274_ instanceof ServerPlayer) {
                    vec3 = vec3.subtract(0.0, 1.0, 0.0);
                }
            } else {
                f = leveldata$respawndata.yaw();
                f1 = leveldata$respawndata.pitch();
                set = Relative.union(Relative.DELTA, Relative.ROTATION);
                if (p_350274_ instanceof ServerPlayer serverplayer) {
                    return serverplayer.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
                }

                vec3 = p_350274_.adjustSpawnLocation(serverlevel, blockpos).getBottomCenter();
            }

            return new TeleportTransition(
                serverlevel, vec3, Vec3.ZERO, f, f1, set, TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET)
            );
        }
    }

    @Override
    public void animateTick(BlockState p_221102_, Level p_221103_, BlockPos p_221104_, RandomSource p_221105_) {
        double d0 = p_221104_.getX() + p_221105_.nextDouble();
        double d1 = p_221104_.getY() + 0.8;
        double d2 = p_221104_.getZ() + p_221105_.nextDouble();
        p_221103_.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader p_304508_, BlockPos p_53022_, BlockState p_53023_, boolean p_388548_) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    protected RenderShape getRenderShape(BlockState p_389588_) {
        return RenderShape.INVISIBLE;
    }
}
