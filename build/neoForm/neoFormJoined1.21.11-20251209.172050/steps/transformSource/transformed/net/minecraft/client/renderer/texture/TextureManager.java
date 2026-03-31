package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureManager implements PreparableReloadListener, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Identifier INTENTIONAL_MISSING_TEXTURE = Identifier.withDefaultNamespace("");
    private final Map<Identifier, AbstractTexture> byPath = new HashMap<>();
    private final Set<TickableTexture> tickableTextures = new HashSet<>();
    private final ResourceManager resourceManager;

    public TextureManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        NativeImage nativeimage = MissingTextureAtlasSprite.generateMissingImage();
        this.register(MissingTextureAtlasSprite.getLocation(), new DynamicTexture(() -> "(intentionally-)Missing Texture", nativeimage));
    }

    public void registerAndLoad(Identifier textureId, ReloadableTexture texture) {
        try {
            texture.apply(this.loadContentsSafe(textureId, texture));
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Uploading texture");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Uploaded texture");
            crashreportcategory.setDetail("Resource location", texture.resourceId());
            crashreportcategory.setDetail("Texture id", textureId);
            throw new ReportedException(crashreport);
        }

        this.register(textureId, texture);
    }

    private TextureContents loadContentsSafe(Identifier textureId, ReloadableTexture texture) {
        try {
            return loadContents(this.resourceManager, textureId, texture);
        } catch (Exception exception) {
            LOGGER.error("Failed to load texture {} into slot {}", texture.resourceId(), textureId, exception);
            return TextureContents.createMissing();
        }
    }

    public void registerForNextReload(Identifier textureId) {
        this.register(textureId, new SimpleTexture(textureId));
    }

    public void register(Identifier path, AbstractTexture texture) {
        AbstractTexture abstracttexture = this.byPath.put(path, texture);
        if (abstracttexture != texture) {
            if (abstracttexture != null) {
                this.safeClose(path, abstracttexture);
            }

            if (texture instanceof TickableTexture tickabletexture) {
                this.tickableTextures.add(tickabletexture);
            }
        }
    }

    private void safeClose(Identifier path, AbstractTexture texture) {
        this.tickableTextures.remove(texture);

        try {
            texture.close();
        } catch (Exception exception) {
            LOGGER.warn("Failed to close texture {}", path, exception);
        }
    }

    public AbstractTexture getTexture(Identifier path) {
        AbstractTexture abstracttexture = this.byPath.get(path);
        if (abstracttexture != null) {
            return abstracttexture;
        } else {
            SimpleTexture simpletexture = new SimpleTexture(path);
            this.registerAndLoad(path, simpletexture);
            return simpletexture;
        }
    }

    public void tick() {
        for (TickableTexture tickabletexture : this.tickableTextures) {
            tickabletexture.tick();
        }
    }

    public void release(Identifier path) {
        AbstractTexture abstracttexture = this.byPath.remove(path);
        if (abstracttexture != null) {
            this.safeClose(path, abstracttexture);
        }
    }

    @Override
    public void close() {
        this.byPath.forEach(this::safeClose);
        this.byPath.clear();
        this.tickableTextures.clear();
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState p_435296_, Executor p_118480_, PreparableReloadListener.PreparationBarrier p_118476_, Executor p_118481_
    ) {
        ResourceManager resourcemanager = p_435296_.resourceManager();
        List<TextureManager.PendingReload> list = new ArrayList<>();
        this.byPath.forEach((p_465732_, p_465733_) -> {
            if (p_465733_ instanceof ReloadableTexture reloadabletexture) {
                list.add(scheduleLoad(resourcemanager, p_465732_, reloadabletexture, p_118480_));
            }
        });
        return CompletableFuture.allOf(list.stream().map(TextureManager.PendingReload::newContents).toArray(CompletableFuture[]::new))
            .thenCompose(p_118476_::wait)
            .thenAcceptAsync(p_389351_ -> {
                AddRealmPopupScreen.updateCarouselImages(this.resourceManager);

                for (TextureManager.PendingReload texturemanager$pendingreload : list) {
                    texturemanager$pendingreload.texture.apply(texturemanager$pendingreload.newContents.join());
                }
            }, p_118481_);
    }

    public void dumpAllSheets(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ioexception) {
            LOGGER.error("Failed to create directory {}", path, ioexception);
            return;
        }

        this.byPath.forEach((p_465727_, p_465728_) -> {
            if (p_465728_ instanceof Dumpable dumpable) {
                try {
                    dumpable.dumpContents(p_465727_, path);
                } catch (Exception exception) {
                    LOGGER.error("Failed to dump texture {}", p_465727_, exception);
                }
            }
        });
    }

    private static TextureContents loadContents(ResourceManager resourceManager, Identifier textureId, ReloadableTexture texture) throws IOException {
        try {
            return texture.loadContents(resourceManager);
        } catch (FileNotFoundException filenotfoundexception) {
            if (textureId != INTENTIONAL_MISSING_TEXTURE) {
                LOGGER.warn("Missing resource {} referenced from {}", texture.resourceId(), textureId);
            }

            return TextureContents.createMissing();
        }
    }

    private static TextureManager.PendingReload scheduleLoad(ResourceManager resourceManager, Identifier textureId, ReloadableTexture texture, Executor executor) {
        return new TextureManager.PendingReload(texture, CompletableFuture.supplyAsync(() -> {
            try {
                return loadContents(resourceManager, textureId, texture);
            } catch (IOException ioexception) {
                throw new UncheckedIOException(ioexception);
            }
        }, executor));
    }

    @OnlyIn(Dist.CLIENT)
    record PendingReload(ReloadableTexture texture, CompletableFuture<TextureContents> newContents) {
    }
}
