package fdk123.myinfinitecreation.reliquified;

import it.hurts.sskirillss.relics.api.events.common.ContainerSlotClickEvent;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Simple NBT-backed bundle behavior shared by the relics that store other items. */
public abstract class RelicStorageItem extends RelicItem {
    private static final String CONTENTS = "mic_relic_contents";

    protected abstract boolean accepts(ItemStack item);

    protected abstract int maxSlots(ItemStack relic);

    protected int maxStackSize(ItemStack relic) {
        return 1;
    }

    protected final List<ItemStack> contents(ItemStack relic) {
        List<ItemStack> result = new ArrayList<>();
        if (!relic.hasTag()) {
            return result;
        }
        ListTag list = relic.getTag().getList(CONTENTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack item = ItemStack.of(list.getCompound(i));
            if (!item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    protected final void setContents(ItemStack relic, List<ItemStack> items) {
        ListTag list = new ListTag();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                list.add(item.save(new CompoundTag()));
            }
        }
        relic.getOrCreateTag().put(CONTENTS, list);
    }

    private boolean insert(ItemStack relic, ItemStack incoming) {
        if (incoming.isEmpty() || incoming.getItem() instanceof RelicStorageItem || !accepts(incoming)) {
            return false;
        }
        List<ItemStack> items = contents(relic);
        int stackLimit = Math.max(1, maxStackSize(relic));
        for (ItemStack stored : items) {
            if (ItemStack.isSameItemSameTags(stored, incoming) && stored.getCount() < stackLimit) {
                int moved = Math.min(incoming.getCount(), stackLimit - stored.getCount());
                stored.grow(moved);
                incoming.shrink(moved);
                setContents(relic, items);
                return true;
            }
        }
        if (items.size() >= Math.max(0, maxSlots(relic))) {
            return false;
        }
        int moved = Math.min(incoming.getCount(), stackLimit);
        ItemStack stored = incoming.copy();
        stored.setCount(moved);
        incoming.shrink(moved);
        items.add(stored);
        setContents(relic, items);
        return true;
    }

    private ItemStack pop(ItemStack relic) {
        List<ItemStack> items = contents(relic);
        if (items.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = items.remove(items.size() - 1);
        setContents(relic, items);
        return result;
    }

    protected final void dropExcess(Player player, ItemStack relic) {
        List<ItemStack> items = contents(relic);
        int slots = Math.max(0, maxSlots(relic));
        int stackLimit = Math.max(1, maxStackSize(relic));
        boolean changed = false;
        while (items.size() > slots) {
            player.drop(items.remove(items.size() - 1), false);
            changed = true;
        }
        for (ItemStack item : items) {
            if (item.getCount() > stackLimit) {
                ItemStack excess = item.copy();
                excess.setCount(item.getCount() - stackLimit);
                item.setCount(stackLimit);
                player.drop(excess, false);
                changed = true;
            }
        }
        if (changed) {
            setContents(relic, items);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        List<ItemStack> items = contents(stack);
        tooltip.add(Component.translatable("tooltip.myinfinitecreation.relic_storage.slots",
                items.size(), maxSlots(stack)));
        for (ItemStack item : items) {
            tooltip.add(Component.literal("  ").append(item.getHoverName()).append(" x" + item.getCount()));
        }
    }

    @SubscribeEvent
    public static void onSlotClick(ContainerSlotClickEvent event) {
        if (event.getAction() != ClickAction.SECONDARY) {
            return;
        }
        ItemStack slot = event.getSlotStack();
        ItemStack held = event.getHeldStack();
        if (slot.getItem() instanceof RelicStorageItem storage) {
            if (!held.isEmpty() && storage.insert(slot, held)) {
                event.getEntity().playSound(SoundEvents.ITEM_PICKUP, 0.7f, 1.2f);
                event.setCanceled(true);
            } else if (held.isEmpty()) {
                ItemStack popped = storage.pop(slot);
                if (!popped.isEmpty()) {
                    event.getEntity().containerMenu.setCarried(popped);
                    event.setCanceled(true);
                }
            }
        } else if (held.getItem() instanceof RelicStorageItem storage) {
            if (!slot.isEmpty() && storage.insert(held, slot)) {
                event.getEntity().playSound(SoundEvents.ITEM_PICKUP, 0.7f, 1.2f);
                event.setCanceled(true);
            } else if (slot.isEmpty()) {
                ItemStack popped = storage.pop(held);
                if (!popped.isEmpty()) {
                    event.getSlot().set(popped);
                    event.setCanceled(true);
                }
            }
        }
    }
}
