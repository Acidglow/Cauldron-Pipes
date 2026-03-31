package net.minecraft.client.gui.components.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryLookingAtBlock implements DebugScreenEntry {
    public static final Identifier GROUP = Identifier.withDefaultNamespace("looking_at_block");

    @Override
    public void display(DebugScreenDisplayer p_435940_, @Nullable Level p_434234_, @Nullable LevelChunk p_435783_, @Nullable LevelChunk p_432884_) {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        Level level = (Level)(SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES ? p_434234_ : Minecraft.getInstance().level);
        if (entity != null && level != null) {
            HitResult hitresult = entity.pick(20.0, 0.0F, false);
            List<String> list = new ArrayList<>();
            if (hitresult.getType() == HitResult.Type.BLOCK) {
                BlockPos blockpos = ((BlockHitResult)hitresult).getBlockPos();
                BlockState blockstate = level.getBlockState(blockpos);
                list.add(ChatFormatting.UNDERLINE + "Targeted Block: " + blockpos.getX() + ", " + blockpos.getY() + ", " + blockpos.getZ());
                list.add(String.valueOf(BuiltInRegistries.BLOCK.getKey(blockstate.getBlock())));

                for (Entry<Property<?>, Comparable<?>> entry : blockstate.getValues().entrySet()) {
                    list.add(this.getPropertyValueString(entry));
                }

                blockstate.getTags().map(p_465458_ -> "#" + p_465458_.location()).forEach(list::add);
            }

            p_435940_.addToGroup(GROUP, list);
        }
    }

    private String getPropertyValueString(Entry<Property<?>, Comparable<?>> entry) {
        Property<?> property = entry.getKey();
        Comparable<?> comparable = entry.getValue();
        String s = Util.getPropertyName(property, comparable);
        if (Boolean.TRUE.equals(comparable)) {
            s = ChatFormatting.GREEN + s;
        } else if (Boolean.FALSE.equals(comparable)) {
            s = ChatFormatting.RED + s;
        }

        return property.getName() + ": " + s;
    }
}
