package net.minecraft.client.renderer.gizmos;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class DrawableGizmoPrimitives implements GizmoPrimitives {
    private final DrawableGizmoPrimitives.Group opaque = new DrawableGizmoPrimitives.Group(true);
    private final DrawableGizmoPrimitives.Group translucent = new DrawableGizmoPrimitives.Group(false);
    private boolean isEmpty = true;

    private DrawableGizmoPrimitives.Group getGroup(int color) {
        return ARGB.alpha(color) < 255 ? this.translucent : this.opaque;
    }

    @Override
    public void addPoint(Vec3 p_455217_, int p_454781_, float p_456157_) {
        this.getGroup(p_454781_).points.add(new DrawableGizmoPrimitives.Point(p_455217_, p_454781_, p_456157_));
        this.isEmpty = false;
    }

    @Override
    public void addLine(Vec3 p_456109_, Vec3 p_455970_, int p_455120_, float p_455368_) {
        this.getGroup(p_455120_).lines.add(new DrawableGizmoPrimitives.Line(p_456109_, p_455970_, p_455120_, p_455368_));
        this.isEmpty = false;
    }

    @Override
    public void addTriangleFan(Vec3[] p_456174_, int p_456047_) {
        this.getGroup(p_456047_).triangleFans.add(new DrawableGizmoPrimitives.TriangleFan(p_456174_, p_456047_));
        this.isEmpty = false;
    }

    @Override
    public void addQuad(Vec3 p_455156_, Vec3 p_455820_, Vec3 p_455725_, Vec3 p_456267_, int p_455041_) {
        this.getGroup(p_455041_).quads.add(new DrawableGizmoPrimitives.Quad(p_455156_, p_455820_, p_455725_, p_456267_, p_455041_));
        this.isEmpty = false;
    }

    @Override
    public void addText(Vec3 p_455439_, String p_455651_, TextGizmo.Style p_455425_) {
        this.getGroup(p_455425_.color()).texts.add(new DrawableGizmoPrimitives.Text(p_455439_, p_455651_, p_455425_));
        this.isEmpty = false;
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, CameraRenderState cameraRenderState, Matrix4f frustumMatrix) {
        this.opaque.render(poseStack, bufferSource, cameraRenderState, frustumMatrix);
        this.translucent.render(poseStack, bufferSource, cameraRenderState, frustumMatrix);
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    @OnlyIn(Dist.CLIENT)
    record Group(
        boolean opaque,
        List<DrawableGizmoPrimitives.Line> lines,
        List<DrawableGizmoPrimitives.Quad> quads,
        List<DrawableGizmoPrimitives.TriangleFan> triangleFans,
        List<DrawableGizmoPrimitives.Text> texts,
        List<DrawableGizmoPrimitives.Point> points
    ) {
        Group(boolean p_470643_) {
            this(p_470643_, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        public void render(PoseStack poseStack, MultiBufferSource bufferSource, CameraRenderState cameraRenderState, Matrix4f frustumMatrix) {
            this.renderQuads(poseStack, bufferSource, cameraRenderState);
            this.renderTriangleFans(poseStack, bufferSource, cameraRenderState);
            this.renderLines(poseStack, bufferSource, cameraRenderState, frustumMatrix);
            this.renderTexts(poseStack, bufferSource, cameraRenderState);
            this.renderPoints(poseStack, bufferSource, cameraRenderState);
        }

        private void renderTexts(PoseStack poseStack, MultiBufferSource bufferSource, CameraRenderState cameraRenderState) {
            Minecraft minecraft = Minecraft.getInstance();
            Font font = minecraft.font;
            if (cameraRenderState.initialized) {
                double d0 = cameraRenderState.pos.x();
                double d1 = cameraRenderState.pos.y();
                double d2 = cameraRenderState.pos.z();

                for (DrawableGizmoPrimitives.Text drawablegizmoprimitives$text : this.texts) {
                    poseStack.pushPose();
                    poseStack.translate(
                        (float)(drawablegizmoprimitives$text.pos().x() - d0),
                        (float)(drawablegizmoprimitives$text.pos().y() - d1),
                        (float)(drawablegizmoprimitives$text.pos().z() - d2)
                    );
                    poseStack.mulPose(cameraRenderState.orientation);
                    poseStack.scale(
                        drawablegizmoprimitives$text.style.scale() / 16.0F,
                        -drawablegizmoprimitives$text.style.scale() / 16.0F,
                        drawablegizmoprimitives$text.style.scale() / 16.0F
                    );
                    float f;
                    if (drawablegizmoprimitives$text.style.adjustLeft().isEmpty()) {
                        f = -font.width(drawablegizmoprimitives$text.text) / 2.0F;
                    } else {
                        f = (float)(-drawablegizmoprimitives$text.style.adjustLeft().getAsDouble()) / drawablegizmoprimitives$text.style.scale();
                    }

                    font.drawInBatch(
                        drawablegizmoprimitives$text.text,
                        f,
                        0.0F,
                        drawablegizmoprimitives$text.style.color(),
                        false,
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0,
                        15728880
                    );
                    poseStack.popPose();
                }
            }
        }

        private void renderLines(PoseStack poseStack, MultiBufferSource bufferSource, CameraRenderState cameraRenderState, Matrix4f frustumMatrix) {
            VertexConsumer vertexconsumer = bufferSource.getBuffer(this.opaque ? RenderTypes.lines() : RenderTypes.linesTranslucent());
            PoseStack.Pose posestack$pose = poseStack.last();
            Vector4f vector4f = new Vector4f();
            Vector4f vector4f1 = new Vector4f();
            Vector4f vector4f2 = new Vector4f();
            Vector4f vector4f3 = new Vector4f();
            Vector4f vector4f4 = new Vector4f();
            double d0 = cameraRenderState.pos.x();
            double d1 = cameraRenderState.pos.y();
            double d2 = cameraRenderState.pos.z();

            for (DrawableGizmoPrimitives.Line drawablegizmoprimitives$line : this.lines) {
                vector4f.set(
                    drawablegizmoprimitives$line.start().x() - d0,
                    drawablegizmoprimitives$line.start().y() - d1,
                    drawablegizmoprimitives$line.start().z() - d2,
                    1.0
                );
                vector4f1.set(
                    drawablegizmoprimitives$line.end().x() - d0, drawablegizmoprimitives$line.end().y() - d1, drawablegizmoprimitives$line.end().z() - d2, 1.0
                );
                vector4f.mul(frustumMatrix, vector4f2);
                vector4f1.mul(frustumMatrix, vector4f3);
                boolean flag = vector4f2.z > -0.05F;
                boolean flag1 = vector4f3.z > -0.05F;
                if (!flag || !flag1) {
                    if (flag || flag1) {
                        float f = vector4f3.z - vector4f2.z;
                        if (Math.abs(f) < 1.0E-9F) {
                            continue;
                        }

                        float f1 = Mth.clamp((-0.05F - vector4f2.z) / f, 0.0F, 1.0F);
                        vector4f.lerp(vector4f1, f1, vector4f4);
                        if (flag) {
                            vector4f.set(vector4f4);
                        } else {
                            vector4f1.set(vector4f4);
                        }
                    }

                    vertexconsumer.addVertex(posestack$pose, vector4f.x, vector4f.y, vector4f.z)
                        .setNormal(posestack$pose, vector4f1.x - vector4f.x, vector4f1.y - vector4f.y, vector4f1.z - vector4f.z)
                        .setColor(drawablegizmoprimitives$line.color())
                        .setLineWidth(drawablegizmoprimitives$line.width());
                    vertexconsumer.addVertex(posestack$pose, vector4f1.x, vector4f1.y, vector4f1.z)
                        .setNormal(posestack$pose, vector4f1.x - vector4f.x, vector4f1.y - vector4f.y, vector4f1.z - vector4f.z)
                        .setColor(drawablegizmoprimitives$line.color())
                        .setLineWidth(drawablegizmoprimitives$line.width());
                }
            }
        }

        private void renderTriangleFans(PoseStack poseStack, MultiBufferSource bufferSource, CameraRenderState cameraRenderState) {
            PoseStack.Pose posestack$pose = poseStack.last();
            double d0 = cameraRenderState.pos.x();
            double d1 = cameraRenderState.pos.y();
            double d2 = cameraRenderState.pos.z();

            for (DrawableGizmoPrimitives.TriangleFan drawablegizmoprimitives$trianglefan : this.triangleFans) {
                VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderTypes.debugTriangleFan());

                for (Vec3 vec3 : drawablegizmoprimitives$trianglefan.points()) {
                    vertexconsumer.addVertex(posestack$pose, (float)(vec3.x() - d0), (float)(vec3.y() - d1), (float)(vec3.z() - d2))
                        .setColor(drawablegizmoprimitives$trianglefan.color());
                }
            }
        }

        private void renderQuads(PoseStack poseStack, MultiBufferSource bufferSource, CameraRenderState cameraRenderState) {
            VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderTypes.debugFilledBox());
            PoseStack.Pose posestack$pose = poseStack.last();
            double d0 = cameraRenderState.pos.x();
            double d1 = cameraRenderState.pos.y();
            double d2 = cameraRenderState.pos.z();

            for (DrawableGizmoPrimitives.Quad drawablegizmoprimitives$quad : this.quads) {
                vertexconsumer.addVertex(
                        posestack$pose,
                        (float)(drawablegizmoprimitives$quad.a().x() - d0),
                        (float)(drawablegizmoprimitives$quad.a().y() - d1),
                        (float)(drawablegizmoprimitives$quad.a().z() - d2)
                    )
                    .setColor(drawablegizmoprimitives$quad.color());
                vertexconsumer.addVertex(
                        posestack$pose,
                        (float)(drawablegizmoprimitives$quad.b().x() - d0),
                        (float)(drawablegizmoprimitives$quad.b().y() - d1),
                        (float)(drawablegizmoprimitives$quad.b().z() - d2)
                    )
                    .setColor(drawablegizmoprimitives$quad.color());
                vertexconsumer.addVertex(
                        posestack$pose,
                        (float)(drawablegizmoprimitives$quad.c().x() - d0),
                        (float)(drawablegizmoprimitives$quad.c().y() - d1),
                        (float)(drawablegizmoprimitives$quad.c().z() - d2)
                    )
                    .setColor(drawablegizmoprimitives$quad.color());
                vertexconsumer.addVertex(
                        posestack$pose,
                        (float)(drawablegizmoprimitives$quad.d().x() - d0),
                        (float)(drawablegizmoprimitives$quad.d().y() - d1),
                        (float)(drawablegizmoprimitives$quad.d().z() - d2)
                    )
                    .setColor(drawablegizmoprimitives$quad.color());
            }
        }

        private void renderPoints(PoseStack poseStack, MultiBufferSource bufferSource, CameraRenderState cameraRenderState) {
            VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderTypes.debugPoint());
            PoseStack.Pose posestack$pose = poseStack.last();
            double d0 = cameraRenderState.pos.x();
            double d1 = cameraRenderState.pos.y();
            double d2 = cameraRenderState.pos.z();

            for (DrawableGizmoPrimitives.Point drawablegizmoprimitives$point : this.points) {
                vertexconsumer.addVertex(
                        posestack$pose,
                        (float)(drawablegizmoprimitives$point.pos.x() - d0),
                        (float)(drawablegizmoprimitives$point.pos.y() - d1),
                        (float)(drawablegizmoprimitives$point.pos.z() - d2)
                    )
                    .setColor(drawablegizmoprimitives$point.color())
                    .setLineWidth(drawablegizmoprimitives$point.size());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    record Line(Vec3 start, Vec3 end, int color, float width) {
    }

    @OnlyIn(Dist.CLIENT)
    record Point(Vec3 pos, int color, float size) {
    }

    @OnlyIn(Dist.CLIENT)
    record Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
    }

    @OnlyIn(Dist.CLIENT)
    record Text(Vec3 pos, String text, TextGizmo.Style style) {
    }

    @OnlyIn(Dist.CLIENT)
    record TriangleFan(Vec3[] points, int color) {
    }
}
