package fdk123.myinfinitecreation.reliquified;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import fdk123.myinfinitecreation.MyInfiniteCreation;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Forge 1.20.1 adaptations of the remaining Iron's Spells Reliquified items. */
public final class IronRelics {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final Map<String, RegistryObject<Item>> ALL = new LinkedHashMap<>();

    static {
        for (String id : List.of(
                "bloody_voodoo_doll", "cardiac_trap", "cloak_of_the_bloody_feather", "dimension_key",
                "dragon_blood_vial", "ender_bow", "flask_of_the_red_mist", "galaxy_devourer_diadem",
                "hat_of_omniscience", "immaterial_disperser", "living_flesh", "lunar_sextant",
                "mask_of_hunger", "mirror_of_transgression", "pulsar_mantle", "ring_of_blades",
                "ring_of_elusiveness", "sealed_claymore", "sealed_rapier", "sealed_sword",
                "shadow_claws", "sinner_crown", "slicer")) {
            ALL.put(id, ITEMS.register(id, () -> new IronRelic(id)));
        }
    }

    private IronRelics() {}

    private static final class IronRelic extends RelicItem {
        private final String id;

        private IronRelic(String id) {
            this.id = id;
        }

        @Override
        public RelicData constructDefaultRelicData() {
            double cooldown = switch (id == null ? "" : id) {
                case "cardiac_trap", "flask_of_the_red_mist" -> 1_200;
                case "ring_of_elusiveness" -> 700;
                case "mask_of_hunger", "pulsar_mantle", "dimension_key", "mirror_of_transgression" -> 600;
                default -> 400;
            };
            return RelicData.builder()
                    .abilities(AbilitiesData.builder()
                            .ability(AbilityData.builder("core")
                                    .stat(StatData.builder("chance").initialValue(0.12, 0.18)
                                            .upgradeModifier(UpgradeOperation.ADD, 0.025).build())
                                    .stat(StatData.builder("power").initialValue(3, 4)
                                            .upgradeModifier(UpgradeOperation.ADD, 0.75).build())
                                    .stat(StatData.builder("radius").initialValue(4, 5)
                                            .upgradeModifier(UpgradeOperation.ADD, 0.5).build())
                                    .stat(StatData.builder("cooldown").initialValue(cooldown, cooldown)
                                            .upgradeModifier(UpgradeOperation.ADD, -Math.max(20, cooldown / 15)).build())
                                    .maxLevel(5).build())
                            .build())
                    .leveling(LevelingData.builder().initialCost(150).step(150).maxLevel(5).build())
                    .build();
        }

