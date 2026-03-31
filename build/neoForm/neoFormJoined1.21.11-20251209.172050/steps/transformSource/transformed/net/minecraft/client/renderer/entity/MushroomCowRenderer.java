package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.MushroomCowMushroomLayer;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MushroomCowRenderer extends AgeableMobRenderer<MushroomCow, MushroomCowRenderState, CowModel> {
    private static final Map<MushroomCow.Variant, Identifier> TEXTURES = Util.make(Maps.newHashMap(), p_477736_ -> {
        p_477736_.put(MushroomCow.Variant.BROWN, Identifier.withDefaultNamespace("textures/entity/cow/brown_mooshroom.png"));
        p_477736_.put(MushroomCow.Variant.RED, Identifier.withDefaultNamespace("textures/entity/cow/red_mooshroom.png"));
    });

    public MushroomCowRenderer(EntityRendererProvider.Context p_174324_) {
        super(p_174324_, new CowModel(p_174324_.bakeLayer(ModelLayers.MOOSHROOM)), new CowModel(p_174324_.bakeLayer(ModelLayers.MOOSHROOM_BABY)), 0.7F);
        this.addLayer(new MushroomCowMushroomLayer(this, p_174324_.getBlockRenderDispatcher()));
    }

    public Identifier getTextureLocation(MushroomCowRenderState p_362880_) {
        return TEXTURES.get(p_362880_.variant);
    }

    public MushroomCowRenderState createRenderState() {
        return new MushroomCowRenderState();
    }

    public void extractRenderState(MushroomCow p_480093_, MushroomCowRenderState p_362285_, float p_363308_) {
        super.extractRenderState(p_480093_, p_362285_, p_363308_);
        p_362285_.variant = p_480093_.getVariant();
    }
}
