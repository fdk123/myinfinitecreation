package fdk123.myinfinitecreation.client;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MyInfiniteCreation.MOD_ID, value = Dist.CLIENT)
public class ClientJeiProgressionEvents {
    private static int retryTick = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++retryTick < 40) {
            return;
        }
        retryTick = 0;
        JeiProgressionPlugin.retryForcedVisibleMineColoniesRecipes();
    }
}
