package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import it.hurts.sskirillss.relics.init.EffectRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

/** A crown that protects from thorns and harms adjacent enemies. */
public final class ThornCrown extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("thorn_crown", ThornCrown::new);

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("poking")
                                .stat(StatData.builder("damage")
                                        .initialValue(1, 2).upgradeModifier(UpgradeOperation.ADD, 0.5).build())
                                .stat(StatData.builder("paralyze_chance")
                                        .initialValue(0.05, 0.10).upgradeModifier(UpgradeOperation.ADD, 0.02).build())
                                .stat(StatData.builder("paralyze_duration")
                                        .initialValue(10, 20).upgradeModifier(UpgradeOperation.ADD, 10).build())
                                .build())
                        .build())
                .leveling(LevelingData.builder().initialCost(100).step(125).maxLevel(5).build())
                .build();
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        LivingEntity wearer = context.entity();
        if (wearer.level().isClientSide()) {
            return;
        }
        float damage = (float) Math.max(0, getAbilityValue(stack, "poking", "damage"));
        double paralyzeChance = Math.max(0, Math.min(1, getAbilityValue(stack, "poking", "paralyze_chance")));
        int duration = (int) Math.round(getAbilityValue(stack, "poking", "paralyze_duration"));
        for (LivingEntity target : wearer.level().getEntitiesOfClass(LivingEntity.class,
                wearer.getBoundingBox().inflate(0.1), candidate -> candidate != wearer
                        && candidate.isAlive() && !wearer.isAlliedTo(candidate))) {
            if (target.hurt(wearer.damageSources().thorns(wearer), damage)
                    && wearer.getRandom().nextDouble() < paralyzeChance) {
                target.addEffect(new MobEffectInstance(EffectRegistry.PARALYSIS.get(), Math.max(1, duration), 0, false, false));
                addExperience(wearer, stack, 1);
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        boolean thornDamage = event.getSource().typeHolder().unwrapKey()
                .map(key -> key.location().getPath().contains("thorn")).orElse(false);
        if (event.getEntity().level().isClientSide()
                || !(event.getSource().is(DamageTypes.CACTUS) || event.getSource().is(DamageTypes.SWEET_BERRY_BUSH)
                || thornDamage)) {
            return;
        }
        CuriosApi.getCuriosInventory(event.getEntity()).ifPresent(curios -> {
            if (curios.isEquipped(ITEM.get())) {
                event.setCanceled(true);
            }
        });
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (event.getName() == null || !event.getName().toString().equals("twilightforest:chests/hedge_maze")) {
            return;
        }
        event.getTable().addPool(LootPool.lootPool()
                .name("myinfinitecreation_thorn_crown")
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(ITEM.get())
                        .when(LootItemRandomChanceCondition.randomChance(0.10f)))
                .build());
    }
}
