package fdk123.myinfinitecreation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import fdk123.myinfinitecreation.twilight.CastleKeeperTrophy;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CastleKeeperTrophyItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation BACKING_MODEL = new ModelResourceLocation(
            new ResourceLocation("twilightforest", "trophy"), "inventory");

    public CastleKeeperTrophyItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // Twilight Forest uses this filled gold trophy sprite behind its rotating GUI models.
        if (context == ItemDisplayContext.GUI) {
            renderBacking(poseStack);
        }

        poseStack.pushPose();
        if (context == ItemDisplayContext.HEAD) {
            // The item model already applies Twilight Forest's head-slot transform here.
            // Keep the face still and center the crown above the player's head.
            poseStack.translate(0.5, 0.4, 0.5);
            poseStack.scale(0.9F, 0.9F, 0.9F);
            poseStack.translate(-0.5, -0.5, -0.5);
        } else {
            poseStack.translate(0.5, 0.36, 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            float angle = (Util.getMillis() % 8000L) * 360.0F / 8000.0F;
            poseStack.mulPose(Axis.YN.rotationDegrees(angle));
            poseStack.translate(-0.5, -0.25, -0.5);
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                CastleKeeperTrophy.BLOCK.get().defaultBlockState(), poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

    }

    private static void renderBacking(PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(BACKING_MODEL);
        Lighting.setupForFlatItems();
        MultiBufferSource.BufferSource backingBuffer = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, -1.5);
        BakedModel transformedModel = model.applyTransform(ItemDisplayContext.GUI, poseStack, false);
        minecraft.getItemRenderer().render(new ItemStack(Items.STICK), ItemDisplayContext.GUI,
                false, poseStack, backingBuffer, 0x00F000F0, OverlayTexture.NO_OVERLAY, transformedModel);
        poseStack.popPose();
        backingBuffer.endBatch();
        Lighting.setupFor3DItems();
    }
}
