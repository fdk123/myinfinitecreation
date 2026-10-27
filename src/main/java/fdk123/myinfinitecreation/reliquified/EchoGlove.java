package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/** A Forge 1.20.1 Echo Glove using the Relics 0.8 ability data model. */
public final class EchoGlove extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("echo_glove", EchoGlove::new);

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("echo_strike")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.05, 0.15)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.05)
                                        .build())
                                .stat(StatData.builder("damage_fraction")
                                        .initialValue(0.15, 0.25)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.10)
                                        .build())
                                .stat(StatData.builder("radius")
                                        .initialValue(3, 5)
                                        .upgradeModifier(UpgradeOperation.ADD, 1)
                                        .build())
                                .maxLevel(5)
                                .build())
                        .build())
                .leveling(LevelingData.builder().initialCost(100).step(125).maxLevel(5).build())
                .build();
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player
                || event.getEntity() == player || event.getAmount() <= 0) {
            return;
        }

        LivingEntity victim = event.getEntity();
        CuriosApi.getCuriosInventory(player).ifPresent(curios -> curios.findFirstCurio(ITEM.get()).ifPresent(slot -> {
            ItemStack stack = slot.stack();
            if (!(stack.getItem() instanceof EchoGlove glove)) {
                return;
            }
            double chance = Math.max(0, Math.min(1, glove.getAbilityValue(stack, "echo_strike", "chance")));
            if (player.getRandom().nextDouble() >= chance) {
                return;
            }
            double radius = Math.max(0, glove.getAbilityValue(stack, "echo_strike", "radius"));
            float damage = (float) (event.getAmount() * Math.max(0,
                    glove.getAbilityValue(stack, "echo_strike", "damage_fraction")));
            if (radius <= 0 || damage <= 0) {
                return;
            }
            List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                    victim.getBoundingBox().inflate(radius), candidate -> candidate instanceof Enemy
                            && candidate != victim && candidate.isAlive() && player.hasLineOfSight(candidate));
            if (targets.isEmpty()) {
                return;
            }
            LivingEntity echoTarget = targets.get(player.getRandom().nextInt(targets.size()));
            if (echoTarget.hurt(player.damageSources().magic(), damage)) {
                glove.addExperience(player, stack, 1);
            }
        }));
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (event.getName() == null || !event.getName().getNamespace().equals("irons_spellbooks")
                || !event.getName().getPath().startsWith("chests/")
                || event.getName().getPath().startsWith("chests/additional_")) {
            return;
        }
        event.getTable().addPool(LootPool.lootPool()
                .name("myinfinitecreation_echo_glove")
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(ITEM.get())
                        .when(LootItemRandomChanceCondition.randomChance(0.04f)))
                .build());
    }
}
