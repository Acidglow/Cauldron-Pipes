package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.IdentifierException;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.FileUtil;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ShaderManager extends SimplePreparableReloadListener<ShaderManager.Configs> implements AutoCloseable {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_LOG_LENGTH = 32768;
    public static final String SHADER_PATH = "shaders";
    private static final String SHADER_INCLUDE_PATH = "shaders/include/";
    private static final FileToIdConverter POST_CHAIN_ID_CONVERTER = FileToIdConverter.json("post_effect");
    final TextureManager textureManager;
    private final Consumer<Exception> recoveryHandler;
    private ShaderManager.CompilationCache compilationCache = new ShaderManager.CompilationCache(ShaderManager.Configs.EMPTY);
    final CachedOrthoProjectionMatrixBuffer postChainProjectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer("post", 0.1F, 1000.0F, false);

    public ShaderManager(TextureManager textureManager, Consumer<Exception> recoveryHandler) {
        this.textureManager = textureManager;
        this.recoveryHandler = recoveryHandler;
    }

    protected ShaderManager.Configs prepare(ResourceManager p_366761_, ProfilerFiller p_366562_) {
        Builder<ShaderManager.ShaderSourceKey, String> builder = ImmutableMap.builder();
        Map<Identifier, Resource> map = p_366761_.listResources("shaders", ShaderManager::isShader);

        for (Entry<Identifier, Resource> entry : map.entrySet()) {
            Identifier identifier = entry.getKey();
            ShaderType shadertype = ShaderType.byLocation(identifier);
            if (shadertype != null) {
                loadShader(identifier, entry.getValue(), shadertype, map, builder);
            }
        }

        Builder<Identifier, PostChainConfig> builder1 = ImmutableMap.builder();

        for (Entry<Identifier, Resource> entry1 : POST_CHAIN_ID_CONVERTER.listMatchingResources(p_366761_).entrySet()) {
            loadPostChain(entry1.getKey(), entry1.getValue(), builder1);
        }

        return new ShaderManager.Configs(builder.build(), builder1.build());
    }

    private static void loadShader(
        Identifier location,
        Resource shader,
        ShaderType type,
        Map<Identifier, Resource> shaderResources,
        Builder<ShaderManager.ShaderSourceKey, String> output
    ) {
        Identifier identifier = type.idConverter().fileToId(location);
        GlslPreprocessor glslpreprocessor = createPreprocessor(shaderResources, location);

        try (Reader reader = shader.openAsReader()) {
            String s = IOUtils.toString(reader);
            output.put(new ShaderManager.ShaderSourceKey(identifier, type), String.join("", glslpreprocessor.process(s)));
        } catch (IOException ioexception) {
            LOGGER.error("Failed to load shader source at {}", location, ioexception);
        }
    }

    private static GlslPreprocessor createPreprocessor(final Map<Identifier, Resource> shaderResources, Identifier shaderLocation) {
        final Identifier identifier = shaderLocation.withPath(FileUtil::getFullResourcePath);
        return new GlslPreprocessor() {
            private final Set<Identifier> importedLocations = new ObjectArraySet<>();

            @Override
            public @Nullable String applyImport(boolean p_366551_, String p_366739_) {
                Identifier identifier1;
                try {
                    if (p_366551_) {
                        identifier1 = identifier.withPath(p_465617_ -> FileUtil.normalizeResourcePath(p_465617_ + p_366739_));
                    } else {
                        identifier1 = Identifier.parse(p_366739_).withPrefix("shaders/include/");
                    }
                } catch (IdentifierException identifierexception) {
                    ShaderManager.LOGGER.error("Malformed GLSL import {}: {}", p_366739_, identifierexception.getMessage());
                    return "#error " + identifierexception.getMessage();
                }

                if (!this.importedLocations.add(identifier1)) {
                    return null;
                } else {
                    try {
                        String s;
                        try (Reader reader = shaderResources.get(identifier1).openAsReader()) {
                            s = IOUtils.toString(reader);
                        }

                        return s;
                    } catch (IOException ioexception) {
                        ShaderManager.LOGGER.error("Could not open GLSL import {}: {}", identifier1, ioexception.getMessage());
                        return "#error " + ioexception.getMessage();
                    }
                }
            }
        };
    }

    private static void loadPostChain(Identifier location, Resource postChain, Builder<Identifier, PostChainConfig> output) {
        Identifier identifier = POST_CHAIN_ID_CONVERTER.fileToId(location);

        try (Reader reader = postChain.openAsReader()) {
            JsonElement jsonelement = StrictJsonParser.parse(reader);
            output.put(identifier, PostChainConfig.CODEC.parse(JsonOps.INSTANCE, jsonelement).getOrThrow(JsonSyntaxException::new));
        } catch (JsonParseException | IOException ioexception) {
            LOGGER.error("Failed to parse post chain at {}", location, ioexception);
        }
    }

    private static boolean isShader(Identifier location) {
        return ShaderType.byLocation(location) != null || location.getPath().endsWith(".glsl");
    }

    protected void apply(ShaderManager.Configs p_366597_, ResourceManager p_366533_, ProfilerFiller p_366866_) {
        ShaderManager.CompilationCache shadermanager$compilationcache = new ShaderManager.CompilationCache(p_366597_);
        Set<RenderPipeline> set = new HashSet<>(RenderPipelines.getStaticPipelines());
        List<Identifier> list = new ArrayList<>();
        GpuDevice gpudevice = RenderSystem.getDevice();
        gpudevice.clearPipelineCache();

        for (RenderPipeline renderpipeline : set) {
            CompiledRenderPipeline compiledrenderpipeline = gpudevice.precompilePipeline(renderpipeline, shadermanager$compilationcache::getShaderSource);
            if (!compiledrenderpipeline.isValid()) {
                list.add(renderpipeline.getLocation());
            }
        }

        if (!list.isEmpty()) {
            gpudevice.clearPipelineCache();
            throw new RuntimeException(
                "Failed to load required shader programs:\n" + list.stream().map(p_467862_ -> " - " + p_467862_).collect(Collectors.joining("\n"))
            );
        } else {
            this.compilationCache.close();
            this.compilationCache = shadermanager$compilationcache;
        }
    }

    @Override
    public String getName() {
        return "Shader Loader";
    }

    private void tryTriggerRecovery(Exception exception) {
        if (!this.compilationCache.triggeredRecovery) {
            this.recoveryHandler.accept(exception);
            this.compilationCache.triggeredRecovery = true;
        }
    }

    public @Nullable PostChain getPostChain(Identifier id, Set<Identifier> externalTargets) {
        try {
            return this.compilationCache.getOrLoadPostChain(id, externalTargets);
        } catch (ShaderManager.CompilationException shadermanager$compilationexception) {
            LOGGER.error("Failed to load post chain: {}", id, shadermanager$compilationexception);
            this.compilationCache.postChains.put(id, Optional.empty());
            this.tryTriggerRecovery(shadermanager$compilationexception);
            return null;
        }
    }

    @Override
    public void close() {
        this.compilationCache.close();
        this.postChainProjectionMatrixBuffer.close();
    }

    public @Nullable String getShader(Identifier id, ShaderType type) {
        return this.compilationCache.getShaderSource(id, type);
    }

    @OnlyIn(Dist.CLIENT)
    class CompilationCache implements AutoCloseable {
        private final ShaderManager.Configs configs;
        final Map<Identifier, Optional<PostChain>> postChains = new HashMap<>();
        boolean triggeredRecovery;

        CompilationCache(ShaderManager.Configs configs) {
            this.configs = configs;
        }

        public @Nullable PostChain getOrLoadPostChain(Identifier name, Set<Identifier> externalTargets) throws ShaderManager.CompilationException {
            Optional<PostChain> optional = this.postChains.get(name);
            if (optional != null) {
                return optional.orElse(null);
            } else {
                PostChain postchain = this.loadPostChain(name, externalTargets);
                this.postChains.put(name, Optional.of(postchain));
                return postchain;
            }
        }

        private PostChain loadPostChain(Identifier name, Set<Identifier> externalTargets) throws ShaderManager.CompilationException {
            PostChainConfig postchainconfig = this.configs.postChains.get(name);
            if (postchainconfig == null) {
                throw new ShaderManager.CompilationException("Could not find post chain with id: " + name);
            } else {
                return PostChain.load(
                    postchainconfig, ShaderManager.this.textureManager, externalTargets, name, ShaderManager.this.postChainProjectionMatrixBuffer
                );
            }
        }

        @Override
        public void close() {
            this.postChains.values().forEach(p_418047_ -> p_418047_.ifPresent(PostChain::close));
            this.postChains.clear();
        }

        public @Nullable String getShaderSource(Identifier id, ShaderType type) {
            return this.configs.shaderSources.get(new ShaderManager.ShaderSourceKey(id, type));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class CompilationException extends Exception {
        public CompilationException(String message) {
            super(message);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Configs(Map<ShaderManager.ShaderSourceKey, String> shaderSources, Map<Identifier, PostChainConfig> postChains) {
        public static final ShaderManager.Configs EMPTY = new ShaderManager.Configs(Map.of(), Map.of());
    }

    @OnlyIn(Dist.CLIENT)
    record ShaderSourceKey(Identifier id, ShaderType type) {
        @Override
        public String toString() {
            return this.id + " (" + this.type + ")";
        }
    }
}
