package net.minecraft.client.gui.components.debug;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryNoop implements DebugScreenEntry {
    private final boolean isAllowedWithReducedDebugInfo;

    public DebugEntryNoop() {
        this(false);
    }

    public DebugEntryNoop(boolean isAllowedWithReducedDebugInfo) {
        this.isAllowedWithReducedDebugInfo = isAllowedWithReducedDebugInfo;
    }

    @Override
    public void display(DebugScreenDisplayer p_434632_, @Nullable Level p_433263_, @Nullable LevelChunk p_433297_, @Nullable LevelChunk p_433230_) {
    }

    @Override
    public boolean isAllowed(boolean p_435815_) {
        return this.isAllowedWithReducedDebugInfo || !p_435815_;
    }

    @Override
    public DebugEntryCategory category() {
        return DebugEntryCategory.RENDERER;
    }
}
