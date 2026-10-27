package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.SlotContext;
import twilightforest.capabilities.CapabilityList;
import twilightforest.entity.monster.LoyalZombie;
import twilightforest.init.TFEntities;
import twilightforest.init.TFDamageTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LichCrown extends RelicStorageItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("lich_crown", LichCrown::new);
    private static final ResourceLocation TWILIGHT = ResourceLocation.fromNamespaceAndPath("twilightforest", "twilight_forest");
    private static final String ZOMBIES = "mic_lich_zombies";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder().abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("bone_pact").maxLevel(0).build())
                        .ability(AbilityData.builder("soulbound_gems")
                                .stat(stat("gem_amount", 1, 3, 1)).maxLevel(15).build())
                        .ability(AbilityData.builder("fortification")
                                .stat(stat("max_shields", 3, 5, 1))
                                .stat(stat("interval", 200, 160, -15)).maxLevel(15).build())
                        .ability(AbilityData.builder("zombie")
                                .stat(stat("max_zombies", 1, 2, 1))
                                .stat(stat("damage", 2, 3, 0.5))
                                .stat(stat("interval", 400, 320, -25)).maxLevel(15).build())
                        .ability(AbilityData.builder("twilight")
                                .stat(stat("damage", 3, 4, 0.5))
                                .stat(stat("speed_scale", 0.9, 1.2, 0.1)).maxLevel(15).build())
                        .ability(AbilityData.builder("lifedrain")
                                .stat(stat("heal_percentage", 0.0075, 0.015, 0.0035))
                                .stat(stat("radius", 2, 5, 0.5))
                                .stat(stat("interval", 300, 260, -20)).maxLevel(15).build())
                        .ability(AbilityData.builder("frostbite")
                                .stat(stat("duration", 80, 100, 10)).maxLevel(15).build())
                        .ability(AbilityData.builder("biome_burn")
                                .stat(stat("multiplier", 0.02, 0.05, 0.01)).maxLevel(15).build())
                        .ability(AbilityData.builder("vendetta")
                                .stat(stat("lifetime", 6, 10, 2))
                                .stat(stat("multiplier", 0.25, 0.5, 0.1)).maxLevel(15).build())
                        .ability(AbilityData.builder("ethereal_guard")
                                .stat(stat("max_projectiles", 3, 6, 2))
                                .stat(stat("cooldown", 300, 200, -15)).maxLevel(15).build())
                        .ability(AbilityData.builder("mirror_leech")
                                .stat(stat("chance", 0.05, 0.1, 0.02)).maxLevel(15).build())
                        .ability(AbilityData.builder("frenzy")
                                .stat(stat("additive_multiplier", 0.025, 0.05, 0.02)).maxLevel(15).build())
                        .ability(AbilityData.builder("twilight_sovereign").maxLevel(0).build())
                        .build())
                .leveling(LevelingData.builder().initialCost(250).step(250).maxLevel(15).build())
                .build();
    }

    private static StatData stat(String name, double min, double max, double increment) {
        return StatData.builder(name).initialValue(min, max)
                .upgradeModifier(UpgradeOperation.ADD, increment).build();
    }

    @Override
    protected boolean accepts(ItemStack item) {
        return item.getItem() instanceof LichGem;
    }

    @Override
    protected int maxSlots(ItemStack relic) {
        return Math.max(1, (int) Math.round(getAbilityValue(relic, "soulbound_gems", "gem_amount")));
    }

    public int gemCount(ItemStack stack, String ability) {
        RegistryObject<Item> gem = LichGem.BY_ABILITY.get(ability);
        if (gem == null) return 0;
        return (int) contents(stack).stream().filter(item -> item.is(gem.get())).count();
    }

    public boolean hasGem(ItemStack stack, String ability) {
        return gemCount(stack, ability) > 0;
    }

    public boolean isSovereign(ItemStack stack, LivingEntity wearer) {
        return wearer.level().dimension().location().equals(TWILIGHT)
                && LichGem.BY_ABILITY.keySet().stream().allMatch(ability -> hasGem(stack, ability));
    }

    @Override
    public int getAbilityPoints(ItemStack stack, String ability) {
        if (LichGem.BY_ABILITY.containsKey(ability)) {
            return gemCount(stack, ability);
        }
        return super.getAbilityPoints(stack, ability);
    }

    @Override
    public boolean mayUpgrade(ItemStack stack, String ability) {
        return !LichGem.BY_ABILITY.containsKey(ability) && super.mayUpgrade(stack, ability);
    }

    @Override
    public boolean mayReset(ItemStack stack, String ability) {
        return !LichGem.BY_ABILITY.containsKey(ability) && super.mayReset(stack, ability);
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        LivingEntity wearer = context.entity();
        if (!(wearer.level() instanceof ServerLevel level)) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (hasGem(stack, "fortification")) {
            tag.putBoolean("mic_lich_had_shield_gem", true);
            tickShields(wearer, stack);
        } else if (tag.getBoolean("mic_lich_had_shield_gem")) {
            wearer.getCapability(CapabilityList.SHIELDS).ifPresent(shields -> shields.setShields(0, false));
            tag.remove("mic_lich_had_shield_gem");
        }
        if (hasGem(stack, "zombie")) {
            tickZombies(level, wearer, stack);
        } else if (!zombieIds(stack).isEmpty()) {
            clearZombies(level, stack);
        }
        if (hasGem(stack, "lifedrain")) tickLifedrain(wearer, stack);
        tickTimers(stack);
        if (wearer instanceof Player player && wearer.tickCount % 20 == 0) {
            dropExcess(player, stack);
        }
    }

    private void tickShields(LivingEntity wearer, ItemStack stack) {
        int interval = Math.max(20, (int) Math.round(getAbilityValue(stack, "fortification", "interval")));
        CompoundTag tag = stack.getOrCreateTag();
        int time = tag.getInt("mic_lich_shield_timer");
        if (time > 0) {
            tag.putInt("mic_lich_shield_timer", time - 1);
            return;
        }
        wearer.getCapability(CapabilityList.SHIELDS).ifPresent(shields -> {
            int max = Math.max(1, (int) Math.round(getAbilityValue(stack, "fortification", "max_shields")));
            if (shields.permanentShieldsLeft() < max) {
                shields.addShields(1, false);
                addExperience(wearer, stack, 1);
            }
        });
        tag.putInt("mic_lich_shield_timer", interval);
    }

    private void tickZombies(ServerLevel level, LivingEntity wearer, ItemStack stack) {
        List<UUID> zombies = zombieIds(stack);
        if (wearer.tickCount % 20 == 0) {
            zombies.removeIf(id -> !(level.getEntity(id) instanceof LoyalZombie zombie) || !zombie.isAlive());
        }
        int max = Math.max(1, (int) Math.round(getAbilityValue(stack, "zombie", "max_zombies")));
        while (zombies.size() > max) {
            Entity extra = level.getEntity(zombies.remove(zombies.size() - 1));
            if (extra != null) extra.discard();
        }
        CompoundTag tag = stack.getOrCreateTag();
        int time = tag.getInt("mic_lich_zombie_timer");
        if (time > 0) {
            tag.putInt("mic_lich_zombie_timer", time - 1);
        } else if (zombies.size() < max) {
            LoyalZombie zombie = TFEntities.LOYAL_ZOMBIE.get().create(level);
            if (zombie != null) {
                zombie.moveTo(wearer.getX(), wearer.getY(), wearer.getZ(), wearer.getYRot(), 0);
                zombie.setTame(true);
                zombie.setOwnerUUID(wearer.getUUID());
                zombie.setBaby(true);
                if (zombie.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                    zombie.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(getAbilityValue(stack,
                            "zombie", "damage"));
                }
                if (level.noCollision(zombie)) {
                    level.addFreshEntity(zombie);
                    zombies.add(zombie.getUUID());
                    addExperience(wearer, stack, 1);
                }
            }
            tag.putInt("mic_lich_zombie_timer", Math.max(20,
                    (int) Math.round(getAbilityValue(stack, "zombie", "interval"))));
        }
        setZombieIds(stack, zombies);
    }

    private void tickLifedrain(LivingEntity wearer, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        int active = tag.getInt("mic_lich_lifedrain_active");
        if (active > 0) {
            tag.putInt("mic_lich_lifedrain_active", active - 1);
            if (wearer.tickCount % 5 == 0 && wearer.getHealth() < wearer.getMaxHealth()) {
                double radius = getAbilityValue(stack, "lifedrain", "radius");
                List<LivingEntity> targets = wearer.level().getEntitiesOfClass(LivingEntity.class,
                        wearer.getBoundingBox().inflate(radius), target -> target != wearer && target.isAlive()
                                && !wearer.isAlliedTo(target) && wearer.hasLineOfSight(target));
                if (!targets.isEmpty()) {
                    float heal = (float) (wearer.getMaxHealth() * getAbilityValue(stack,
                            "lifedrain", "heal_percentage"));
                    float damage = heal / targets.size();
                    for (LivingEntity target : targets) {
                        target.invulnerableTime = 0;
                        target.hurt(TFDamageTypes.getEntityDamageSource(wearer.level(),
                                TFDamageTypes.LIFEDRAIN, wearer), damage);
                    }
                    wearer.heal(heal);
                    if (wearer.tickCount % 10 == 0) addExperience(wearer, stack, 1);
                }
            }
        } else {
            int cooldown = tag.getInt("mic_lich_lifedrain_cooldown");
            if (cooldown > 0) tag.putInt("mic_lich_lifedrain_cooldown", cooldown - 1);
            else if (wearer.getHealth() <= wearer.getMaxHealth() * 0.2f) {
                tag.putInt("mic_lich_lifedrain_active", 100);
                tag.putInt("mic_lich_lifedrain_cooldown", Math.max(20,
                        (int) Math.round(getAbilityValue(stack, "lifedrain", "interval"))));
            }
        }
    }

    private void tickTimers(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        for (String key : List.of("mic_lich_twilight_cooldown", "mic_lich_ether_cooldown", "mic_lich_frenzy_time")) {
            int value = tag.getInt(key);
            if (value > 0) tag.putInt(key, value - 1);
        }
        if (tag.getInt("mic_lich_frenzy_time") == 0) {
            tag.remove("mic_lich_frenzy_targets");
            tag.remove("mic_lich_frenzy_multiplier");
        }
        if (tag.getInt("mic_lich_ether_cooldown") == 0) {
            tag.remove("mic_lich_ether_count");
        }
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        super.onUnequip(context, newStack, stack);
        if (!(context.entity().level() instanceof ServerLevel level)) return;
        clearZombies(level, stack);
        if (stack.getOrCreateTag().getBoolean("mic_lich_had_shield_gem")) {
            context.entity().getCapability(CapabilityList.SHIELDS)
                    .ifPresent(shields -> shields.setShields(0, false));
            stack.getOrCreateTag().remove("mic_lich_had_shield_gem");
        }
    }

    private static void clearZombies(ServerLevel level, ItemStack stack) {
        for (UUID id : zombieIds(stack)) {
            Entity zombie = level.getEntity(id);
            if (zombie != null) zombie.discard();
        }
        setZombieIds(stack, List.of());
    }

    private static List<UUID> zombieIds(ItemStack stack) {
        List<UUID> result = new ArrayList<>();
        ListTag list = stack.getOrCreateTag().getList(ZOMBIES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                result.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private static void setZombieIds(ItemStack stack, List<UUID> ids) {
        ListTag list = new ListTag();
        ids.forEach(id -> list.add(StringTag.valueOf(id.toString())));
        stack.getOrCreateTag().put(ZOMBIES, list);
    }
}
