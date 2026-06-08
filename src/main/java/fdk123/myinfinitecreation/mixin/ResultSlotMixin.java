package fdk123.myinfinitecreation.mixin;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public class ResultSlotMixin {
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void myinfinitecreation$blockLockedCraftingResult(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = (Slot) (Object) this;
        if (slot instanceof ResultSlot && !MyInfiniteCreation.PROGRESSION_GATES.mayTakeCraftingResult(player, slot.getItem())) {
            cir.setReturnValue(false);
        }
    }
}
