package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class GiantGlove extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("giant_glove", GiantGlove::new);

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("oversized_grip")
                                .stat(StatData.builder("multiplier")
                                        .initialValue(0.05, 0.15)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.035).build())
                                .build())
                        .build())
                .leveling(LevelingData.builder().build())
                .build();
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        super.onUnequip(context, newStack, stack);
        if (newStack.getItem() != stack.getItem()) {
            clearModifiers(context.entity(), context.identifier());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        Player player = event.player;
        CuriosApi.getCuriosInventory(player).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (!(stack.getItem() instanceof GiantGlove glove)) {
                    continue;
                }
                String identifier = slot.slotContext().identifier();
                clearModifiers(player, identifier);
                ItemStack held = player.getMainHandItem();
                if (held.isEmpty()) {
                    continue;
                }
                UUID modifierId = modifierId(identifier);
                double multiplier = glove.getAbilityValue(stack, "oversized_grip", "multiplier");
                Set<Attribute> modified = new HashSet<>();
                held.getAttributeModifiers(EquipmentSlot.MAINHAND).forEach((attribute, heldModifier) -> {
                    if (!modified.add(attribute)) {
                        return;
                    }
                    AttributeInstance instance = player.getAttribute(attribute);
                    if (instance == null) {
                        return;
                    }
                    AttributeModifier modifier;
                    if (instance.getBaseValue() < 0) {
                        modifier = new AttributeModifier(modifierId, "Giant Glove",
                                Math.abs(instance.getValue() * multiplier), AttributeModifier.Operation.ADDITION);
                    } else {
                        modifier = new AttributeModifier(modifierId, "Giant Glove",
                                multiplier, AttributeModifier.Operation.MULTIPLY_TOTAL);
                    }
                    instance.addTransientModifier(modifier);
                });
            }
        });
    }

    @SubscribeEvent
    public static void onHit(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getSource().getDirectEntity() instanceof LivingEntity source)
                || source.getMainHandItem().isEmpty()) {
            return;
        }
        CuriosApi.getCuriosInventory(source).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (stack.getItem() instanceof GiantGlove glove) {
                    glove.addExperience(source, stack, 1);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide() || player.getMainHandItem().isEmpty()
                || event.getState().getDestroySpeed(event.getLevel(), event.getPos()) <= 0) {
            return;
        }
        CuriosApi.getCuriosInventory(player).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (stack.getItem() instanceof GiantGlove glove) {
                    glove.addExperience(player, stack, 1);
                }
            }
        });
    }

    private static UUID modifierId(String slotName) {
        return UUID.nameUUIDFromBytes((MyInfiniteCreation.MOD_ID + ":giant_glove_" + slotName)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static void clearModifiers(LivingEntity wearer, String slotName) {
        UUID id = modifierId(slotName);
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES) {
            AttributeInstance instance = wearer.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(id);
            }
        }
    }
}
