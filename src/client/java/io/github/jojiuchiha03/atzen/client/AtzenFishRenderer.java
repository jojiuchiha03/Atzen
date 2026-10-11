package io.github.jojiuchiha03.atzen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.fish.AtzenFishEntity;
import io.github.jojiuchiha03.atzen.fish.AtzenFishRegistry;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class AtzenFishRenderer extends MobRenderer<AtzenFishEntity, LivingEntityRenderState, AtzenFishModel> {
    private final Identifier texture;

    private AtzenFishRenderer(EntityRendererProvider.Context context, FishSpecies species, ModelLayerLocation layer) {
        super(context, new AtzenFishModel(context.bakeLayer(layer)), 0.2f);
        texture = Atzen.id("textures/entity/fish/" + species.id() + ".png");
    }

    public static void register() {
        for (FishSpecies species : FishSpecies.values()) {
            var layer = new ModelLayerLocation(Atzen.id(species.id()), "main");
            ModelLayerRegistry.registerModelLayer(layer, () -> AtzenFishModel.layer(species));
            EntityRendererRegistry.register(AtzenFishRegistry.entity(species), context -> new AtzenFishRenderer(context, species, layer));
        }
    }

    @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return texture; }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }

    @Override protected void setupRotations(LivingEntityRenderState state, PoseStack poses, float rotation, float scale) {
        super.setupRotations(state, poses, rotation, scale);
        poses.mulPose(Axis.YP.rotationDegrees(4.3f * Mth.sin(0.6f * state.ageInTicks)));
        if (!state.isInWater) {
            poses.translate(0.1f, 0.1f, -0.1f);
            poses.mulPose(Axis.ZP.rotationDegrees(90));
        }
    }
}
