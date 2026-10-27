package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import twilightforest.init.TFItems;

public final class BrokenCharm extends Item {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> LIFE_1 = ITEMS.register("broken_charm_of_life_1",
            () -> new BrokenCharm(TFItems.CHARM_OF_LIFE_1.get(), 1));
    public static final RegistryObject<Item> LIFE_2 = ITEMS.register("broken_charm_of_life_2",
            () -> new BrokenCharm(TFItems.CHARM_OF_LIFE_2.get(), 2));
    public static final RegistryObject<Item> KEEPING_1 = ITEMS.register("broken_charm_of_keeping_1",
            () -> new BrokenCharm(TFItems.CHARM_OF_KEEPING_1.get(), 1));
    public static final RegistryObject<Item> KEEPING_2 = ITEMS.register("broken_charm_of_keeping_2",
            () -> new BrokenCharm(TFItems.CHARM_OF_KEEPING_2.get(), 2));
    public static final RegistryObject<Item> KEEPING_3 = ITEMS.register("broken_charm_of_keeping_3",
            () -> new BrokenCharm(TFItems.CHARM_OF_KEEPING_3.get(), 3));

    private final Item original;

    private BrokenCharm(Item original, int tier) {
        super(new Properties().durability(300 * tier));
        this.original = original;
    }

    public Item original() {
        return original;
    }

    public static Item brokenVersion(Item original) {
        if (original == TFItems.CHARM_OF_LIFE_1.get()) return LIFE_1.get();
        if (original == TFItems.CHARM_OF_LIFE_2.get()) return LIFE_2.get();
        if (original == TFItems.CHARM_OF_KEEPING_1.get()) return KEEPING_1.get();
        if (original == TFItems.CHARM_OF_KEEPING_2.get()) return KEEPING_2.get();
        if (original == TFItems.CHARM_OF_KEEPING_3.get()) return KEEPING_3.get();
        return null;
    }

    public static ItemStack spent(Item original) {
        Item broken = brokenVersion(original);
        if (broken == null) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(broken);
        result.setDamageValue(result.getMaxDamage() - 1);
        return result;
    }
}
