package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.SlotContext;
import twilightforest.init.TFBlocks;
import twilightforest.init.TFParticleType;

public final class FireflyQueen extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("firefly_queen", FireflyQueen::new);
    private static final String CHARGES = "mic_firefly_charges";
    private static final String COOLDOWN = "mic_firefly_cooldown";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("glowkeeper")
                                .stat(StatData.builder("cooldown")
                                        .initialValue(200, 140)
                                        .upgradeModifier(UpgradeOperation.ADD, -20).build())
                                .stat(StatData.builder("max_charges")
                                        .initialValue(1, 3)
                                        .upgradeModifier(UpgradeOperation.ADD, 1).build())
                                .maxLevel(5).build())
                        .build())
                .leveling(LevelingData.builder().maxLevel(5).build())
                .build();
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        LivingEntity wearer = context.entity();
        Level level = wearer.level();
        if (level.isClientSide()) {
            if (context.visible() && wearer.tickCount % 10 == 0 && wearer.getRandom().nextFloat() < 0.6f) {
                level.addParticle(TFParticleType.FIREFLY.get(), wearer.getRandomX(0.5),
                        wearer.getRandomY(), wearer.getRandomZ(0.5), 0, 0, 0);
            }
            return;
        }

        int maxCooldown = Math.max(1, (int) Math.round(getAbilityValue(stack, "glowkeeper", "cooldown")));
        int maxCharges = Math.max(1, (int) Math.round(getAbilityValue(stack, "glowkeeper", "max_charges")));
        int cooldown = stack.getOrCreateTag().contains(COOLDOWN)
                ? stack.getTag().getInt(COOLDOWN) : maxCooldown;
        int charges = Math.min(maxCharges, stack.getOrCreateTag().getInt(CHARGES));
        BlockPos pos = wearer.blockPosition();
        BlockState firefly = TFBlocks.FIREFLY.get().defaultBlockState();

        if (charges > 0 && wearer.onGround() && !wearer.isInWaterOrBubble() && !wearer.isInLava()
                && (!level.isDay() || level.getBrightness(LightLayer.SKY, pos) == 0)
                && level.getBrightness(LightLayer.BLOCK, pos) == 0
                && firefly.canSurvive(level, pos) && level.getBlockState(pos).canBeReplaced()) {
            if (level.setBlock(pos, firefly, 3)) {
                charges--;
                addExperience(wearer, stack, 1);
            }
        }

        if (cooldown <= 0) {
            if (charges < maxCharges) {
                charges++;
                cooldown = maxCooldown;
            }
        } else {
            cooldown--;
        }
        stack.getOrCreateTag().putInt(COOLDOWN, cooldown);
        stack.getOrCreateTag().putInt(CHARGES, charges);
    }
}
