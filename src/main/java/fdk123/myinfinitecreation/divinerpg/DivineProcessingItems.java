package fdk123.myinfinitecreation.divinerpg;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Intermediate ore-processing items shared by Create, IE, Thermal and Mekanism recipes. */
public final class DivineProcessingItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);

    public static final Map<String, RegistryObject<Item>> ALL = new LinkedHashMap<>();

    static {
        for (String material : new String[]{"arlemite", "realmite", "rupee", "torridite"}) {
            register(material + "_dust");
            register("dirty_dust_" + material);
            register("clump_" + material);
            register("shard_" + material);
        }
    }

    private DivineProcessingItems() {}

    private static void register(String name) {
        ALL.put(name, ITEMS.register(name, () -> new Item(new Item.Properties())));
    }
}
