package org.progamer.tame_the_fox.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.progamer.tame_the_fox.CustomFox;
import org.progamer.tame_the_fox.Models.CustomFoxModel;
import org.progamer.tame_the_fox.Tame_the_fox;

@OnlyIn(Dist.CLIENT)
public class CustomFoxRenderer extends MobRenderer<CustomFox, CustomFoxModel<CustomFox>> {
    private static final ResourceLocation RED_CUSTOM_FOX_TEXTURE = new ResourceLocation(Tame_the_fox.MODID, "textures/entity/custom_fox/custom_fox.png");
    private static final ResourceLocation RED_CUSTOM_FOX_SLEEP_TEXTURE = new ResourceLocation(Tame_the_fox.MODID, "textures/entity/custom_fox/custom_fox_sleep.png");
    private static final ResourceLocation SNOW_CUSTOM_FOX_TEXTURE = new ResourceLocation(Tame_the_fox.MODID, "textures/entity/custom_fox/custom_snow_fox.png");
    private static final ResourceLocation SNOW_CUSTOM_FOX_SLEEP_TEXTURE = new ResourceLocation(Tame_the_fox.MODID, "textures/entity/custom_fox/custom_snow_fox_sleep.png");

    public CustomFoxRenderer(EntityRendererProvider.Context p_174127_) {
        super(p_174127_, new CustomFoxModel<>(p_174127_.bakeLayer(CustomFoxModel.LAYER_LOCATION)), 0.4F);
        this.addLayer(new CustomFoxHeldItemLayer(this, p_174127_.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(CustomFox p_114482_) {
        if (p_114482_.getVariant() == CustomFox.Type.RED) {
            return p_114482_.isSleeping() ? RED_CUSTOM_FOX_SLEEP_TEXTURE : RED_CUSTOM_FOX_TEXTURE;
        } else {
            return p_114482_.isSleeping() ? SNOW_CUSTOM_FOX_SLEEP_TEXTURE : SNOW_CUSTOM_FOX_TEXTURE;
        }
    }

    protected void setupRotations(CustomFox p_114738_, PoseStack p_114739_, float p_114740_, float p_114741_, float p_114742_) {
        super.setupRotations(p_114738_, p_114739_, p_114740_, p_114741_, p_114742_);
        if (p_114738_.isPouncing() || p_114738_.isFaceplanted()) {
            float f = -Mth.lerp(p_114742_, p_114738_.xRotO, p_114738_.getXRot());
            p_114739_.mulPose(Axis.XP.rotationDegrees(f));
        }

    }
}
