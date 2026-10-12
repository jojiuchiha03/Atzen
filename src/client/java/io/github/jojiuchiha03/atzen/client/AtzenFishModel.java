package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** Species-specific geometry; opaque skin, fin and eye swatches in a 64x64 atlas. */
public final class AtzenFishModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart tail, leftFin, rightFin;
    private final boolean flat;

    public AtzenFishModel(ModelPart root, FishSpecies species) {
        super(root);
        flat = species.flat();
        tail = root.getChild("tail");
        leftFin = root.getChild("left_fin");
        rightFin = root.getChild("right_fin");
    }

    public static LayerDefinition layer(FishSpecies species) {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();
        float width = species.modelWidth();
        float height = species.modelHeight();
        float length = species.modelBodyLength();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-width / 2, -height / 2, 0, width, height, length), PartPose.offset(0, 22, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-width / 2, -height / 2, -3, width, height, 3), PartPose.offset(0, 22, 0));
        float nose = species.modelNoseLength();
        if (nose > 4) {
            // the swordfish's sword: long and thin
            root.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(0, 32)
                    .addBox(-0.35f, -0.35f, -nose, 0.7f, 0.7f, nose), PartPose.offset(0, 22, -3));
        } else {
            root.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(0, 32)
                    .addBox(-width / 2, -height / 3, -nose, width, height * 0.6f, nose), PartPose.offset(0, 22, -3));
        }
        boolean flat = species.flat();
        // flatfish lie on their side, so their tail is a horizontal fan
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 49)
                .addBox(flat ? -2 : 0, flat ? 0 : -2, 0, flat ? 4 : 0, flat ? 0 : 4, species.modelTailLength()),
                PartPose.offset(0, 22, length));
        root.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(16, 50)
                .addBox(0, 0, -1, 2.5f, 0, 2), PartPose.offsetAndRotation(width / 2, 23, 1, 0, 0, 0.5f));
        root.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(16, 50)
                .addBox(-2.5f, 0, -1, 2.5f, 0, 2), PartPose.offsetAndRotation(-width / 2, 23, 1, 0, 0, -0.5f));
        if (flat) {
            // a fringe of fin all along both edges instead of a dorsal fin
            for (int side : new int[]{-1, 1}) {
                root.addOrReplaceChild("fringe_" + side, CubeListBuilder.create().texOffs(28, 50)
                        .addBox(side < 0 ? -1 : 0, 0, 0, 1, 0, length), PartPose.offset(side * width / 2, 22, 0));
            }
            // both eyes on the upper side
            for (int side : new int[]{-1, 1}) {
                root.addOrReplaceChild("eye_" + side, CubeListBuilder.create().texOffs(55, 60)
                        .addBox(-0.4f, -0.4f, -0.4f, 0.8f, 0.8f, 0.8f), PartPose.offset(side * 0.6f, 22 - height / 2 - 0.3f, -1.6f));
                root.addOrReplaceChild("pupil_" + side, CubeListBuilder.create().texOffs(61, 60)
                        .addBox(-0.23f, -0.23f, -0.23f, 0.46f, 0.46f, 0.46f), PartPose.offset(side * 0.6f, 22 - height / 2 - 0.6f, -1.65f));
            }
        } else {
            root.addOrReplaceChild("dorsal", CubeListBuilder.create().texOffs(28, 50)
                    .addBox(0, -1.8f, 0, 0, 1.8f, 4), PartPose.offset(0, 22 - height / 2, 1));
            for (int side : new int[]{-1, 1}) {
                root.addOrReplaceChild("eye_" + side, CubeListBuilder.create().texOffs(55, 60)
                        .addBox(-0.4f, -0.4f, -0.4f, 0.8f, 0.8f, 0.8f), PartPose.offset(side * width / 2, 21.5f, -2));
                root.addOrReplaceChild("pupil_" + side, CubeListBuilder.create().texOffs(61, 60)
                        .addBox(-0.23f, -0.23f, -0.23f, 0.46f, 0.46f, 0.46f), PartPose.offset(side * (width / 2 + 0.3f), 21.5f, -2.05f));
            }
        }
        if (species.hasBarbels()) {
            for (int side : new int[]{-1, 1}) {
                root.addOrReplaceChild("barbel_" + side, CubeListBuilder.create().texOffs(32, 50)
                        .addBox(-0.15f, 0, -3.5f, 0.3f, 0.3f, 3.5f),
                        PartPose.offsetAndRotation(side * width / 2, 22.5f, -2.5f, 0.2f, side * -0.6f, 0));
            }
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float beat = Mth.sin(state.ageInTicks * (state.isInWater ? 0.6f : 1.0f));
        // a flatfish beats its tail up and down, every other fish from side to side
        if (flat) tail.xRot = beat * (state.isInWater ? 0.35f : 0.6f);
        else tail.yRot = beat * (state.isInWater ? 0.45f : 0.7f);
        leftFin.yRot = beat * 0.18f;
        rightFin.yRot = -beat * 0.18f;
    }
}
