package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.shaders.UniformType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PostChain implements AutoCloseable {
    public static final Identifier MAIN_TARGET_ID = Identifier.withDefaultNamespace("main");
    private final List<PostPass> passes;
    private final Map<Identifier, PostChainConfig.InternalTarget> internalTargets;
    private final Set<Identifier> externalTargets;
    private final Map<Identifier, RenderTarget> persistentTargets = new HashMap<>();
    private final CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer;

    private PostChain(
        List<PostPass> passes,
        Map<Identifier, PostChainConfig.InternalTarget> internalTargets,
        Set<Identifier> externalTargets,
        CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer
    ) {
        this.passes = passes;
        this.internalTargets = internalTargets;
        this.externalTargets = externalTargets;
        this.projectionMatrixBuffer = projectionMatrixBuffer;
    }

    public static PostChain load(
        PostChainConfig config, TextureManager textureManager, Set<Identifier> externalTargets, Identifier name, CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer
    ) throws ShaderManager.CompilationException {
        Stream<Identifier> stream = config.passes().stream().flatMap(PostChainConfig.Pass::referencedTargets);
        Set<Identifier> set = stream.filter(p_467997_ -> !config.internalTargets().containsKey(p_467997_)).collect(Collectors.toSet());
        Set<Identifier> set1 = Sets.difference(set, externalTargets);
        if (!set1.isEmpty()) {
            throw new ShaderManager.CompilationException("Referenced external targets are not available in this context: " + set1);
        } else {
            Builder<PostPass> builder = ImmutableList.builder();

            for (int i = 0; i < config.passes().size(); i++) {
                PostChainConfig.Pass postchainconfig$pass = config.passes().get(i);
                builder.add(createPass(textureManager, postchainconfig$pass, name.withSuffix("/" + i)));
            }

            return new PostChain(builder.build(), config.internalTargets(), set, projectionMatrixBuffer);
        }
    }

    private static PostPass createPass(TextureManager textureManager, PostChainConfig.Pass pass, Identifier location) throws ShaderManager.CompilationException {
        RenderPipeline.Builder renderpipeline$builder = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withFragmentShader(pass.fragmentShaderId())
            .withVertexShader(pass.vertexShaderId())
            .withLocation(location);

        for (PostChainConfig.Input postchainconfig$input : pass.inputs()) {
            renderpipeline$builder.withSampler(postchainconfig$input.samplerName() + "Sampler");
        }

        renderpipeline$builder.withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER);

        for (String s1 : pass.uniforms().keySet()) {
            renderpipeline$builder.withUniform(s1, UniformType.UNIFORM_BUFFER);
        }

        RenderPipeline renderpipeline = renderpipeline$builder.build();
        List<PostPass.Input> list = new ArrayList<>();

        for (PostChainConfig.Input postchainconfig$input1 : pass.inputs()) {
            switch (postchainconfig$input1) {
                case PostChainConfig.TextureInput(String s2, Identifier identifier, int i, int j, boolean flag):
                    AbstractTexture abstracttexture = textureManager.getTexture(identifier.withPath(p_359199_ -> "textures/effect/" + p_359199_ + ".png"));
                    list.add(new PostPass.TextureInput(s2, abstracttexture, i, j, flag));
                    break;
                case PostChainConfig.TargetInput(String s, Identifier identifier1, boolean flag1, boolean flag2):
                    list.add(new PostPass.TargetInput(s, identifier1, flag1, flag2));
                    break;
                default:
                    throw new MatchException(null, null);
            }
        }

        return new PostPass(renderpipeline, pass.outputTarget(), pass.uniforms(), list);
    }

    public void addToFrame(FrameGraphBuilder frameGraphBuilder, int width, int height, PostChain.TargetBundle targetBundle) {
        GpuBufferSlice gpubufferslice = this.projectionMatrixBuffer.getBuffer(width, height);
        Map<Identifier, ResourceHandle<RenderTarget>> map = new HashMap<>(this.internalTargets.size() + this.externalTargets.size());

        for (Identifier identifier : this.externalTargets) {
            map.put(identifier, targetBundle.getOrThrow(identifier));
        }

        for (Entry<Identifier, PostChainConfig.InternalTarget> entry : this.internalTargets.entrySet()) {
            Identifier identifier1 = entry.getKey();
            PostChainConfig.InternalTarget postchainconfig$internaltarget = entry.getValue();
            RenderTargetDescriptor rendertargetdescriptor = new RenderTargetDescriptor(
                postchainconfig$internaltarget.width().orElse(width),
                postchainconfig$internaltarget.height().orElse(height),
                true,
                postchainconfig$internaltarget.clearColor()
            );
            if (postchainconfig$internaltarget.persistent()) {
                RenderTarget rendertarget = this.getOrCreatePersistentTarget(identifier1, rendertargetdescriptor);
                map.put(identifier1, frameGraphBuilder.importExternal(identifier1.toString(), rendertarget));
            } else {
                map.put(identifier1, frameGraphBuilder.createInternal(identifier1.toString(), rendertargetdescriptor));
            }
        }

        for (PostPass postpass : this.passes) {
            postpass.addToFrame(frameGraphBuilder, map, gpubufferslice);
        }

        for (Identifier identifier2 : this.externalTargets) {
            targetBundle.replace(identifier2, map.get(identifier2));
        }
    }

    @Deprecated
    public void process(RenderTarget target, GraphicsResourceAllocator graphicsResourceAllocator) {
        FrameGraphBuilder framegraphbuilder = new FrameGraphBuilder();
        PostChain.TargetBundle postchain$targetbundle = PostChain.TargetBundle.of(MAIN_TARGET_ID, framegraphbuilder.importExternal("main", target));
        this.addToFrame(framegraphbuilder, target.width, target.height, postchain$targetbundle);
        framegraphbuilder.execute(graphicsResourceAllocator);
    }

    private RenderTarget getOrCreatePersistentTarget(Identifier name, RenderTargetDescriptor descriptor) {
        RenderTarget rendertarget = this.persistentTargets.get(name);
        if (rendertarget == null || rendertarget.width != descriptor.width() || rendertarget.height != descriptor.height()) {
            if (rendertarget != null) {
                rendertarget.destroyBuffers();
            }

            rendertarget = descriptor.allocate();
            descriptor.prepare(rendertarget);
            this.persistentTargets.put(name, rendertarget);
        }

        return rendertarget;
    }

    @Override
    public void close() {
        this.persistentTargets.values().forEach(RenderTarget::destroyBuffers);
        this.persistentTargets.clear();

        for (PostPass postpass : this.passes) {
            postpass.close();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface TargetBundle {
        static PostChain.TargetBundle of(final Identifier id, final ResourceHandle<RenderTarget> p_handle) {
            return new PostChain.TargetBundle() {
                private ResourceHandle<RenderTarget> handle = p_handle;

                @Override
                public void replace(Identifier p_468670_, ResourceHandle<RenderTarget> p_360603_) {
                    if (p_468670_.equals(id)) {
                        this.handle = p_360603_;
                    } else {
                        throw new IllegalArgumentException("No target with id " + p_468670_);
                    }
                }

                @Override
                public @Nullable ResourceHandle<RenderTarget> get(Identifier p_468117_) {
                    return p_468117_.equals(id) ? this.handle : null;
                }
            };
        }

        void replace(Identifier id, ResourceHandle<RenderTarget> handle);

        @Nullable ResourceHandle<RenderTarget> get(Identifier id);

        default ResourceHandle<RenderTarget> getOrThrow(Identifier id) {
            ResourceHandle<RenderTarget> resourcehandle = this.get(id);
            if (resourcehandle == null) {
                throw new IllegalArgumentException("Missing target with id " + id);
            } else {
                return resourcehandle;
            }
        }
    }
}
