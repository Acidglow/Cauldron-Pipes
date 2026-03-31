package com.mojang.blaze3d.vertex;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public interface VertexConsumer extends net.neoforged.neoforge.client.extensions.IVertexConsumerExtension {
    VertexConsumer addVertex(float x, float y, float z);

    VertexConsumer setColor(int red, int green, int blue, int alpha);

    VertexConsumer setColor(int color);

    VertexConsumer setUv(float u, float v);

    VertexConsumer setUv1(int u, int v);

    VertexConsumer setUv2(int u, int v);

    VertexConsumer setNormal(float normalX, float normalY, float normalZ);

    VertexConsumer setLineWidth(float lineWidth);

    default void addVertex(
        float x,
        float y,
        float z,
        int color,
        float u,
        float v,
        int packedOverlay,
        int packedLight,
        float normalX,
        float normalY,
        float normalZ
    ) {
        this.addVertex(x, y, z);
        this.setColor(color);
        this.setUv(u, v);
        this.setOverlay(packedOverlay);
        this.setLight(packedLight);
        this.setNormal(normalX, normalY, normalZ);
    }

    default VertexConsumer setColor(float red, float green, float blue, float alpha) {
        return this.setColor((int)(red * 255.0F), (int)(green * 255.0F), (int)(blue * 255.0F), (int)(alpha * 255.0F));
    }

    default VertexConsumer setLight(int packedLight) {
        return this.setUv2(packedLight & 65535, packedLight >> 16 & 65535);
    }

    default VertexConsumer setOverlay(int packedOverlay) {
        return this.setUv1(packedOverlay & 65535, packedOverlay >> 16 & 65535);
    }

    default void putBulkData(
        PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay
    ) {
        this.putBulkData(
            pose,
            quad,
            new float[]{1.0F, 1.0F, 1.0F, 1.0F},
            red,
            green,
            blue,
            alpha,
            new int[]{packedLight, packedLight, packedLight, packedLight},
            packedOverlay
        );
    }

    default void putBulkData(
        PoseStack.Pose pose,
        BakedQuad quad,
        float[] brightness,
        float red,
        float green,
        float blue,
        float alpha,
        int[] lightmap,
        int packedOverlay
    ) {
        Vector3fc vector3fc = quad.direction().getUnitVec3f();
        Matrix4f matrix4f = pose.pose();
        Vector3f vector3f = pose.transformNormal(vector3fc, new Vector3f());
        int i = quad.lightEmission();

        for (int j = 0; j < 4; j++) {
            Vector3fc vector3fc1 = quad.position(j);
            long k = quad.packedUV(j);
            float f = brightness[j];
            int l = ARGB.colorFromFloat(alpha, f * red, f * green, f * blue);
            l = ARGB.multiply(l, ARGB.toABGR(quad.bakedColors().color(j))); // Neo: apply baked color from the quad
            int i1 = LightTexture.lightCoordsWithEmission(lightmap[j], i);
            Vector3f vector3f1 = matrix4f.transformPosition(vector3fc1, new Vector3f());
            float f1 = UVPair.unpackU(k);
            float f2 = UVPair.unpackV(k);
            applyBakedNormals(vector3f, quad.bakedNormals(), j, pose.normal()); // Neo: apply baked normals from the quad
            this.addVertex(vector3f1.x(), vector3f1.y(), vector3f1.z(), l, f1, f2, packedOverlay, i1, vector3f.x(), vector3f.y(), vector3f.z());
        }
    }

    default VertexConsumer addVertex(Vector3fc pos) {
        return this.addVertex(pos.x(), pos.y(), pos.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose pose, Vector3f pos) {
        return this.addVertex(pose, pos.x(), pos.y(), pos.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) {
        return this.addVertex(pose.pose(), x, y, z);
    }

    default VertexConsumer addVertex(Matrix4fc pose, float x, float y, float z) {
        Vector3f vector3f = pose.transformPosition(x, y, z, new Vector3f());
        return this.addVertex(vector3f.x(), vector3f.y(), vector3f.z());
    }

    default VertexConsumer addVertexWith2DPose(Matrix3x2fc pose, float x, float y) {
        Vector2f vector2f = pose.transformPosition(x, y, new Vector2f());
        return this.addVertex(vector2f.x(), vector2f.y(), 0.0F);
    }

    default VertexConsumer setNormal(PoseStack.Pose pose, float normalX, float normalY, float normalZ) {
        Vector3f vector3f = pose.transformNormal(normalX, normalY, normalZ, new Vector3f());
        return this.setNormal(vector3f.x(), vector3f.y(), vector3f.z());
    }

    default VertexConsumer setNormal(PoseStack.Pose pose, Vector3f normalVector) {
        return this.setNormal(pose, normalVector.x(), normalVector.y(), normalVector.z());
    }
}
