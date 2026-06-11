package fdk123.myinfinitecreation.progression;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class ResearchStageUnlockService {
    private final ResearchStageUnlockLoader loader = new ResearchStageUnlockLoader();
    private final MineColoniesResearchAccess mineColoniesResearchAccess = new MineColoniesResearchAccess();
    private final StageAccess stageAccess = new StageAccess();
    private List<ResearchStageUnlockRule> rules = List.of();

    public void load(MinecraftServer server) {
        rules = loader.load(server.getResourceManager());
    }

    public void clear() {
        rules = List.of();
    }

    public boolean apply(ServerPlayer player) {
        return applyAndCount(player) > 0;
    }

    public int applyAndCount(ServerPlayer player) {
        int changed = 0;
        for (ResearchStageUnlockRule rule : rules) {
            if (!stageAccess.hasStage(player, rule.stage)
                    && mineColoniesResearchAccess.hasCompletedResearch(player, rule.research)
                    && stageAccess.addStage(player, rule.stage)) {
                changed++;
                MyInfiniteCreation.LOGGER.info(
                        "Unlocked TeamStage '{}' for {} from MineColonies research '{}' ({})",
                        rule.stage,
                        player.getGameProfile().getName(),
                        rule.research,
                        rule.name
                );
            }
        }
        return changed;
    }

    public List<String> status(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        for (ResearchStageUnlockRule rule : rules) {
            boolean researchDone = mineColoniesResearchAccess.hasCompletedResearch(player, rule.research);
            boolean stageDone = stageAccess.hasStage(player, rule.stage);
            lines.add(rule.research + " -> " + rule.stage
                    + " research=" + (researchDone ? "done" : "missing")
                    + " stage=" + (stageDone ? "present" : "missing"));
        }
        return lines;
    }

    public List<ResearchStageUnlockRule> rules() {
        return List.copyOf(rules);
    }
}
