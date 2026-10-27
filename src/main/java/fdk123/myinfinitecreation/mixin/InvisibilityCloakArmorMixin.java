package fdk123.myinfinitecreation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fdk123.myinfinitecreation.reliquified.InvisibilityCloak;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.CuriosApi;

@Mixin(HumanoidArmorLayer.class)
public abstract class InvisibilityCloakArmorMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void myinfinitecreation$hideArmor(PoseStack pose, MultiBufferSource buffer, int light,
            LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTick,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callback) {
        CuriosApi.getCuriosInventory(entity).ifPresent(curios -> {
            if (curios.isEquipped(InvisibilityCloak.ITEM.get())) {
                callback.cancel();
            }
        });
    }
}
