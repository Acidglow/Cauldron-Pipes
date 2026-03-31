package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.shulker.ShulkerModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShulkerRenderer extends MobRenderer<Shulker, ShulkerRenderState, ShulkerModel> {
    private static final Identifier DEFAULT_TEXTURE_LOCATION = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION
        .texture()
        .withPath(p_349906_ -> "textures/" + p_349906_ + ".png");
    private static final Identifier[] TEXTURE_LOCATION = Sheets.SHULKER_TEXTURE_LOCATION
        .stream()
        .map(p_465643_ -> p_465643_.texture().withPath(p_349905_ -> "textures/" + p_349905_ + ".png"))
        .toArray(Identifier[]::new);

    public ShulkerRenderer(EntityRendererProvider.Context p_174370_) {
        super(p_174370_, new ShulkerModel(p_174370_.bakeLayer(ModelLayers.SHULKER)), 0.0F);
    }

    public Vec3 getRenderOffset(ShulkerRenderState p_360997_) {
        return p_360997_.renderOffset;
    }

    public boolean shouldRender(Shulker livingEntity, Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntity, camera, camX, camY, camZ)) {
            return true;
        } else {
            Vec3 vec3 = livingEntity.getRenderPosition(0.0F);
            if (vec3 == null) {
                return false;
            } else {
                EntityType<?> entitytype = livingEntity.getType();
                float f = entitytype.getHeight() / 2.0F;
                float f1 = entitytype.getWidth() / 2.0F;
                Vec3 vec31 = Vec3.atBottomCenterOf(livingEntity.blockPosition());
                return camera.isVisible(new AABB(vec3.x, vec3.y + f, vec3.z, vec31.x, vec31.y + f, vec31.z).inflate(f1, f, f1));
            }
        }
    }

    public Identifier getTextureLocation(ShulkerRenderState p_469915_) {
        return getTextureLocation(p_469915_.color);
    }

    public ShulkerRenderState createRenderState() {
        return new ShulkerRenderState();
    }

    public void extractRenderState(Shulker p_365400_, ShulkerRenderState p_361284_, float p_360863_) {
        super.extractRenderState(p_365400_, p_361284_, p_360863_);
        p_361284_.renderOffset = Objects.requireNonNullElse(p_365400_.getRenderPosition(p_360863_), Vec3.ZERO);
        p_361284_.color = p_365400_.getColor();
        p_361284_.peekAmount = p_365400_.getClientPeekAmount(p_360863_);
        p_361284_.yHeadRot = p_365400_.yHeadRot;
        p_361284_.yBodyRot = p_365400_.yBodyRot;
        p_361284_.attachFace = p_365400_.getAttachFace();
    }

    public static Identifier getTextureLocation(@Nullable DyeColor color) {
        return color == null ? DEFAULT_TEXTURE_LOCATION : TEXTURE_LOCATION[color.getId()];
    }

    protected void setupRotations(ShulkerRenderState p_364147_, PoseStack p_115908_, float p_115909_, float p_115910_) {
        super.setupRotations(p_364147_, p_115908_, p_115909_ + 180.0F, p_115910_);
        p_115908_.rotateAround(p_364147_.attachFace.getOpposite().getRotation(), 0.0F, 0.5F, 0.0F);
    }
}
