package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

public final class DeerAntler extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("deer_antler", DeerAntler::new);
    private static final String RIDER_MARK = "mic_deer_antler_rider";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("ride_along")
                                .stat(StatData.builder("entity_volume")
                                        .initialValue(0.5, 2).upgradeModifier(UpgradeOperation.MULTIPLY_TOTAL, 1).build())
                                .active(CastData.builder().type(CastType.INSTANTANEOUS).build())
                                .maxLevel(5).build())
                        .build())
                .leveling(LevelingData.builder().maxLevel(5).build())
                .build();
    }

    @Override
    public void castActiveAbility(ItemStack stack, Player player, String ability, CastType type, CastStage stage) {
        super.castActiveAbility(stack, player, ability, type, stage);
        if (player.level().isClientSide() || !"ride_along".equals(ability) || stage != CastStage.END
                || !isEquipped(player, stack)) {
            return;
        }
        Entity passenger = player.getFirstPassenger();
        if (passenger != null && passenger.getPersistentData().getBoolean(RIDER_MARK)) {
            passenger.stopRiding();
            return;
        }
        Vec3 eye = player.getEyePosition(1);
        Vec3 look = player.getViewVector(1);
        double reach = 5;
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(look.scale(reach)),
                player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1),
                target -> target instanceof LivingEntity && target.isAlive() && target != player
                        && player.hasLineOfSight(target) && !target.isPassenger(), reach * reach);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        double volume = target.getBoundingBox().getXsize() * target.getBoundingBox().getYsize()
                * target.getBoundingBox().getZsize();
        if (volume > getAbilityValue(stack, "ride_along", "entity_volume")) {
            return;
        }
        target.getPersistentData().putBoolean(RIDER_MARK, true);
        if (!target.startRiding(player, true)) {
            target.getPersistentData().remove(RIDER_MARK);
        }
    }

    private static boolean isEquipped(Player player, ItemStack castStack) {
        return CuriosApi.getCuriosInventory(player).map(curios ->
                curios.findCurios(ITEM.get()).stream().anyMatch(slot -> slot.stack() == castStack
                        || ItemStack.isSameItemSameTags(slot.stack(), castStack))).orElse(false);
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        LivingEntity wearer = context.entity();
        Entity passenger = wearer.getFirstPassenger();
        if (!wearer.level().isClientSide() && wearer.tickCount % 60 == 0 && passenger != null
                && passenger.getPersistentData().getBoolean(RIDER_MARK)) {
            addExperience(wearer, stack, 1);
        }
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        super.onUnequip(context, newStack, stack);
        Entity passenger = context.entity().getFirstPassenger();
        if (!context.entity().level().isClientSide() && passenger != null
                && passenger.getPersistentData().getBoolean(RIDER_MARK)) {
            passenger.stopRiding();
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!event.getLevel().isClientSide() && event.isDismounting()) {
            event.getEntityMounting().getPersistentData().remove(RIDER_MARK);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && attacker.getPersistentData().getBoolean(RIDER_MARK)
                && attacker.getVehicle() == event.getEntity()) {
            event.setCanceled(true);
        }
    }
}
