package net.minecraft.client.gui.components.debug;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryBiome implements DebugScreenEntry {
    public static final Identifier GROUP = Identifier.withDefaultNamespace("biome");

    @Override
    public void display(DebugScreenDisplayer p_434656_, @Nullable Level p_433603_, @Nullable LevelChunk p_435839_, @Nullable LevelChunk p_434431_) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity != null && minecraft.level != null) {
            BlockPos blockpos = entity.blockPosition();
            if (minecraft.level.isInsideBuildHeight(blockpos.getY())) {
                if (SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES && p_433603_ instanceof ServerLevel) {
                    p_434656_.addToGroup(
                        GROUP, List.of("Biome: " + printBiome(minecraft.level.getBiome(blockpos)), "Server Biome: " + printBiome(p_433603_.getBiome(blockpos)))
                    );
                } else {
                    p_434656_.addLine("Biome: " + printBiome(minecraft.level.getBiome(blockpos)));
                }
            }
        }
    }

    private static String printBiome(Holder<Biome> biome) {
        return biome.unwrap().map(p_465457_ -> p_465457_.identifier().toString(), p_434936_ -> "[unregistered " + p_434936_ + "]");
    }
}
