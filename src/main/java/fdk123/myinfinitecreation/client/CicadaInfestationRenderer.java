package fdk123.myinfinitecreation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fdk123.myinfinitecreation.MyInfiniteCreation;
import fdk123.myinfinitecreation.reliquified.CicadaBottle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.TwilightForestMod;
import twilightforest.client.model.entity.CicadaModel;

@Mod.EventBusSubscriber(modid = MyInfiniteCreation.MOD_ID, value = Dist.CLIENT)
public final class CicadaInfestationRenderer {
    private static final ResourceLocation TEXTURE = TwilightForestMod.getModelTexture("cicada-model.png");
    private static final CicadaModel MODEL = new CicadaModel(CicadaModel.create().bakeRoot());

    private CicadaInfestationRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLivingEvent.Post<?, ?> event) {
        if (!event.getEntity().hasEffect(CicadaBottle.INFESTATION.get())) {
            return;
        }
        float width = event.getEntity().getBbWidth();
        float height = event.getEntity().getBbHeight();
        float scale = Mth.clamp(Math.min(width, height) / 1.5f, 0.25f, 1.5f);
        int count = Math.max(1, (int) ((Mth.ceil(width * 1.25f) + 1) / scale));
        float time = event.getEntity().tickCount + event.getPartialTick();
        for (int i = 0; i < count; i++) {
            float sine = Mth.sin(time / 4f + i);
            float cosine = Mth.cos(time / 4f + i);
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(time * 2 + 360f * i / count));
            pose.translate(0, height / 2 + sine * height / 20f + 0.25, width / 2 + scale / 3);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.mulPose(Axis.XP.rotationDegrees(-90 - cosine * 10));
            pose.translate(0, 0.5, 0);
            pose.scale(scale, -scale, scale);
            MODEL.renderToBuffer(pose, event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            pose.popPose();
        }
    }
}