        private double value(ItemStack stack, String stat) {
            return getAbilityValue(stack, "core", stat);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            if (slot != EquipmentSlot.MAINHAND || !(id.equals("slicer") || id.equals("shadow_claws"))) {
                return super.getDefaultAttributeModifiers(slot);
            }
            return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                            "Iron relic weapon damage", id.equals("slicer") ? 6 : 5, AttributeModifier.Operation.ADDITION))
                    .put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                            "Iron relic weapon speed", id.equals("slicer") ? -2.5 : -2.1, AttributeModifier.Operation.ADDITION))
                    .build();
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide()) return InteractionResultHolder.success(stack);
            if (!(level instanceof ServerLevel server)) return InteractionResultHolder.pass(stack);
            long now = server.getGameTime();
            CompoundTag tag = stack.getOrCreateTag();
            if (now < tag.getLong("mic_iron_next_use")) return InteractionResultHolder.fail(stack);
            int cooldown = Math.max(40, (int) Math.round(value(stack, "cooldown")));
            boolean used = switch (id) {
                case "dimension_key" -> useDimensionKey(server, player, stack);
                case "mirror_of_transgression" -> useMirror(server, player);
                case "ender_bow" -> useEnderBow(server, player, stack);
                case "shadow_claws", "slicer" -> useSlash(server, player, stack);
                default -> false;
            };
            if (!used) return InteractionResultHolder.pass(stack);
            tag.putLong("mic_iron_next_use", now + cooldown);
            addExperience(player, stack, 2);
            return InteractionResultHolder.success(stack);
        }

        private boolean useDimensionKey(ServerLevel level, Player player, ItemStack stack) {
            CompoundTag tag = stack.getOrCreateTag();
            if (player.isShiftKeyDown()) {
                tag.remove("mic_iron_key_point");
                return true;
            }
            if (!tag.contains("mic_iron_key_point")) {
                CompoundTag point = new CompoundTag();
                point.putString("dimension", level.dimension().location().toString());
                point.putDouble("x", player.getX());
                point.putDouble("y", player.getY());
                point.putDouble("z", player.getZ());
                tag.put("mic_iron_key_point", point);
                return true;
            }
            CompoundTag point = tag.getCompound("mic_iron_key_point");
            if (!point.getString("dimension").equals(level.dimension().location().toString())) return false;
            double x = point.getDouble("x"), y = point.getDouble("y"), z = point.getDouble("z");
            BlockPos destination = BlockPos.containing(x, y, z);
            if (!level.getBlockState(destination).getCollisionShape(level, destination).isEmpty()
                    || !level.getBlockState(destination.above()).getCollisionShape(level, destination.above()).isEmpty()
                    || level.getBlockState(destination.below()).getCollisionShape(level, destination.below()).isEmpty()) return false;
            point.putDouble("x", player.getX());
            point.putDouble("y", player.getY());
            point.putDouble("z", player.getZ());
            player.teleportTo(x, y, z);
            return true;
        }

        private boolean useMirror(ServerLevel level, Player player) {
            if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getRespawnPosition() == null
                    || serverPlayer.getRespawnDimension() != level.dimension()) return false;
            BlockPos home = serverPlayer.getRespawnPosition();
            for (int radius = 0; radius <= 2; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            BlockPos pos = home.offset(dx, dy, dz);
                            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                                    || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                                    || level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) continue;
                            serverPlayer.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private boolean useEnderBow(ServerLevel level, Player player, ItemStack stack) {
            LivingEntity target = aimedTarget(level, player, 32);
            if (target == null) return false;
            if (target.hurt(player.damageSources().magic(), (float) value(stack, "power"))) {
                Vec3 spot = target.position().subtract(player.getLookAngle().scale(1.5));
                BlockPos pos = BlockPos.containing(spot);
                if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                        && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
                    player.teleportTo(spot.x, spot.y, spot.z);
                }
            }
            return true;
        }

        private boolean useSlash(ServerLevel level, Player player, ItemStack stack) {
            LivingEntity target = aimedTarget(level, player, Math.max(4, value(stack, "radius") + 3));
            if (target == null) return false;
            float damage = (float) value(stack, "power");
            if (id.equals("slicer")) damage *= 1.5f;
            return target.hurt(player.damageSources().magic(), damage);
        }
    }

    private static LivingEntity aimedTarget(ServerLevel level, Player player, double range) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        LivingEntity best = null;
        double bestDistance = range;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), candidate -> candidate != player && candidate.isAlive()
                        && !player.isAlliedTo(candidate) && player.hasLineOfSight(candidate))) {
            Vec3 direction = target.getEyePosition().subtract(eye);
            double distance = direction.length();
            if (distance < bestDistance && direction.normalize().dot(look) > 0.95) {
                best = target;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void equipped(LivingEntity wearer, String id, java.util.function.BiConsumer<IronRelic, ItemStack> action) {
        CuriosApi.getCuriosInventory(wearer).ifPresent(curios ->
                curios.findFirstCurio(ALL.get(id).get()).ifPresent(slot -> {
                    if (slot.stack().getItem() instanceof IronRelic relic) action.accept(relic, slot.stack());
                }));
    }

    private static boolean ready(LivingEntity wearer, String id, long now) {
        return now >= wearer.getPersistentData().getLong("mic_iron_" + id + "_next");
    }

    private static void consume(LivingEntity wearer, String id, long now, IronRelic relic, ItemStack stack) {
        int ticks = Math.max(40, (int) Math.round(relic.value(stack, "cooldown")));
        wearer.getPersistentData().putLong("mic_iron_" + id + "_next", now + ticks);
        relic.addExperience(wearer, stack, 1);
    }

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || event.getSource().getDirectEntity() != attacker || event.getAmount() <= 0
                || event.getEntity() == attacker) return;
        LivingEntity victim = event.getEntity();
        ServerLevel level = (ServerLevel) attacker.level();
        long now = level.getGameTime();

        equipped(attacker, "bloody_voodoo_doll", (relic, stack) -> {
            if (attacker.getRandom().nextDouble() > relic.value(stack, "chance")) return;
            CompoundTag tag = victim.getPersistentData();
            if (tag.hasUUID("mic_iron_voodoo_owner")
                    && tag.getUUID("mic_iron_voodoo_owner").equals(attacker.getUUID())) {
                tag.putFloat("mic_iron_voodoo_damage", tag.getFloat("mic_iron_voodoo_damage") + event.getAmount());
            } else {
                tag.putUUID("mic_iron_voodoo_owner", attacker.getUUID());
                tag.putFloat("mic_iron_voodoo_damage", event.getAmount());
            }
            tag.putLong("mic_iron_voodoo_expiry", now + 60);
            relic.addExperience(attacker, stack, 1);
        });
        equipped(attacker, "living_flesh", (relic, stack) -> {
            CompoundTag tag = victim.getPersistentData();
            tag.putUUID("mic_iron_flesh_owner", attacker.getUUID());
            tag.putLong("mic_iron_flesh_expiry", now + 100);
            tag.putFloat("mic_iron_flesh_power", (float) relic.value(stack, "power"));
            relic.addExperience(attacker, stack, 1);
        });
        equipped(attacker, "lunar_sextant", (relic, stack) -> {
            if (level.isNight() && attacker.getRandom().nextDouble() < relic.value(stack, "chance")) {
                if (victim.hurt(attacker.damageSources().magic(), (float) relic.value(stack, "power"))) {
                    level.playSound(null, victim.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                            SoundSource.PLAYERS, 1, 0.7f);
                    relic.addExperience(attacker, stack, 1);
                }
            }
        });
        equipped(attacker, "ring_of_blades", (relic, stack) -> {
            if (!ready(attacker, "ring_of_blades", now)) return;
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            consume(attacker, "ring_of_blades", now, relic, stack);
        });
        for (String id : List.of("sealed_sword", "sealed_rapier", "sealed_claymore")) {
            equipped(attacker, id, (relic, stack) -> {
                if (!ready(attacker, id, now)) return;
                float damage = (float) relic.value(stack, "power");
                if (id.equals("sealed_rapier")) damage *= 0.7f;
                if (id.equals("sealed_claymore")) damage *= 1.5f;
                if (victim.hurt(attacker.damageSources().magic(), damage)) {
                    if (id.equals("sealed_claymore")) victim.knockback(0.8, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
                    consume(attacker, id, now, relic, stack);
                }
            });
        }
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.getItem() instanceof IronRelic relic && relic.id.equals("slicer")) {
            CompoundTag tag = weapon.getOrCreateTag();
            int combo = now - tag.getLong("mic_iron_combo_time") <= 60 ? Math.min(5, tag.getInt("mic_iron_combo") + 1) : 1;
            tag.putInt("mic_iron_combo", combo);
            tag.putLong("mic_iron_combo_time", now);
            relic.addExperience(attacker, weapon, 1);
        }
        if (weapon.getItem() instanceof IronRelic relic && relic.id.equals("shadow_claws")
                && victim instanceof Player) {
            MagicData victimMagic = MagicData.getPlayerMagicData(victim);
            float drained = Math.min(victimMagic.getMana(), (float) relic.value(weapon, "power"));
            if (drained > 0) {
                victimMagic.setMana(victimMagic.getMana() - drained);
                MagicData.getPlayerMagicData(attacker).addMana(drained);
                relic.addExperience(attacker, weapon, 2);
            }
        }
    }

    @SubscribeEvent
    public static void onDamageTaken(LivingHurtEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide() || event.getAmount() <= 0 || !(wearer.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getSource().getDirectEntity() == attacker
                && attacker.getMainHandItem().getItem() instanceof IronRelic weapon
                && weapon.id.equals("slicer")) {
            int combo = attacker.getMainHandItem().getOrCreateTag().getInt("mic_iron_combo");
            event.setAmount(event.getAmount() * (1 + 0.08f * Math.min(5, combo)));
        }
        equipped(wearer, "ring_of_elusiveness", (relic, stack) -> {
            if (!ready(wearer, "ring_of_elusiveness", now)) return;
            for (int attempt = 0; attempt < 12; attempt++) {
                BlockPos pos = wearer.blockPosition().offset(wearer.getRandom().nextInt(25) - 12,
                        wearer.getRandom().nextInt(9) - 4, wearer.getRandom().nextInt(25) - 12);
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                        || level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) continue;
                wearer.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                event.setCanceled(true);
                consume(wearer, "ring_of_elusiveness", now, relic, stack);
                break;
            }
        });
        if (event.isCanceled()) return;
        equipped(wearer, "dragon_blood_vial", (relic, stack) -> {
            if (wearer.getRandom().nextDouble() >= relic.value(stack, "chance") || !ready(wearer, "dragon_blood_vial", now)) return;
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    wearer.getBoundingBox().inflate(relic.value(stack, "radius")),
                    candidate -> candidate != wearer && candidate instanceof Enemy && !wearer.isAlliedTo(candidate))) {
                target.hurt(wearer.damageSources().magic(), (float) relic.value(stack, "power"));
            }
            consume(wearer, "dragon_blood_vial", now, relic, stack);
        });
        equipped(wearer, "cloak_of_the_bloody_feather", (relic, stack) -> {
            if (wearer.getRandom().nextDouble() >= relic.value(stack, "chance")) return;
            CompoundTag tag = wearer.getPersistentData();
            tag.putLong("mic_iron_feather_release", now + 60);
            tag.putInt("mic_iron_feather_count", Math.min(12, tag.getInt("mic_iron_feather_count") + 2));
            relic.addExperience(wearer, stack, 1);
        });
        equipped(wearer, "mask_of_hunger", (relic, stack) -> {
            if (wearer.getHealth() > wearer.getMaxHealth() * 0.35 || !ready(wearer, "mask_of_hunger", now)) return;
            float healed = 0;
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    wearer.getBoundingBox().inflate(relic.value(stack, "radius")),
                    candidate -> candidate != wearer && candidate instanceof Enemy && !wearer.isAlliedTo(candidate))) {
                if (target.hurt(wearer.damageSources().magic(), (float) relic.value(stack, "power"))) healed += 1;
                if (healed >= 3) break;
            }
            if (healed > 0) {
                wearer.heal(healed);
                consume(wearer, "mask_of_hunger", now, relic, stack);
            }
        });
        equipped(wearer, "pulsar_mantle", (relic, stack) -> {
            if (wearer.getHealth() > wearer.getMaxHealth() * 0.4 || !ready(wearer, "pulsar_mantle", now)) return;
            int hits = 0;
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    wearer.getBoundingBox().inflate(relic.value(stack, "radius")),
                    candidate -> candidate != wearer && candidate instanceof Enemy && !wearer.isAlliedTo(candidate))) {
                if (target.hurt(wearer.damageSources().magic(), (float) relic.value(stack, "power"))) hits++;
                if (hits >= 4) break;
            }
            if (hits > 0) consume(wearer, "pulsar_mantle", now, relic, stack);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        LivingEntity victim = event.getEntity();
        long now = level.getGameTime();
        if (victim instanceof ServerPlayer player && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            equipped(player, "cardiac_trap", (relic, stack) -> {
                if (!ready(player, "cardiac_trap", now)) return;
                player.setHealth(1);
                player.getPersistentData().putLong("mic_iron_heart_stop", now + 100);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
                consume(player, "cardiac_trap", now, relic, stack);
                player.level().broadcastEntityEvent(player, (byte) 35);
                event.setCanceled(true);
            });
            if (event.isCanceled()) return;
            equipped(player, "flask_of_the_red_mist", (relic, stack) -> {
                if (!ready(player, "flask_of_the_red_mist", now)) return;
                if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
                Vec3 behind = attacker.position().subtract(attacker.getLookAngle().scale(2));
                BlockPos pos = BlockPos.containing(behind);
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) return;
                player.setHealth(1);
                player.teleportTo(behind.x, behind.y, behind.z);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                consume(player, "flask_of_the_red_mist", now, relic, stack);
                player.level().broadcastEntityEvent(player, (byte) 35);
                event.setCanceled(true);
            });
            if (event.isCanceled()) return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        equipped(killer, "galaxy_devourer_diadem", (relic, stack) -> {
            if (killer.getRandom().nextDouble() >= relic.value(stack, "chance")) return;
            CompoundTag tag = killer.getPersistentData();
            tag.putLong("mic_iron_vortex_end", now + 100);
            tag.putDouble("mic_iron_vortex_x", victim.getX());
            tag.putDouble("mic_iron_vortex_y", victim.getY());
            tag.putDouble("mic_iron_vortex_z", victim.getZ());
            tag.putFloat("mic_iron_vortex_power", (float) relic.value(stack, "power"));
            relic.addExperience(killer, stack, 1);
        });
        equipped(killer, "sinner_crown", (relic, stack) -> {
            if (victim.getBbHeight() > 3 || victim.getBbHeight() < 0.5) return;
            if (level.getEntitiesOfClass(twilightforest.entity.monster.LoyalZombie.class,
                    killer.getBoundingBox().inflate(32), zombie -> zombie.getPersistentData().hasUUID("mic_iron_summon_owner")
                            && zombie.getPersistentData().getUUID("mic_iron_summon_owner").equals(killer.getUUID())).size() >= 3) return;
            twilightforest.entity.monster.LoyalZombie summoned = twilightforest.init.TFEntities.LOYAL_ZOMBIE.get().create(level);
            if (summoned == null) return;
            summoned.moveTo(victim.position());
            summoned.setOwnerUUID(killer.getUUID());
            summoned.getPersistentData().putLong("mic_iron_summon_end", now + 200);
            summoned.getPersistentData().putUUID("mic_iron_summon_owner", killer.getUUID());
            if (level.noCollision(summoned)) {
                level.addFreshEntity(summoned);
                relic.addExperience(killer, stack, 1);
            }
        });
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        CompoundTag tag = entity.getPersistentData();
        if (tag.contains("mic_iron_summon_end") && now >= tag.getLong("mic_iron_summon_end")) {
            entity.discard();
            return;
        }
        if (tag.contains("mic_iron_heart_stop") && now >= tag.getLong("mic_iron_heart_stop")) {
            tag.remove("mic_iron_heart_stop");
            if (entity.getHealth() <= 1) entity.hurt(entity.damageSources().magic(), 1000);
        }
        if (tag.contains("mic_iron_voodoo_expiry") && now >= tag.getLong("mic_iron_voodoo_expiry")) {
            if (tag.hasUUID("mic_iron_voodoo_owner") && level.getEntity(tag.getUUID("mic_iron_voodoo_owner")) instanceof LivingEntity owner) {
                float damage = Math.min(20, tag.getFloat("mic_iron_voodoo_damage") * 0.3f);
                entity.hurt(owner.damageSources().magic(), damage);
            }
            tag.remove("mic_iron_voodoo_expiry");
            tag.remove("mic_iron_voodoo_owner");
            tag.remove("mic_iron_voodoo_damage");
        }
        if (tag.contains("mic_iron_flesh_expiry")) {
            if (now >= tag.getLong("mic_iron_flesh_expiry")) {
                tag.remove("mic_iron_flesh_expiry");
                tag.remove("mic_iron_flesh_owner");
            } else if (tag.hasUUID("mic_iron_flesh_owner")
                    && level.getEntity(tag.getUUID("mic_iron_flesh_owner")) instanceof LivingEntity owner
                    && entity.distanceToSqr(owner) > 256) {
                entity.hurt(owner.damageSources().magic(), tag.getFloat("mic_iron_flesh_power"));
                tag.remove("mic_iron_flesh_expiry");
                tag.remove("mic_iron_flesh_owner");
            }
        }
        if (tag.contains("mic_iron_feather_release") && now >= tag.getLong("mic_iron_feather_release")) {
            int count = tag.getInt("mic_iron_feather_count");
            tag.remove("mic_iron_feather_release");
            tag.remove("mic_iron_feather_count");
            equipped(entity, "cloak_of_the_bloody_feather", (relic, stack) -> {
                int hits = 0;
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                        entity.getBoundingBox().inflate(relic.value(stack, "radius")),
                        candidate -> candidate != entity && candidate instanceof Enemy && !entity.isAlliedTo(candidate))) {
                    if (target.hurt(entity.damageSources().magic(), (float) relic.value(stack, "power"))) hits++;
                    if (hits >= count) break;
                }
                if (hits > 0) relic.addExperience(entity, stack, hits);
            });
        }
        if (tag.contains("mic_iron_vortex_end")) {
            if (now >= tag.getLong("mic_iron_vortex_end")) {
                tag.remove("mic_iron_vortex_end");
            } else if (now % 10 == 0) {
                Vec3 center = new Vec3(tag.getDouble("mic_iron_vortex_x"), tag.getDouble("mic_iron_vortex_y"), tag.getDouble("mic_iron_vortex_z"));
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(center, center).inflate(8), candidate -> candidate instanceof Enemy && !entity.isAlliedTo(candidate)
                                && candidate.position().distanceToSqr(center) < 64)) {
                    Vec3 pull = center.subtract(target.position()).normalize().scale(0.25);
                    target.push(pull.x, pull.y, pull.z);
                    target.hurt(entity.damageSources().magic(), tag.getFloat("mic_iron_vortex_power") / 2);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer wearer) || event.getAmount() <= 0) return;
        equipped(wearer, "immaterial_disperser", (relic, stack) -> {
            if (wearer.getRandom().nextDouble() >= relic.value(stack, "chance")) return;
            event.setAmount(0);
            relic.addExperience(wearer, stack, 1);
        });
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        CompoundTag previous = event.getOriginal().getPersistentData();
        CompoundTag current = event.getEntity().getPersistentData();
        for (String id : List.of("cardiac_trap", "flask_of_the_red_mist", "ring_of_elusiveness")) {
            String key = "mic_iron_" + id + "_next";
            if (previous.contains(key)) current.putLong(key, previous.getLong(key));
        }
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (name == null || !name.getNamespace().equals("irons_spellbooks")
                || !name.getPath().startsWith("chests/") || name.getPath().startsWith("chests/additional_")) return;
        LootPool.Builder pool = LootPool.lootPool().name("myinfinitecreation_iron_relics")
                .setRolls(ConstantValue.exactly(1)).when(LootItemRandomChanceCondition.randomChance(0.12f));
        ALL.values().forEach(item -> pool.add(LootItem.lootTableItem(item.get())));
        event.getTable().addPool(pool.build());
    }
}
