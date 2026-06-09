package fdk123.myinfinitecreation.client;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import fdk123.myinfinitecreation.progression.ModGateRule;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MyInfiniteCreation.MOD_ID, value = Dist.CLIENT)
public final class ClientModGateEvents {
    private ClientModGateEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ModGateRule rule = ClientProgressionHooks.lockedModItemRule(event.getEntity(), event.getItemStack());
        if (rule == null || !rule.maskName) {
            return;
        }
        event.getToolTip().clear();
        event.getToolTip().add(Component.translatable("item.myinfinitecreation.unknown_item"));
        event.getToolTip().add(Component.translatable("tooltip.myinfinitecreation.unknown_item"));
    }
}
