package net.minecraft.client.model.object.skull;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.monster.piglin.PiglinModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PiglinHeadModel extends SkullModelBase {
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;

    public PiglinHeadModel(ModelPart p_478563_) {
        super(p_478563_);
        this.head = p_478563_.getChild("head");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
    }

    public static MeshDefinition createHeadModel() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PiglinModel.addHead(CubeDeformation.NONE, meshdefinition);
        return meshdefinition;
    }

    public void setupAnim(SkullModelBase.State p_481312_) {
        super.setupAnim(p_481312_);
        this.head.yRot = p_481312_.yRot * (float) (Math.PI / 180.0);
        this.head.xRot = p_481312_.xRot * (float) (Math.PI / 180.0);
        float f = 1.2F;
        this.leftEar.zRot = (float)(-(Math.cos(p_481312_.animationPos * (float) Math.PI * 0.2F * 1.2F) + 2.5)) * 0.2F;
        this.rightEar.zRot = (float)(Math.cos(p_481312_.animationPos * (float) Math.PI * 0.2F) + 2.5) * 0.2F;
    }
}
