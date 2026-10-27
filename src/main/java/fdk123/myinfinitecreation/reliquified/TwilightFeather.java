package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import twilightforest.entity.passive.TinyBird;
import twilightforest.init.TFEntities;
import twilightforest.init.custom.TinyBirdVariant;

public final class TwilightFeather extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("twilight_feather", TwilightFeather::new);

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("execution")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.005, 0.01)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.004).build())
                                .maxLevel(10).build())
                        .build())
                .leveling(LevelingData.builder().maxLevel(10).build())
                .build();
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.getSource().is(DamageTypes.GENERIC_KILL)
                || victim instanceof TinyBird
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || victim == attacker || victim.getHealth() > attacker.getMaxHealth()) {
            return;
        }
        CuriosApi.getCuriosInventory(attacker).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (!(stack.getItem() instanceof TwilightFeather feather)
                        || attacker.getRandom().nextDouble()
                        >= feather.getAbilityValue(stack, "execution", "chance")) {
                    continue;
                }
                DamageSource execution = new DamageSource(victim.level().registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC_KILL), attacker);
                if (victim.hurt(execution, Float.MAX_VALUE) && victim.isDeadOrDying()) {
                    TinyBird bird = new TinyBird(TFEntities.TINY_BIRD.get(), victim.level());
                    bird.setBirdType(TinyBirdVariant.getVariantId(TinyBirdVariant.getRandomVariant(bird.getRandom())));
                    bird.moveTo(victim.getEyePosition());
                    bird.setDeltaMovement(victim.getDeltaMovement());
                    victim.level().addFreshEntity(bird);
                    victim.level().playSound(null, bird.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                            SoundSource.NEUTRAL, 1f, 0.8f);
                    feather.addExperience(attacker, stack, 1);
                    event.setAmount(0);
                    break;
                }
            }
        });
    }
}
