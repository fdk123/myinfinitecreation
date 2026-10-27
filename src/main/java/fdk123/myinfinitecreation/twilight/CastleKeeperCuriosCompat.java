package fdk123.myinfinitecreation.twilight;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.capability.CurioItemCapability;

final class CastleKeeperCuriosCompat {
    private CastleKeeperCuriosCompat() {}

    static ICapabilityProvider createProvider(ItemStack stack) {
        return CurioItemCapability.createProvider(new ICurio() {
            @Override
            public ItemStack getStack() {
                return stack;
            }
        });
    }
}
