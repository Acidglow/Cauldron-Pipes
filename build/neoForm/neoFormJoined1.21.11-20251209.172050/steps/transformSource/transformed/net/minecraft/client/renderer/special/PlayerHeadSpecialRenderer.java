package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PlayerHeadSpecialRenderer implements SpecialModelRenderer<PlayerSkinRenderCache.RenderInfo> {
    private final PlayerSkinRenderCache playerSkinRenderCache;
    private final SkullModelBase modelBase;

    PlayerHeadSpecialRenderer(PlayerSkinRenderCache playerSkinRenderCache, SkullModelBase modelBase) {
        this.playerSkinRenderCache = playerSkinRenderCache;
        this.modelBase = modelBase;
    }

    public void submit(
        PlayerSkinRenderCache.@Nullable RenderInfo p_451691_,
        ItemDisplayContext p_439036_,
        PoseStack p_439012_,
        SubmitNodeCollector p_439704_,
        int p_439598_,
        int p_440685_,
        boolean p_439388_,
        int p_451687_
    ) {
        RenderType rendertype = p_451691_ != null ? p_451691_.renderType() : PlayerSkinRenderCache.DEFAULT_PLAYER_SKIN_RENDER_TYPE;
        SkullBlockRenderer.submitSkull(null, 180.0F, 0.0F, p_439012_, p_439704_, p_439598_, this.modelBase, rendertype, p_451687_, null);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470817_) {
        PoseStack posestack = new PoseStack();
        posestack.translate(0.5F, 0.0F, 0.5F);
        posestack.scale(-1.0F, -1.0F, 1.0F);
        this.modelBase.root().getExtentsForGui(posestack, p_470817_);
    }

    public PlayerSkinRenderCache.@Nullable RenderInfo extractArgument(ItemStack p_428504_) {
        ResolvableProfile resolvableprofile = p_428504_.get(DataComponents.PROFILE);
        return resolvableprofile == null ? null : this.playerSkinRenderCache.getOrDefault(resolvableprofile);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<PlayerHeadSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(PlayerHeadSpecialRenderer.Unbaked::new);

        @Override
        public MapCodec<PlayerHeadSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public @Nullable SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_433416_) {
            SkullModelBase skullmodelbase = SkullBlockRenderer.createModel(p_433416_.entityModelSet(), SkullBlock.Types.PLAYER);
            return skullmodelbase == null ? null : new PlayerHeadSpecialRenderer(p_433416_.playerSkinRenderCache(), skullmodelbase);
        }
    }
}
