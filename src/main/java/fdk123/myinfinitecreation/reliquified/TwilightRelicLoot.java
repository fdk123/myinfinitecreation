package fdk123.myinfinitecreation.reliquified;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TwilightRelicLoot {
    private TwilightRelicLoot() {
    }

    @SubscribeEvent
    public static void addToChests(LootTableLoadEvent event) {
        if (event.getName() == null || !event.getName().getNamespace().equals("twilightforest")) {
            return;
        }
        String path = event.getName().getPath();
        if (path.equals("entities/questing_ram_rewards")) {
            add(event.getTable(), "chromatic_cloak", ChromaticCloak.ITEM.get(), 0.33f);
            return;
        }
        if (!path.startsWith("chests/")) {
            return;
        }
        if (path.equals("chests/hedge_maze")) {
            add(event.getTable(), "invisibility_cloak", InvisibilityCloak.ITEM.get(), 0.10f);
        }
        if (path.equals("chests/tree_cache")) {
            add(event.getTable(), "twilight_feather", TwilightFeather.ITEM.get(), 0.10f);
        }
        if (path.equals("chests/troll_vault") || path.equals("chests/troll_vault_with_lamp")
                || path.equals("chests/troll_garden")) {
            add(event.getTable(), "giant_glove", GiantGlove.ITEM.get(), 0.10f);
            add(event.getTable(), "goblin_nose", GoblinNose.ITEM.get(), 0.10f);
        }
        if (path.startsWith("chests/labyrinth_") || path.equals("chests/stronghold_cache")
                || path.equals("chests/stronghold_room")) {
            add(event.getTable(), "maple_syrup_bottle", MapleSyrupBottle.ITEM.get(), 0.10f);
        }
        if (path.startsWith("chests/labyrinth_")) {
            add(event.getTable(), "charm_backpack", CharmBackpack.ITEM.get(), 0.10f);
        }
        if (path.equals("chests/stronghold_cache") || path.equals("chests/stronghold_room")) {
            add(event.getTable(), "steel_cape", SteelCape.ITEM.get(), 0.10f);
        }
        if (path.startsWith("chests/darktower_")) {
            add(event.getTable(), "parasite_115", Parasite115.ITEM.get(), 0.10f);
        }
        add(event.getTable(), "firefly_queen", FireflyQueen.ITEM.get(), 0.02f);
        add(event.getTable(), "cicada_bottle", CicadaBottle.ITEM.get(), 0.02f);
        add(event.getTable(), "deer_antler", DeerAntler.ITEM.get(), 0.02f);
    }

    private static void add(LootTable table, String name, Item item, float chance) {
        table.addPool(LootPool.lootPool().name("myinfinitecreation_" + name)
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(item).when(LootItemRandomChanceCondition.randomChance(chance)))
                .build());
    }
}
