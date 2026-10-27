package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

public final class GoblinNose extends RelicStorageItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("goblin_nose", GoblinNose::new);

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder().abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("vein_seeker")
                                .stat(StatData.builder("radius").initialValue(4, 8)
                                        .upgradeModifier(UpgradeOperation.ADD, 1.6).build())
                                .stat(StatData.builder("filter_slots").initialValue(0, 0)
                                        .upgradeModifier(UpgradeOperation.ADD, 1).build())
                                .active(CastData.builder().type(CastType.TOGGLEABLE).build())
                                .maxLevel(5).build()).build())
                .leveling(LevelingData.builder().maxLevel(5).build()).build();
    }

    @Override
    protected boolean accepts(ItemStack item) {
        return item.getItem() instanceof BlockItem block
                && block.getBlock().defaultBlockState().is(Tags.Blocks.ORES);
    }

    @Override
    protected int maxSlots(ItemStack relic) {
        return Math.max(0, (int) Math.round(getAbilityValue(relic, "vein_seeker", "filter_slots")));
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (!(context.entity() instanceof ServerPlayer player) || !isAbilityTicking(stack, "vein_seeker")) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, true, false, false));
        if (player.tickCount % 20 != 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int radius = Math.min(16, Math.max(1, (int) Math.round(getAbilityValue(stack,
                "vein_seeker", "radius"))));
        BlockPos center = player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        List<ItemStack> filter = contents(stack);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            double distance = pos.distSqr(center);
            if (distance >= bestDistance || distance > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(Tags.Blocks.ORES) || !filter.isEmpty()
                    && filter.stream().noneMatch(item -> item.is(state.getBlock().asItem()))) {
                continue;
            }
            best = pos.immutable();
            bestDistance = distance;
        }
        if (best == null) {
            player.displayClientMessage(Component.translatable("message.myinfinitecreation.goblin_nose.none"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.myinfinitecreation.goblin_nose.found",
                    level.getBlockState(best).getBlock().getName(), best.getX() - center.getX(),
                    best.getY() - center.getY(), best.getZ() - center.getZ(),
                    Math.round(Math.sqrt(bestDistance))), true);
        }
        dropExcess(player, stack);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer().level().isClientSide() || !event.getState().is(Tags.Blocks.ORES)) {
            return;
        }
        CuriosApi.getCuriosInventory(event.getPlayer()).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (stack.getItem() instanceof GoblinNose nose && nose.isAbilityTicking(stack, "vein_seeker")) {
                    nose.addExperience(event.getPlayer(), stack, 1);
                }
            }
        });
    }
}
