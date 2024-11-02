package org.progamer.tame_the_fox.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.progamer.tame_the_fox.CustomFox;
import org.progamer.tame_the_fox.Models.CustomFoxModel;

@OnlyIn(Dist.CLIENT)
public class CustomFoxHeldItemLayer extends RenderLayer<CustomFox, CustomFoxModel<CustomFox>> {
    private final ItemInHandRenderer itemInHandRenderer;

    public CustomFoxHeldItemLayer(RenderLayerParent<CustomFox, CustomFoxModel<CustomFox>> renderLayerParent, ItemInHandRenderer itemRenderer) {
        super(renderLayerParent);
        this.itemInHandRenderer = itemRenderer;
    }

//    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, Fox fox, float fl1, float fl2, float fl3, float fl4, float fl5, float fl6) {
//        boolean flag = fox.isSleeping();
//        boolean flag1 = fox.isBaby();
//        poseStack.pushPose();
//        if (flag1) {
//            float f = 0.75F;
//            poseStack.scale(0.75F, 0.75F, 0.75F);
//            poseStack.translate(0.0F, 0.5F, 0.209375F);
//        }
//
//        poseStack.translate((this.getParentModel()).head.x / 16.0F, (this.getParentModel()).head.y / 16.0F, (this.getParentModel()).head.z / 16.0F);
//        float f1 = fox.getHeadRollAngle(fl3);
//        poseStack.mulPose(Axis.ZP.rotation(f1));
//        poseStack.mulPose(Axis.YP.rotationDegrees(fl5));
//        poseStack.mulPose(Axis.XP.rotationDegrees(fl6));
//        if (fox.isBaby()) {
//            if (flag) {
//                poseStack.translate(0.4F, 0.26F, 0.15F);
//            } else {
//                poseStack.translate(0.06F, 0.26F, -0.5F);
//            }
//        } else if (flag) {
//            poseStack.translate(0.46F, 0.26F, 0.22F);
//        } else {
//            poseStack.translate(0.06F, 0.27F, -0.5F);
//        }
//
//        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
//        if (flag) {
//            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
//        }
//
//        ItemStack itemstack = fox.getItemBySlot(EquipmentSlot.MAINHAND);
//        this.itemInHandRenderer.renderItem(fox, itemstack, ItemDisplayContext.GROUND, false, poseStack, multiBufferSource, i);
//        poseStack.popPose();
//    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CustomFox customFox, float fl1, float fl2, float fl3, float fl4, float fl5, float fl6) {
        boolean flag = customFox.isSleeping();
        boolean flag1 = customFox.isBaby();
        poseStack.pushPose();
        if (flag1) {
            float f = 0.75F;
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.translate(0.0F, 0.5F, 0.209375F);
        }

        poseStack.translate((this.getParentModel()).head.x / 16.0F, (this.getParentModel()).head.y / 16.0F, (this.getParentModel()).head.z / 16.0F);
        float f1 = customFox.getHeadRollAngle(fl3);
        poseStack.mulPose(Axis.ZP.rotation(f1));
        poseStack.mulPose(Axis.YP.rotationDegrees(fl5));
        poseStack.mulPose(Axis.XP.rotationDegrees(fl6));
        if (customFox.isBaby()) {
            if (flag) {
                poseStack.translate(0.4F, 0.26F, 0.15F);
            } else {
                poseStack.translate(0.06F, 0.26F, -0.5F);
            }
        } else if (flag) {
            poseStack.translate(0.46F, 0.26F, 0.22F);
        } else {
            poseStack.translate(0.06F, 0.27F, -0.5F);
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        if (flag) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }

        ItemStack itemstack = customFox.getItemBySlot(EquipmentSlot.MAINHAND);
        this.itemInHandRenderer.renderItem(customFox, itemstack, ItemDisplayContext.GROUND, false, poseStack, multiBufferSource, i);
        poseStack.popPose();
    }
}
