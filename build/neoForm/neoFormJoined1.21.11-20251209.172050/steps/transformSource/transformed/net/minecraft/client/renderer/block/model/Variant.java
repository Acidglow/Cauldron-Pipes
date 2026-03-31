package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record Variant(Identifier modelLocation, Variant.SimpleModelState modelState) implements BlockModelPart.Unbaked {
    public static final MapCodec<Variant> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_465619_ -> p_465619_.group(
                Identifier.CODEC.fieldOf("model").forGetter(Variant::modelLocation), Variant.SimpleModelState.MAP_CODEC.forGetter(Variant::modelState)
            )
            .apply(p_465619_, Variant::new)
    );
    public static final Codec<Variant> CODEC = MAP_CODEC.codec();

    public Variant(Identifier p_468720_) {
        this(p_468720_, Variant.SimpleModelState.DEFAULT);
    }

    public Variant withXRot(Quadrant xRot) {
        return this.withState(this.modelState.withX(xRot));
    }

    public Variant withYRot(Quadrant yRot) {
        return this.withState(this.modelState.withY(yRot));
    }

    public Variant withZRot(Quadrant zRot) {
        return this.withState(this.modelState.withZ(zRot));
    }

    public Variant withUvLock(boolean uvLock) {
        return this.withState(this.modelState.withUvLock(uvLock));
    }

    public Variant withModel(Identifier modelLocation) {
        return new Variant(modelLocation, this.modelState);
    }

    public Variant withState(Variant.SimpleModelState modelState) {
        return new Variant(this.modelLocation, modelState);
    }

    public Variant with(VariantMutator mutator) {
        return mutator.apply(this);
    }

    @Override
    public BlockModelPart bake(ModelBaker p_410762_) {
        return SimpleModelWrapper.bake(p_410762_, this.modelLocation, this.modelState.asModelState());
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver p_410425_) {
        p_410425_.markDependency(this.modelLocation);
    }

    @OnlyIn(Dist.CLIENT)
    public record SimpleModelState(Quadrant x, Quadrant y, Quadrant z, boolean uvLock) {
        public static final MapCodec<Variant.SimpleModelState> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_470510_ -> p_470510_.group(
                    Quadrant.CODEC.optionalFieldOf("x", Quadrant.R0).forGetter(Variant.SimpleModelState::x),
                    Quadrant.CODEC.optionalFieldOf("y", Quadrant.R0).forGetter(Variant.SimpleModelState::y),
                    Quadrant.CODEC.optionalFieldOf("z", Quadrant.R0).forGetter(Variant.SimpleModelState::z),
                    Codec.BOOL.optionalFieldOf("uvlock", false).forGetter(Variant.SimpleModelState::uvLock)
                )
                .apply(p_470510_, Variant.SimpleModelState::new)
        );
        public static final Variant.SimpleModelState DEFAULT = new Variant.SimpleModelState(Quadrant.R0, Quadrant.R0, Quadrant.R0, false);

        public ModelState asModelState() {
            BlockModelRotation blockmodelrotation = BlockModelRotation.get(Quadrant.fromXYZAngles(this.x, this.y, this.z));
            return (ModelState)(this.uvLock ? blockmodelrotation.withUvLock() : blockmodelrotation);
        }

        public Variant.SimpleModelState withX(Quadrant xRot) {
            return new Variant.SimpleModelState(xRot, this.y, this.z, this.uvLock);
        }

        public Variant.SimpleModelState withY(Quadrant yRot) {
            return new Variant.SimpleModelState(this.x, yRot, this.z, this.uvLock);
        }

        public Variant.SimpleModelState withZ(Quadrant zRot) {
            return new Variant.SimpleModelState(this.x, this.y, zRot, this.uvLock);
        }

        public Variant.SimpleModelState withUvLock(boolean uvLock) {
            return new Variant.SimpleModelState(this.x, this.y, this.z, uvLock);
        }
    }
}
