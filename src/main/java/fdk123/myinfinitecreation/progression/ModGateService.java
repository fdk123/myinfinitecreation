package fdk123.myinfinitecreation.progression;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ModGateService {
    private final ModGateLoader loader = new ModGateLoader();
    private final StageAccess stageAccess = new StageAccess();
    private final MineColoniesResearchAccess mineColoniesResearchAccess = new MineColoniesResearchAccess();
    private List<ModGateRule> rules = List.of();

    public void load(MinecraftServer server) {
        rules = loader.load(server.getResourceManager());
    }

    public void clear() {
        rules = List.of();
    }

    public List<ModGateRule> rules() {
        return List.copyOf(rules);
    }

    public ModGateRule lockedItemRule(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (ModGateRule rule : rules) {
            if (matchesItem(rule, stack) && !isUnlocked(player, rule)) {
                return rule;
            }
        }
        return null;
    }

    public ModGateRule lockedBlockRule(Player player, BlockState state) {
        for (ModGateRule rule : rules) {
            if (matchesBlock(rule, state) && !isUnlocked(player, rule)) {
                return rule;
            }
        }
        return null;
    }

    public boolean mayUseItem(Player player, ItemStack stack) {
        ModGateRule rule = lockedItemRule(player, stack);
        if (rule == null || !rule.preventUse) {
            return true;
        }
        notifyBlocked(player, rule);
        return false;
    }

    public boolean mayUseBlock(Player player, BlockState state) {
        ModGateRule rule = lockedBlockRule(player, state);
        if (rule == null || !rule.preventUse) {
            return true;
        }
        notifyBlocked(player, rule);
        return false;
    }

    public boolean mayPlaceBlock(Player player, BlockState state) {
        ModGateRule rule = lockedBlockRule(player, state);
        if (rule == null || !rule.preventPlace) {
            return true;
        }
        notifyBlocked(player, rule);
        return false;
    }

    public boolean mayBreakBlock(Player player, BlockState state) {
        ModGateRule rule = lockedBlockRule(player, state);
        if (rule == null || !rule.preventBreakBlocks) {
            return true;
        }
        notifyBlocked(player, rule);
        return false;
    }

    public boolean isUnlocked(Player player, ModGateRule rule) {
        return player != null && (rule.requiredStages.stream().anyMatch(stage -> stageAccess.hasStage(player, stage))
                || rule.requiredResearches.stream().anyMatch(research -> mineColoniesResearchAccess.hasCompletedResearch(player, research)));
    }

    public boolean isUnlocked(Level level, ModGateRule rule) {
        return rule.requiredStages.stream().anyMatch(stage -> hasWorldStage(level, stage));
    }

    public boolean matchesItem(ModGateRule rule, ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null || rule.exceptItems.contains(itemId)) {
            return false;
        }
        if (!rule.items.isEmpty() && rule.items.contains(itemId)) {
            return true;
        }
        if (rule.modids.contains(itemId.getNamespace())) {
            return true;
        }
        return rule.itemTags.stream().anyMatch(tag -> stack.is(TagKey.create(BuiltInRegistries.ITEM.key(), tag)));
    }

    public boolean matchesBlock(ModGateRule rule, BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null || rule.exceptBlocks.contains(blockId)) {
            return false;
        }
        if (!rule.blocks.isEmpty() && rule.blocks.contains(blockId)) {
            return true;
        }
        if (rule.modids.contains(blockId.getNamespace())) {
            return true;
        }
        return rule.blockTags.stream().anyMatch(tag -> state.is(TagKey.create(BuiltInRegistries.BLOCK.key(), tag)));
    }

    private boolean hasWorldStage(Level level, String stage) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }
        String activeStage = fdk123.myinfinitecreation.recipe.RecipeStageState.get(server).getActiveStage();
        return activeStage.equals(stage) || activeStage.equals("stage_" + stage);
    }

    private void notifyBlocked(Player player, ModGateRule rule) {
        if (!player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("message.myinfinitecreation.mod_gate.locked"), true);
            MyInfiniteCreation.LOGGER.debug(
                    "Blocked gated mod interaction for {} by rule '{}'",
                    player.getGameProfile().getName(),
                    rule.name
            );
        }
    }
}
