package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import twilightforest.network.CreateMovingCicadaSoundPacket;
import twilightforest.network.TFPacketHandler;

public final class CicadaBottle extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("cicada_bottle", CicadaBottle::new);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,
            MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<MobEffect> INFESTATION = EFFECTS.register("cicada_infestation",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xf7f5bd) {});

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("cicada_infestation")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.1, 0.2).upgradeModifier(UpgradeOperation.ADD, 0.1).build())
                                .stat(StatData.builder("duration")
                                        .initialValue(60, 140).upgradeModifier(UpgradeOperation.ADD, 20).build())
                                .maxLevel(5).build())
                        .build())
                .leveling(LevelingData.builder().maxLevel(5).build())
                .build();
    }

    @SubscribeEvent
    public static void onHit(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || event.getEntity().hasEffect(INFESTATION.get())) {
            return;
        }
        CuriosApi.getCuriosInventory(attacker).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (!(stack.getItem() instanceof CicadaBottle bottle)
                        || attacker.getRandom().nextDouble()
                        >= bottle.getAbilityValue(stack, "cicada_infestation", "chance")) {
                    continue;
                }
                int duration = (int) Math.round(bottle.getAbilityValue(stack, "cicada_infestation", "duration"));
                if (event.getEntity().addEffect(new MobEffectInstance(INFESTATION.get(), Math.max(1, duration)))) {
                    bottle.addExperience(attacker, stack, 1);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity().level().isClientSide() || event.getEffectInstance().getEffect() != INFESTATION.get()
                || event.getOldEffectInstance() != null) {
            return;
        }
        TFPacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(event::getEntity),
                new CreateMovingCicadaSoundPacket(event.getEntity().getId()));
    }

    @SubscribeEvent
    public static void onMobSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        mob.targetSelector.addGoal(-100, new NearestAttackableTargetGoal<>(mob, LivingEntity.class, false,
                target -> target.hasEffect(INFESTATION.get())));
    }
}
