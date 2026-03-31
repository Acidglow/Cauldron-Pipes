package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ReloadableTexture extends AbstractTexture {
    private final Identifier resourceId;

    public ReloadableTexture(Identifier resourceId) {
        this.resourceId = resourceId;
    }

    public Identifier resourceId() {
        return this.resourceId;
    }

    public void apply(TextureContents textureContents) {
        boolean flag = textureContents.clamp();
        boolean flag1 = textureContents.blur();
        AddressMode addressmode = flag ? AddressMode.CLAMP_TO_EDGE : AddressMode.REPEAT;
        FilterMode filtermode = flag1 ? FilterMode.LINEAR : FilterMode.NEAREST;
        this.sampler = RenderSystem.getSamplerCache().getSampler(addressmode, addressmode, filtermode, filtermode, false);

        try (NativeImage nativeimage = textureContents.image()) {
            this.doLoad(nativeimage);
        }
    }

    protected void doLoad(NativeImage image) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.close();
        this.texture = gpudevice.createTexture(this.resourceId::toString, 5, TextureFormat.RGBA8, image.getWidth(), image.getHeight(), 1, 1);
        this.textureView = gpudevice.createTextureView(this.texture);
        gpudevice.createCommandEncoder().writeToTexture(this.texture, image);
    }

    public abstract TextureContents loadContents(ResourceManager resourceManager) throws IOException;
}
