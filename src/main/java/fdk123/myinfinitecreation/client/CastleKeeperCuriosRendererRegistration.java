package fdk123.myinfinitecreation.client;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import fdk123.myinfinitecreation.twilight.CastleKeeperTrophy;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import twilightforest.compat.curios.renderer.CurioHeadRenderer;

@Mod.EventBusSubscriber(modid = MyInfiniteCreation.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CastleKeeperCuriosRendererRegistration {
    private CastleKeeperCuriosRendererRegistration() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("curios") && ModList.get().isLoaded("twilightforest")) {
            event.enqueueWork(() -> CuriosRendererRegistry.register(
                    CastleKeeperTrophy.ITEM.get(), CurioHeadRenderer::new));
        }
    }
}
