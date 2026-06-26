package fdk123.myinfinitecreation.mixin;

import fdk123.myinfinitecreation.progression.CraftingResultSlotGuard;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "slimeknights.tconstruct.tables.menu.CraftingStationContainerMenu", remap = false)
public class TConstructCraftingStationMenuMixin {
    @Inject(method = {"quickMoveStack", "m_7648_"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void myinfinitecreation$blockLockedCraftingStationShiftClick(
            Player player,
            int index,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (index < 0 || index >= menu.slots.size()) {
            return;
        }

        Slot slot = menu.slots.get(index);
        if (CraftingResultSlotGuard.shouldBlock(player, slot)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
