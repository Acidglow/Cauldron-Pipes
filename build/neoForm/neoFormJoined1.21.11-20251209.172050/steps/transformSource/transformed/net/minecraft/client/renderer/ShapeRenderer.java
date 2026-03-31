package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ShapeRenderer {
    public static void renderShape(
        PoseStack poseStack,
        VertexConsumer consumer,
        VoxelShape shape,
        double dx,
        double dy,
        double dz,
        int color,
        float lineWidth
    ) {
        PoseStack.Pose posestack$pose = poseStack.last();
        shape.forAllEdges(
            (p_454255_, p_454256_, p_454257_, p_454258_, p_454259_, p_454260_) -> {
                Vector3f vector3f = new Vector3f((float)(p_454258_ - p_454255_), (float)(p_454259_ - p_454256_), (float)(p_454260_ - p_454257_)).normalize();
                consumer.addVertex(posestack$pose, (float)(p_454255_ + dx), (float)(p_454256_ + dy), (float)(p_454257_ + dz))
                    .setColor(color)
                    .setNormal(posestack$pose, vector3f)
                    .setLineWidth(lineWidth);
                consumer.addVertex(posestack$pose, (float)(p_454258_ + dx), (float)(p_454259_ + dy), (float)(p_454260_ + dz))
                    .setColor(color)
                    .setNormal(posestack$pose, vector3f)
                    .setLineWidth(lineWidth);
            }
        );
    }
}
