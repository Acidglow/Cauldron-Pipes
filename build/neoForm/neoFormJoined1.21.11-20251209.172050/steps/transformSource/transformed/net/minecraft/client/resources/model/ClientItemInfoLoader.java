package net.minecraft.client.resources.model;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.PlaceholderLookupProvider;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientItemInfoLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter LISTER = FileToIdConverter.json("items");

    public static CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> scheduleLoad(ResourceManager resourceManager, Executor executor) {
        RegistryAccess.Frozen registryaccess$frozen = ClientRegistryLayer.createRegistryAccess().compositeAccess();
        return CompletableFuture.<Map<Identifier, Resource>>supplyAsync(() -> LISTER.listMatchingResources(resourceManager), executor)
            .thenCompose(
                p_465770_ -> {
                    List<CompletableFuture<ClientItemInfoLoader.PendingLoad>> list = new ArrayList<>(p_465770_.size());
                    p_465770_.forEach(
                        (p_466844_, p_399362_) -> list.add(
                            CompletableFuture.supplyAsync(
                                () -> {
                                    Identifier identifier = LISTER.fileToId(p_466844_);

                                    try {
                                        ClientItemInfoLoader.PendingLoad clientiteminfoloader$pendingload;
                                        try (Reader reader = p_399362_.openAsReader()) {
                                            PlaceholderLookupProvider placeholderlookupprovider = new PlaceholderLookupProvider(registryaccess$frozen);
                                            DynamicOps<JsonElement> dynamicops = placeholderlookupprovider.createSerializationContext(JsonOps.INSTANCE);
                                            ClientItem clientitem = ClientItem.CODEC
                                                .parse(dynamicops, StrictJsonParser.parse(reader))
                                                .ifError(
                                                    p_390349_ -> LOGGER.error(
                                                        "Couldn't parse item model '{}' from pack '{}': {}",
                                                        identifier,
                                                        p_399362_.sourcePackId(),
                                                        p_390349_.message()
                                                    )
                                                )
                                                .result()
                                                .map(
                                                    p_399364_ -> placeholderlookupprovider.hasRegisteredPlaceholders()
                                                        ? p_399364_.withRegistrySwapper(placeholderlookupprovider.createSwapper())
                                                        : p_399364_
                                                )
                                                .orElse(null);
                                            clientiteminfoloader$pendingload = new ClientItemInfoLoader.PendingLoad(identifier, clientitem);
                                        }

                                        return clientiteminfoloader$pendingload;
                                    } catch (Exception exception) {
                                        LOGGER.error("Failed to open item model {} from pack '{}'", p_466844_, p_399362_.sourcePackId(), exception);
                                        return new ClientItemInfoLoader.PendingLoad(identifier, null);
                                    }
                                },
                                executor
                            )
                        )
                    );
                    return Util.sequence(list).thenApply(p_465767_ -> {
                        Map<Identifier, ClientItem> map = new HashMap<>();

                        for (ClientItemInfoLoader.PendingLoad clientiteminfoloader$pendingload : p_465767_) {
                            if (clientiteminfoloader$pendingload.clientItemInfo != null) {
                                map.put(clientiteminfoloader$pendingload.id, clientiteminfoloader$pendingload.clientItemInfo);
                            }
                        }

                        return new ClientItemInfoLoader.LoadedClientInfos(map);
                    });
                }
            );
    }

    @OnlyIn(Dist.CLIENT)
    public record LoadedClientInfos(Map<Identifier, ClientItem> contents) {
    }

    @OnlyIn(Dist.CLIENT)
    record PendingLoad(Identifier id, @Nullable ClientItem clientItemInfo) {
    }
}
