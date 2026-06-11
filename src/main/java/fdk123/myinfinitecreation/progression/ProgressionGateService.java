package fdk123.myinfinitecreation.progression;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class ProgressionGateService {
    private static final ResourceLocation VANILLA_CRAFTING = BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.CRAFTING);
    private static final ResourceLocation CREATE_MECHANICAL_CRAFTING = ResourceLocation.fromNamespaceAndPath("create", "mechanical_crafting");

    private final ProgressionGateLoader loader = new ProgressionGateLoader();
    private final StageAccess stageAccess = new StageAccess();
    private final MineColoniesResearchAccess mineColoniesResearchAccess = new MineColoniesResearchAccess();
    private List<ProgressionGateRule> rules = List.of();

    public void load(MinecraftServer server) {
        rules = loader.load(server.getResourceManager());
    }

    public void clear() {
        rules = List.of();
    }

    public List<ProgressionGateRule> rules() {
        return List.copyOf(rules);
    }

    public boolean mayTakeCraftingResult(Player player, ItemStack stack) {
        ProgressionGateRule rule = lockedRule(player, VANILLA_CRAFTING, stack);
        if (rule == null) {
            return true;
        }
        String requirements = ruleRequirements(rule);
        if (!player.level().isClientSide) {
            player.displayClientMessage(
                    Component.literal("Locked by progression: " + rule.name + " requires " + requirements),
                    true
            );
            MyInfiniteCreation.LOGGER.debug(
                    "Blocked crafting result {} for {} by gate '{}' requiring {}",
                    resultId(stack),
                    player.getGameProfile().getName(),
                    rule.name,
                    requirements
            );
        }
        return false;
    }

    public boolean mayCreateMechanicalCraftingResult(Level level, ItemStack stack) {
        ProgressionGateRule rule = lockedRule(level, CREATE_MECHANICAL_CRAFTING, stack);
        if (rule == null) {
            return true;
        }
        MyInfiniteCreation.LOGGER.debug(
                "Blocked Create mechanical crafting result {} by gate '{}' requiring {}",
                resultId(stack),
                rule.name,
                ruleRequirements(rule)
        );
        return false;
    }

    private ProgressionGateRule lockedRule(Player player, ResourceLocation type, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (ProgressionGateRule rule : rules) {
            if (matchesType(rule, type)
                    && matchesOutput(rule, stack)
                    && !isUnlocked(player, rule)) {
                return rule;
            }
        }
        return null;
    }

    private ProgressionGateRule lockedRule(Level level, ResourceLocation type, ItemStack stack) {
        if (level.isClientSide || stack.isEmpty()) {
            return null;
        }
        for (ProgressionGateRule rule : rules) {
            if (matchesType(rule, type)
                    && matchesOutput(rule, stack)
                    && !isUnlocked(level, rule)) {
                return rule;
            }
        }
        return null;
    }

    private boolean matchesType(ProgressionGateRule rule, ResourceLocation type) {
        return rule.types.isEmpty() || rule.types.contains(type);
    }

    private boolean isUnlocked(Player player, ProgressionGateRule rule) {
        return rule.requiredStages.stream().anyMatch(stage -> stageAccess.hasStage(player, stage))
                || rule.requiredResearches.stream().anyMatch(research -> mineColoniesResearchAccess.hasCompletedResearch(player, research));
    }

    private boolean isUnlocked(Level level, ProgressionGateRule rule) {
        return rule.requiredStages.stream().anyMatch(stage -> hasWorldStage(level, stage));
    }

    private String ruleRequirements(ProgressionGateRule rule) {
        List<String> requirements = new java.util.ArrayList<>();
        requirements.addAll(rule.requiredStages);
        rule.requiredResearches.stream().map(ResourceLocation::toString).forEach(requirements::add);
        return String.join(", ", requirements);
    }

    private boolean hasWorldStage(Level level, String stage) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }
        if (server.getPlayerList().getPlayers().stream().anyMatch(player -> stageAccess.hasStage(player, stage))) {
            return true;
        }
        String activeStage = fdk123.myinfinitecreation.recipe.RecipeStageState.get(server).getActiveStage();
        return activeStage.equals(stage) || activeStage.equals("stage_" + stage);
    }

    private boolean matchesOutput(ProgressionGateRule rule, ItemStack stack) {
        ResourceLocation output = resultId(stack);
        if (!rule.outputs.isEmpty() && output != null && rule.outputs.contains(output)) {
            return true;
        }
        return rule.outputTags.stream().anyMatch(tag -> stack.is(TagKey.create(BuiltInRegistries.ITEM.key(), tag)));
    }

    private ResourceLocation resultId(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }
}
