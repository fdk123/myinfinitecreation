package fdk123.myinfinitecreation.mixin;

import fdk123.myinfinitecreation.reliquified.CharmBackpack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.util.TFItemStackUtils;

@Mixin(value = TFItemStackUtils.class, remap = false)
public abstract class CharmBackpackConsumeMixin {
    @Inject(method = "consumeInventoryItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/Item;)Z",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void mic$consumeStoredCharm(Player player, Item item, CallbackInfoReturnable<Boolean> result) {
        if (!player.level().isClientSide() && CharmBackpack.consumeStored(player, item)) {
            result.setReturnValue(true);
        }
    }
}
