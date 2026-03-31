package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

/**
 * A carver which creates Minecraft's most common cave types.
 */
public class CaveWorldCarver extends WorldCarver<CaveCarverConfiguration> {
    public CaveWorldCarver(Codec<CaveCarverConfiguration> p_159194_) {
        super(p_159194_);
    }

    public boolean isStartChunk(CaveCarverConfiguration p_224894_, RandomSource p_224895_) {
        return p_224895_.nextFloat() <= p_224894_.probability;
    }

    public boolean carve(
        CarvingContext p_224885_,
        CaveCarverConfiguration p_224886_,
        ChunkAccess p_224887_,
        Function<BlockPos, Holder<Biome>> p_224888_,
        RandomSource p_224889_,
        Aquifer p_224890_,
        ChunkPos p_224891_,
        CarvingMask p_224892_
    ) {
        int i = SectionPos.sectionToBlockCoord(this.getRange() * 2 - 1);
        int j = p_224889_.nextInt(p_224889_.nextInt(p_224889_.nextInt(this.getCaveBound()) + 1) + 1);

        for (int k = 0; k < j; k++) {
            double d0 = p_224891_.getBlockX(p_224889_.nextInt(16));
            double d1 = p_224886_.y.sample(p_224889_, p_224885_);
            double d2 = p_224891_.getBlockZ(p_224889_.nextInt(16));
            double d3 = p_224886_.horizontalRadiusMultiplier.sample(p_224889_);
            double d4 = p_224886_.verticalRadiusMultiplier.sample(p_224889_);
            double d5 = p_224886_.floorLevel.sample(p_224889_);
            WorldCarver.CarveSkipChecker worldcarver$carveskipchecker = (p_159202_, p_159203_, p_159204_, p_159205_, p_159206_) -> shouldSkip(
                p_159203_, p_159204_, p_159205_, d5
            );
            int l = 1;
            if (p_224889_.nextInt(4) == 0) {
                double d6 = p_224886_.yScale.sample(p_224889_);
                float f1 = 1.0F + p_224889_.nextFloat() * 6.0F;
                this.createRoom(p_224885_, p_224886_, p_224887_, p_224888_, p_224890_, d0, d1, d2, f1, d6, p_224892_, worldcarver$carveskipchecker);
                l += p_224889_.nextInt(4);
            }

            for (int k1 = 0; k1 < l; k1++) {
                float f = p_224889_.nextFloat() * (float) (Math.PI * 2);
                float f3 = (p_224889_.nextFloat() - 0.5F) / 4.0F;
                float f2 = this.getThickness(p_224889_);
                int i1 = i - p_224889_.nextInt(i / 4);
                int j1 = 0;
                this.createTunnel(
                    p_224885_,
                    p_224886_,
                    p_224887_,
                    p_224888_,
                    p_224889_.nextLong(),
                    p_224890_,
                    d0,
                    d1,
                    d2,
                    d3,
                    d4,
                    f2,
                    f,
                    f3,
                    0,
                    i1,
                    this.getYScale(),
                    p_224892_,
                    worldcarver$carveskipchecker
                );
            }
        }

        return true;
    }

    protected int getCaveBound() {
        return 15;
    }

    protected float getThickness(RandomSource random) {
        float f = random.nextFloat() * 2.0F + random.nextFloat();
        if (random.nextInt(10) == 0) {
            f *= random.nextFloat() * random.nextFloat() * 3.0F + 1.0F;
        }

        return f;
    }

    protected double getYScale() {
        return 1.0;
    }

    protected void createRoom(
        CarvingContext context,
        CaveCarverConfiguration config,
        ChunkAccess chunk,
        Function<BlockPos, Holder<Biome>> biomeAccessor,
        Aquifer aquifer,
        double x,
        double y,
        double z,
        float radius,
        double horizontalVerticalRatio,
        CarvingMask carvingMask,
        WorldCarver.CarveSkipChecker skipChecker
    ) {
        double d0 = 1.5 + Mth.sin((float) (Math.PI / 2)) * radius;
        double d1 = d0 * horizontalVerticalRatio;
        this.carveEllipsoid(context, config, chunk, biomeAccessor, aquifer, x + 1.0, y, z, d0, d1, carvingMask, skipChecker);
    }

