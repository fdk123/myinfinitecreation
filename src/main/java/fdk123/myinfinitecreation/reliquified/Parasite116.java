package fdk123.myinfinitecreation.reliquified;

import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;

public final class Parasite116 extends Parasite115 {
    private static final UUID DAMAGE_ID = UUID.fromString("d9962782-1f29-4ba2-9425-31a00a5e01ab");
    private static final UUID SPEED_ID = UUID.fromString("c76dbc7b-f00d-4f13-93f7-f6b7b1ac43e3");
    private static final UUID MOVE_ID = UUID.fromString("1ac5d49f-6778-4cf1-b2ef-54fd93e8fa7a");
    private static final UUID STEP_ID = UUID.fromString("7c7d5997-805c-43f7-9e1e-510eae86c932");

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder().abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("infectious_bloom")
                                .stat(StatData.builder("chance").initialValue(0.1, 0.15)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.4).build())
                                .stat(StatData.builder("max_attacks").initialValue(1, 3)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.5).build())
                                .stat(StatData.builder("damage").initialValue(0.75, 1.25)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.54).build())
                                .stat(StatData.builder("drops").initialValue(2, 5)
                                        .upgradeModifier(UpgradeOperation.ADD, 1).build())
                                .maxLevel(10).build())
                        .ability(AbilityData.builder("rage_consumption")
                                .stat(StatData.builder("amount_restored").initialValue(50, 100)
                                        .upgradeModifier(UpgradeOperation.ADD, 20).build())
                                .stat(StatData.builder("multiplier").initialValue(0.4, 0.75)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.25).build())
                                .requiredLevel(5).maxLevel(5).build()).build())
                .leveling(LevelingData.builder().maxLevel(10).build()).build();
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (!context.entity().level().isClientSide()) {
            int charge = stack.getOrCreateTag().getInt("mic_parasite_rage");
            if (charge > 0) {
                stack.getOrCreateTag().putInt("mic_parasite_rage", charge - 1);
            }
            updateModifiers(context, stack, charge);
        }
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        super.onUnequip(context, newStack, stack);
        if (!context.entity().level().isClientSide()) {
            updateModifiers(context, stack, 0);
        }
    }

    private void updateModifiers(SlotContext context, ItemStack stack, int charge) {
        AttributeInstance damage = context.entity().getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance attackSpeed = context.entity().getAttribute(Attributes.ATTACK_SPEED);
        AttributeInstance movement = context.entity().getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance step = context.entity().getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (damage != null) damage.removeModifier(DAMAGE_ID);
        if (attackSpeed != null) attackSpeed.removeModifier(SPEED_ID);
        if (movement != null) movement.removeModifier(MOVE_ID);
        if (step != null) step.removeModifier(STEP_ID);
        if (charge <= 0 || getLevel(stack) < 5) return;
        double power = getAbilityValue(stack, "rage_consumption", "multiplier") * charge / 200.0;
        if (damage != null) damage.addTransientModifier(new AttributeModifier(DAMAGE_ID,
                "Parasite rage damage", power, AttributeModifier.Operation.MULTIPLY_TOTAL));
        if (attackSpeed != null) attackSpeed.addTransientModifier(new AttributeModifier(SPEED_ID,
                "Parasite rage attack speed", power, AttributeModifier.Operation.MULTIPLY_TOTAL));
        if (movement != null) movement.addTransientModifier(new AttributeModifier(MOVE_ID,
                "Parasite rage move speed", power, AttributeModifier.Operation.MULTIPLY_TOTAL));
        if (step != null) step.addTransientModifier(new AttributeModifier(STEP_ID,
                "Parasite rage step height", charge * 1.5 / 200,
                AttributeModifier.Operation.ADDITION));
    }
}
