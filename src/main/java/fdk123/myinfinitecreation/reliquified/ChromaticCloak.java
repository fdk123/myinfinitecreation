package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.SlotContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChromaticCloak extends RelicStorageItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("chromatic_cloak", ChromaticCloak::new);
    private static final Map<Item, MobEffect> WOOL_EFFECTS = Map.ofEntries(
            Map.entry(Items.RED_WOOL, MobEffects.HEALTH_BOOST),
            Map.entry(Items.ORANGE_WOOL, MobEffects.FIRE_RESISTANCE),
            Map.entry(Items.YELLOW_WOOL, MobEffects.DAMAGE_BOOST),
            Map.entry(Items.LIME_WOOL, MobEffects.JUMP),
            Map.entry(Items.CYAN_WOOL, MobEffects.WATER_BREATHING),
            Map.entry(Items.LIGHT_BLUE_WOOL, MobEffects.MOVEMENT_SPEED),
            Map.entry(Items.BLUE_WOOL, MobEffects.NIGHT_VISION),
            Map.entry(Items.GREEN_WOOL, MobEffects.LUCK),
            Map.entry(Items.PURPLE_WOOL, MobEffects.DAMAGE_RESISTANCE),
            Map.entry(Items.LIGHT_GRAY_WOOL, MobEffects.INVISIBILITY),
            Map.entry(Items.PINK_WOOL, MobEffects.SLOW_FALLING),
            Map.entry(Items.BROWN_WOOL, MobEffects.DIG_SPEED),
            Map.entry(Items.GRAY_WOOL, MobEffects.LEVITATION),
            Map.entry(Items.WHITE_WOOL, MobEffects.GLOWING),
            Map.entry(Items.BLACK_WOOL, MobEffects.BAD_OMEN),
            Map.entry(Items.MAGENTA_WOOL, MobEffects.REGENERATION));

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder().abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("wool_storage")
                                .stat(StatData.builder("max_slots").initialValue(3, 6)
                                        .upgradeModifier(UpgradeOperation.ADD, 1).build())
                                .stat(StatData.builder("max_stack_size").initialValue(5, 8)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.5).build())
                                .stat(StatData.builder("duration").initialValue(200, 280)
                                        .upgradeModifier(UpgradeOperation.ADD, 30).build())
                                .build())
                        .ability(AbilityData.builder("effect_stacking")
                                .stat(StatData.builder("max_amplifier").initialValue(2, 4)
                                        .upgradeModifier(UpgradeOperation.ADD, 1).build())
                                .requiredLevel(5).requiredPoints(2).build())
                        .build())
                .leveling(LevelingData.builder().maxLevel(10).build()).build();
    }

    @Override
    protected boolean accepts(ItemStack item) {
        return WOOL_EFFECTS.containsKey(item.getItem());
    }

    @Override
    protected int maxSlots(ItemStack relic) {
        return Math.max(1, (int) Math.round(getAbilityValue(relic, "wool_storage", "max_slots")));
    }

    @Override
    protected int maxStackSize(ItemStack relic) {
        return Math.max(1, (int) Math.round(getAbilityValue(relic, "wool_storage", "max_stack_size")));
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (context.entity().level().isClientSide() || !(context.entity() instanceof Player player)) {
            return;
        }
        List<ItemStack> stored = contents(stack);
        Map<MobEffect, Integer> toApply = new HashMap<>();
        int maxAmplifier = getLevel(stack) >= 5
                ? Math.max(1, (int) Math.round(getAbilityValue(stack, "effect_stacking", "max_amplifier"))) : 1;
        for (ItemStack item : stored) {
            MobEffect effect = WOOL_EFFECTS.get(item.getItem());
            if (effect == null || toApply.getOrDefault(effect, 0) >= maxAmplifier) {
                continue;
            }
            int durationOffset = durationOffset(effect);
            MobEffectInstance current = player.getEffect(effect);
            if (current != null && current.getDuration() > durationOffset) {
                continue;
            }
            item.shrink(1);
            toApply.merge(effect, 1, Integer::sum);
        }
        if (!toApply.isEmpty()) {
            stored.removeIf(ItemStack::isEmpty);
            setContents(stack, stored);
            int baseDuration = Math.max(1, (int) Math.round(getAbilityValue(stack,
                    "wool_storage", "duration")));
            toApply.forEach((effect, amplifier) -> {
                player.addEffect(new MobEffectInstance(effect, baseDuration + durationOffset(effect),
                        amplifier - 1));
                addExperience(player, stack, amplifier);
            });
        }
        dropExcess(player, stack);
    }

    private static int durationOffset(MobEffect effect) {
        if (effect == MobEffects.NIGHT_VISION) return 210;
        if (effect == MobEffects.HEALTH_BOOST || effect == MobEffects.ABSORPTION) return 10;
        return 1;
    }
}
