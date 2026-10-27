package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.network.MicNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import twilightforest.entity.projectile.TwilightWandBolt;
import twilightforest.init.TFEntities;
import twilightforest.init.TFItems;
import twilightforest.init.TFDamageTypes;

import java.util.List;

public final class LichCrownEffects {
    private static final String VENDETTA = "mic_lich_vendetta";
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> false);

    private LichCrownEffects() {
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide() || REFLECTING.get() || event.getAmount() <= 0) return;
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (source instanceof LivingEntity attacker && attacker != victim) {
            CuriosApi.getCuriosInventory(attacker).ifPresent(curios -> {
                for (var slot : curios.findCurios(LichCrown.ITEM.get())) {
                    ItemStack stack = slot.stack();
                    if (!(stack.getItem() instanceof LichCrown crown)) continue;
                    if (crown.hasGem(stack, "biome_burn")) {
                        float temperature = attacker.level().getBiome(attacker.blockPosition()).value().getBaseTemperature();
                        if (temperature > 0.5f) {
                            double multiplier = crown.getAbilityValue(stack, "biome_burn", "multiplier");
                            event.setAmount((float) (event.getAmount() * (1 + multiplier * (temperature * 10 - 5))));
                            crown.addExperience(attacker, stack, 1);
                        }
                    }
                    if (crown.hasGem(stack, "frostbite")) {
                        victim.setTicksFrozen(victim.getTicksFrozen() + Math.max(1,
                                (int) Math.round(crown.getAbilityValue(stack, "frostbite", "duration"))));
                        crown.addExperience(attacker, stack, 1);
                    }
                    if (crown.hasGem(stack, "vendetta")) {
                        CompoundTag vengeance = victim.getPersistentData().getCompound(VENDETTA);
                        if (vengeance.contains(attacker.getStringUUID())) {
                            double multiplier = crown.getAbilityValue(stack, "vendetta", "multiplier");
                            event.setAmount((float) (event.getAmount() * (1 + multiplier)));
                            vengeance.remove(attacker.getStringUUID());
                            victim.getPersistentData().put(VENDETTA, vengeance);
                            crown.addExperience(attacker, stack, 1);
                        }
                    }
                    if (crown.hasGem(stack, "frenzy")) {
                        applyFrenzy(event, attacker, victim, crown, stack);
                    }
                }
            });
        }
        CuriosApi.getCuriosInventory(victim).ifPresent(curios -> {
            for (var slot : curios.findCurios(LichCrown.ITEM.get())) {
                ItemStack stack = slot.stack();
                if (!(stack.getItem() instanceof LichCrown crown)) continue;
                if (source instanceof LivingEntity attacker && attacker != victim) {
                    if (crown.hasGem(stack, "mirror_leech") && victim.getRandom().nextDouble()
                            < crown.getAbilityValue(stack, "mirror_leech", "chance")) {
                        float amount = event.getAmount();
                        REFLECTING.set(true);
                        try {
                            attacker.hurt(victim.damageSources().magic(), amount);
                        } finally {
                            REFLECTING.set(false);
                        }
                        victim.heal(amount);
                        event.setAmount(0);
                        crown.addExperience(victim, stack, Math.max(1, (int) amount));
                        victim.level().playSound(null, victim, SoundEvents.ENCHANTMENT_TABLE_USE,
                                SoundSource.NEUTRAL, 1, 2);
                        continue;
                    }
                    if (crown.hasGem(stack, "vendetta")) {
                        CompoundTag vengeance = attacker.getPersistentData().getCompound(VENDETTA);
                        vengeance.putInt(victim.getStringUUID(), Math.max(1,
                                (int) Math.round(crown.getAbilityValue(stack, "vendetta", "lifetime"))));
                        attacker.getPersistentData().put(VENDETTA, vengeance);
                    }
                }
                if (crown.isSovereign(stack, victim) && event.getAmount() >= victim.getHealth()) {
                    event.setAmount(Math.max(0, victim.getHealth() - 1));
                }
            }
        });
    }

    private static void applyFrenzy(LivingDamageEvent event, LivingEntity attacker,
                                    LivingEntity victim, LichCrown crown, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag targets = tag.getList("mic_lich_frenzy_targets", Tag.TAG_STRING);
        for (int i = 0; i < targets.size(); i++) {
            if (targets.getString(i).equals(victim.getStringUUID())) {
                tag.remove("mic_lich_frenzy_targets");
                tag.remove("mic_lich_frenzy_multiplier");
                tag.remove("mic_lich_frenzy_time");
                return;
            }
        }
        event.setAmount(event.getAmount() * (1 + tag.getFloat("mic_lich_frenzy_multiplier")));
        tag.putFloat("mic_lich_frenzy_multiplier", tag.getFloat("mic_lich_frenzy_multiplier")
                + (float) crown.getAbilityValue(stack, "frenzy", "additive_multiplier"));
        targets.add(StringTag.valueOf(victim.getStringUUID()));
        tag.put("mic_lich_frenzy_targets", targets);
        tag.putInt("mic_lich_frenzy_time", 100);
        crown.addExperience(attacker, stack, 1);
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        CompoundTag vengeance = entity.getPersistentData().getCompound(VENDETTA);
        if (!vengeance.isEmpty()) {
            for (String key : List.copyOf(vengeance.getAllKeys())) {
                int remaining = vengeance.getInt(key) - 1;
                if (remaining > 0) vengeance.putInt(key, remaining);
                else vengeance.remove(key);
            }
            entity.getPersistentData().put(VENDETTA, vengeance);
        }
        if (entity instanceof AbstractSkeleton skeleton && entity.tickCount % 10 == 0
                && skeleton.getTarget() != null && hasCrown(skeleton.getTarget())) {
            skeleton.setTarget(null);
        }
    }

    private static boolean hasCrown(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).map(curios ->
                !curios.findCurios(LichCrown.ITEM.get()).isEmpty()).orElse(false);
    }

    @SubscribeEvent
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof AbstractSkeleton
                && event.getNewTarget() != null && hasCrown(event.getNewTarget())) {
            event.setNewTarget(null);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)) return;
        if (blockWithEtherealGuard(event, target)) return;
        if (event.getEntity() instanceof TwilightWandBolt bolt
                && bolt.getPersistentData().getBoolean("mic_lich_twilight_bolt")) {
            if (bolt.getOwner() instanceof LivingEntity owner) {
                target.hurt(TFDamageTypes.getIndirectEntityDamageSource(target.level(),
                                TFDamageTypes.TWILIGHT_SCEPTER, bolt, owner),
                        bolt.getPersistentData().getFloat("mic_lich_twilight_damage"));
                CuriosApi.getCuriosInventory(owner).ifPresent(curios -> {
                    for (var slot : curios.findCurios(LichCrown.ITEM.get())) {
                        if (slot.stack().getItem() instanceof LichCrown crown) {
                            crown.addExperience(owner, slot.stack(), 1);
                        }
                    }
                });
            }
            event.setCanceled(true);
            bolt.discard();
            return;
        }
    }

    private static boolean blockWithEtherealGuard(ProjectileImpactEvent event, LivingEntity target) {
        return CuriosApi.getCuriosInventory(target).map(curios -> {
            for (var slot : curios.findCurios(LichCrown.ITEM.get())) {
                ItemStack stack = slot.stack();
                if (!(stack.getItem() instanceof LichCrown crown) || !crown.hasGem(stack, "ethereal_guard")) continue;
                CompoundTag tag = stack.getOrCreateTag();
                int count = tag.getInt("mic_lich_ether_count");
                int limit = Math.max(1, (int) Math.round(crown.getAbilityValue(stack,
                        "ethereal_guard", "max_projectiles")));
                if (count >= limit) continue;
                tag.putInt("mic_lich_ether_count", count + 1);
                tag.putInt("mic_lich_ether_cooldown", Math.max(1,
                        (int) Math.round(crown.getAbilityValue(stack, "ethereal_guard", "cooldown"))));
                target.level().playSound(null, target, SoundEvents.FIREWORK_ROCKET_LAUNCH,
                        SoundSource.NEUTRAL, 1, 2);
                crown.addExperience(target, stack, 1);
                event.setCanceled(true);
                event.getEntity().discard();
                return true;
            }
            return false;
        }).orElse(false);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        CuriosApi.getCuriosInventory(attacker).ifPresent(curios -> {
            for (var slot : curios.findCurios(LichCrown.ITEM.get())) {
                ItemStack stack = slot.stack();
                if (stack.getItem() instanceof LichCrown crown && crown.hasGem(stack, "frenzy")) {
                    stack.getOrCreateTag().remove("mic_lich_frenzy_targets");
                    stack.getOrCreateTag().remove("mic_lich_frenzy_multiplier");
                    stack.getOrCreateTag().remove("mic_lich_frenzy_time");
                }
            }
        });
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        LivingEntity entity = event.getEntity();
        if (entity.getType() == TFEntities.LICH.get()) {
            if (entity.getRandom().nextFloat() < 0.33f) drop(event, level, entity, LichCrown.ITEM.get());
            for (ItemEntity dropped : event.getDrops()) {
                Item item = dropped.getItem().getItem();
                Item gem = null;
                if (item == TFItems.TWILIGHT_SCEPTER.get()) gem = LichGem.TWILIGHT.get();
                else if (item == TFItems.LIFEDRAIN_SCEPTER.get()) gem = LichGem.ABSORPTION.get();
                else if (item == TFItems.ZOMBIE_SCEPTER.get()) gem = LichGem.NECROMANCY.get();
                else if (item == TFItems.FORTIFICATION_SCEPTER.get()) gem = LichGem.SHIELDING.get();
                if (gem != null) dropped.setItem(new ItemStack(gem, dropped.getItem().getCount()));
            }
        } else if (entity.getType() == TFEntities.SNOW_QUEEN.get()) maybeGem(event, level, entity, LichGem.FROST.get());
        else if (entity.getType() == TFEntities.UR_GHAST.get()) maybeGem(event, level, entity, LichGem.CARMINITE.get());
        else if (entity.getType() == TFEntities.KNIGHT_PHANTOM.get()) maybeGem(event, level, entity, LichGem.ETHER.get());
        else if (entity.getType() == TFEntities.HYDRA.get()) maybeGem(event, level, entity, LichGem.FIRE.get());
        else if (entity.getType() == TFEntities.MINOSHROOM.get()) maybeGem(event, level, entity, LichGem.FRENZY.get());
        else if (entity.getType() == TFEntities.NAGA.get()) maybeGem(event, level, entity, LichGem.VENGEFUL.get());
    }

    private static void maybeGem(LivingDropsEvent event, ServerLevel level, LivingEntity entity, Item gem) {
        if (entity.getRandom().nextFloat() < 0.33f) drop(event, level, entity, gem);
    }

    private static void drop(LivingDropsEvent event, ServerLevel level, Entity entity, Item item) {
        event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(),
                new ItemStack(item)));
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getEntity().level().isClientSide()) MicNetwork.sendLichTwilightBolt();
    }
}
