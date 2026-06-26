package fdk123.myinfinitecreation.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.minecolonies.core.compatibility.jei.JobBasedRecipeCategory", remap = false)
public class MineColoniesJeiJobCategoryMixin {
    @Inject(method = "createCitizenWithJob", at = @At("RETURN"), remap = false, require = 0)
    private static void myinfinitecreation$hideJeiCitizenPreviewName(
            @Coerce Object job,
            CallbackInfoReturnable<Entity> cir
    ) {
        Entity entity = cir.getReturnValue();
        if (entity == null) {
            return;
        }
        entity.setCustomName(Component.empty());
        entity.setCustomNameVisible(false);
    }
}
