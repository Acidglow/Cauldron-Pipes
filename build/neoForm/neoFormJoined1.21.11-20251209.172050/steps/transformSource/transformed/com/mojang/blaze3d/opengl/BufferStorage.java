package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public abstract class BufferStorage {
    public static BufferStorage create(GLCapabilities capabilities, Set<String> enabledExtensions) {
        if (capabilities.GL_ARB_buffer_storage && GlDevice.USE_GL_ARB_buffer_storage) {
            enabledExtensions.add("GL_ARB_buffer_storage");
            return new BufferStorage.Immutable();
        } else {
            return new BufferStorage.Mutable();
        }
    }

    public abstract GlBuffer createBuffer(DirectStateAccess directStateAccess, @Nullable Supplier<String> label, @GpuBuffer.Usage int usage, long size);

    public abstract GlBuffer createBuffer(
        DirectStateAccess directStateAccess, @Nullable Supplier<String> label, @GpuBuffer.Usage int usage, ByteBuffer data
    );

    public abstract GlBuffer.GlMappedView mapBuffer(DirectStateAccess directStateAccess, GlBuffer buffer, long offset, long length, int access);

    @OnlyIn(Dist.CLIENT)
    static class Immutable extends BufferStorage {
        @Override
        public GlBuffer createBuffer(DirectStateAccess p_418138_, @Nullable Supplier<String> p_418472_, @GpuBuffer.Usage int p_418304_, long p_482326_) {
            int i = p_418138_.createBuffer();
            p_418138_.bufferStorage(i, p_482326_, p_418304_);
            ByteBuffer bytebuffer = this.tryMapBufferPersistent(p_418138_, p_418304_, i, p_482326_);
            return new GlBuffer(p_418472_, p_418138_, p_418304_, p_482326_, i, bytebuffer);
        }

        @Override
        public GlBuffer createBuffer(DirectStateAccess p_418437_, @Nullable Supplier<String> p_418106_, @GpuBuffer.Usage int p_418386_, ByteBuffer p_418383_) {
            int i = p_418437_.createBuffer();
            int j = p_418383_.remaining();
            p_418437_.bufferStorage(i, p_418383_, p_418386_);
            ByteBuffer bytebuffer = this.tryMapBufferPersistent(p_418437_, p_418386_, i, j);
            return new GlBuffer(p_418106_, p_418437_, p_418386_, j, i, bytebuffer);
        }

        private @Nullable ByteBuffer tryMapBufferPersistent(DirectStateAccess directStateAccess, @GpuBuffer.Usage int usage, int buffer, long length) {
            int i = 0;
            if ((usage & 1) != 0) {
                i |= 1;
            }

            if ((usage & 2) != 0) {
                i |= 18;
            }

            ByteBuffer bytebuffer;
            if (i != 0) {
                GlStateManager.clearGlErrors();
                bytebuffer = directStateAccess.mapBufferRange(buffer, 0L, length, i | 64, usage);
                if (bytebuffer == null) {
                    throw new IllegalStateException("Can't persistently map buffer, opengl error " + GlStateManager._getError());
                }
            } else {
                bytebuffer = null;
            }

            return bytebuffer;
        }

        @Override
        public GlBuffer.GlMappedView mapBuffer(DirectStateAccess p_418435_, GlBuffer p_418399_, long p_482317_, long p_482287_, int p_418357_) {
            if (p_418399_.persistentBuffer == null) {
                throw new IllegalStateException("Somehow trying to map an unmappable buffer");
            } else if (p_482317_ > 2147483647L || p_482287_ > 2147483647L) {
                throw new IllegalArgumentException("Mapping buffers larger than 2GB is not supported");
            } else if (p_482317_ >= 0L && p_482287_ >= 0L) {
                return new GlBuffer.GlMappedView(() -> {
                    if ((p_418357_ & 2) != 0) {
                        p_418435_.flushMappedBufferRange(p_418399_.handle, p_482317_, p_482287_, p_418399_.usage());
                    }
                }, p_418399_, MemoryUtil.memSlice(p_418399_.persistentBuffer, (int)p_482317_, (int)p_482287_));
            } else {
                throw new IllegalArgumentException("Offset or length must be positive integer values");
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Mutable extends BufferStorage {
        @Override
        public GlBuffer createBuffer(DirectStateAccess p_418247_, @Nullable Supplier<String> p_418177_, @GpuBuffer.Usage int p_418191_, long p_482313_) {
            int i = p_418247_.createBuffer();
            p_418247_.bufferData(i, p_482313_, p_418191_);
            return new GlBuffer(p_418177_, p_418247_, p_418191_, p_482313_, i, null);
        }

        @Override
        public GlBuffer createBuffer(DirectStateAccess p_418102_, @Nullable Supplier<String> p_418521_, @GpuBuffer.Usage int p_418167_, ByteBuffer p_418232_) {
            int i = p_418102_.createBuffer();
            int j = p_418232_.remaining();
            p_418102_.bufferData(i, p_418232_, p_418167_);
            return new GlBuffer(p_418521_, p_418102_, p_418167_, j, i, null);
        }

        @Override
        public GlBuffer.GlMappedView mapBuffer(DirectStateAccess p_418209_, GlBuffer p_418012_, long p_482285_, long p_482277_, int p_418273_) {
            GlStateManager.clearGlErrors();
            ByteBuffer bytebuffer = p_418209_.mapBufferRange(p_418012_.handle, p_482285_, p_482277_, p_418273_, p_418012_.usage());
            if (bytebuffer == null) {
                throw new IllegalStateException("Can't map buffer, opengl error " + GlStateManager._getError());
            } else {
                return new GlBuffer.GlMappedView(() -> p_418209_.unmapBuffer(p_418012_.handle, p_418012_.usage()), p_418012_, bytebuffer);
            }
        }
    }
}
