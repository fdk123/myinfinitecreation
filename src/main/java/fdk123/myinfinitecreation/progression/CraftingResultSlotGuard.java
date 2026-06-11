package fdk123.myinfinitecreation.progression;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;

public final class CraftingResultSlotGuard {
    private static final String TCONSTRUCT_CRAFTING_STATION_RESULT_SLOT =
            "slimeknights.tconstruct.tables.menu.slot.PlayerSensitiveLazyResultSlot";

    private CraftingResultSlotGuard() {
    }

    public static boolean shouldBlock(Player player, Slot slot) {
        return isCraftingResultSlot(slot)
                && !MyInfiniteCreation.PROGRESSION_GATES.mayTakeCraftingResult(player, slot.getItem());
    }

    private static boolean isCraftingResultSlot(Slot slot) {
        return slot instanceof ResultSlot || hasClassName(slot.getClass(), TCONSTRUCT_CRAFTING_STATION_RESULT_SLOT);
    }

    private static boolean hasClassName(Class<?> type, String className) {
        Class<?> current = type;
        while (current != null) {
            if (className.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
