package fdk123.myinfinitecreation.recipe;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MyInfiniteCreation.MOD_ID);

    public static final RegistryObject<RecipeSerializer<ShapedRemainderRecipe>> SHAPED_WITH_REMAINDERS =
            RECIPE_SERIALIZERS.register("shaped_with_remainders", ShapedRemainderRecipe.Serializer::new);

    private ModRecipeSerializers() {
    }
}
