package fdk123.myinfinitecreation.recipe;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

public class RecipePolicyService {
    private final RecipePolicyLoader loader = new RecipePolicyLoader();
    private List<Recipe<?>> originalRecipes = List.of();
    private boolean applied = false;

    public void apply(MinecraftServer server) {
        apply(server, RecipeStageState.get(server).getActiveStage(), false);
    }

    public void reapply(MinecraftServer server) {
        apply(server, RecipeStageState.get(server).getActiveStage(), true);
    }

    public void setStage(MinecraftServer server, String stage) {
        RecipeStageState state = RecipeStageState.get(server);
        state.setActiveStage(stage);
        apply(server, stage, true);
    }

    public String activeStage(MinecraftServer server) {
        return RecipeStageState.get(server).getActiveStage();
    }

    public void clear() {
        originalRecipes = List.of();
        applied = false;
    }

    private void apply(MinecraftServer server, String activeStage, boolean force) {
        RecipeManager recipeManager = server.getRecipeManager();

        if (originalRecipes.isEmpty() || !applied) {
            originalRecipes = List.copyOf(recipeManager.getRecipes());
        } else if (force) {
            recipeManager.replaceRecipes(originalRecipes);
        } else {
            return;
        }

        List<RecipePolicyRule> rules = loader.load(server.getResourceManager(), activeStage);
        if (rules.isEmpty()) {
            MyInfiniteCreation.LOGGER.info("No recipe policy rules found for stage '{}'", activeStage);
            applied = true;
            syncRecipes(server);
            return;
        }

        List<Recipe<?>> filteredRecipes = new ArrayList<>();
        List<RemovedRecipe> removedRecipes = new ArrayList<>();

        for (Recipe<?> recipe : originalRecipes) {
            RecipePolicyRule matchedRule = findMatch(recipe.getId(), recipe, server, rules);
            if (matchedRule == null) {
                filteredRecipes.add(recipe);
            } else {
                removedRecipes.add(new RemovedRecipe(recipe.getId(), typeId(recipe), resultId(recipe, server), matchedRule.name));
            }
        }

        recipeManager.replaceRecipes(filteredRecipes);

        if (removedRecipes.isEmpty()) {
            MyInfiniteCreation.LOGGER.info("Recipe policy did not remove any recipes for stage '{}'", activeStage);
        } else {
            for (RemovedRecipe removed : removedRecipes) {
                MyInfiniteCreation.LOGGER.info(
                        "Removed recipe {} type={} output={} stage='{}' rule='{}'",
                        removed.id(),
                        removed.type(),
                        removed.output(),
                        activeStage,
                        removed.ruleName()
                );
            }
            MyInfiniteCreation.LOGGER.info("Recipe policy removed {} recipe(s) for stage '{}'", removedRecipes.size(), activeStage);
        }

        applied = true;
        syncRecipes(server);
    }

    private void syncRecipes(MinecraftServer server) {
        ClientboundUpdateRecipesPacket packet = new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes());
        server.getPlayerList().getPlayers().forEach(player -> player.connection.send(packet));
    }

    private RecipePolicyRule findMatch(ResourceLocation id, Recipe<?> recipe, MinecraftServer server, List<RecipePolicyRule> rules) {
        ResourceLocation type = typeId(recipe);
        ItemStack result = resultItem(recipe, server);
        ResourceLocation output = resultId(result);

        for (RecipePolicyRule rule : rules) {
            if (matches(rule.ids, id)
                    && matches(rule.namespaces, id.getNamespace())
                    && matches(rule.types, type)
                    && matches(rule.outputs, output)
                    && matchesOutputTags(rule.outputTags, result)) {
                return rule;
            }
        }
        return null;
    }

    private <T> boolean matches(java.util.Set<T> ruleValues, T recipeValue) {
        return ruleValues.isEmpty() || recipeValue != null && ruleValues.contains(recipeValue);
    }

    private ResourceLocation typeId(Recipe<?> recipe) {
        return BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
    }

    private ResourceLocation resultId(Recipe<?> recipe, MinecraftServer server) {
        return resultId(resultItem(recipe, server));
    }

    private ItemStack resultItem(Recipe<?> recipe, MinecraftServer server) {
        return recipe.getResultItem(server.registryAccess());
    }

    private ResourceLocation resultId(ItemStack result) {
        if (result.isEmpty()) {
            return null;
        }
        return BuiltInRegistries.ITEM.getKey(result.getItem());
    }

    private boolean matchesOutputTags(java.util.Set<ResourceLocation> ruleTags, ItemStack result) {
        if (ruleTags.isEmpty()) {
            return true;
        }
        if (result.isEmpty()) {
            return false;
        }
        return ruleTags.stream().anyMatch(tag -> result.is(TagKey.create(BuiltInRegistries.ITEM.key(), tag)));
    }

    private record RemovedRecipe(ResourceLocation id, ResourceLocation type, ResourceLocation output, String ruleName) {
    }
}
