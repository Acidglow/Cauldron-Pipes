package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public interface ModelBaker extends net.neoforged.neoforge.client.extensions.ModelBakerExtension {
    ResolvedModel getModel(Identifier modelLocation);

    BlockModelPart missingBlockModelPart();

    SpriteGetter sprites();

    ModelBaker.PartCache parts();

    <T> T compute(ModelBaker.SharedOperationKey<T> key);

    @OnlyIn(Dist.CLIENT)
    public interface PartCache extends net.neoforged.neoforge.client.extensions.ModelBakerPartCacheExtension {
        default Vector3fc vector(float x, float y, float z) {
            return this.vector(new Vector3f(x, y, z));
        }

        Vector3fc vector(Vector3fc vector);
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface SharedOperationKey<T> {
        T compute(ModelBaker baker);
    }
}
