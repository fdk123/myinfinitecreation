package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import twilightforest.init.TFItems;

public final class MapleSyrupBottle extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("maple_syrup_bottle", MapleSyrupBottle::new);
    private static final String REGEN_TICKS = "mic_maple_regeneration_ticks";
    private static final String PRESERVE_WAFER = "mic_maple_preserve_wafer";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("sugar_rush")
                                .stat(StatData.builder("chance")
                                        .initialValue(0.1, 0.2)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.055).build())
                                .stat(StatData.builder("regen_multiplier")
                                        .initialValue(0.3, 0.5)
                                        .upgradeModifier(UpgradeOperation.ADD, 0.15).build())
                                .stat(StatData.builder("regen_time")
                                        .initialValue(140, 200)
                                        .upgradeModifier(UpgradeOperation.ADD, 20).build())
                                .build())
                        .build())
                .leveling(LevelingData.builder().build())
                .build();
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (!context.entity().level().isClientSide()) {
            int ticks = stack.getOrCreateTag().getInt(REGEN_TICKS);
            if (ticks > 0) {
                stack.getOrCreateTag().putInt(REGEN_TICKS, ticks - 1);
            }
        }
    }

    @SubscribeEvent
    public static void onEatStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity().level().isClientSide() || event.getItem().getItem() != TFItems.MAZE_WAFER.get()) {
            return;
        }
        event.getItem().getOrCreateTag().remove(PRESERVE_WAFER);
        CuriosApi.getCuriosInventory(event.getEntity()).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (stack.getItem() instanceof MapleSyrupBottle bottle
                        && event.getEntity().getRandom().nextDouble()
                        < Math.max(0, Math.min(1, bottle.getAbilityValue(stack, "sugar_rush", "chance")))) {
                    event.getItem().getOrCreateTag().putBoolean(PRESERVE_WAFER, true);
                    break;
                }
            }
        });
    }

    @SubscribeEvent
    public static void onEatFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getItem().getItem() != TFItems.MAZE_WAFER.get()) {
            return;
        }
        boolean preserve = event.getItem().hasTag() && event.getItem().getTag().getBoolean(PRESERVE_WAFER);
        if (preserve) {
            ItemStack returned = event.getItem().copy();
            returned.getOrCreateTag().remove(PRESERVE_WAFER);
            event.setResultStack(returned);
        }
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        CuriosApi.getCuriosInventory(event.getEntity()).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (stack.getItem() instanceof MapleSyrupBottle bottle) {
                    int ticks = (int) Math.round(bottle.getAbilityValue(stack, "sugar_rush", "regen_time"));
                    stack.getOrCreateTag().putInt(REGEN_TICKS,
                            Math.max(0, stack.getOrCreateTag().getInt(REGEN_TICKS) + ticks));
                    bottle.addExperience(event.getEntity(), stack, 1);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        CuriosApi.getCuriosInventory(event.getEntity()).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (stack.getItem() instanceof MapleSyrupBottle bottle
                        && stack.getOrCreateTag().getInt(REGEN_TICKS) > 0) {
                    event.setAmount(event.getAmount() * (float) (1.0
                            + bottle.getAbilityValue(stack, "sugar_rush", "regen_multiplier")));
                }
            }
        });
    }
}
