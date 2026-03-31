package net.minecraft.client.renderer.block.model;

import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record SimpleUnbakedGeometry(List<BlockElement> elements) implements UnbakedGeometry, net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry {
    @Override
    public QuadCollection bake(TextureSlots p_405009_, ModelBaker p_404737_, ModelState p_404670_, ModelDebugName p_404742_, net.minecraft.util.context.ContextMap additionalProperties) {
        var transform = additionalProperties.getOptional(net.neoforged.neoforge.client.model.NeoForgeModelProperties.TRANSFORM);
        if (transform != null) {
            p_404670_ = net.neoforged.neoforge.client.model.UnbakedElementsHelper.composeRootTransformIntoModelState(p_404670_, transform);
        }
        return bake(this.elements, p_405009_, p_404737_, p_404670_, p_404742_);
    }

    public static QuadCollection bake(
        List<BlockElement> elements, TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName debugName
    ) {
        QuadCollection.Builder quadcollection$builder = new QuadCollection.Builder();

        for (BlockElement blockelement : elements) {
            boolean flag = true;
            boolean flag1 = true;
            boolean flag2 = true;
            Vector3fc vector3fc = blockelement.from();
            Vector3fc vector3fc1 = blockelement.to();
            if (vector3fc.x() == vector3fc1.x()) {
                flag1 = false;
                flag2 = false;
            }

            if (vector3fc.y() == vector3fc1.y()) {
                flag = false;
                flag2 = false;
            }

            if (vector3fc.z() == vector3fc1.z()) {
                flag = false;
                flag1 = false;
            }

            if (flag || flag1 || flag2) {
                for (Entry<Direction, BlockElementFace> entry : blockelement.faces().entrySet()) {
                    Direction direction = entry.getKey();
                    BlockElementFace blockelementface = entry.getValue();

                    boolean flag3 = switch (direction.getAxis()) {
                        case X -> flag;
                        case Y -> flag1;
                        case Z -> flag2;
                    };
                    if (flag3) {
                        TextureAtlasSprite textureatlassprite = baker.sprites().resolveSlot(textureSlots, blockelementface.texture(), debugName);
                        BakedQuad bakedquad = FaceBakery.bakeQuad(
                            baker.parts(),
                            vector3fc,
                            vector3fc1,
                            blockelementface,
                            textureatlassprite,
                            direction,
                            modelState,
                            blockelement.rotation(),
                            blockelement.shade(),
                            blockelement.lightEmission()
                        );
                        if (blockelementface.cullForDirection() == null) {
                            quadcollection$builder.addUnculledFace(bakedquad);
                        } else {
                            quadcollection$builder.addCulledFace(
                                Direction.rotate(modelState.transformation().getMatrix(), blockelementface.cullForDirection()), bakedquad
                            );
                        }
                    }
                }
            }
        }

        return quadcollection$builder.build();
    }
}
