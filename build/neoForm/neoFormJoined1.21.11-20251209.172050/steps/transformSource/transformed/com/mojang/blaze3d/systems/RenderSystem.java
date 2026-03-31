package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeSource;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class RenderSystem {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final int MINIMUM_ATLAS_TEXTURE_SIZE = 1024;
    public static final int PROJECTION_MATRIX_UBO_SIZE = new Std140SizeCalculator().putMat4f().get();
    private static @Nullable Thread renderThread;
    private static @Nullable GpuDevice DEVICE;
    private static double lastDrawTime = Double.MIN_VALUE;
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequential = new RenderSystem.AutoStorageIndexBuffer(1, 1, IntConsumer::accept);
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialQuad = new RenderSystem.AutoStorageIndexBuffer(4, 6, (p_403829_, p_403830_) -> {
        p_403829_.accept(p_403830_);
        p_403829_.accept(p_403830_ + 1);
        p_403829_.accept(p_403830_ + 2);
        p_403829_.accept(p_403830_ + 2);
        p_403829_.accept(p_403830_ + 3);
        p_403829_.accept(p_403830_);
    });
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialLines = new RenderSystem.AutoStorageIndexBuffer(4, 6, (p_403827_, p_403828_) -> {
        p_403827_.accept(p_403828_);
        p_403827_.accept(p_403828_ + 1);
        p_403827_.accept(p_403828_ + 2);
        p_403827_.accept(p_403828_ + 3);
        p_403827_.accept(p_403828_ + 2);
        p_403827_.accept(p_403828_ + 1);
    });
    private static ProjectionType projectionType = ProjectionType.PERSPECTIVE;
    private static ProjectionType savedProjectionType = ProjectionType.PERSPECTIVE;
    private static final Matrix4fStack modelViewStack = new Matrix4fStack(16);
    private static @Nullable GpuBufferSlice shaderFog = null;
    private static @Nullable GpuBufferSlice shaderLightDirections;
    private static @Nullable GpuBufferSlice projectionMatrixBuffer;
    private static @Nullable GpuBufferSlice savedProjectionMatrixBuffer;
    private static String apiDescription = "Unknown";
    private static final AtomicLong pollEventsWaitStart = new AtomicLong();
    private static final AtomicBoolean pollingEvents = new AtomicBoolean(false);
    private static final ArrayListDeque<RenderSystem.GpuAsyncTask> PENDING_FENCES = new ArrayListDeque<>();
    public static @Nullable GpuTextureView outputColorTextureOverride;
    public static @Nullable GpuTextureView outputDepthTextureOverride;
    private static @Nullable GpuBuffer globalSettingsUniform;
    private static @Nullable DynamicUniforms dynamicUniforms;
    private static final ScissorState scissorStateForRenderTypeDraws = new ScissorState();
    private static SamplerCache samplerCache = new SamplerCache();
    private static final net.neoforged.neoforge.client.pipeline.PipelineModifierStack PIPELINE_MODIFIERS = new net.neoforged.neoforge.client.pipeline.PipelineModifierStack();

    public static SamplerCache getSamplerCache() {
        return samplerCache;
    }

    public static void initRenderThread() {
        if (renderThread != null) {
            throw new IllegalStateException("Could not initialize render thread");
        } else {
            renderThread = Thread.currentThread();
        }
    }

    public static boolean isOnRenderThread() {
        return Thread.currentThread() == renderThread;
    }

    public static void assertOnRenderThread() {
        if (!isOnRenderThread()) {
            throw constructThreadException();
        }
    }

    private static IllegalStateException constructThreadException() {
        return new IllegalStateException("Rendersystem called from wrong thread");
    }

    private static void pollEvents() {
        pollEventsWaitStart.set(Util.getMillis());
        pollingEvents.set(true);
        GLFW.glfwPollEvents();
        pollingEvents.set(false);
    }

    public static boolean isFrozenAtPollEvents() {
        return pollingEvents.get() && Util.getMillis() - pollEventsWaitStart.get() > 200L;
    }

    public static void flipFrame(Window window, @Nullable TracyFrameCapture frameCapture) {
        pollEvents();
        Tesselator.getInstance().clear();
        GLFW.glfwSwapBuffers(window.handle());
        if (frameCapture != null) {
            frameCapture.endFrame();
        }

        dynamicUniforms.reset();
        Minecraft.getInstance().levelRenderer.endFrame();
        pollEvents();
    }

    public static void limitDisplayFPS(int frameRateLimit) {
        double d0 = lastDrawTime + 1.0 / frameRateLimit;

        double d1;
        for (d1 = GLFW.glfwGetTime(); d1 < d0; d1 = GLFW.glfwGetTime()) {
            GLFW.glfwWaitEventsTimeout(d0 - d1);
        }

        lastDrawTime = d1;
    }

    public static void setShaderFog(GpuBufferSlice p_shaderFog) {
        shaderFog = p_shaderFog;
    }

    public static @Nullable GpuBufferSlice getShaderFog() {
        return shaderFog;
    }

    public static void setShaderLights(GpuBufferSlice shaderLights) {
        shaderLightDirections = shaderLights;
    }

    public static @Nullable GpuBufferSlice getShaderLights() {
        return shaderLightDirections;
    }

    public static void enableScissorForRenderTypeDraws(int x, int y, int width, int height) {
        scissorStateForRenderTypeDraws.enable(x, y, width, height);
    }

    public static void disableScissorForRenderTypeDraws() {
        scissorStateForRenderTypeDraws.disable();
    }

    public static ScissorState getScissorStateForRenderTypeDraws() {
        return scissorStateForRenderTypeDraws;
    }

    public static String getBackendDescription() {
        return String.format(Locale.ROOT, "LWJGL version %s", GLX._getLWJGLVersion());
    }

    public static String getApiDescription() {
        return apiDescription;
    }

    public static TimeSource.NanoTimeSource initBackendSystem() {
        return GLX._initGlfw()::getAsLong;
    }

    public static void initRenderer(long window, int debugVerbosity, boolean synchronous, ShaderSource defaultShaderSource, boolean renderDebugLabels) {
        DEVICE = net.neoforged.neoforge.client.ClientHooks.createGpuDevice(window, debugVerbosity, synchronous, defaultShaderSource, renderDebugLabels);
        apiDescription = getDevice().getImplementationInformation();
        dynamicUniforms = new DynamicUniforms();
        samplerCache.initialize();
    }

    public static void setErrorCallback(GLFWErrorCallbackI callback) {
        GLX._setGlfwErrorCallback(callback);
    }

    public static void setupDefaultState() {
        modelViewStack.clear();
    }

    public static void setProjectionMatrix(GpuBufferSlice p_projectionMatrixBuffer, ProjectionType p_projectionType) {
        assertOnRenderThread();
        projectionMatrixBuffer = p_projectionMatrixBuffer;
        projectionType = p_projectionType;
    }

    public static void backupProjectionMatrix() {
        assertOnRenderThread();
        savedProjectionMatrixBuffer = projectionMatrixBuffer;
        savedProjectionType = projectionType;
    }

    public static void restoreProjectionMatrix() {
        assertOnRenderThread();
        projectionMatrixBuffer = savedProjectionMatrixBuffer;
        projectionType = savedProjectionType;
    }

    public static @Nullable GpuBufferSlice getProjectionMatrixBuffer() {
        assertOnRenderThread();
        return projectionMatrixBuffer;
    }

    public static Matrix4f getModelViewMatrix() {
        assertOnRenderThread();
        return modelViewStack;
    }

    public static Matrix4fStack getModelViewStack() {
        assertOnRenderThread();
        return modelViewStack;
    }

    public static RenderSystem.AutoStorageIndexBuffer getSequentialBuffer(VertexFormat.Mode formatMode) {
        assertOnRenderThread();

        return switch (formatMode) {
            case QUADS -> sharedSequentialQuad;
            case LINES -> sharedSequentialLines;
            default -> sharedSequential;
        };
    }

    public static void setGlobalSettingsUniform(GpuBuffer p_globalSettingsUniform) {
        globalSettingsUniform = p_globalSettingsUniform;
    }

    public static @Nullable GpuBuffer getGlobalSettingsUniform() {
        return globalSettingsUniform;
    }

    public static ProjectionType getProjectionType() {
        assertOnRenderThread();
        return projectionType;
    }

    public static void queueFencedTask(Runnable task) {
        PENDING_FENCES.addLast(new RenderSystem.GpuAsyncTask(task, getDevice().createCommandEncoder().createFence()));
    }

    public static void executePendingTasks() {
        for (RenderSystem.GpuAsyncTask rendersystem$gpuasynctask = PENDING_FENCES.peekFirst();
            rendersystem$gpuasynctask != null;
            rendersystem$gpuasynctask = PENDING_FENCES.peekFirst()
        ) {
            if (!rendersystem$gpuasynctask.fence.awaitCompletion(0L)) {
                return;
            }

            try {
                rendersystem$gpuasynctask.callback.run();
            } finally {
                rendersystem$gpuasynctask.fence.close();
            }

            PENDING_FENCES.removeFirst();
        }
    }

    public static GpuDevice getDevice() {
        if (DEVICE == null) {
            throw new IllegalStateException("Can't getDevice() before it was initialized");
        } else {
            return DEVICE;
        }
    }

    public static @Nullable GpuDevice tryGetDevice() {
        return DEVICE;
    }

    public static DynamicUniforms getDynamicUniforms() {
        if (dynamicUniforms == null) {
            throw new IllegalStateException("Can't getDynamicUniforms() before device was initialized");
        } else {
            return dynamicUniforms;
        }
    }

    public static void bindDefaultUniforms(RenderPass renderPass) {
        GpuBufferSlice gpubufferslice = getProjectionMatrixBuffer();
        if (gpubufferslice != null) {
            renderPass.setUniform("Projection", gpubufferslice);
        }

        GpuBufferSlice gpubufferslice1 = getShaderFog();
        if (gpubufferslice1 != null) {
            renderPass.setUniform("Fog", gpubufferslice1);
        }

        GpuBuffer gpubuffer = getGlobalSettingsUniform();
        if (gpubuffer != null) {
            renderPass.setUniform("Globals", gpubuffer);
        }

        GpuBufferSlice gpubufferslice2 = getShaderLights();
        if (gpubufferslice2 != null) {
            renderPass.setUniform("Lighting", gpubufferslice2);
        }
    }

    /**
     * Neo: Push the provided {@link net.neoforged.neoforge.client.pipeline.PipelineModifier PipelineModifier} to be applied to subsequent rendering.
     * <p>
     * Must be paired with a corresponding {@link #popPipelineModifier()} call after flushing the used buffers
     */
    public static void pushPipelineModifier(net.minecraft.resources.ResourceKey<net.neoforged.neoforge.client.pipeline.PipelineModifier> modifier) {
        PIPELINE_MODIFIERS.push(modifier);
    }

    /**
     * Neo: Pop the last {@link net.neoforged.neoforge.client.pipeline.PipelineModifier PipelineModifier} off the modifier stack
     */
    public static void popPipelineModifier() {
        PIPELINE_MODIFIERS.pop();
    }

    /**
     * Neo: Run the provided {@link Runnable} with the provided {@link net.neoforged.neoforge.client.pipeline.PipelineModifier PipelineModifier} applied
     * to the pipelines used by the rendering done in the {@link Runnable}
     */
    public static void renderWithPipelineModifier(net.minecraft.resources.ResourceKey<net.neoforged.neoforge.client.pipeline.PipelineModifier> modifier, Runnable renderTask) {
        PIPELINE_MODIFIERS.renderWithModifier(modifier, renderTask);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static com.mojang.blaze3d.pipeline.RenderPipeline applyPipelineModifiers(com.mojang.blaze3d.pipeline.RenderPipeline pipeline) {
        return PIPELINE_MODIFIERS.apply(pipeline);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static void ensurePipelineModifiersEmpty() {
        PIPELINE_MODIFIERS.ensureEmpty();
    }

    @OnlyIn(Dist.CLIENT)
    public static final class AutoStorageIndexBuffer {
        private final int vertexStride;
        private final int indexStride;
        private final RenderSystem.AutoStorageIndexBuffer.IndexGenerator generator;
        private @Nullable GpuBuffer buffer;
        private VertexFormat.IndexType type = VertexFormat.IndexType.SHORT;
        private int indexCount;

        AutoStorageIndexBuffer(int vertexStride, int indexStride, RenderSystem.AutoStorageIndexBuffer.IndexGenerator generator) {
            this.vertexStride = vertexStride;
            this.indexStride = indexStride;
            this.generator = generator;
        }

        public boolean hasStorage(int index) {
            return index <= this.indexCount;
        }

        public GpuBuffer getBuffer(int index) {
            this.ensureStorage(index);
            return this.buffer;
        }

        private void ensureStorage(int neededIndexCount) {
            if (!this.hasStorage(neededIndexCount)) {
                neededIndexCount = Mth.roundToward(neededIndexCount * 2, this.indexStride);
                RenderSystem.LOGGER.debug("Growing IndexBuffer: Old limit {}, new limit {}.", this.indexCount, neededIndexCount);
                int i = neededIndexCount / this.indexStride;
                int j = i * this.vertexStride;
                VertexFormat.IndexType vertexformat$indextype = VertexFormat.IndexType.least(j);
                int k = Mth.roundToward(neededIndexCount * vertexformat$indextype.bytes, 4);
                ByteBuffer bytebuffer = MemoryUtil.memAlloc(k);

                try {
                    this.type = vertexformat$indextype;
                    it.unimi.dsi.fastutil.ints.IntConsumer intconsumer = this.intConsumer(bytebuffer);

                    for (int l = 0; l < neededIndexCount; l += this.indexStride) {
                        this.generator.accept(intconsumer, l * this.vertexStride / this.indexStride);
                    }

                    bytebuffer.flip();
                    if (this.buffer != null) {
                        this.buffer.close();
                    }

                    this.buffer = RenderSystem.getDevice().createBuffer(() -> "Auto Storage index buffer", 64, bytebuffer);
                } finally {
                    MemoryUtil.memFree(bytebuffer);
                }

                this.indexCount = neededIndexCount;
            }
        }

        private it.unimi.dsi.fastutil.ints.IntConsumer intConsumer(ByteBuffer buffer) {
            switch (this.type) {
                case SHORT:
                    return p_157482_ -> buffer.putShort((short)p_157482_);
                case INT:
                default:
                    return buffer::putInt;
            }
        }

        public VertexFormat.IndexType type() {
            return this.type;
        }

        @OnlyIn(Dist.CLIENT)
        interface IndexGenerator {
            void accept(it.unimi.dsi.fastutil.ints.IntConsumer consumer, int index);
        }
    }

    @OnlyIn(Dist.CLIENT)
    record GpuAsyncTask(Runnable callback, GpuFence fence) {
    }
}
