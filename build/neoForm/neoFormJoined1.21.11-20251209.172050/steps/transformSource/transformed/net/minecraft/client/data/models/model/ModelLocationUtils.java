package net.minecraft.client.data.models.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelLocationUtils {
    @Deprecated
    public static Identifier decorateBlockModelLocation(String name) {
        // Neo: Use Identifier.parse to support modded paths
        return Identifier.parse(name).withPrefix("block/");
    }

    public static Identifier decorateItemModelLocation(String name) {
        // Neo: Use Identifier.parse to support modded paths
        return Identifier.parse(name).withPrefix("item/");
    }

    public static Identifier getModelLocation(Block block, String suffix) {
        Identifier identifier = BuiltInRegistries.BLOCK.getKey(block);
        return identifier.withPath(p_388420_ -> "block/" + p_388420_ + suffix);
    }

    public static Identifier getModelLocation(Block block) {
        Identifier identifier = BuiltInRegistries.BLOCK.getKey(block);
        return identifier.withPrefix("block/");
    }

    public static Identifier getModelLocation(Item item) {
        Identifier identifier = BuiltInRegistries.ITEM.getKey(item);
        return identifier.withPrefix("item/");
    }

    public static Identifier getModelLocation(Item item, String suffix) {
        Identifier identifier = BuiltInRegistries.ITEM.getKey(item);
        return identifier.withPath(p_386751_ -> "item/" + p_386751_ + suffix);
    }
}
