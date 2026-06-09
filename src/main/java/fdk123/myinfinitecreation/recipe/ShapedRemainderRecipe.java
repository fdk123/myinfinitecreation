package fdk123.myinfinitecreation.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class ShapedRemainderRecipe extends ShapedRecipe {
    private final NonNullList<ItemStack> remainders;

    public ShapedRemainderRecipe(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            int width,
            int height,
            NonNullList<Ingredient> ingredients,
            ItemStack result,
            boolean showNotification,
            NonNullList<ItemStack> remainders
    ) {
        super(id, group, category, width, height, ingredients, result, showNotification);
        this.remainders = remainders;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);

        for (int index = 0; index < remaining.size(); index++) {
            ItemStack configured = index < remainders.size() ? remainders.get(index) : ItemStack.EMPTY;
            if (!configured.isEmpty()) {
                remaining.set(index, configured.copy());
                continue;
            }

            ItemStack input = container.getItem(index);
            if (input.hasCraftingRemainingItem()) {
                remaining.set(index, input.getCraftingRemainingItem().copy());
            }
        }

        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.SHAPED_WITH_REMAINDERS.get();
    }

    public NonNullList<ItemStack> getConfiguredRemainders() {
        NonNullList<ItemStack> copy = NonNullList.withSize(remainders.size(), ItemStack.EMPTY);
        for (int index = 0; index < remainders.size(); index++) {
            copy.set(index, remainders.get(index).copy());
        }
        return copy;
    }

    public static class Serializer implements RecipeSerializer<ShapedRemainderRecipe> {
        @Override
        public ShapedRemainderRecipe fromJson(ResourceLocation id, JsonObject json) {
            ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
            return new ShapedRemainderRecipe(
                    id,
                    base.getGroup(),
                    base.category(),
                    base.getWidth(),
                    base.getHeight(),
                    base.getIngredients(),
                    base.getResultItem(RegistryAccess.EMPTY),
                    base.showNotification(),
                    readRemainders(json, base.getWidth(), base.getHeight())
            );
        }

        @Override
        public ShapedRemainderRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer);
            NonNullList<ItemStack> remainders = NonNullList.withSize(buffer.readVarInt(), ItemStack.EMPTY);
            for (int index = 0; index < remainders.size(); index++) {
                remainders.set(index, buffer.readItem());
            }

            return new ShapedRemainderRecipe(
                    id,
                    base.getGroup(),
                    base.category(),
                    base.getWidth(),
                    base.getHeight(),
                    base.getIngredients(),
                    base.getResultItem(RegistryAccess.EMPTY),
                    base.showNotification(),
                    remainders
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ShapedRemainderRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe);
            NonNullList<ItemStack> remainders = recipe.getConfiguredRemainders();
            buffer.writeVarInt(remainders.size());
            for (ItemStack remainder : remainders) {
                buffer.writeItem(remainder);
            }
        }

        private static NonNullList<ItemStack> readRemainders(JsonObject json, int width, int height) {
            Map<Character, ItemStack> keyRemainders = readKeyRemainders(GsonHelper.getAsJsonObject(json, "key"));
            String[] pattern = shrink(GsonHelper.getAsJsonArray(json, "pattern"));
            if (pattern.length != height || (height > 0 && pattern[0].length() != width)) {
                throw new JsonSyntaxException("Remainder pattern does not match shaped recipe dimensions");
            }

            NonNullList<ItemStack> remainders = NonNullList.withSize(width * height, ItemStack.EMPTY);
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    char key = pattern[row].charAt(column);
                    if (key != ' ' && keyRemainders.containsKey(key)) {
                        remainders.set(column + width * row, keyRemainders.get(key).copy());
                    }
                }
            }
            return remainders;
        }

        private static Map<Character, ItemStack> readKeyRemainders(JsonObject key) {
            Map<Character, ItemStack> remainders = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : key.entrySet()) {
                if (entry.getKey().length() != 1) {
                    throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol");
                }

                JsonObject ingredient = GsonHelper.convertToJsonObject(entry.getValue(), "key entry");
                ItemStack remainder = readRemainder(ingredient);
                if (!remainder.isEmpty()) {
                    remainders.put(entry.getKey().charAt(0), remainder);
                }
            }
            return remainders;
        }

        private static ItemStack readRemainder(JsonObject ingredient) {
            if (GsonHelper.getAsBoolean(ingredient, "reuse", false)) {
                if (!ingredient.has("item")) {
                    throw new JsonSyntaxException("reuse can only be used with item ingredients");
                }
                return new ItemStack(ShapedRecipe.itemFromJson(ingredient));
            }

            if (!ingredient.has("remainder")) {
                return ItemStack.EMPTY;
            }

            JsonElement remainder = ingredient.get("remainder");
            if (remainder.isJsonPrimitive()) {
                ResourceLocation id = ResourceLocation.parse(GsonHelper.convertToString(remainder, "remainder"));
                return new ItemStack(ForgeRegistries.ITEMS.getValue(id));
            }

            return ShapedRecipe.itemStackFromJson(GsonHelper.convertToJsonObject(remainder, "remainder"));
        }

        private static String[] shrink(com.google.gson.JsonArray rawPattern) {
            String[] pattern = new String[rawPattern.size()];
            for (int index = 0; index < pattern.length; index++) {
                pattern[index] = GsonHelper.convertToString(rawPattern.get(index), "pattern[" + index + "]");
            }

            int firstColumn = Integer.MAX_VALUE;
            int lastColumn = 0;
            int firstRow = 0;
            int lastRow = 0;

            for (int row = 0; row < pattern.length; row++) {
                String line = pattern[row];
                firstColumn = Math.min(firstColumn, firstNonSpace(line));
                int lastNonSpace = lastNonSpace(line);
                lastColumn = Math.max(lastColumn, lastNonSpace);

                if (lastNonSpace < 0) {
                    if (firstRow == row) {
                        firstRow++;
                    }
                    lastRow++;
                } else {
                    lastRow = row;
                }
            }

            if (pattern.length == lastRow) {
                return new String[0];
            }

            String[] shrunk = new String[lastRow - firstRow + 1];
            for (int row = 0; row < shrunk.length; row++) {
                shrunk[row] = pattern[row + firstRow].substring(firstColumn, lastColumn + 1);
            }
            return shrunk;
        }

        private static int firstNonSpace(String line) {
            int index = 0;
            while (index < line.length() && line.charAt(index) == ' ') {
                index++;
            }
            return index;
        }

        private static int lastNonSpace(String line) {
            int index = line.length() - 1;
            while (index >= 0 && line.charAt(index) == ' ') {
                index--;
            }
            return index;
        }
    }
}
