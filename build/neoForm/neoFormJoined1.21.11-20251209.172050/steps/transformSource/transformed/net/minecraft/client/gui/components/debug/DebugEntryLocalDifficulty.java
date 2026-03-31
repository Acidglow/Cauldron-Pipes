package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryLocalDifficulty implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_433498_, @Nullable Level p_435699_, @Nullable LevelChunk p_435922_, @Nullable LevelChunk p_432782_) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity != null && p_432782_ != null && p_435699_ instanceof ServerLevel serverlevel) {
            BlockPos $$8 = entity.blockPosition();
            if (serverlevel.isInsideBuildHeight($$8.getY())) {
                float f = serverlevel.getMoonBrightness($$8);
                long i = p_432782_.getInhabitedTime();
                DifficultyInstance difficultyinstance = new DifficultyInstance(serverlevel.getDifficulty(), serverlevel.getDayTime(), i, f);
                p_433498_.addLine(
                    String.format(
                        Locale.ROOT,
                        "Local Difficulty: %.2f // %.2f (Day %d)",
                        difficultyinstance.getEffectiveDifficulty(),
                        difficultyinstance.getSpecialMultiplier(),
                        serverlevel.getDayCount()
                    )
                );
            }
        }
    }
}
