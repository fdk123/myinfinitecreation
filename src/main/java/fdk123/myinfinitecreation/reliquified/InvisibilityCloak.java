package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.init.EffectRegistry;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.SlotContext;

public final class InvisibilityCloak extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("invisibility_cloak", InvisibilityCloak::new);
    private static final String IDLE_TICKS = "mic_invisibility_idle_ticks";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("cosmetic_armor").maxLevel(0).build())
                        .ability(AbilityData.builder("invisibility")
                                .stat(StatData.builder("duration")
                                        .initialValue(160, 120)
                                        .upgradeModifier(UpgradeOperation.ADD, -10)
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingData.builder().build())
                .build();
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        LivingEntity wearer = context.entity();
        if (wearer.level().isClientSide()) {
            return;
        }
        int idleTicks = stack.getOrCreateTag().getInt(IDLE_TICKS);
        int neededTicks = Math.max(1, (int) Math.round(getAbilityValue(stack, "invisibility", "duration")));
        if (wearer.getDeltaMovement().lengthSqr() < 0.005 || wearer.isCrouching()) {
            if (idleTicks < neededTicks) {
                idleTicks++;
            } else {
                if (!wearer.hasEffect(EffectRegistry.VANISHING.get())) {
                    addExperience(wearer, stack, 1);
                }
                wearer.addEffect(new MobEffectInstance(EffectRegistry.VANISHING.get(), 24, 0, true, false));
            }
        } else {
            idleTicks = 0;
            wearer.removeEffect(EffectRegistry.VANISHING.get());
        }
        stack.getOrCreateTag().putInt(IDLE_TICKS, idleTicks);
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        super.onUnequip(context, newStack, stack);
        if (!context.entity().level().isClientSide() && newStack.getItem() != stack.getItem()) {
            context.entity().removeEffect(EffectRegistry.VANISHING.get());
            stack.getOrCreateTag().putInt(IDLE_TICKS, 0);
        }
    }
}
