package fdk123.myinfinitecreation.client;

import fdk123.myinfinitecreation.network.MicNetwork;
import fdk123.myinfinitecreation.reliquified.ScaledCloak;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ScaledCloakClient {
    private ScaledCloakClient() {
    }

    public static void tick(LivingEntity wearer, ItemStack stack) {
        if (wearer != Minecraft.getInstance().player || !wearer.horizontalCollision) {
            return;
        }
        int remaining = stack.getOrCreateTag().getInt(ScaledCloak.CLIMB_TICKS);
        Vec3 velocity = wearer.getDeltaMovement();
        wearer.setDeltaMovement(velocity.x, remaining > 0 ? 0.1 : -0.07, velocity.z);
        MicNetwork.sendScaledCloakClimb(true);
    }
}
