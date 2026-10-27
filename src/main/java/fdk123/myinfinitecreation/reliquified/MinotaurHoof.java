package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import twilightforest.init.TFEntities;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class MinotaurHoof extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("minotaur_hoof", MinotaurHoof::new);
    private static final UUID MODIFIER_ID = UUID.nameUUIDFromBytes(
            "myinfinitecreation:momentum_rush".getBytes(StandardCharsets.UTF_8));
    private static final String MOMENTUM = "mic_minotaur_momentum";
    private static final int MAX_MOMENTUM = 60;

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("momentum_rush")
                                .stat(StatData.builder("max_speed_multiplier")
                                        .initialValue(0.25, 0.3).upgradeModifier(UpgradeOperation.ADD, 0.1).build())
                                .stat(StatData.builder("damage_reduction")
                                        .initialValue(0.25, 0.3).upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.125).build())
                                .stat(StatData.builder("damage")
                                        .initialValue(1, 2).upgradeModifier(UpgradeOperation.ADD, 1).build())
                                .build())
                        .build())
                .leveling(LevelingData.builder().build())
                .build();
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (!(context.entity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        int momentum = Mth.clamp(stack.getOrCreateTag().getInt(MOMENTUM) + (player.isSprinting() ? 1 : -1),
                0, MAX_MOMENTUM);
        stack.getOrCreateTag().putInt(MOMENTUM, momentum);
        removeModifiers(player);
        if (momentum == 0) {
            return;
        }
        double charge = momentum / (double) MAX_MOMENTUM;
        addModifier(player, Attributes.MOVEMENT_SPEED, "Momentum Rush Speed",
                getAbilityValue(stack, "momentum_rush", "max_speed_multiplier") * charge,
                AttributeModifier.Operation.MULTIPLY_BASE);
        addModifier(player, Attributes.KNOCKBACK_RESISTANCE, "Momentum Rush Knockback",
                charge, AttributeModifier.Operation.ADDITION);
        addModifier(player, ForgeMod.STEP_HEIGHT_ADDITION.get(), "Momentum Rush Step",
                momentum == MAX_MOMENTUM ? 0.55 : 0.0, AttributeModifier.Operation.ADDITION);
        if (momentum == MAX_MOMENTUM) {
            float damage = (float) getAbilityValue(stack, "momentum_rush", "damage");
            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(0.025), living -> living != player && !player.isAlliedTo(living))) {
                if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                    addExperience(player, stack, 1);
                }
            }
        }
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        super.onUnequip(context, newStack, stack);
        if (!context.entity().level().isClientSide() && newStack.getItem() != stack.getItem()) {
            stack.getOrCreateTag().putInt(MOMENTUM, 0);
            removeModifiers(context.entity());
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        CuriosApi.getCuriosInventory(event.getEntity()).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (stack.getItem() instanceof MinotaurHoof hoof
                        && stack.getOrCreateTag().getInt(MOMENTUM) == MAX_MOMENTUM) {
                    double reduction = Mth.clamp(
                            hoof.getAbilityValue(stack, "momentum_rush", "damage_reduction"), 0, 1);
                    event.setAmount((float) (event.getAmount() * (1 - reduction)));
                }
            }
        });
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide() || event.getEntity().getType() != TFEntities.MINOSHROOM.get()
                || event.getEntity().getRandom().nextFloat() >= 0.33f) {
            return;
        }
        event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(),
                event.getEntity().getY(), event.getEntity().getZ(), new ItemStack(ITEM.get())));
    }

    private static void addModifier(LivingEntity wearer, Attribute attribute, String name,
            double value, AttributeModifier.Operation operation) {
        AttributeInstance instance = wearer.getAttribute(attribute);
        if (instance != null) {
            instance.addTransientModifier(new AttributeModifier(MODIFIER_ID, name, value, operation));
        }
    }

    private static void removeModifiers(LivingEntity wearer) {
        for (Attribute attribute : new Attribute[] {Attributes.MOVEMENT_SPEED, Attributes.KNOCKBACK_RESISTANCE,
                ForgeMod.STEP_HEIGHT_ADDITION.get()}) {
            AttributeInstance instance = wearer.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(MODIFIER_ID);
            }
        }
    }
}
