package net.minecraft.client.renderer.texture.atlas;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface SpriteResourceLoader {
    Logger LOGGER = LogUtils.getLogger();

    static SpriteResourceLoader create(Set<MetadataSectionType<?>> sectionTypes) {
        return (p_477742_, p_477743_, constructor) -> {
            Optional<AnimationMetadataSection> optional;
            Optional<TextureMetadataSection> optional1;
            List<MetadataSectionType.WithValue<?>> list;
            try {
                ResourceMetadata resourcemetadata = p_477743_.metadata();
                optional = resourcemetadata.getSection(AnimationMetadataSection.TYPE);
                optional1 = resourcemetadata.getSection(TextureMetadataSection.TYPE);
                list = resourcemetadata.getTypedSections(sectionTypes);
            } catch (Exception exception) {
                LOGGER.error("Unable to parse metadata from {}", p_477742_, exception);
                return null;
            }

            NativeImage nativeimage;
            try (InputStream inputstream = p_477743_.open()) {
                nativeimage = NativeImage.read(inputstream);
            } catch (IOException ioexception) {
                LOGGER.error("Using missing texture, unable to load {}", p_477742_, ioexception);
                return null;
            }

            FrameSize framesize;
            if (optional.isPresent()) {
                framesize = optional.get().calculateFrameSize(nativeimage.getWidth(), nativeimage.getHeight());
                if (!Mth.isMultipleOf(nativeimage.getWidth(), framesize.width()) || !Mth.isMultipleOf(nativeimage.getHeight(), framesize.height())) {
                    LOGGER.error(
                        "Image {} size {},{} is not multiple of frame size {},{}",
                        p_477742_,
                        nativeimage.getWidth(),
                        nativeimage.getHeight(),
                        framesize.width(),
                        framesize.height()
                    );
                    nativeimage.close();
                    return null;
                }
            } else {
                framesize = new FrameSize(nativeimage.getWidth(), nativeimage.getHeight());
            }

            return constructor.create(p_477742_, framesize, nativeimage, optional, list, optional1);
        };
    }

    default SpriteContents loadSprite(Identifier location, Resource resource) {
        return loadSprite(location, resource, SpriteContents::new);
    }

    @Nullable
    SpriteContents loadSprite(Identifier location, Resource resource, net.neoforged.neoforge.client.textures.SpriteContentsConstructor constructor);
}
