package net.minecraft.world.item;

import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class DebugStickItem extends Item {
    public DebugStickItem(Item.Properties p_40948_) {
        super(p_40948_);
    }

    @Override
    public boolean canDestroyBlock(ItemStack p_393705_, BlockState p_393781_, Level p_393500_, BlockPos p_394185_, LivingEntity p_394515_) {
        if (!p_393500_.isClientSide() && p_394515_ instanceof Player player) {
            this.handleInteraction(player, p_393781_, p_393500_, p_394185_, false, p_393705_);
        }

        return false;
    }

    /**
     * Called when this item is used when targeting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (!level.isClientSide() && player != null) {
            BlockPos blockpos = context.getClickedPos();
            if (!this.handleInteraction(player, level.getBlockState(blockpos), level, blockpos, true, context.getItemInHand())) {
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.SUCCESS;
    }

    private boolean handleInteraction(
        Player player, BlockState stateClicked, LevelAccessor accessor, BlockPos pos, boolean shouldCycleState, ItemStack debugStack
    ) {
        if (!player.canUseGameMasterBlocks()) {
            return false;
        } else {
            Holder<Block> holder = stateClicked.getBlockHolder();
            StateDefinition<Block, BlockState> statedefinition = holder.value().getStateDefinition();
            Collection<Property<?>> collection = statedefinition.getProperties();
            if (collection.isEmpty()) {
                message(player, Component.translatable(this.descriptionId + ".empty", holder.getRegisteredName()));
                return false;
            } else {
                DebugStickState debugstickstate = debugStack.get(DataComponents.DEBUG_STICK_STATE);
                if (debugstickstate == null) {
                    return false;
                } else {
                    Property<?> property = debugstickstate.properties().get(holder);
                    if (shouldCycleState) {
                        if (property == null) {
                            property = collection.iterator().next();
                        }

                        BlockState blockstate = cycleState(stateClicked, property, player.isSecondaryUseActive());
                        accessor.setBlock(pos, blockstate, 18);
                        message(player, Component.translatable(this.descriptionId + ".update", property.getName(), getNameHelper(blockstate, property)));
                    } else {
                        property = getRelative(collection, property, player.isSecondaryUseActive());
                        debugStack.set(DataComponents.DEBUG_STICK_STATE, debugstickstate.withProperty(holder, property));
                        message(player, Component.translatable(this.descriptionId + ".select", property.getName(), getNameHelper(stateClicked, property)));
                    }

                    return true;
                }
            }
        }
    }

    private static <T extends Comparable<T>> BlockState cycleState(BlockState state, Property<T> property, boolean backwards) {
        return state.setValue(property, getRelative(property.getPossibleValues(), state.getValue(property), backwards));
    }

    private static <T> T getRelative(Iterable<T> allowedValues, @Nullable T currentValue, boolean backwards) {
        return backwards ? Util.findPreviousInIterable(allowedValues, currentValue) : Util.findNextInIterable(allowedValues, currentValue);
    }

    private static void message(Player player, Component messageComponent) {
        ((ServerPlayer)player).sendSystemMessage(messageComponent, true);
    }

    private static <T extends Comparable<T>> String getNameHelper(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
