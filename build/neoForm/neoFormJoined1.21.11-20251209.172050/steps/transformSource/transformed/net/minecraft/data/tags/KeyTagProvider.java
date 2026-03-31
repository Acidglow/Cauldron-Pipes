package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;

public abstract class KeyTagProvider<T> extends TagsProvider<T> {
    /**
     * @deprecated Neo: Use {@link #KeyTagProvider(PackOutput,ResourceKey,CompletableFuture,String)}
     */
    @Deprecated
    protected KeyTagProvider(PackOutput p_421572_, ResourceKey<? extends Registry<T>> p_421754_, CompletableFuture<HolderLookup.Provider> p_422211_) {
        this(p_421572_, p_421754_, p_422211_, "vanilla");
    }

    protected KeyTagProvider(PackOutput p_421572_, ResourceKey<? extends Registry<T>> p_421754_, CompletableFuture<HolderLookup.Provider> p_422211_, String modId) {
        super(p_421572_, p_421754_, p_422211_, modId);
    }

    protected TagAppender<ResourceKey<T>, T> tag(TagKey<T> key) {
        TagBuilder tagbuilder = this.getOrCreateRawBuilder(key);
        return TagAppender.forBuilder(tagbuilder);
    }
}
