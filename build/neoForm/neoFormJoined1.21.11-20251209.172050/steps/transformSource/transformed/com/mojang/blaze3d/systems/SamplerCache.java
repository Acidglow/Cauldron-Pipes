package com.mojang.blaze3d.systems;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.OptionalDouble;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SamplerCache {
    private final GpuSampler[] samplers = new GpuSampler[32];

    public void initialize() {
        GpuDevice gpudevice = RenderSystem.getDevice();
        if (AddressMode.values().length == 2 && FilterMode.values().length == 2) {
            for (AddressMode addressmode : AddressMode.values()) {
                for (AddressMode addressmode1 : AddressMode.values()) {
                    for (FilterMode filtermode : FilterMode.values()) {
                        for (FilterMode filtermode1 : FilterMode.values()) {
                            for (boolean flag : new boolean[]{true, false}) {
                                this.samplers[encode(addressmode, addressmode1, filtermode, filtermode1, flag)] = gpudevice.createSampler(
                                    addressmode, addressmode1, filtermode, filtermode1, 1, flag ? OptionalDouble.empty() : OptionalDouble.of(0.0)
                                );
                            }
                        }
                    }
                }
            }
        } else {
            throw new IllegalStateException("AddressMode and FilterMode enum sizes must be 2 - if you expanded them, please update SamplerCache");
        }
    }

    public GpuSampler getSampler(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, boolean useMipmaps) {
        return this.samplers[encode(addressModeU, addressModeV, minFilter, magFilter, useMipmaps)];
    }

    public GpuSampler getClampToEdge(FilterMode filter) {
        return this.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filter, filter, false);
    }

    public GpuSampler getClampToEdge(FilterMode filter, boolean useMipmaps) {
        return this.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filter, filter, useMipmaps);
    }

    public GpuSampler getRepeat(FilterMode filter) {
        return this.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, filter, filter, false);
    }

    public GpuSampler getRepeat(FilterMode filter, boolean useMipmaps) {
        return this.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, filter, filter, useMipmaps);
    }

    public void close() {
        for (GpuSampler gpusampler : this.samplers) {
            gpusampler.close();
        }
    }

    @VisibleForTesting
    static int encode(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, boolean useMipmaps) {
        int i = 0;
        i |= addressModeU.ordinal() & 1;
        i |= (addressModeV.ordinal() & 1) << 1;
        i |= (minFilter.ordinal() & 1) << 2;
        i |= (magFilter.ordinal() & 1) << 3;
        if (useMipmaps) {
            i |= 16;
        }

        return i;
    }
}
