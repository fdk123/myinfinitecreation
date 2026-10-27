package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import fdk123.myinfinitecreation.client.ScaledCloakClient;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import twilightforest.init.TFEntities;

public final class ScaledCloak extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("scaled_cloak", ScaledCloak::new);
    public static final String CLIMB_TICKS = "mic_scaled_cloak_climb_ticks";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("wall_crawler")
                                .stat(StatData.builder("max_time")
                                        .initialValue(60, 100).upgradeModifier(UpgradeOperation.ADD, 40).build())
                                .maxLevel(10).build())
                        .ability(AbilityData.builder("elusive_stare")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.1, 0.25).upgradeModifier(UpgradeOperation.ADD, 0.1).build())
                                .maxLevel(10).build())
                        .build())
                .leveling(LevelingData.builder().maxLevel(15).build())
                .build();
    }

    public int maxClimbTicks(ItemStack stack) {
        return Math.max(1, (int) Math.round(getAbilityValue(stack, "wall_crawler", "max_time")));
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        LivingEntity wearer = context.entity();
        if (wearer.level().isClientSide()) {
            ScaledCloakClient.tick(wearer, stack);
        } else if (wearer.onGround()) {
            stack.getOrCreateTag().putInt(CLIMB_TICKS, maxClimbTicks(stack));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity wearer = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        if (wearer.level().isClientSide() || attacker == null) {
            return;
        }
        Vec3 eye = wearer.getEyePosition(1);
        Vec3 look = wearer.getViewVector(1);
        EntityHitResult lookedAt = ProjectileUtil.getEntityHitResult(wearer, eye, eye.add(look.scale(100)),
                wearer.getBoundingBox().expandTowards(look.scale(100)).inflate(1),
                candidate -> candidate instanceof LivingEntity && candidate.isAlive()
                        && wearer.hasLineOfSight(candidate) && candidate != wearer,
                100 * 100);
        if (lookedAt == null || lookedAt.getEntity() != attacker) {
            return;
        }
        CuriosApi.getCuriosInventory(wearer).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (stack.getItem() instanceof ScaledCloak cloak && cloak.getLevel(stack) >= 5
                        && wearer.getRandom().nextDouble()
                        < cloak.getAbilityValue(stack, "elusive_stare", "chance")) {
                    event.setCanceled(true);
                    cloak.addExperience(wearer, stack, 1);
                    wearer.level().playSound(null, wearer, SoundEvents.PLAYER_ATTACK_SWEEP,
                            SoundSource.NEUTRAL, 0.6f, 1.35f);
                    break;
                }
            }
        });
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide() || event.getEntity().getType() != TFEntities.NAGA.get()
                || event.getEntity().getRandom().nextFloat() >= 0.33f) {
            return;
        }
        event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(),
                event.getEntity().getY(), event.getEntity().getZ(), new ItemStack(ITEM.get())));
    }
}