    protected void createTunnel(
        CarvingContext context,
        CaveCarverConfiguration config,
        ChunkAccess chunk,
        Function<BlockPos, Holder<Biome>> biomeAccessor,
        long seed,
        Aquifer aquifer,
        double x,
        double y,
        double z,
        double horizontalRadiusMultiplier,
        double verticalRadiusMultiplier,
        float thickness,
        float yaw,
        float pitch,
        int branchIndex,
        int branchCount,
        double horizontalVerticalRatio,
        CarvingMask carvingMask,
        WorldCarver.CarveSkipChecker skipChecker
    ) {
        RandomSource randomsource = RandomSource.create(seed);
        int i = randomsource.nextInt(branchCount / 2) + branchCount / 4;
        boolean flag = randomsource.nextInt(6) == 0;
        float f = 0.0F;
        float f1 = 0.0F;

        for (int j = branchIndex; j < branchCount; j++) {
            double d0 = 1.5 + Mth.sin((float) Math.PI * j / branchCount) * thickness;
            double d1 = d0 * horizontalVerticalRatio;
            float f2 = Mth.cos(pitch);
            x += Mth.cos(yaw) * f2;
            y += Mth.sin(pitch);
            z += Mth.sin(yaw) * f2;
            pitch *= flag ? 0.92F : 0.7F;
            pitch += f1 * 0.1F;
            yaw += f * 0.1F;
            f1 *= 0.9F;
            f *= 0.75F;
            f1 += (randomsource.nextFloat() - randomsource.nextFloat()) * randomsource.nextFloat() * 2.0F;
            f += (randomsource.nextFloat() - randomsource.nextFloat()) * randomsource.nextFloat() * 4.0F;
            if (j == i && thickness > 1.0F) {
                this.createTunnel(
                    context,
                    config,
                    chunk,
                    biomeAccessor,
                    randomsource.nextLong(),
                    aquifer,
                    x,
                    y,
                    z,
                    horizontalRadiusMultiplier,
                    verticalRadiusMultiplier,
                    randomsource.nextFloat() * 0.5F + 0.5F,
                    yaw - (float) (Math.PI / 2),
                    pitch / 3.0F,
                    j,
                    branchCount,
                    1.0,
                    carvingMask,
                    skipChecker
                );
                this.createTunnel(
                    context,
                    config,
                    chunk,
                    biomeAccessor,
                    randomsource.nextLong(),
                    aquifer,
                    x,
                    y,
                    z,
                    horizontalRadiusMultiplier,
                    verticalRadiusMultiplier,
                    randomsource.nextFloat() * 0.5F + 0.5F,
                    yaw + (float) (Math.PI / 2),
                    pitch / 3.0F,
                    j,
                    branchCount,
                    1.0,
                    carvingMask,
                    skipChecker
                );
                return;
            }

            if (randomsource.nextInt(4) != 0) {
                if (!canReach(chunk.getPos(), x, z, j, branchCount, thickness)) {
                    return;
                }

                this.carveEllipsoid(
                    context,
                    config,
                    chunk,
                    biomeAccessor,
                    aquifer,
                    x,
                    y,
                    z,
                    d0 * horizontalRadiusMultiplier,
                    d1 * verticalRadiusMultiplier,
                    carvingMask,
                    skipChecker
                );
            }
        }
    }

    private static boolean shouldSkip(double relative, double relativeY, double relativeZ, double minrelativeY) {
        return relativeY <= minrelativeY ? true : relative * relative + relativeY * relativeY + relativeZ * relativeZ >= 1.0;
    }
}
