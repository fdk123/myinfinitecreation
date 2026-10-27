package fdk123.myinfinitecreation.mixin;

import fdk123.myinfinitecreation.reliquified.SteelCape;
import it.hurts.sskirillss.relics.init.EffectRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.ChainBlock;

@Mixin(ChainBlock.class)
public abstract class SteelCapeChainBlockMixin {
    @Shadow(remap = false) private ItemStack stack;

    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 1)
    private boolean myinfinitecreation$applyCapeDamage(Entity target, DamageSource source, float originalAmount) {
        if (!(stack.getItem() instanceof SteelCape cape)) {
            return target.hurt(source, originalAmount);
        }
        boolean damaged = target.hurt(source, cape.chainDamage(stack));
        if (damaged && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(EffectRegistry.STUN.get(), cape.stunDuration(stack),
                    0, false, true, true));
        }
        return damaged;
    }
}
