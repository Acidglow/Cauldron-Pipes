package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemInput {
    private static final Dynamic2CommandExceptionType ERROR_STACK_TOO_BIG = new Dynamic2CommandExceptionType(
        (p_304131_, p_304132_) -> Component.translatableEscape("arguments.item.overstacked", p_304131_, p_304132_)
    );
    private final Holder<Item> item;
    private final DataComponentPatch components;

    public ItemInput(Holder<Item> item, DataComponentPatch components) {
        this.item = item;
        this.components = components;
    }

    public Item getItem() {
        return this.item.value();
    }

    public ItemStack createItemStack(int count, boolean allowOversizedStacks) throws CommandSyntaxException {
        ItemStack itemstack = new ItemStack(this.item, count);
        itemstack.applyComponents(this.components);
        if (allowOversizedStacks && count > itemstack.getMaxStackSize()) {
            throw ERROR_STACK_TOO_BIG.create(this.getItemName(), itemstack.getMaxStackSize());
        } else {
            return itemstack;
        }
    }

    public String serialize(HolderLookup.Provider levelRegistry) {
        StringBuilder stringbuilder = new StringBuilder(this.getItemName());
        String s = this.serializeComponents(levelRegistry);
        if (!s.isEmpty()) {
            stringbuilder.append('[');
            stringbuilder.append(s);
            stringbuilder.append(']');
        }

        return stringbuilder.toString();
    }

    private String serializeComponents(HolderLookup.Provider levelRegistries) {
        DynamicOps<Tag> dynamicops = levelRegistries.createSerializationContext(NbtOps.INSTANCE);
        return this.components.entrySet().stream().flatMap(p_465851_ -> {
            DataComponentType<?> datacomponenttype = p_465851_.getKey();
            Identifier identifier = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(datacomponenttype);
            if (identifier == null) {
                return Stream.empty();
            } else {
                Optional<?> optional = p_465851_.getValue();
                if (optional.isPresent()) {
                    TypedDataComponent<?> typeddatacomponent = TypedDataComponent.createUnchecked(datacomponenttype, optional.get());
                    return typeddatacomponent.encodeValue(dynamicops).result().stream().map(p_465849_ -> identifier.toString() + "=" + p_465849_);
                } else {
                    return Stream.of("!" + identifier.toString());
                }
            }
        }).collect(Collectors.joining(String.valueOf(',')));
    }

    private String getItemName() {
        return this.item.unwrapKey().<Object>map(ResourceKey::identifier).orElseGet(() -> "unknown[" + this.item + "]").toString();
    }
}
