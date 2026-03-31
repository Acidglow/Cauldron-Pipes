package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {
    protected static final int SLOT_INPUT = 0;
    protected static final int SLOT_FUEL = 1;
    protected static final int SLOT_RESULT = 2;
    public static final int DATA_LIT_TIME = 0;
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_COOKING_PROGRESS = 2;
    public static final int DATA_COOKING_TOTAL_TIME = 3;
    public static final int NUM_DATA_VALUES = 4;
    public static final int BURN_TIME_STANDARD = 200;
    public static final int BURN_COOL_SPEED = 2;
    private static final Codec<Map<ResourceKey<Recipe<?>>, Integer>> RECIPES_USED_CODEC = Codec.unboundedMap(Recipe.KEY_CODEC, Codec.INT);
    private static final short DEFAULT_COOKING_TIMER = 0;
    private static final short DEFAULT_COOKING_TOTAL_TIME = 0;
    private static final short DEFAULT_LIT_TIME_REMAINING = 0;
    private static final short DEFAULT_LIT_TOTAL_TIME = 0;
    protected NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    int litTimeRemaining;
    int litTotalTime;
    int cookingTimer;
    int cookingTotalTime;
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int p_58431_) {
            switch (p_58431_) {
                case 0:
                    if (litTotalTime > Short.MAX_VALUE) {
                        // Neo: preserve litTime / litDuration ratio on the client as data slots are synced as shorts.
                        return net.minecraft.util.Mth.floor(((double) litTimeRemaining / litTotalTime) * Short.MAX_VALUE);
                    }

                    return AbstractFurnaceBlockEntity.this.litTimeRemaining;
                case 1:
                    return Math.min(AbstractFurnaceBlockEntity.this.litTotalTime, Short.MAX_VALUE);
                case 2:
                    return AbstractFurnaceBlockEntity.this.cookingTimer;
                case 3:
                    return AbstractFurnaceBlockEntity.this.cookingTotalTime;
                default:
                    return 0;
            }
        }

        @Override
        public void set(int p_58433_, int p_58434_) {
            switch (p_58433_) {
                case 0:
                    AbstractFurnaceBlockEntity.this.litTimeRemaining = p_58434_;
                    break;
                case 1:
                    AbstractFurnaceBlockEntity.this.litTotalTime = p_58434_;
                    break;
                case 2:
                    AbstractFurnaceBlockEntity.this.cookingTimer = p_58434_;
                    break;
                case 3:
                    AbstractFurnaceBlockEntity.this.cookingTotalTime = p_58434_;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };
    private final Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;

    protected AbstractFurnaceBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState, RecipeType<? extends AbstractCookingRecipe> recipeType
    ) {
        super(type, pos, blockState);
        this.quickCheck = RecipeManager.createCheck((RecipeType<AbstractCookingRecipe>)recipeType);
        this.recipeType = recipeType;
    }

    private boolean isLit() {
        return this.litTimeRemaining > 0;
    }

    @Override
    protected void loadAdditional(ValueInput p_421736_) {
        super.loadAdditional(p_421736_);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(p_421736_, this.items);
        this.cookingTimer = p_421736_.getIntOr("cooking_time_spent", (short)0);
        this.cookingTotalTime = p_421736_.getIntOr("cooking_total_time", (short)0);
        this.litTimeRemaining = p_421736_.getIntOr("lit_time_remaining", (short)0);
        this.litTotalTime = p_421736_.getIntOr("lit_total_time", (short)0);
        this.recipesUsed.clear();
        this.recipesUsed.putAll(p_421736_.read("RecipesUsed", RECIPES_USED_CODEC).orElse(Map.of()));
    }

    @Override
    protected void saveAdditional(ValueOutput p_421785_) {
        super.saveAdditional(p_421785_);
        p_421785_.putInt("cooking_time_spent", this.cookingTimer);
        p_421785_.putInt("cooking_total_time", this.cookingTotalTime);
        p_421785_.putInt("lit_time_remaining", this.litTimeRemaining);
        p_421785_.putInt("lit_total_time", this.litTotalTime);
        ContainerHelper.saveAllItems(p_421785_, this.items);
        p_421785_.store("RecipesUsed", RECIPES_USED_CODEC, this.recipesUsed);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace) {
        boolean flag = furnace.isLit();
        boolean flag1 = false;
        if (furnace.isLit()) {
            furnace.litTimeRemaining--;
        }

        ItemStack itemstack = furnace.items.get(1);
        ItemStack itemstack1 = furnace.items.get(0);
        boolean flag2 = !itemstack1.isEmpty();
        boolean flag3 = !itemstack.isEmpty();
        if (furnace.isLit() || flag3 && flag2) {
            SingleRecipeInput singlerecipeinput = new SingleRecipeInput(itemstack1);
            RecipeHolder<? extends AbstractCookingRecipe> recipeholder;
            if (flag2) {
                recipeholder = furnace.quickCheck.getRecipeFor(singlerecipeinput, level).orElse(null);
            } else {
                recipeholder = null;
            }

            int i = furnace.getMaxStackSize();
            if (!furnace.isLit() && canBurn(level.registryAccess(), recipeholder, singlerecipeinput, furnace.items, i)) {
                furnace.litTimeRemaining = furnace.getBurnDuration(level.fuelValues(), itemstack);
                furnace.litTotalTime = furnace.litTimeRemaining;
                if (furnace.isLit()) {
                    flag1 = true;
                    var remainder = itemstack.getCraftingRemainder();
                    if (!remainder.isEmpty())
                        furnace.items.set(1, remainder);
                    else
                    if (flag3) {
                        Item item = itemstack.getItem();
                        itemstack.shrink(1);
                        if (itemstack.isEmpty()) {
                            furnace.items.set(1, item.getCraftingRemainder()); // Neo: Remainder is handled in the `if` check above.
                        }
                    }
                }
            }

            if (furnace.isLit() && canBurn(level.registryAccess(), recipeholder, singlerecipeinput, furnace.items, i)) {
                furnace.cookingTimer++;
                if (furnace.cookingTimer == furnace.cookingTotalTime) {
                    furnace.cookingTimer = 0;
                    furnace.cookingTotalTime = getTotalCookTime(level, furnace);
                    if (burn(level.registryAccess(), recipeholder, singlerecipeinput, furnace.items, i)) {
                        furnace.setRecipeUsed(recipeholder);
                    }

                    flag1 = true;
                }
            } else {
                furnace.cookingTimer = 0;
            }
        } else if (!furnace.isLit() && furnace.cookingTimer > 0) {
            furnace.cookingTimer = Mth.clamp(furnace.cookingTimer - 2, 0, furnace.cookingTotalTime);
        }

        if (flag != furnace.isLit()) {
            flag1 = true;
            state = state.setValue(AbstractFurnaceBlock.LIT, furnace.isLit());
            level.setBlock(pos, state, 3);
        }

        if (flag1) {
            setChanged(level, pos, state);
        }
    }

    private static boolean canBurn(
        RegistryAccess registryAccess,
        @Nullable RecipeHolder<? extends AbstractCookingRecipe> recipe,
        SingleRecipeInput recipeInput,
        NonNullList<ItemStack> items,
        int maxStackSize
    ) {
        if (!items.get(0).isEmpty() && recipe != null) {
            ItemStack itemstack = recipe.value().assemble(recipeInput, registryAccess);
            if (itemstack.isEmpty()) {
                return false;
            } else {
                ItemStack itemstack1 = items.get(2);
                if (itemstack1.isEmpty()) {
                    return true;
                } else if (!ItemStack.isSameItemSameComponents(itemstack1, itemstack)) {
                    return false;
                } else {
                    return itemstack1.getCount() + itemstack.getCount() <= maxStackSize && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize() // Neo fix: make furnace respect stack sizes in furnace recipes
                        ? true
                        : itemstack1.getCount() + itemstack.getCount() <= itemstack.getMaxStackSize(); // Neo fix: make furnace respect stack sizes in furnace recipes
                }
            }
        } else {
            return false;
        }
    }

    private static boolean burn(
        RegistryAccess registryAccess,
        @Nullable RecipeHolder<? extends AbstractCookingRecipe> recipe,
        SingleRecipeInput recipeInput,
        NonNullList<ItemStack> items,
        int maxStackSize
    ) {
        if (recipe != null && canBurn(registryAccess, recipe, recipeInput, items, maxStackSize)) {
            ItemStack itemstack = items.get(0);
            ItemStack itemstack1 = recipe.value().assemble(recipeInput, registryAccess);
            ItemStack itemstack2 = items.get(2);
            if (itemstack2.isEmpty()) {
                items.set(2, itemstack1.copy());
            } else if (ItemStack.isSameItemSameComponents(itemstack2, itemstack1)) {
                itemstack2.grow(itemstack1.getCount());
            }

            if (itemstack.is(Blocks.WET_SPONGE.asItem()) && !items.get(1).isEmpty() && items.get(1).is(Items.BUCKET)) {
                items.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemstack.shrink(1);
            return true;
        } else {
            return false;
        }
    }

    protected int getBurnDuration(FuelValues fuelValues, ItemStack stack) {
        return stack.getBurnTime(this.recipeType, fuelValues);
    }

    private static int getTotalCookTime(ServerLevel level, AbstractFurnaceBlockEntity furnace) {
        SingleRecipeInput singlerecipeinput = new SingleRecipeInput(furnace.getItem(0));
        return furnace.quickCheck.getRecipeFor(singlerecipeinput, level).map(p_379263_ -> p_379263_.value().cookingTime()).orElse(200);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            return side == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return this.canPlaceItem(index, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return direction == Direction.DOWN && index == 1 ? stack.is(Items.WATER_BUCKET) || stack.is(Items.BUCKET) : true;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> p_332808_) {
        this.items = p_332808_;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        setItem(index, stack, false);
    }

    // Neo: Skip side-effects if insideTransaction is true so the caller can defer them until the transaction commits
    @Override
    public void setItem(int index, ItemStack stack, boolean insideTransaction) {
        ItemStack itemstack = this.items.get(index);
        boolean flag = !stack.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, stack);
        this.items.set(index, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        if (index == 0 && !flag && this.level instanceof ServerLevel serverlevel && !insideTransaction) {
            this.cookingTotalTime = getTotalCookTime(serverlevel, this);
            this.cookingTimer = 0;
            this.setChanged();
        }
    }

    // Neo: Reset cooking time when the input changes inside a transaction
    private boolean needsCookingReset = false;
    private final net.neoforged.neoforge.transfer.transaction.SnapshotJournal<Boolean> cookingResetJournal = new net.neoforged.neoforge.transfer.transaction.SnapshotJournal<Boolean>() {
        @Override
        protected Boolean createSnapshot() {
            return needsCookingReset;
        }
        @Override
        protected void revertToSnapshot(Boolean snapshot) {
            needsCookingReset = snapshot;
        }
        @Override
        protected void onRootCommit(Boolean originalState) {
            if (needsCookingReset) {
                if (level instanceof ServerLevel serverLevel) {
                    cookingTotalTime = getTotalCookTime(serverLevel, AbstractFurnaceBlockEntity.this);
                    cookingTimer = 0;
                }
                needsCookingReset = false;
            }
        }
    };

    @Override
    public void onTransfer(int slot, int amountChange, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
        if (slot == 0) {
            int currentAmount = getItem(slot).getCount();
            if (currentAmount == 0 || currentAmount == amountChange) {
                cookingResetJournal.updateSnapshots(transaction);
                needsCookingReset = true;
            }
        }
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index == 2) {
            return false;
        } else if (index != 1) {
            return true;
        } else {
            ItemStack itemstack = this.items.get(1);
            return stack.getBurnTime(this.recipeType, this.level.fuelValues()) > 0 || stack.is(Items.BUCKET) && !itemstack.is(Items.BUCKET);
        }
    }

    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> p_301245_) {
        if (p_301245_ != null) {
            ResourceKey<Recipe<?>> resourcekey = p_301245_.id();
            this.recipesUsed.addTo(resourcekey, 1);
        }
    }

    @Override
    public @Nullable RecipeHolder<?> getRecipeUsed() {
        return null;
    }

    @Override
    public void awardUsedRecipes(Player p_58396_, List<ItemStack> p_282202_) {
    }

    public void awardUsedRecipesAndPopExperience(ServerPlayer player) {
        List<RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(player.level(), player.position());
        player.awardRecipes(list);

        for (RecipeHolder<?> recipeholder : list) {
            player.triggerRecipeCrafted(recipeholder, this.items);
        }

        this.recipesUsed.clear();
    }

    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel level, Vec3 popVec) {
        List<RecipeHolder<?>> list = Lists.newArrayList();

        for (Entry<ResourceKey<Recipe<?>>> entry : this.recipesUsed.reference2IntEntrySet()) {
            level.recipeAccess().byKey(entry.getKey()).ifPresent(p_379268_ -> {
                list.add((RecipeHolder<?>)p_379268_);
                createExperience(level, popVec, entry.getIntValue(), ((AbstractCookingRecipe)p_379268_.value()).experience());
            });
        }

        return list;
    }

    private static void createExperience(ServerLevel level, Vec3 popVec, int recipeIndex, float experience) {
        int i = Mth.floor(recipeIndex * experience);
        float f = Mth.frac(recipeIndex * experience);
        if (f != 0.0F && level.random.nextFloat() < f) {
            i++;
        }

        ExperienceOrb.award(level, popVec, i);
    }

    @Override
    public void fillStackedContents(StackedItemContents p_363281_) {
        for (ItemStack itemstack : this.items) {
            p_363281_.accountStack(itemstack);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos p_393693_, BlockState p_393780_) {
        super.preRemoveSideEffects(p_393693_, p_393780_);
        if (this.level instanceof ServerLevel serverlevel) {
            this.getRecipesToAwardAndPopExperience(serverlevel, Vec3.atCenterOf(p_393693_));
        }
    }
}
