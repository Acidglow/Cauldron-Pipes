package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ItemModelGenerator implements UnbakedModel {
    public static final Identifier GENERATED_ITEM_MODEL_ID = Identifier.withDefaultNamespace("builtin/generated");
    public static final List<String> LAYERS = List.of("layer0", "layer1", "layer2", "layer3", "layer4");
    private static final float MIN_Z = 7.5F;
    private static final float MAX_Z = 8.5F;
    private static final TextureSlots.Data TEXTURE_SLOTS = new TextureSlots.Data.Builder().addReference("particle", "layer0").build();
    private static final BlockElementFace.UVs SOUTH_FACE_UVS = new BlockElementFace.UVs(0.0F, 0.0F, 16.0F, 16.0F);
    private static final BlockElementFace.UVs NORTH_FACE_UVS = new BlockElementFace.UVs(16.0F, 0.0F, 0.0F, 16.0F);
    private static final float UV_SHRINK = 0.1F;

    @Override
    public TextureSlots.Data textureSlots() {
        return TEXTURE_SLOTS;
    }

    @Override
    public UnbakedGeometry geometry() {
        return ItemModelGenerator::bake;
    }

    @Override
    public UnbakedModel.@Nullable GuiLight guiLight() {
        return UnbakedModel.GuiLight.FRONT;
    }

    private static QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName debugName) {
        List<BlockElement> list = new ArrayList<>();

        for (int i = 0; i < LAYERS.size(); i++) {
            String s = LAYERS.get(i);
            Material material = textureSlots.getMaterial(s);
            if (material == null) {
                break;
            }

            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = baker.sprites().get(material, debugName);
            list.addAll(processFrames(i, s, sprite.contents()));
        }

        return SimpleUnbakedGeometry.bake(list, textureSlots, baker, modelState, debugName);
    }

    public static List<BlockElement> processFrames(int tintIndex, String texture, SpriteContents sprite) {
        Map<Direction, BlockElementFace> map = Map.of(
            Direction.SOUTH,
            new BlockElementFace(null, tintIndex, texture, SOUTH_FACE_UVS, Quadrant.R0),
            Direction.NORTH,
            new BlockElementFace(null, tintIndex, texture, NORTH_FACE_UVS, Quadrant.R0)
        );
        List<BlockElement> list = new ArrayList<>();
        list.add(new BlockElement(new Vector3f(0.0F, 0.0F, 7.5F), new Vector3f(16.0F, 16.0F, 8.5F), map));
        list.addAll(createSideElements(sprite, texture, tintIndex));
        return list;
    }

    private static List<BlockElement> createSideElements(SpriteContents sprite, String texture, int tintIndex) {
        float f = 16.0F / sprite.width();
        float f1 = 16.0F / sprite.height();
        List<BlockElement> list = new ArrayList<>();

        for (ItemModelGenerator.SideFace itemmodelgenerator$sideface : getSideFaces(sprite)) {
            float f2 = itemmodelgenerator$sideface.x();
            float f3 = itemmodelgenerator$sideface.y();
            ItemModelGenerator.SideDirection itemmodelgenerator$sidedirection = itemmodelgenerator$sideface.facing();
            float f4 = f2 + 0.1F;
            float f5 = f2 + 1.0F - 0.1F;
            float f6;
            float f7;
            if (itemmodelgenerator$sidedirection.isHorizontal()) {
                f6 = f3 + 0.1F;
                f7 = f3 + 1.0F - 0.1F;
            } else {
                f6 = f3 + 1.0F - 0.1F;
                f7 = f3 + 0.1F;
            }

            float f8 = f2;
            float f9 = f3;
            float f10 = f2;
            float f11 = f3;
            switch (itemmodelgenerator$sidedirection) {
                case UP:
                    f10 = f2 + 1.0F;
                    break;
                case DOWN:
                    f10 = f2 + 1.0F;
                    f9 = f3 + 1.0F;
                    f11 = f3 + 1.0F;
                    break;
                case LEFT:
                    f11 = f3 + 1.0F;
                    break;
                case RIGHT:
                    f8 = f2 + 1.0F;
                    f10 = f2 + 1.0F;
                    f11 = f3 + 1.0F;
            }

            f8 *= f;
            f10 *= f;
            f9 *= f1;
            f11 *= f1;
            f9 = 16.0F - f9;
            f11 = 16.0F - f11;
            Map<Direction, BlockElementFace> map = Map.of(
                itemmodelgenerator$sidedirection.getDirection(),
                new BlockElementFace(null, tintIndex, texture, new BlockElementFace.UVs(f4 * f, f6 * f, f5 * f1, f7 * f1), Quadrant.R0)
            );
            switch (itemmodelgenerator$sidedirection) {
                case UP:
                    list.add(new BlockElement(new Vector3f(f8, f9, 7.5F), new Vector3f(f10, f9, 8.5F), map));
                    break;
                case DOWN:
                    list.add(new BlockElement(new Vector3f(f8, f11, 7.5F), new Vector3f(f10, f11, 8.5F), map));
                    break;
                case LEFT:
                    list.add(new BlockElement(new Vector3f(f8, f9, 7.5F), new Vector3f(f8, f11, 8.5F), map));
                    break;
                case RIGHT:
                    list.add(new BlockElement(new Vector3f(f10, f9, 7.5F), new Vector3f(f10, f11, 8.5F), map));
            }
        }

        return list;
    }

    private static Collection<ItemModelGenerator.SideFace> getSideFaces(SpriteContents sprite) {
        int i = sprite.width();
        int j = sprite.height();
        Set<ItemModelGenerator.SideFace> set = new HashSet<>();
        sprite.getUniqueFrames().forEach(p_482388_ -> {
            for (int k = 0; k < j; k++) {
                for (int l = 0; l < i; l++) {
                    boolean flag = !isTransparent(sprite, p_482388_, l, k, i, j);
                    if (flag) {
                        checkTransition(ItemModelGenerator.SideDirection.UP, set, sprite, p_482388_, l, k, i, j);
                        checkTransition(ItemModelGenerator.SideDirection.DOWN, set, sprite, p_482388_, l, k, i, j);
                        checkTransition(ItemModelGenerator.SideDirection.LEFT, set, sprite, p_482388_, l, k, i, j);
                        checkTransition(ItemModelGenerator.SideDirection.RIGHT, set, sprite, p_482388_, l, k, i, j);
                    }
                }
            }
        });
        return set;
    }

    private static void checkTransition(
        ItemModelGenerator.SideDirection sideDirection,
        Set<ItemModelGenerator.SideFace> output,
        SpriteContents sprite,
        int frame,
        int x,
        int y,
        int width,
        int height
    ) {
        if (isTransparent(sprite, frame, x - sideDirection.direction.getStepX(), y - sideDirection.direction.getStepY(), width, height)) {
            output.add(new ItemModelGenerator.SideFace(sideDirection, x, y));
        }
    }

    private static boolean isTransparent(SpriteContents sprite, int frameIndex, int pixelX, int pixelY, int spriteWidth, int spriteHeight) {
        return pixelX >= 0 && pixelY >= 0 && pixelX < spriteWidth && pixelY < spriteHeight
            ? sprite.isTransparent(frameIndex, pixelX, pixelY)
            : true;
    }

    @OnlyIn(Dist.CLIENT)
    static enum SideDirection {
        UP(Direction.UP),
        DOWN(Direction.DOWN),
        LEFT(Direction.EAST),
        RIGHT(Direction.WEST);

        final Direction direction;

        private SideDirection(Direction direction) {
            this.direction = direction;
        }

        public Direction getDirection() {
            return this.direction;
        }

        boolean isHorizontal() {
            return this == DOWN || this == UP;
        }
    }

    @OnlyIn(Dist.CLIENT)
    record SideFace(ItemModelGenerator.SideDirection facing, int x, int y) {
    }
}
