package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;

/** Forge 1.20.1 implementation of the hydra's reactive fire and lingering puddle. */
public final class HydraHeart extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("hydra_heart", HydraHeart::new);

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("hydra_fire")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.15, 0.20)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.12)
                                        .build())
                                .stat(StatData.builder("damage")
                                        .initialValue(1.0, 2.0)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.8)
                                        .build())
                                .stat(StatData.builder("lifetime")
                                        .initialValue(80, 100)
                                        .upgradeModifier(UpgradeOperation.ADD, 20)
                                        .build())
                                .maxLevel(5)
                                .build())
                        .build())
                .leveling(LevelingData.builder().initialCost(100).step(125).maxLevel(5).build())
                .build();
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide() || !wearer.isAlive() || event.getAmount() <= 0) {
            return;
        }
        CuriosApi.getCuriosInventory(wearer).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (!(stack.getItem() instanceof HydraHeart heart)) {
                    continue;
                }
                double chance = Math.max(0, Math.min(1, heart.getAbilityValue(stack, "hydra_fire", "chance")));
                if (wearer.getRandom().nextDouble() < chance) {
                    HydraFireProjectile fire = new HydraFireProjectile(HydraFireEntities.PROJECTILE.get(), wearer.level());
                    fire.configure(wearer,
                            (int) Math.round(heart.getAbilityValue(stack, "hydra_fire", "lifetime")),
                            (float) heart.getAbilityValue(stack, "hydra_fire", "damage"));
                    fire.setPos(wearer.getEyePosition());
                    fire.setDeltaMovement((wearer.getRandom().nextFloat() - 0.5f) * 0.1f, 0.4f,
                            (wearer.getRandom().nextFloat() - 0.5f) * 0.1f);
                    wearer.level().addFreshEntity(fire);
                }
            }
        });
    }

    static void awardImpactExperience(LivingEntity wearer) {
        CuriosApi.getCuriosInventory(wearer).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (stack.getItem() instanceof HydraHeart heart) {
                    heart.addExperience(wearer, stack, 1);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide() || !event.getEntity().getType().builtInRegistryHolder()
                .is(net.minecraft.tags.TagKey.create(ForgeRegistries.ENTITY_TYPES.getRegistryKey(),
                        new net.minecraft.resources.ResourceLocation(MyInfiniteCreation.MOD_ID, "hydra_heart_droppers")))) {
            return;
        }
        if (event.getEntity().getRandom().nextFloat() < 0.33f) {
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(event.getEntity().level(),
                    event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), new ItemStack(ITEM.get())));
        }
    }
}
