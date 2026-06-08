package fdk123.myinfinitecreation.client;

import fdk123.myinfinitecreation.progression.ProgressionGateRule;
import fdk123.myinfinitecreation.progression.StageAccess;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public class ClientProgressionHooks {
    private static List<ProgressionGateRule> recipeGateRules = List.of();
    private static Set<ResourceLocation> completedMineColoniesResearches = Set.of();

    public static void handleStageSync(String stage) {
        StageAccess.setClientFallbackStage(stage);
        JeiProgressionPlugin.refreshRuntime();
    }

    public static void handleRecipeGatesSync(List<ProgressionGateRule> rules) {
        recipeGateRules = List.copyOf(rules);
        JeiProgressionPlugin.refreshRuntime();
    }

    public static void handleMineColoniesResearchSync(Set<ResourceLocation> completedResearches) {
        completedMineColoniesResearches = Set.copyOf(completedResearches);
        JeiProgressionPlugin.refreshRuntime();
    }

    public static List<ProgressionGateRule> recipeGateRules() {
        return recipeGateRules;
    }

    public static boolean hasMineColoniesResearch(ResourceLocation research) {
        return completedMineColoniesResearches.contains(research);
    }
}
