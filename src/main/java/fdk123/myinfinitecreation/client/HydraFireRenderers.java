package fdk123.myinfinitecreation.client;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import fdk123.myinfinitecreation.reliquified.HydraFireEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MyInfiniteCreation.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HydraFireRenderers {
    private HydraFireRenderers() {
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HydraFireEntities.PROJECTILE.get(), NoopRenderer::new);
        event.registerEntityRenderer(HydraFireEntities.PUDDLE.get(), NoopRenderer::new);
    }
}
