package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.llama.LlamaModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.layers.LlamaDecorLayer;
import net.minecraft.client.renderer.entity.state.LlamaRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Llama;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LlamaRenderer extends AgeableMobRenderer<Llama, LlamaRenderState, LlamaModel> {
    private static final Identifier CREAMY = Identifier.withDefaultNamespace("textures/entity/llama/creamy.png");
    private static final Identifier WHITE = Identifier.withDefaultNamespace("textures/entity/llama/white.png");
    private static final Identifier BROWN = Identifier.withDefaultNamespace("textures/entity/llama/brown.png");
    private static final Identifier GRAY = Identifier.withDefaultNamespace("textures/entity/llama/gray.png");

    public LlamaRenderer(EntityRendererProvider.Context context, ModelLayerLocation adultModel, ModelLayerLocation babyModel) {
        super(context, new LlamaModel(context.bakeLayer(adultModel)), new LlamaModel(context.bakeLayer(babyModel)), 0.7F);
        this.addLayer(new LlamaDecorLayer(this, context.getModelSet(), context.getEquipmentRenderer()));
    }

    public Identifier getTextureLocation(LlamaRenderState p_469910_) {
        return switch (p_469910_.variant) {
            case CREAMY -> CREAMY;
            case WHITE -> WHITE;
            case BROWN -> BROWN;
            case GRAY -> GRAY;
        };
    }

    public LlamaRenderState createRenderState() {
        return new LlamaRenderState();
    }

    public void extractRenderState(Llama p_480995_, LlamaRenderState p_363082_, float p_361575_) {
        super.extractRenderState(p_480995_, p_363082_, p_361575_);
        p_363082_.variant = p_480995_.getVariant();
        p_363082_.hasChest = !p_480995_.isBaby() && p_480995_.hasChest();
        p_363082_.bodyItem = p_480995_.getBodyArmorItem();
        p_363082_.isTraderLlama = p_480995_.isTraderLlama();
    }
}
