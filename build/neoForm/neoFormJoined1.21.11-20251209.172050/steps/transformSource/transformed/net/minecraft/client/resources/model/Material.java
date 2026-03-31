package net.minecraft.client.resources.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class Material {
    public static final Comparator<Material> COMPARATOR = Comparator.comparing(Material::atlasLocation).thenComparing(Material::texture);
    private final Identifier atlasLocation;
    private final Identifier texture;
    private @Nullable RenderType renderType;

    public Material(Identifier atlasLocation, Identifier texture) {
        this.atlasLocation = atlasLocation;
        this.texture = texture;
    }

    public Identifier atlasLocation() {
        return this.atlasLocation;
    }

    public Identifier texture() {
        return this.texture;
    }

    public RenderType renderType(Function<Identifier, RenderType> renderTypeGetter) {
        if (this.renderType == null) {
            this.renderType = renderTypeGetter.apply(this.atlasLocation);
        }

        return this.renderType;
    }

    public VertexConsumer buffer(MaterialSet materials, MultiBufferSource bufferSource, Function<Identifier, RenderType> renderTypeGetter) {
        return materials.get(this).wrap(bufferSource.getBuffer(this.renderType(renderTypeGetter)));
    }

    public VertexConsumer buffer(
        MaterialSet materials, MultiBufferSource bufferSource, Function<Identifier, RenderType> renderTypeGetter, boolean isItem, boolean glint
    ) {
        return materials.get(this).wrap(ItemRenderer.getFoilBuffer(bufferSource, this.renderType(renderTypeGetter), isItem, glint));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other != null && this.getClass() == other.getClass()) {
            Material material = (Material)other;
            return this.atlasLocation.equals(material.atlasLocation) && this.texture.equals(material.texture);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.atlasLocation, this.texture);
    }

    @Override
    public String toString() {
        return "Material{atlasLocation=" + this.atlasLocation + ", texture=" + this.texture + "}";
    }
}
