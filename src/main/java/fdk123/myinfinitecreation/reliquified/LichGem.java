package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

public final class LichGem extends Item {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> SHIELDING = gem("shielding_gem");
    public static final RegistryObject<Item> NECROMANCY = gem("necromancy_gem");
    public static final RegistryObject<Item> TWILIGHT = gem("twilight_gem");
    public static final RegistryObject<Item> ABSORPTION = gem("absorption_gem");
    public static final RegistryObject<Item> FROST = gem("frost_gem");
    public static final RegistryObject<Item> FIRE = gem("fire_gem");
    public static final RegistryObject<Item> VENGEFUL = gem("vengeful_gem");
    public static final RegistryObject<Item> ETHER = gem("ether_gem");
    public static final RegistryObject<Item> CARMINITE = gem("carminite_gem");
    public static final RegistryObject<Item> FRENZY = gem("frenzy_gem");

    public static final Map<String, RegistryObject<Item>> BY_ABILITY = Map.of(
            "fortification", SHIELDING,
            "zombie", NECROMANCY,
            "twilight", TWILIGHT,
            "lifedrain", ABSORPTION,
            "frostbite", FROST,
            "biome_burn", FIRE,
            "vendetta", VENGEFUL,
            "ethereal_guard", ETHER,
            "mirror_leech", CARMINITE,
            "frenzy", FRENZY);

    private static RegistryObject<Item> gem(String name) {
        return ITEMS.register(name, LichGem::new);
    }

    private LichGem() {
        super(new Properties().rarity(Rarity.RARE));
    }
}
