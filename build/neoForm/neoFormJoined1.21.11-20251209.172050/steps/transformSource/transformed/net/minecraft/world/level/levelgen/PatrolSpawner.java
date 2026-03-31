package net.minecraft.world.level.levelgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

public class PatrolSpawner implements CustomSpawner {
    private int nextTick;

    @Override
    public void tick(ServerLevel p_64570_, boolean p_64571_) {
        if (p_64571_) {
            if (p_64570_.getGameRules().get(GameRules.SPAWN_PATROLS)) {
                RandomSource randomsource = p_64570_.random;
                this.nextTick--;
                if (this.nextTick <= 0) {
                    this.nextTick = this.nextTick + 12000 + randomsource.nextInt(1200);
                    if (p_64570_.isBrightOutside()) {
                        if (randomsource.nextInt(5) == 0) {
                            int i = p_64570_.players().size();
                            if (i >= 1) {
                                Player player = p_64570_.players().get(randomsource.nextInt(i));
                                if (!player.isSpectator()) {
                                    if (!p_64570_.isCloseToVillage(player.blockPosition(), 2)) {
                                        int j = (24 + randomsource.nextInt(24)) * (randomsource.nextBoolean() ? -1 : 1);
                                        int k = (24 + randomsource.nextInt(24)) * (randomsource.nextBoolean() ? -1 : 1);
                                        BlockPos.MutableBlockPos blockpos$mutableblockpos = player.blockPosition().mutable().move(j, 0, k);
                                        int l = 10;
                                        if (p_64570_.hasChunksAt(
                                            blockpos$mutableblockpos.getX() - 10,
                                            blockpos$mutableblockpos.getZ() - 10,
                                            blockpos$mutableblockpos.getX() + 10,
                                            blockpos$mutableblockpos.getZ() + 10
                                        )) {
                                            if (p_64570_.environmentAttributes()
                                                .getValue(EnvironmentAttributes.CAN_PILLAGER_PATROL_SPAWN, blockpos$mutableblockpos)) {
                                                int i1 = (int)Math.ceil(p_64570_.getCurrentDifficultyAt(blockpos$mutableblockpos).getEffectiveDifficulty()) + 1;

                                                for (int j1 = 0; j1 < i1; j1++) {
                                                    blockpos$mutableblockpos.setY(
                                                        p_64570_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockpos$mutableblockpos).getY()
                                                    );
                                                    if (j1 == 0) {
                                                        if (!this.spawnPatrolMember(p_64570_, blockpos$mutableblockpos, randomsource, true)) {
                                                            break;
                                                        }
                                                    } else {
                                                        this.spawnPatrolMember(p_64570_, blockpos$mutableblockpos, randomsource, false);
                                                    }

                                                    blockpos$mutableblockpos.setX(
                                                        blockpos$mutableblockpos.getX() + randomsource.nextInt(5) - randomsource.nextInt(5)
                                                    );
                                                    blockpos$mutableblockpos.setZ(
                                                        blockpos$mutableblockpos.getZ() + randomsource.nextInt(5) - randomsource.nextInt(5)
                                                    );
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean spawnPatrolMember(ServerLevel level, BlockPos pos, RandomSource random, boolean leader) {
        BlockState blockstate = level.getBlockState(pos);
        if (!NaturalSpawner.isValidEmptySpawnBlock(level, pos, blockstate, blockstate.getFluidState(), EntityType.PILLAGER)) {
            return false;
        } else if (!PatrollingMonster.checkPatrollingMonsterSpawnRules(EntityType.PILLAGER, level, EntitySpawnReason.PATROL, pos, random)) {
            return false;
        } else {
            PatrollingMonster patrollingmonster = EntityType.PILLAGER.create(level, EntitySpawnReason.PATROL);
            if (patrollingmonster != null) {
                if (leader) {
                    patrollingmonster.setPatrolLeader(true);
                    patrollingmonster.findPatrolTarget();
                }

                patrollingmonster.setPos(pos.getX(), pos.getY(), pos.getZ());
                patrollingmonster.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.PATROL, null);
                level.addFreshEntityWithPassengers(patrollingmonster);
                return true;
            } else {
                return false;
            }
        }
    }
}
