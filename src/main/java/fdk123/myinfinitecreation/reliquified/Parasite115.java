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
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import twilightforest.entity.boss.UrGhast;
import twilightforest.init.TFItems;

import java.util.UUID;

public class Parasite115 extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("parasite_115", Parasite115::new);
    public static final RegistryObject<Item> EVOLVED = ITEMS.register("parasite_116", Parasite116::new);
    private static final String INFECTIONS = "mic_parasite_infections";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder().abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("infectious_bloom")
                                .stat(StatData.builder("chance").initialValue(0.075, 0.125)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.36).build())
                                .stat(StatData.builder("max_attacks").initialValue(1, 3)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.5).build())
                                .stat(StatData.builder("damage").initialValue(0.5, 1)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.5).build())
                                .stat(StatData.builder("drops").initialValue(1, 3)
                                        .upgradeModifier(UpgradeOperation.ADD, 1).build())
                                .maxLevel(5).build()).build())
                .leveling(LevelingData.builder().maxLevel(5).build()).build();
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.getSource() instanceof BloomDamageSource
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || attacker == victim || event.getAmount() < 1) {
            return;
        }
        CuriosApi.getCuriosInventory(attacker).ifPresent(curios -> {
            CompoundTag infections = victim.getPersistentData().getCompound(INFECTIONS);
            for (var slot : curios.findCurios(stack -> stack.getItem() instanceof Parasite115)) {
                ItemStack stack = slot.stack();
                Parasite115 relic = (Parasite115) stack.getItem();
                String key = attacker.getStringUUID() + "+" + slot.slotContext().identifier()
                        + "+" + slot.slotContext().index();
                if (infections.contains(key)) {
                    CompoundTag infection = infections.getCompound(key);
                    infection.putInt("ticks", 100 + infection.getInt("attacks"));
                } else if (attacker.getRandom().nextDouble()
                        < relic.getAbilityValue(stack, "infectious_bloom", "chance")) {
                    CompoundTag infection = new CompoundTag();
                    int attacks = Math.max(1, (int) Math.round(relic.getAbilityValue(stack,
                            "infectious_bloom", "max_attacks")));
                    infection.putInt("attacks", attacks);
                    infection.putInt("ticks", 100 + attacks);
                    infection.putFloat("damage", (float) relic.getAbilityValue(stack,
                            "infectious_bloom", "damage"));
                    infection.putInt("drops", Math.max(1, (int) Math.round(relic.getAbilityValue(stack,
                            "infectious_bloom", "drops"))));
                    infections.put(key, infection);
                    relic.addExperience(attacker, stack, 1);
                }
            }
            victim.getPersistentData().put(INFECTIONS, infections);
        });
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag infections = victim.getPersistentData().getCompound(INFECTIONS);
        if (infections.isEmpty()) {
            return;
        }
        CompoundTag remaining = new CompoundTag();
        for (String key : infections.getAllKeys()) {
            CompoundTag infection = infections.getCompound(key);
            int ticks = infection.getInt("ticks") - 1;
            if (ticks <= 0) {
                continue;
            }
            infection.putInt("ticks", ticks);
            remaining.put(key, infection);
            if (ticks > infection.getInt("attacks")) {
                continue;
            }
            String[] parts = key.split("\\+");
            if (parts.length != 3) {
                continue;
            }
            Entity source;
            try {
                source = level.getEntity(UUID.fromString(parts[0]));
            } catch (IllegalArgumentException exception) {
                continue;
            }
            if (source == null) {
                continue;
            }
            victim.invulnerableTime = 0;
            victim.hurt(new BloomDamageSource(level, source, infection.getInt("drops"),
                    parts[1], Integer.parseInt(parts[2])), infection.getFloat("damage"));
        }
        victim.getPersistentData().put(INFECTIONS, remaining);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource() instanceof BloomDamageSource source)
                || !(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        CuriosApi.getCuriosInventory(attacker).ifPresent(curios -> curios.findCurio(source.identifier,
                source.index).ifPresent(slot -> {
            ItemStack stack = slot.stack();
            if (!(stack.getItem() instanceof Parasite115 relic)) {
                return;
            }
            int drops = 1 + event.getEntity().getRandom().nextInt(Math.max(1, source.maxDrops));
            for (int i = 0; i < drops; i++) {
                event.getEntity().level().addFreshEntity(new ItemEntity(event.getEntity().level(),
                        event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(),
                        new ItemStack(TFItems.EXPERIMENT_115.get())));
            }
            if (event.getEntity() instanceof UrGhast && !(relic instanceof Parasite116)) {
                ItemStack evolved = new ItemStack(EVOLVED.get());
                if (stack.hasTag()) {
                    evolved.setTag(stack.getTag().copy());
                }
                curios.setEquippedCurio(source.identifier, source.index, evolved);
                attacker.level().playSound(null, attacker, SoundEvents.BEACON_ACTIVATE,
                        SoundSource.PLAYERS, 1, 1.5f);
            }
        }));
    }

    @SubscribeEvent
    public static void onConsume(LivingEntityUseItemEvent.Finish event) {
        if (!event.getEntity().level().isClientSide() && event.getItem().is(TFItems.EXPERIMENT_115.get())) {
            CuriosApi.getCuriosInventory(event.getEntity()).ifPresent(curios -> {
                for (var slot : curios.findCurios(EVOLVED.get())) {
                    ItemStack stack = slot.stack();
                    if (stack.getItem() instanceof Parasite116 relic && relic.getLevel(stack) >= 5) {
                        int charge = stack.getOrCreateTag().getInt("mic_parasite_rage");
                        int gain = (int) Math.round(relic.getAbilityValue(stack,
                                "rage_consumption", "amount_restored"));
                        stack.getOrCreateTag().putInt("mic_parasite_rage", Math.min(200, charge + gain));
                        relic.addExperience(event.getEntity(), stack, 1);
                    }
                }
            });
        }
    }

    private static final class BloomDamageSource extends DamageSource {
        private final int maxDrops;
        private final String identifier;
        private final int index;

        private BloomDamageSource(ServerLevel level, Entity owner, int maxDrops, String identifier, int index) {
            super(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.MAGIC), owner, owner);
            this.maxDrops = maxDrops;
            this.identifier = identifier;
            this.index = index;
        }
    }
}
