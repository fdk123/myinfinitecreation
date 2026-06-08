package fdk123.myinfinitecreation.mixin;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.crafter.RecipeGridHandler", remap = false)
public class CreateRecipeGridHandlerMixin {
    @Inject(method = "tryToApplyRecipe", at = @At("RETURN"), cancellable = true)
    private static void myinfinitecreation$blockLockedMechanicalCrafting(
            Level level,
            @Coerce Object groupedItems,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        ItemStack result = cir.getReturnValue();
        if (!MyInfiniteCreation.PROGRESSION_GATES.mayCreateMechanicalCraftingResult(level, result)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
