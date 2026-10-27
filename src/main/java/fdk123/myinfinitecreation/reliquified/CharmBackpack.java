package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

public final class CharmBackpack extends RelicStorageItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("charm_backpack", CharmBackpack::new);

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder().abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("charm_storage")
                                .stat(StatData.builder("max_slots").initialValue(1, 3)
                                        .upgradeModifier(UpgradeOperation.ADD, 1).build())
                                .stat(StatData.builder("repair_time").initialValue(30, 20)
                                        .upgradeModifier(UpgradeOperation.ADD, -1).build())
                                .build()).build())
                .leveling(LevelingData.builder().maxLevel(10).build()).build();
    }

    @Override
    protected boolean accepts(ItemStack item) {
        return item.getItem() instanceof BrokenCharm || BrokenCharm.brokenVersion(item.getItem()) != null;
    }

    @Override
    protected int maxSlots(ItemStack relic) {
        return Math.max(1, (int) Math.round(getAbilityValue(relic, "charm_storage", "max_slots")));
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (context.entity().level().isClientSide() || !(context.entity() instanceof Player player)) {
            return;
        }
        int interval = Math.max(1, (int) Math.round(getAbilityValue(stack, "charm_storage", "repair_time")));
        List<ItemStack> stored = contents(stack);
        boolean changed = false;
        if (player.tickCount % interval == 0) {
            for (int i = 0; i < stored.size(); i++) {
                ItemStack charm = stored.get(i);
                if (charm.getItem() instanceof BrokenCharm broken && charm.getDamageValue() > 0) {
                    charm.setDamageValue(charm.getDamageValue() - 1);
                    changed = true;
                    if (charm.getDamageValue() == 0) {
                        stored.set(i, new ItemStack(broken.original()));
                    }
                }
            }
        }
        if (changed) {
            setContents(stack, stored);
        }
        dropExcess(player, stack);
    }

    /** Called by the Twilight Forest inventory-consumption mixin. */
    public static boolean consumeStored(Player player, Item original) {
        if (BrokenCharm.brokenVersion(original) == null) {
            return false;
        }
        return CuriosApi.getCuriosInventory(player).map(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack backpack = slot.stack();
                if (!(backpack.getItem() instanceof CharmBackpack relic)) continue;
                List<ItemStack> stored = relic.contents(backpack);
                for (int i = 0; i < stored.size(); i++) {
                    if (stored.get(i).getItem() == original) {
                        stored.set(i, BrokenCharm.spent(original));
                        relic.setContents(backpack, stored);
                        relic.addExperience(player, backpack, 1);
                        return true;
                    }
                }
            }
            return false;
        }).orElse(false);
    }
}
