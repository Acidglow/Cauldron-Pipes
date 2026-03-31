package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface UnbakedModel extends net.neoforged.neoforge.client.extensions.UnbakedModelExtension {
    String PARTICLE_TEXTURE_REFERENCE = "particle";

    default @Nullable Boolean ambientOcclusion() {
        return null;
    }

    default UnbakedModel.@Nullable GuiLight guiLight() {
        return null;
    }

    default @Nullable ItemTransforms transforms() {
        return null;
    }

    default TextureSlots.Data textureSlots() {
        return TextureSlots.Data.EMPTY;
    }

    default @Nullable UnbakedGeometry geometry() {
        return null;
    }

    default @Nullable Identifier parent() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum GuiLight {
        FRONT("front"),
        SIDE("side");

        private final String name;

        private GuiLight(String name) {
            this.name = name;
        }

        public static UnbakedModel.GuiLight getByName(String name) {
            for (UnbakedModel.GuiLight unbakedmodel$guilight : values()) {
                if (unbakedmodel$guilight.name.equals(name)) {
                    return unbakedmodel$guilight;
                }
            }

            throw new IllegalArgumentException("Invalid gui light: " + name);
        }

        public boolean lightLikeBlock() {
            return this == SIDE;
        }

        public String getSerializedName() {
            return name;
        }
    }
}
