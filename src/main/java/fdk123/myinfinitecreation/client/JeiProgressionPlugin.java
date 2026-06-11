package fdk123.myinfinitecreation.client;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import fdk123.myinfinitecreation.progression.ModGateRule;
import fdk123.myinfinitecreation.progression.ProgressionGateRule;
import fdk123.myinfinitecreation.progression.StageAccess;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.common.crafting.IShapedRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.lang.reflect.Method;

@JeiPlugin
public class JeiProgressionPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MyInfiniteCreation.MOD_ID, "progression_gates");
    private static final ResourceLocation CREATE_AUTOMATIC_SHAPED = ResourceLocation.fromNamespaceAndPath("create", "automatic_shaped");
    private static final StageAccess STAGE_ACCESS = new StageAccess();
    private static IJeiRuntime runtime;
    private static List<ItemStack> hiddenStacks = List.of();
    private static List<CraftingRecipe> hiddenCraftingRecipes = List.of();
    private static List<IJeiAnvilRecipe> hiddenAnvilRecipes = List.of();
    private static List<HiddenRecipes> hiddenTypedRecipes = List.of();

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        refreshRuntime();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        hiddenStacks = List.of();
        hiddenCraftingRecipes = List.of();
        hiddenAnvilRecipes = List.of();
        hiddenTypedRecipes = List.of();
    }

    public static void refreshRuntime() {
        if (runtime == null) {
            return;
        }

        if (!hiddenStacks.isEmpty()) {
            runtime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hiddenStacks);
            MyInfiniteCreation.LOGGER.info("Restored {} JEI progression-gated item(s)", hiddenStacks.size());
        }
        if (!hiddenCraftingRecipes.isEmpty()) {
            runtime.getRecipeManager().unhideRecipes(RecipeTypes.CRAFTING, hiddenCraftingRecipes);
            MyInfiniteCreation.LOGGER.info("Restored {} JEI progression-gated crafting recipe(s)", hiddenCraftingRecipes.size());
        }
        if (!hiddenAnvilRecipes.isEmpty()) {
            runtime.getRecipeManager().unhideRecipes(RecipeTypes.ANVIL, hiddenAnvilRecipes);
            MyInfiniteCreation.LOGGER.info("Restored {} JEI progression-gated anvil recipe(s)", hiddenAnvilRecipes.size());
        }
        for (HiddenRecipes hiddenRecipes : hiddenTypedRecipes) {
            unhideTypedRecipes(hiddenRecipes);
            MyInfiniteCreation.LOGGER.info(
                    "Restored {} JEI progression-gated recipe(s) for {}",
                    hiddenRecipes.recipes().size(),
                    hiddenRecipes.type().getUid()
            );
        }

        List<ItemStack> newHiddenStacks = lockedStacks();
        List<CraftingRecipe> newHiddenCraftingRecipes = lockedCraftingRecipes();
        List<IJeiAnvilRecipe> newHiddenAnvilRecipes = lockedAnvilRecipes();
        List<HiddenRecipes> newHiddenTypedRecipes = lockedTypedRecipes();
        if (!newHiddenStacks.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, newHiddenStacks);
            MyInfiniteCreation.LOGGER.info("Hidden {} JEI progression-gated item(s)", newHiddenStacks.size());
        }
        if (!newHiddenCraftingRecipes.isEmpty()) {
            runtime.getRecipeManager().hideRecipes(RecipeTypes.CRAFTING, newHiddenCraftingRecipes);
            MyInfiniteCreation.LOGGER.info("Hidden {} JEI progression-gated crafting recipe(s)", newHiddenCraftingRecipes.size());
        }
        if (!newHiddenAnvilRecipes.isEmpty()) {
            runtime.getRecipeManager().hideRecipes(RecipeTypes.ANVIL, newHiddenAnvilRecipes);
            MyInfiniteCreation.LOGGER.info("Hidden {} JEI progression-gated anvil recipe(s)", newHiddenAnvilRecipes.size());
        }
        for (HiddenRecipes hiddenRecipes : newHiddenTypedRecipes) {
            hideTypedRecipes(hiddenRecipes);
            MyInfiniteCreation.LOGGER.info(
                    "Hidden {} JEI progression-gated recipe(s) for {}",
                    hiddenRecipes.recipes().size(),
                    hiddenRecipes.type().getUid()
            );
        }
        hiddenStacks = newHiddenStacks;
        hiddenCraftingRecipes = newHiddenCraftingRecipes;
        hiddenAnvilRecipes = newHiddenAnvilRecipes;
        hiddenTypedRecipes = newHiddenTypedRecipes;
    }

    private static List<ItemStack> lockedStacks() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        List<ProgressionGateRule> rules = ClientProgressionHooks.recipeGateRules();
        List<ItemStack> stacks = new ArrayList<>();

        for (ProgressionGateRule rule : rules) {
            boolean unlocked = isUnlocked(player, rule);
            if (rule.hideInJei && !unlocked) {
                addOutputStacks(rule, stacks);
            }
        }
        for (ModGateRule rule : lockedModRules()) {
            if (rule.hideInJei) {
                addModGateStacks(player, rule, stacks);
            }
        }
        return stacks;
    }

    private static List<CraftingRecipe> lockedCraftingRecipes() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return List.of();
        }

        List<ProgressionGateRule> lockedRules = lockedRules();
        List<ModGateRule> lockedModRules = lockedModRules();
        List<CraftingRecipe> recipes = new ArrayList<>();
        for (CraftingRecipe recipe : minecraft.level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            ItemStack result = recipe.getResultItem(minecraft.level.registryAccess());
            if (lockedRules.stream().anyMatch(rule -> matchesOutput(rule, result) || hasMatchingIngredient(recipe, rule))
                    || lockedModRules.stream().anyMatch(rule -> rule.hideInJei && (isLockedModGateItem(rule, result) || hasMatchingModGateIngredient(recipe, rule)))) {
                recipes.add(recipe);
            }
        }
        return recipes;
    }

    private static List<HiddenRecipes> lockedTypedRecipes() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || runtime == null) {
            return List.of();
        }

        List<ProgressionGateRule> lockedRules = lockedRules();
        List<ModGateRule> lockedModRules = lockedModRules();
        Map<RecipeType<Object>, List<Object>> recipesByJeiType = new LinkedHashMap<>();
        addCreateAutomaticShapedRecipes(recipesByJeiType);
        addMineColoniesJobRecipes(recipesByJeiType, lockedRules);

        for (Recipe<?> recipe : minecraft.level.getRecipeManager().getRecipes()) {
            ResourceLocation recipeType = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            if (recipeType == null || recipeType.equals(BuiltInRegistries.RECIPE_TYPE.getKey(net.minecraft.world.item.crafting.RecipeType.CRAFTING))) {
                continue;
            }

            ItemStack result = recipe.getResultItem(minecraft.level.registryAccess());
            boolean lockedByRecipeGate = lockedRules.stream().anyMatch(rule -> matchesOutput(rule, result));
            boolean lockedByModGate = lockedModRules.stream().anyMatch(rule -> rule.hideInJei && isLockedModGateItem(rule, result));
            if (!lockedByRecipeGate && !lockedByModGate) {
                continue;
            }

            Optional<RecipeType<Object>> jeiType = jeiRecipeType(recipeType);
            jeiType.ifPresent(type -> recipesByJeiType.computeIfAbsent(type, ignored -> new ArrayList<>()).add(recipe));
        }

        return recipesByJeiType.entrySet().stream()
                .map(entry -> new HiddenRecipes(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static void addMineColoniesJobRecipes(Map<RecipeType<Object>, List<Object>> recipesByJeiType, List<ProgressionGateRule> lockedRules) {
        if (lockedRules.isEmpty()) {
            return;
        }

        runtime.getRecipeManager()
                .createRecipeCategoryLookup()
                .includeHidden()
                .get()
                .map(category -> (RecipeType<Object>) (RecipeType<?>) category.getRecipeType())
                .filter(type -> "minecolonies".equals(type.getUid().getNamespace()))
                .forEach(type -> {
                    List<Object> recipes = runtime.getRecipeManager()
                            .createRecipeLookup(type)
                            .includeHidden()
                            .get()
                            .filter(recipe -> isLockedMineColoniesRecipe(recipe, lockedRules))
                            .toList();
                    if (!recipes.isEmpty()) {
                        recipesByJeiType.computeIfAbsent(type, ignored -> new ArrayList<>()).addAll(recipes);
                    }
                });
    }

    private static void addCreateAutomaticShapedRecipes(Map<RecipeType<Object>, List<Object>> recipesByJeiType) {
        Optional<RecipeType<Object>> automaticShaped = jeiRecipeType(CREATE_AUTOMATIC_SHAPED);
        if (automaticShaped.isEmpty()) {
            return;
        }

        List<Object> recipes = lockedCraftingRecipes().stream()
                .filter(recipe -> recipe instanceof IShapedRecipe<?>)
                .map(recipe -> (Object) recipe)
                .toList();
        if (!recipes.isEmpty()) {
            recipesByJeiType.computeIfAbsent(automaticShaped.get(), ignored -> new ArrayList<>()).addAll(recipes);
        }
    }

    private static List<IJeiAnvilRecipe> lockedAnvilRecipes() {
        List<ProgressionGateRule> lockedRules = lockedRules();
        List<ModGateRule> lockedModRules = lockedModRules();
        if (lockedRules.isEmpty() && lockedModRules.isEmpty() || runtime == null) {
            return List.of();
        }

        return runtime.getRecipeManager()
                .createRecipeLookup(RecipeTypes.ANVIL)
                .includeHidden()
                .get()
                .filter(recipe -> lockedRules.stream().anyMatch(rule -> matchesAnvilRecipe(recipe, rule))
                        || lockedModRules.stream().anyMatch(rule -> rule.hideInJei && matchesModGateAnvilRecipe(recipe, rule)))
                .toList();
    }

    private static List<ProgressionGateRule> lockedRules() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        List<ProgressionGateRule> rules = ClientProgressionHooks.recipeGateRules();
        List<ProgressionGateRule> lockedRules = new ArrayList<>();

        for (ProgressionGateRule rule : rules) {
            boolean unlocked = isUnlocked(player, rule);
            if (rule.hideInJei && !unlocked) {
                lockedRules.add(rule);
            }
        }
        return lockedRules;
    }

    private static List<ModGateRule> lockedModRules() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        List<ModGateRule> lockedRules = new ArrayList<>();

        for (ModGateRule rule : ClientProgressionHooks.modGateRules()) {
            if (rule.isRestrict() && !ClientProgressionHooks.isUnlocked(player, rule)) {
                lockedRules.add(rule);
            }
        }
        return lockedRules;
    }

    private static boolean isUnlocked(Player player, ProgressionGateRule rule) {
        return player != null && rule.requiredStages.stream().anyMatch(stage -> STAGE_ACCESS.hasStage(player, stage))
                || rule.requiredResearches.stream().anyMatch(ClientProgressionHooks::hasMineColoniesResearch);
    }

    private static void addOutputStacks(ProgressionGateRule rule, List<ItemStack> stacks) {
        for (ResourceLocation output : rule.outputs) {
            Item item = BuiltInRegistries.ITEM.get(output);
            if (item != null) {
                stacks.add(item.getDefaultInstance());
            }
        }

        for (ResourceLocation tag : rule.outputTags) {
            TagKey<Item> key = TagKey.create(BuiltInRegistries.ITEM.key(), tag);
            BuiltInRegistries.ITEM.stream()
                    .map(Item::getDefaultInstance)
                    .filter(stack -> stack.is(key))
                    .forEach(stacks::add);
        }
    }

    private static void addModGateStacks(Player player, ModGateRule rule, List<ItemStack> stacks) {
        BuiltInRegistries.ITEM.stream()
                .map(Item::getDefaultInstance)
                .filter(stack -> matchesModGateItem(rule, stack) && ClientProgressionHooks.lockedModItemRule(player, stack) != null)
                .forEach(stacks::add);
    }

    private static boolean matchesOutput(ProgressionGateRule rule, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation output = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!rule.outputs.isEmpty() && rule.outputs.contains(output)) {
            return true;
        }
        return rule.outputTags.stream().anyMatch(tag -> stack.is(TagKey.create(BuiltInRegistries.ITEM.key(), tag)));
    }

    private static boolean matchesModGateItem(ModGateRule rule, ItemStack stack) {
        return ClientProgressionHooks.matchesItem(rule, stack);
    }

    private static boolean isLockedModGateItem(ModGateRule rule, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        return matchesModGateItem(rule, stack) && ClientProgressionHooks.lockedModItemRule(minecraft.player, stack) != null;
    }

    private static boolean matchesRuleRecipeType(ProgressionGateRule rule, ResourceLocation recipeType) {
        return rule.types.isEmpty() || rule.types.contains(recipeType);
    }

    private static boolean hasMatchingIngredient(CraftingRecipe recipe, ProgressionGateRule rule) {
        return recipe.getIngredients().stream()
                .flatMap(ingredient -> Arrays.stream(ingredient.getItems()))
                .anyMatch(stack -> matchesOutput(rule, stack));
    }

    private static boolean hasMatchingModGateIngredient(CraftingRecipe recipe, ModGateRule rule) {
        return recipe.getIngredients().stream()
                .flatMap(ingredient -> Arrays.stream(ingredient.getItems()))
                .anyMatch(stack -> isLockedModGateItem(rule, stack));
    }

    private static boolean isLockedMineColoniesRecipe(Object recipe, List<ProgressionGateRule> lockedRules) {
        if (!hasMethod(recipe, "getPrimaryOutput")) {
            return false;
        }
        return lockedRules.stream().anyMatch(rule -> matchesMineColoniesRecipe(recipe, rule));
    }

    private static boolean matchesMineColoniesRecipe(Object recipe, ProgressionGateRule rule) {
        return itemStackResultMatches(recipe, rule, "getPrimaryOutput")
                || itemStackListResultMatches(recipe, rule, "getAllMultiOutputs")
                || itemStackListResultMatches(recipe, rule, "getAdditionalOutputs")
                || nestedItemStackListResultMatches(recipe, rule, "getInputs");
    }

    private static boolean itemStackResultMatches(Object recipe, ProgressionGateRule rule, String methodName) {
        Object value = invokeNoArgs(recipe, methodName);
        return value instanceof ItemStack stack && matchesOutput(rule, stack);
    }

    private static boolean itemStackListResultMatches(Object recipe, ProgressionGateRule rule, String methodName) {
        Object value = invokeNoArgs(recipe, methodName);
        if (!(value instanceof List<?> stacks)) {
            return false;
        }
        return stacks.stream().anyMatch(stack -> stack instanceof ItemStack itemStack && matchesOutput(rule, itemStack));
    }

    private static boolean nestedItemStackListResultMatches(Object recipe, ProgressionGateRule rule, String methodName) {
        Object value = invokeNoArgs(recipe, methodName);
        if (!(value instanceof List<?> inputGroups)) {
            return false;
        }
        return inputGroups.stream()
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .flatMap(List::stream)
                .anyMatch(stack -> stack instanceof ItemStack itemStack && matchesOutput(rule, itemStack));
    }

    private static boolean hasMethod(Object target, String methodName) {
        return method(target, methodName).isPresent();
    }

    private static Object invokeNoArgs(Object target, String methodName) {
        try {
            Optional<Method> method = method(target, methodName);
            if (method.isPresent()) {
                return method.get().invoke(target);
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    private static Optional<Method> method(Object target, String methodName) {
        try {
            return Optional.of(target.getClass().getMethod(methodName));
        } catch (NoSuchMethodException ignored) {
            return Optional.empty();
        }
    }

    private static boolean matchesAnvilRecipe(IJeiAnvilRecipe recipe, ProgressionGateRule rule) {
        return recipe.getLeftInputs().stream().anyMatch(stack -> matchesOutput(rule, stack))
                || recipe.getRightInputs().stream().anyMatch(stack -> matchesOutput(rule, stack))
                || recipe.getOutputs().stream().anyMatch(stack -> matchesOutput(rule, stack));
    }

    private static boolean matchesModGateAnvilRecipe(IJeiAnvilRecipe recipe, ModGateRule rule) {
        return recipe.getLeftInputs().stream().anyMatch(stack -> isLockedModGateItem(rule, stack))
                || recipe.getRightInputs().stream().anyMatch(stack -> isLockedModGateItem(rule, stack))
                || recipe.getOutputs().stream().anyMatch(stack -> isLockedModGateItem(rule, stack));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Optional<RecipeType<Object>> jeiRecipeType(ResourceLocation recipeType) {
        return (Optional) runtime.getRecipeManager().getRecipeType(recipeType);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void hideTypedRecipes(HiddenRecipes hiddenRecipes) {
        runtime.getRecipeManager().hideRecipes((RecipeType) hiddenRecipes.type(), hiddenRecipes.recipes());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void unhideTypedRecipes(HiddenRecipes hiddenRecipes) {
        runtime.getRecipeManager().unhideRecipes((RecipeType) hiddenRecipes.type(), hiddenRecipes.recipes());
    }

    private record HiddenRecipes(RecipeType<Object> type, List<Object> recipes) {
    }
}
