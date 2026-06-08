package fdk123.myinfinitecreation.progression;

import fdk123.myinfinitecreation.recipe.RecipeStageState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

public class StageAccess {
    private static String clientFallbackStage = RecipeStageState.DEFAULT_STAGE;

    private Method teamStagesPlayerMethod;
    private Method teamStagesHasStageMethod;
    private Method gameStagesHasStageMethod;
    private boolean initialized = false;

    public boolean hasStage(Player player, String stage) {
        initialize();
        if (hasTeamStage(player, stage)) {
            return true;
        }
        if (hasGameStage(player, stage)) {
            return true;
        }
        return hasWorldStage(player, stage);
    }

    public static void setClientFallbackStage(String stage) {
        clientFallbackStage = stage;
    }

    private void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Class<?> helperClass = Class.forName("dev.scsupercraft.teamstages.data.TeamStageHelper");
            teamStagesPlayerMethod = helperClass.getMethod("player");
            Object playerHelper = teamStagesPlayerMethod.invoke(null);
            teamStagesHasStageMethod = playerHelper.getClass().getMethod("hasStage", Player.class, String.class);
        } catch (ReflectiveOperationException ignored) {
            teamStagesPlayerMethod = null;
            teamStagesHasStageMethod = null;
        }
        try {
            Class<?> helperClass = Class.forName("net.darkhax.gamestages.GameStageHelper");
            gameStagesHasStageMethod = helperClass.getMethod("hasStage", Player.class, String.class);
        } catch (ReflectiveOperationException ignored) {
            gameStagesHasStageMethod = null;
        }
    }

    private boolean hasTeamStage(Player player, String stage) {
        if (teamStagesPlayerMethod == null || teamStagesHasStageMethod == null) {
            return false;
        }
        try {
            Object playerHelper = teamStagesPlayerMethod.invoke(null);
            return Boolean.TRUE.equals(teamStagesHasStageMethod.invoke(playerHelper, player, stage));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private boolean hasGameStage(Player player, String stage) {
        if (gameStagesHasStageMethod == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(gameStagesHasStageMethod.invoke(null, player, stage));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private boolean hasWorldStage(Player player, String stage) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return clientFallbackStage.equals(stage) || clientFallbackStage.equals("stage_" + stage);
        }
        String activeStage = RecipeStageState.get(server).getActiveStage();
        return activeStage.equals(stage) || activeStage.equals("stage_" + stage);
    }
}
