package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;

/** An original Relics reward for the Castle Keeper, inspired by its arena shockwaves. */
public final class CastleKeeperSigil extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("castle_keeper_sigil", CastleKeeperSigil::new);
    private static final String NEXT_PULSE = "mic_castle_keeper_sigil_next_pulse";
    private static final String NEXT_RESCUE = "mic_castle_keeper_sigil_next_rescue";
    private static final int MIN_RESCUE_COOLDOWN = 12_000;

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("guardian_pulse")
                                .stat(StatData.builder("shield")
                                        .initialValue(4, 6).upgradeModifier(UpgradeOperation.ADD, 2).build())
                                .stat(StatData.builder("radius")
                                        .initialValue(3, 4).upgradeModifier(UpgradeOperation.ADD, 0.5).build())
                                .stat(StatData.builder("cooldown")
                                        .initialValue(500, 400).upgradeModifier(UpgradeOperation.ADD, -40).build())
                                .maxLevel(5).build())
                        .ability(AbilityData.builder("last_stand")
                                .stat(StatData.builder("cooldown")
                                        .initialValue(18_000, 18_000)
                                        .upgradeModifier(UpgradeOperation.ADD, -1_200).build())
                                .maxLevel(5).build())
                        .build())
                .leveling(LevelingData.builder().initialCost(250).step(250).maxLevel(5).build())
                .build();
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide() || !wearer.isAlive() || event.getAmount() <= 0) {
            return;
        }
        CuriosApi.getCuriosInventory(wearer).ifPresent(curios ->
                curios.findFirstCurio(ITEM.get()).ifPresent(slot -> {
                    ItemStack stack = slot.stack();
                    if (!(stack.getItem() instanceof CastleKeeperSigil sigil)) {
                        return;
                    }
                    long now = wearer.level().getServer().overworld().getGameTime();
                    CompoundTag data = wearer.getPersistentData();
                    if (now < data.getLong(NEXT_PULSE) || now < data.getLong(NEXT_RESCUE)
                            || event.getAmount() >= wearer.getHealth()) {
                        return;
                    }

                    float shield = (float) Math.max(0, sigil.getAbilityValue(stack, "guardian_pulse", "shield"));
                    double radius = Math.max(0, sigil.getAbilityValue(stack, "guardian_pulse", "radius"));
                    int cooldown = Math.max(20, (int) Math.round(sigil.getAbilityValue(stack, "guardian_pulse", "cooldown")));
                    if (shield <= 0 || radius <= 0) {
                        return;
                    }
                    data.putLong(NEXT_PULSE, now + cooldown);
                    wearer.setAbsorptionAmount(Math.max(wearer.getAbsorptionAmount(), shield));
                    for (LivingEntity target : wearer.level().getEntitiesOfClass(LivingEntity.class,
                            wearer.getBoundingBox().inflate(radius), candidate -> candidate instanceof Enemy
                                    && candidate.isAlive() && !wearer.isAlliedTo(candidate)
                                    && candidate.distanceToSqr(wearer) <= radius * radius)) {
                        Vec3 away = target.position().subtract(wearer.position());
                        if (away.horizontalDistanceSqr() > 0.001) {
                            target.knockback(1.0, -away.x, -away.z);
                        }
                    }
                    wearer.level().playSound(null, wearer.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                            SoundSource.PLAYERS, 0.7f, 1.4f);
                    sigil.addExperience(wearer, stack, 1);
                }));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide() || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        CuriosApi.getCuriosInventory(wearer).ifPresent(curios ->
                curios.findFirstCurio(ITEM.get()).ifPresent(slot -> {
                    ItemStack stack = slot.stack();
                    if (!(stack.getItem() instanceof CastleKeeperSigil sigil)) {
                        return;
                    }
                    long now = wearer.level().getServer().overworld().getGameTime();
                    CompoundTag data = wearer.getPersistentData();
                    if (now < data.getLong(NEXT_RESCUE)) {
                        return;
                    }
                    int cooldown = Math.max(MIN_RESCUE_COOLDOWN,
                            (int) Math.round(sigil.getAbilityValue(stack, "last_stand", "cooldown")));
                    data.putLong(NEXT_RESCUE, now + cooldown);
                    wearer.setHealth(1);
                    wearer.removeAllEffects();
                    wearer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                    wearer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                    wearer.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                    wearer.level().broadcastEntityEvent(wearer, (byte) 35);
                    sigil.addExperience(wearer, stack, 25);
                    event.setCanceled(true);
                }));
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        CompoundTag original = event.getOriginal().getPersistentData();
        CompoundTag replacement = event.getEntity().getPersistentData();
        for (String key : new String[]{NEXT_PULSE, NEXT_RESCUE}) {
            if (original.contains(key)) {
                replacement.putLong(key, original.getLong(key));
            }
        }
    }
}
