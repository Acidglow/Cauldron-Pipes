package net.minecraft.client.renderer.block.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record SimpleModelWrapper(QuadCollection quads, boolean useAmbientOcclusion, TextureAtlasSprite particleIcon, net.minecraft.client.renderer.chunk.@Nullable ChunkSectionLayer renderType) implements BlockModelPart {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Deprecated // Neo: Use render type aware version
    public SimpleModelWrapper(QuadCollection quads, boolean useAmbientOcclusion, TextureAtlasSprite particleIcon) {
        this(quads, useAmbientOcclusion, particleIcon, null);
    }

    public static BlockModelPart bake(ModelBaker baker, Identifier modelId, ModelState modelState) {
        ResolvedModel resolvedmodel = baker.getModel(modelId);
        return bake(baker, resolvedmodel, modelState);
    }

    // Neo: split off to allow baking an existing ResolvedModel into a BlockModelPart
    public static BlockModelPart bake(ModelBaker p_405335_, ResolvedModel resolvedmodel, ModelState p_405869_) {
        TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
        boolean flag = resolvedmodel.getTopAmbientOcclusion();
        TextureAtlasSprite textureatlassprite = resolvedmodel.resolveParticleSprite(textureslots, p_405335_);
        QuadCollection quadcollection = resolvedmodel.bakeTopGeometry(textureslots, p_405335_, p_405869_);
        Multimap<Identifier, Identifier> multimap = null;

        for (BakedQuad bakedquad : quadcollection.getAll()) {
            TextureAtlasSprite textureatlassprite1 = bakedquad.sprite();
            if (!textureatlassprite1.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
                if (multimap == null) {
                    multimap = HashMultimap.create();
                }

                multimap.put(textureatlassprite1.atlasLocation(), textureatlassprite1.contents().name());
            }
        }

        if (multimap != null) {
            LOGGER.warn("Rejecting block model {}, since it contains sprites from outside of supported atlas: {}", resolvedmodel.debugName(), multimap);
            return p_405335_.missingBlockModelPart();
        } else {
            var renderTypeGroup = resolvedmodel.getTopAdditionalProperties().getOptional(net.neoforged.neoforge.client.model.NeoForgeModelProperties.RENDER_TYPE);
            var renderTypes = renderTypeGroup == null || renderTypeGroup.isEmpty() ? null : renderTypeGroup.block();
            return new SimpleModelWrapper(quadcollection, flag, textureatlassprite, renderTypes);
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable Direction p_405263_) {
        return this.quads.getQuads(p_405263_);
    }

    @Override
    public net.minecraft.client.renderer.chunk.ChunkSectionLayer getRenderType(net.minecraft.world.level.block.state.BlockState state) {
        return this.renderType != null ? this.renderType : BlockModelPart.super.getRenderType(state);
    }
}
