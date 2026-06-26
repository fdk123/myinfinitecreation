package fdk123.myinfinitecreation.client;

import fdk123.myinfinitecreation.progression.ProgressionGateRule;
import fdk123.myinfinitecreation.progression.ModGateRule;
import fdk123.myinfinitecreation.progression.StageAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

public class ClientProgressionHooks {
    private static List<ProgressionGateRule> recipeGateRules = List.of();
    private static List<ModGateRule> modGateRules = List.of();
    private static Set<ResourceLocation> completedMineColoniesResearches = Set.of();
    private static String clientFallbackStage = "";

    public static void handleStageSync(String stage) {
        clientFallbackStage = stage;
        StageAccess.setClientFallbackStage(stage);
        JeiProgressionPlugin.refreshRuntime();
    }

    public static void handleRecipeGatesSync(List<ProgressionGateRule> rules) {
        recipeGateRules = List.copyOf(rules);
        JeiProgressionPlugin.refreshRuntime();
    }

    public static void handleModGatesSync(List<ModGateRule> rules) {
        modGateRules = List.copyOf(rules);
        JeiProgressionPlugin.refreshRuntime();
    }

    public static void handleMineColoniesResearchSync(Set<ResourceLocation> completedResearches) {
        if (completedMineColoniesResearches.equals(completedResearches)) {
            return;
        }
        completedMineColoniesResearches = Set.copyOf(completedResearches);
        JeiProgressionPlugin.refreshRuntime();
    }

    public static List<ProgressionGateRule> recipeGateRules() {
        return recipeGateRules;
    }

    public static List<ModGateRule> modGateRules() {
        return modGateRules;
    }

    public static ModGateRule lockedModItemRule(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (isAllowedItem(player, stack)) {
            return null;
        }
        for (ModGateRule rule : modGateRules) {
            if (rule.isRestrict() && matchesItem(rule, stack) && !isUnlocked(player, rule)) {
                return rule;
            }
        }
        return null;
    }

    public static boolean isUnlocked(Player player, ModGateRule rule) {
        if (rule.requiredStages.isEmpty() && rule.requiredResearches.isEmpty()) {
            return true;
        }
        return player != null && rule.requiredStages.stream().anyMatch(stage -> new StageAccess().hasStage(player, stage))
                || rule.requiredResearches.stream().anyMatch(ClientProgressionHooks::hasMineColoniesResearch);
    }

    public static boolean isAllowedItem(Player player, ItemStack stack) {
        return modGateRules.stream().anyMatch(rule -> rule.isAllow() && matchesItem(rule, stack) && isUnlocked(player, rule));
    }

    public static boolean matchesItem(ModGateRule rule, ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null || rule.exceptItems.contains(itemId)) {
            return false;
        }
        if (!rule.items.isEmpty() && rule.items.contains(itemId)) {
            return true;
        }
        if (ModGateRule.matchesLocationPattern(rule.itemPatterns, itemId)) {
            return true;
        }
        if (rule.modids.contains(itemId.getNamespace())) {
            return true;
        }
        return rule.itemTags.stream().anyMatch(tag -> stack.is(TagKey.create(BuiltInRegistries.ITEM.key(), tag)));
    }

    public static boolean matchesItemId(ModGateRule rule, ResourceLocation itemId) {
        if (itemId == null || rule.exceptItems.contains(itemId)) {
            return false;
        }
        if (!rule.items.isEmpty() && rule.items.contains(itemId)) {
            return true;
        }
        if (ModGateRule.matchesLocationPattern(rule.itemPatterns, itemId)) {
            return true;
        }
        return rule.modids.contains(itemId.getNamespace());
    }

    public static boolean hasMineColoniesResearch(ResourceLocation research) {
        return completedMineColoniesResearches.contains(research);
    }
}
