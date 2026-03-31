package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SpriteContents implements Stitcher.Entry, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int UBO_SIZE = new Std140SizeCalculator().putMat4f().putMat4f().putFloat().putFloat().putInt().get();
    final Identifier name;
    final int width;
    final int height;
    private final NativeImage originalImage;
    public NativeImage[] byMipLevel;
    final SpriteContents.@Nullable AnimatedTexture animatedTexture;
    private final List<MetadataSectionType.WithValue<?>> additionalMetadata;
    private final MipmapStrategy mipmapStrategy;
    private final float alphaCutoffBias;

    public SpriteContents(Identifier name, FrameSize size, NativeImage originalImage) {
        this(name, size, originalImage, Optional.empty(), List.of(), Optional.empty());
    }

    public SpriteContents(
        Identifier name,
        FrameSize size,
        NativeImage originalImage,
        Optional<AnimationMetadataSection> animationMetadata,
        List<MetadataSectionType.WithValue<?>> additionalMetadata,
        Optional<TextureMetadataSection> textureMetadata
    ) {
        this.name = name;
        this.width = size.width();
        this.height = size.height();
        this.additionalMetadata = additionalMetadata;
        this.animatedTexture = animationMetadata.<SpriteContents.AnimatedTexture>map(
                p_389349_ -> this.createAnimatedTexture(size, originalImage.getWidth(), originalImage.getHeight(), p_389349_)
            )
            .orElse(null);
        this.originalImage = originalImage;
        this.byMipLevel = new NativeImage[]{this.originalImage};
        this.mipmapStrategy = textureMetadata.map(TextureMetadataSection::mipmapStrategy).orElse(MipmapStrategy.AUTO);
        this.alphaCutoffBias = textureMetadata.map(TextureMetadataSection::alphaCutoffBias).orElse(0.0F);
    }

    public NativeImage getOriginalImage() {
        return this.originalImage;
    }

    public void increaseMipLevel(int mipLevel) {
        try {
            this.byMipLevel = MipmapGenerator.generateMipLevels(this.name, this.byMipLevel, mipLevel, this.mipmapStrategy, this.alphaCutoffBias);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Generating mipmaps for frame");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Frame being iterated");
            crashreportcategory.setDetail("Sprite name", this.name);
            crashreportcategory.setDetail("Sprite size", () -> this.width + " x " + this.height);
            crashreportcategory.setDetail("Sprite frames", () -> this.getFrameCount() + " frames");
            crashreportcategory.setDetail("Mipmap levels", mipLevel);
            crashreportcategory.setDetail("Original image size", () -> this.originalImage.getWidth() + "x" + this.originalImage.getHeight());
            throw new ReportedException(crashreport);
        }
    }

    private int getFrameCount() {
        return this.animatedTexture != null ? this.animatedTexture.frames.size() : 1;
    }

    public boolean isAnimated() {
        return this.getFrameCount() > 1;
    }

    private SpriteContents.@Nullable AnimatedTexture createAnimatedTexture(
        FrameSize frameSize, int width, int height, AnimationMetadataSection metadata
    ) {
        int i = width / frameSize.width();
        int j = height / frameSize.height();
        int k = i * j;
        int l = metadata.defaultFrameTime();
        List<SpriteContents.FrameInfo> list;
        if (metadata.frames().isEmpty()) {
            list = new ArrayList<>(k);

            for (int i1 = 0; i1 < k; i1++) {
                list.add(new SpriteContents.FrameInfo(i1, l));
            }
        } else {
            List<AnimationFrame> list1 = metadata.frames().get();
            list = new ArrayList<>(list1.size());

            for (AnimationFrame animationframe : list1) {
                list.add(new SpriteContents.FrameInfo(animationframe.index(), animationframe.timeOr(l)));
            }

            int j1 = 0;
            IntSet intset = new IntOpenHashSet();

            for (Iterator<SpriteContents.FrameInfo> iterator = list.iterator(); iterator.hasNext(); j1++) {
                SpriteContents.FrameInfo spritecontents$frameinfo = iterator.next();
                boolean flag = true;
                if (spritecontents$frameinfo.time <= 0) {
                    LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", this.name, j1, spritecontents$frameinfo.time);
                    flag = false;
                }

                if (spritecontents$frameinfo.index < 0 || spritecontents$frameinfo.index >= k) {
                    LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", this.name, j1, spritecontents$frameinfo.index);
                    flag = false;
                }

                if (flag) {
                    intset.add(spritecontents$frameinfo.index);
                } else {
                    iterator.remove();
                }
            }

            int[] aint = IntStream.range(0, k).filter(p_251185_ -> !intset.contains(p_251185_)).toArray();
            if (aint.length > 0) {
                LOGGER.warn("Unused frames in sprite {}: {}", this.name, Arrays.toString(aint));
            }
        }

        return list.size() <= 1 ? null : new SpriteContents.AnimatedTexture(List.copyOf(list), i, metadata.interpolatedFrames());
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    @Override
    public Identifier name() {
        return this.name;
    }

    public IntStream getUniqueFrames() {
        return this.animatedTexture != null ? this.animatedTexture.getUniqueFrames() : IntStream.of(1);
    }

    public SpriteContents.@Nullable AnimationState createAnimationState(GpuBufferSlice buffer, int size) {
        return this.animatedTexture != null ? this.animatedTexture.createAnimationState(buffer, size) : null;
    }

    public <T> Optional<T> getAdditionalMetadata(MetadataSectionType<T> sectionType) {
        for (MetadataSectionType.WithValue<?> withvalue : this.additionalMetadata) {
            Optional<T> optional = withvalue.unwrapToType(sectionType);
            if (optional.isPresent()) {
                return optional;
            }
        }

        return Optional.empty();
    }

    @Override
    public void close() {
        for (NativeImage nativeimage : this.byMipLevel) {
            nativeimage.close();
        }
    }

    @Override
    public String toString() {
        return "SpriteContents{name=" + this.name + ", frameCount=" + this.getFrameCount() + ", height=" + this.height + ", width=" + this.width + "}";
    }

    public boolean isTransparent(int frame, int x, int y) {
        int i = x;
        int j = y;
        if (this.animatedTexture != null) {
            i = x + this.animatedTexture.getFrameX(frame) * this.width;
            j = y + this.animatedTexture.getFrameY(frame) * this.height;
        }

        return ARGB.alpha(this.originalImage.getPixel(i, j)) == 0;
    }

    public void uploadFirstFrame(GpuTexture texture, int mipLevel) {
        RenderSystem.getDevice()
            .createCommandEncoder()
            .writeToTexture(texture, this.byMipLevel[mipLevel], mipLevel, 0, 0, 0, this.width >> mipLevel, this.height >> mipLevel, 0, 0);
    }

    @OnlyIn(Dist.CLIENT)
    class AnimatedTexture {
        final List<SpriteContents.FrameInfo> frames;
        private final int frameRowSize;
        final boolean interpolateFrames;

        AnimatedTexture(List<SpriteContents.FrameInfo> frames, int frameRowSize, boolean interpolateFrames) {
            this.frames = frames;
            this.frameRowSize = frameRowSize;
            this.interpolateFrames = interpolateFrames;
        }

        int getFrameX(int frameIndex) {
            return frameIndex % this.frameRowSize;
        }

        int getFrameY(int frameIndex) {
            return frameIndex / this.frameRowSize;
        }

        public SpriteContents.AnimationState createAnimationState(GpuBufferSlice buffer, int size) {
            GpuDevice gpudevice = RenderSystem.getDevice();
            Int2ObjectMap<GpuTextureView> int2objectmap = new Int2ObjectOpenHashMap<>();
            GpuBufferSlice[] agpubufferslice = new GpuBufferSlice[SpriteContents.this.byMipLevel.length];

            for (int i : this.getUniqueFrames().toArray()) {
                GpuTexture gputexture = gpudevice.createTexture(
                    () -> SpriteContents.this.name + " animation frame " + i,
                    5,
                    TextureFormat.RGBA8,
                    SpriteContents.this.width,
                    SpriteContents.this.height,
                    1,
                    SpriteContents.this.byMipLevel.length + 1
                );
                int j = this.getFrameX(i) * SpriteContents.this.width;
                int k = this.getFrameY(i) * SpriteContents.this.height;

                for (int l = 0; l < SpriteContents.this.byMipLevel.length; l++) {
                    RenderSystem.getDevice()
                        .createCommandEncoder()
                        .writeToTexture(
                            gputexture,
                            SpriteContents.this.byMipLevel[l],
                            l,
                            0,
                            0,
                            0,
                            SpriteContents.this.width >> l,
                            SpriteContents.this.height >> l,
                            j >> l,
                            k >> l
                        );
                }

                int2objectmap.put(i, RenderSystem.getDevice().createTextureView(gputexture));
            }

            for (int i1 = 0; i1 < SpriteContents.this.byMipLevel.length; i1++) {
                agpubufferslice[i1] = buffer.slice(i1 * size, size);
            }

            return SpriteContents.this.new AnimationState(this, int2objectmap, agpubufferslice);
        }

        public IntStream getUniqueFrames() {
            return this.frames.stream().mapToInt(p_249981_ -> p_249981_.index).distinct();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class AnimationState implements AutoCloseable {
        private int frame;
        private int subFrame;
        private final SpriteContents.AnimatedTexture animationInfo;
        private final Int2ObjectMap<GpuTextureView> frameTexturesByIndex;
        private final GpuBufferSlice[] spriteUbosByMip;
        private boolean isDirty = true;

        AnimationState(SpriteContents.AnimatedTexture animationInfo, Int2ObjectMap<GpuTextureView> frameTexturesByIndex, GpuBufferSlice[] spriteUbosByMip) {
            this.animationInfo = animationInfo;
            this.frameTexturesByIndex = frameTexturesByIndex;
            this.spriteUbosByMip = spriteUbosByMip;
        }

        public void tick() {
            this.subFrame++;
            this.isDirty = false;
            SpriteContents.FrameInfo spritecontents$frameinfo = this.animationInfo.frames.get(this.frame);
            if (this.subFrame >= spritecontents$frameinfo.time) {
                int i = spritecontents$frameinfo.index;
                this.frame = (this.frame + 1) % this.animationInfo.frames.size();
                this.subFrame = 0;
                int j = this.animationInfo.frames.get(this.frame).index;
                if (i != j) {
                    this.isDirty = true;
                }
            }
        }

        public GpuBufferSlice getDrawUbo(int mipLevel) {
            return this.spriteUbosByMip[mipLevel];
        }

        public boolean needsToDraw() {
            return this.animationInfo.interpolateFrames || this.isDirty;
        }

        public void drawToAtlas(RenderPass renderPass, GpuBufferSlice ubo) {
            GpuSampler gpusampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);
            List<SpriteContents.FrameInfo> list = this.animationInfo.frames;
            int i = list.get(this.frame).index;
            float f = (float)this.subFrame / this.animationInfo.frames.get(this.frame).time;
            int j = (int)(f * 1000.0F);
            if (this.animationInfo.interpolateFrames) {
                int k = list.get((this.frame + 1) % list.size()).index;
                renderPass.setPipeline(RenderPipelines.ANIMATE_SPRITE_INTERPOLATE);
                renderPass.bindTexture("CurrentSprite", this.frameTexturesByIndex.get(i), gpusampler);
                renderPass.bindTexture("NextSprite", this.frameTexturesByIndex.get(k), gpusampler);
            } else if (this.isDirty) {
                renderPass.setPipeline(RenderPipelines.ANIMATE_SPRITE_BLIT);
                renderPass.bindTexture("Sprite", this.frameTexturesByIndex.get(i), gpusampler);
            }

            renderPass.setUniform("SpriteAnimationInfo", ubo);
            renderPass.draw(j << 3, 6);
        }

        @Override
        public void close() {
            for (GpuTextureView gputextureview : this.frameTexturesByIndex.values()) {
                gputextureview.texture().close();
                gputextureview.close();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    record FrameInfo(int index, int time) {
    }
}
