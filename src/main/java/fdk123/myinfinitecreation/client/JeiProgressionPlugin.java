package fdk123.myinfinitecreation.client;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import fdk123.myinfinitecreation.progression.ModGateRule;
import fdk123.myinfinitecreation.progression.ProgressionGateRule;
import fdk123.myinfinitecreation.progression.StageAccess;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.registration.ISubtypeRegistration;
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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private static List<HiddenRecipes> forcedVisibleTypedRecipes = List.of();
    private static Set<ResourceLocation> forcedAddedMineColoniesRecipes = new LinkedHashSet<>();
    private static boolean refreshInProgress = false;
    private static String hiddenStateKey = "";

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registerNoSubtype(registration, ResourceLocation.fromNamespaceAndPath("sophisticatedstorage", "chest"));
        registerNoSubtype(registration, ResourceLocation.fromNamespaceAndPath("sophisticatedstorage", "barrel"));
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
        forcedVisibleTypedRecipes = List.of();
        forcedAddedMineColoniesRecipes = new LinkedHashSet<>();
        refreshInProgress = false;
        hiddenStateKey = "";
    }

    public static void refreshRuntime() {
        if (runtime == null || refreshInProgress) {
            return;
        }

        refreshInProgress = true;
        try {
            List<HiddenRecipes> newForcedVisibleTypedRecipes = forcedVisibleTypedRecipes();
            List<ItemStack> newHiddenStacks = lockedStacks();
            List<CraftingRecipe> newHiddenCraftingRecipes = lockedCraftingRecipes();
            List<IJeiAnvilRecipe> newHiddenAnvilRecipes = lockedAnvilRecipes();
            List<HiddenRecipes> newHiddenTypedRecipes = lockedTypedRecipes();
            String newHiddenStateKey = hiddenStateKey(
                    newHiddenStacks,
                    newHiddenCraftingRecipes,
                    newHiddenAnvilRecipes,
                    newHiddenTypedRecipes,
                    newForcedVisibleTypedRecipes
            );
            if (newHiddenStateKey.equals(hiddenStateKey)) {
                addForcedVisibleMineColoniesCustomRecipes();
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

        for (HiddenRecipes visibleRecipes : newForcedVisibleTypedRecipes) {
            unhideTypedRecipes(visibleRecipes);
            MyInfiniteCreation.LOGGER.info(
                    "Forced {} JEI progression-visible recipe(s) for {}",
                    visibleRecipes.recipes().size(),
                    visibleRecipes.type().getUid()
            );
        }
        addForcedVisibleMineColoniesCustomRecipes();

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
        forcedVisibleTypedRecipes = newForcedVisibleTypedRecipes;
        hiddenStateKey = newHiddenStateKey;
        } finally {
            refreshInProgress = false;
        }
    }

    public static void retryForcedVisibleMineColoniesRecipes() {
        addForcedVisibleMineColoniesCustomRecipes();
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

    private static String hiddenStateKey(
            List<ItemStack> stacks,
            List<CraftingRecipe> craftingRecipes,
            List<IJeiAnvilRecipe> anvilRecipes,
            List<HiddenRecipes> typedRecipes,
            List<HiddenRecipes> visibleTypedRecipes
    ) {
        return "items=" + stackKeys(stacks)
                + "|crafting=" + recipeKeys(craftingRecipes)
                + "|anvil=" + anvilRecipeKeys(anvilRecipes)
                + "|typed=" + hiddenRecipeKeys(typedRecipes)
                + "|visible=" + hiddenRecipeKeys(visibleTypedRecipes);
    }

    private static List<String> stackKeys(List<ItemStack> stacks) {
        return stacks.stream()
                .filter(stack -> !stack.isEmpty())
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                .sorted()
                .toList();
    }

    private static List<String> recipeKeys(List<? extends Recipe<?>> recipes) {
        return recipes.stream()
                .map(recipe -> recipe.getId().toString())
                .sorted()
                .toList();
    }

    private static List<String> anvilRecipeKeys(List<IJeiAnvilRecipe> recipes) {
        return recipes.stream()
                .map(recipe -> "left=" + stackKeys(recipe.getLeftInputs())
                        + ",right=" + stackKeys(recipe.getRightInputs())
                        + ",out=" + stackKeys(recipe.getOutputs()))
                .sorted()
                .toList();
    }

    private static List<String> hiddenRecipeKeys(List<HiddenRecipes> recipesByType) {
        return recipesByType.stream()
                .map(hiddenRecipes -> hiddenRecipes.type().getUid() + "=" + hiddenRecipes.recipes().stream()
                        .map(JeiProgressionPlugin::recipeObjectKey)
                        .sorted()
                        .toList())
                .sorted()
                .toList();
    }

    private static String recipeObjectKey(Object recipe) {
        if (recipe instanceof Recipe<?> minecraftRecipe) {
            return minecraftRecipe.getId().toString();
        }
        ResourceLocation customId = customRecipeId(recipe);
        if (customId != null) {
            return customId.toString();
        }
        return recipe.getClass().getName() + "@" + System.identityHashCode(recipe);
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
                    || lockedModRules.stream().anyMatch(rule -> rule.hideInJei && (isLockedModGateItem(rule, result) || hasRequiredLockedModGateIngredient(recipe, rule)))) {
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
        List<ProgressionGateRule> visibleRules = visibleRecipeRules();
        Map<RecipeType<Object>, List<Object>> recipesByJeiType = new LinkedHashMap<>();
        addCreateAutomaticShapedRecipes(recipesByJeiType);
        addLockedMineColoniesJobRecipes(recipesByJeiType, lockedRules, lockedModRules, visibleRules);

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

    private static List<HiddenRecipes> forcedVisibleTypedRecipes() {
        if (runtime == null) {
            return List.of();
        }

        List<ProgressionGateRule> visibleRules = visibleRecipeRules();
        if (visibleRules.isEmpty()) {
            return List.of();
        }

        Map<RecipeType<Object>, List<Object>> recipesByJeiType = new LinkedHashMap<>();
        addMineColoniesRecipeGateRecipes(recipesByJeiType, visibleRules);
        return recipesByJeiType.entrySet().stream()
                .map(entry -> new HiddenRecipes(entry.getKey(), entry.getValue()))
                .toList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addForcedVisibleMineColoniesCustomRecipes() {
        if (runtime == null) {
            return;
        }

        List<ProgressionGateRule> visibleRules = visibleRecipeRules();
        if (visibleRules.isEmpty()) {
            return;
        }

        try {
            Class<?> managerClass = Class.forName("com.minecolonies.core.colony.crafting.CustomRecipeManager");
            Object manager = managerClass.getMethod("getInstance").invoke(null);
            Object allRecipes = managerClass.getMethod("getAllRecipes").invoke(manager);
            if (!(allRecipes instanceof Map<?, ?> recipesByCrafter)) {
                return;
            }

            Class<?> utilsClass = Class.forName("com.minecolonies.core.colony.crafting.GenericRecipeUtils");
            Method create = utilsClass.getMethod(
                    "create",
                    Class.forName("com.minecolonies.core.colony.crafting.CustomRecipe"),
                    Class.forName("com.minecolonies.api.crafting.IRecipeStorage")
            );

            int added = 0;
            for (Map.Entry<?, ?> crafterEntry : recipesByCrafter.entrySet()) {
                if (!(crafterEntry.getKey() instanceof String crafter) || !(crafterEntry.getValue() instanceof Map<?, ?> recipes)) {
                    continue;
                }

                Optional<RecipeType<Object>> jeiType = jeiRecipeType(mineColoniesJobRecipeTypeId(crafter));
                if (jeiType.isEmpty()) {
                    continue;
                }

                Set<ResourceLocation> existingRecipeIds = runtime.getRecipeManager()
                        .createRecipeLookup(jeiType.get())
                        .includeHidden()
                        .get()
                        .map(JeiProgressionPlugin::customRecipeId)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toSet());
                List<Object> recipesToAdd = new ArrayList<>();
                for (Object recipe : recipes.values()) {
                    ResourceLocation recipeId = customRecipeId(recipe);
                    if (recipeId == null || existingRecipeIds.contains(recipeId) || forcedAddedMineColoniesRecipes.contains(recipeId)) {
                        continue;
                    }
                    if (!visibleRules.stream().anyMatch(rule -> matchesMineColoniesRecipe(recipe, rule))) {
                        continue;
                    }

                    Object storage = recipe.getClass().getMethod("getRecipeStorage").invoke(recipe);
                    Object genericRecipe = create.invoke(null, recipe, storage);
                    recipesToAdd.add(genericRecipe);
                    forcedAddedMineColoniesRecipes.add(recipeId);
                }

                if (!recipesToAdd.isEmpty()) {
                    runtime.getRecipeManager().addRecipes((RecipeType) jeiType.get(), recipesToAdd);
                    added += recipesToAdd.size();
                    MyInfiniteCreation.LOGGER.info(
                            "Added {} forced-visible MineColonies custom recipe(s) to JEI type {}",
                            recipesToAdd.size(),
                            jeiType.get().getUid()
                    );
                }
            }
            if (added == 0) {
                MyInfiniteCreation.LOGGER.debug("No forced-visible MineColonies custom recipes matched current JEI progression rules");
            }
        } catch (ReflectiveOperationException exception) {
            MyInfiniteCreation.LOGGER.warn("Could not add forced-visible MineColonies custom recipes to JEI", exception);
        }
    }

    private static ResourceLocation mineColoniesJobRecipeTypeId(String crafter) {
        String path = crafter.endsWith("_crafting") ? crafter.substring(0, crafter.length() - "_crafting".length()) : crafter;
        return ResourceLocation.fromNamespaceAndPath("minecolonies", path);
    }

    private static ResourceLocation customRecipeId(Object recipe) {
        Object value = invokeNoArgs(recipe, "getRecipeId");
        return value instanceof ResourceLocation recipeId ? recipeId : null;
    }

    private static List<ProgressionGateRule> visibleRecipeRules() {
        return ClientProgressionHooks.recipeGateRules().stream()
                .filter(rule -> !rule.hideInJei)
                .toList();
    }

    private static void addMineColoniesRecipeGateRecipes(Map<RecipeType<Object>, List<Object>> recipesByJeiType, List<ProgressionGateRule> rules) {
        if (rules.isEmpty()) {
            return;
        }

        addMineColoniesRecipes(recipesByJeiType, recipe -> isLockedMineColoniesRecipe(recipe, rules));
    }

    private static void addLockedMineColoniesJobRecipes(
            Map<RecipeType<Object>, List<Object>> recipesByJeiType,
            List<ProgressionGateRule> lockedRules,
            List<ModGateRule> lockedModRules,
            List<ProgressionGateRule> visibleRules
    ) {
        if (lockedRules.isEmpty() && lockedModRules.isEmpty()) {
            return;
        }

        addMineColoniesRecipes(recipesByJeiType, recipe -> {
            if (isLockedMineColoniesRecipe(recipe, visibleRules)) {
                return false;
            }
            return isLockedMineColoniesRecipe(recipe, lockedRules)
                    || isLockedMineColoniesModGateRecipe(recipe, lockedModRules);
        });
    }

    private static void addMineColoniesRecipes(Map<RecipeType<Object>, List<Object>> recipesByJeiType, java.util.function.Predicate<Object> predicate) {
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
                            .filter(predicate)
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

    private static void registerNoSubtype(ISubtypeRegistration registration, ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (itemId.equals(BuiltInRegistries.ITEM.getKey(item))) {
            registration.registerSubtypeInterpreter(item, (stack, context) -> IIngredientSubtypeInterpreter.NONE);
        }
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

    private static boolean hasRequiredLockedModGateIngredient(CraftingRecipe recipe, ModGateRule rule) {
        return recipe.getIngredients().stream()
                .map(ingredient -> Arrays.stream(ingredient.getItems()).filter(stack -> !stack.isEmpty()).toList())
                .anyMatch(stacks -> !stacks.isEmpty() && stacks.stream().allMatch(stack -> isLockedModGateItem(rule, stack)));
    }

    private static boolean isLockedMineColoniesRecipe(Object recipe, List<ProgressionGateRule> lockedRules) {
        if (!hasMethod(recipe, "getPrimaryOutput")) {
            return false;
        }
        return lockedRules.stream().anyMatch(rule -> matchesMineColoniesRecipe(recipe, rule));
    }

    private static boolean isLockedMineColoniesModGateRecipe(Object recipe, List<ModGateRule> lockedModRules) {
        if (!hasMethod(recipe, "getPrimaryOutput")) {
            return false;
        }
        return lockedModRules.stream()
                .filter(rule -> rule.hideInJei)
                .anyMatch(rule -> itemStackResultMatches(recipe, rule, "getPrimaryOutput")
                        || itemStackListResultMatches(recipe, rule, "getAllMultiOutputs")
                        || itemStackListResultMatches(recipe, rule, "getAdditionalOutputs")
                        || nestedItemStackListResultMatches(recipe, rule, "getInputs"));
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

    private static boolean itemStackResultMatches(Object recipe, ModGateRule rule, String methodName) {
        Object value = invokeNoArgs(recipe, methodName);
        return value instanceof ItemStack stack && isLockedModGateItem(rule, stack);
    }

    private static boolean itemStackListResultMatches(Object recipe, ModGateRule rule, String methodName) {
        Object value = invokeNoArgs(recipe, methodName);
        if (!(value instanceof List<?> stacks)) {
            return false;
        }
        return stacks.stream().anyMatch(stack -> stack instanceof ItemStack itemStack && isLockedModGateItem(rule, itemStack));
    }

    private static boolean nestedItemStackListResultMatches(Object recipe, ModGateRule rule, String methodName) {
        Object value = invokeNoArgs(recipe, methodName);
        if (!(value instanceof List<?> inputGroups)) {
            return false;
        }
        return inputGroups.stream()
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .flatMap(List::stream)
                .anyMatch(stack -> stack instanceof ItemStack itemStack && isLockedModGateItem(rule, itemStack));
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
