package fdk123.myinfinitecreation.progression;

import fdk123.myinfinitecreation.recipe.RecipeStageState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Method;

public class StageAccess {
    private static String clientFallbackStage = RecipeStageState.DEFAULT_STAGE;

    private Method teamStagesTeamMethod;
    private Method teamStagesTeamHasStageMethod;
    private Method teamStagesTeamAddStageMethod;
    private Method teamStagesTeamRemoveStageMethod;
    private Method teamStagesTeamGetDataMethod;
    private Method gameStagesHasStageMethod;
    private Method gameStagesAddStageMethod;
    private Method gameStagesRemoveStageMethod;
    private Method gameStagesGetDataMethod;
    private boolean initialized = false;

    public boolean hasStage(Player player, String stage) {
        initialize();
        for (String alias : stageAliases(stage)) {
            if (hasTeamStage(player, alias)) {
                return true;
            }
            if (hasGameStage(player, alias)) {
                return true;
            }
            if (hasWorldStage(player, alias)) {
                return true;
            }
        }
        return false;
    }

    public boolean addStage(ServerPlayer player, String stage) {
        initialize();
        if (addTeamStage(player, stage)) {
            return true;
        }
        return addGameStage(player, stage);
    }

    public boolean removeStage(ServerPlayer player, String stage) {
        initialize();
        if (removeTeamStage(player, stage)) {
            return true;
        }
        return removeGameStage(player, stage);
    }

    public Collection<String> stages(Player player) {
        initialize();
        Collection<String> teamStages = teamStages(player);
        if (!teamStages.isEmpty()) {
            return teamStages;
        }
        Collection<String> gameStages = gameStages(player);
        if (!gameStages.isEmpty()) {
            return gameStages;
        }
        return List.of();
    }

    public String primaryBackend() {
        initialize();
        if (teamStagesTeamMethod != null) {
            return "TeamStages";
        }
        if (gameStagesHasStageMethod != null) {
            return "GameStages";
        }
        return "MIC fallback";
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
            teamStagesTeamMethod = helperClass.getMethod("team");
            Object teamHelper = teamStagesTeamMethod.invoke(null);
            Class<?> teamHelperClass = teamHelper.getClass();
            teamStagesTeamHasStageMethod = teamHelperClass.getMethod("hasStage", Player.class, String.class);
            teamStagesTeamAddStageMethod = teamHelperClass.getMethod("addStage", ServerPlayer.class, String[].class);
            teamStagesTeamRemoveStageMethod = teamHelperClass.getMethod("removeStage", ServerPlayer.class, String[].class);
            teamStagesTeamGetDataMethod = teamHelperClass.getMethod("getTeamData", Player.class);
        } catch (ReflectiveOperationException ignored) {
            teamStagesTeamMethod = null;
            teamStagesTeamHasStageMethod = null;
            teamStagesTeamAddStageMethod = null;
            teamStagesTeamRemoveStageMethod = null;
            teamStagesTeamGetDataMethod = null;
        }
        try {
            Class<?> helperClass = Class.forName("net.darkhax.gamestages.GameStageHelper");
            gameStagesHasStageMethod = helperClass.getMethod("hasStage", Player.class, String.class);
            gameStagesAddStageMethod = helperClass.getMethod("addStage", ServerPlayer.class, String[].class);
            gameStagesRemoveStageMethod = helperClass.getMethod("removeStage", ServerPlayer.class, String[].class);
            gameStagesGetDataMethod = helperClass.getMethod("getPlayerData", Player.class);
        } catch (ReflectiveOperationException ignored) {
            gameStagesHasStageMethod = null;
            gameStagesAddStageMethod = null;
            gameStagesRemoveStageMethod = null;
            gameStagesGetDataMethod = null;
        }
    }

    private boolean hasTeamStage(Player player, String stage) {
        if (teamStagesTeamMethod == null || teamStagesTeamHasStageMethod == null) {
            return false;
        }
        try {
            Object teamHelper = teamStagesTeamMethod.invoke(null);
            return Boolean.TRUE.equals(teamStagesTeamHasStageMethod.invoke(teamHelper, player, stage));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private boolean addTeamStage(ServerPlayer player, String stage) {
        if (teamStagesTeamMethod == null || teamStagesTeamAddStageMethod == null) {
            return false;
        }
        try {
            Object teamHelper = teamStagesTeamMethod.invoke(null);
            teamStagesTeamAddStageMethod.invoke(teamHelper, player, new String[]{stage});
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private boolean removeTeamStage(ServerPlayer player, String stage) {
        if (teamStagesTeamMethod == null || teamStagesTeamRemoveStageMethod == null) {
            return false;
        }
        try {
            Object teamHelper = teamStagesTeamMethod.invoke(null);
            teamStagesTeamRemoveStageMethod.invoke(teamHelper, player, new String[]{stage});
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private Collection<String> teamStages(Player player) {
        if (teamStagesTeamMethod == null || teamStagesTeamGetDataMethod == null) {
            return List.of();
        }
        try {
            Object teamHelper = teamStagesTeamMethod.invoke(null);
            Object data = teamStagesTeamGetDataMethod.invoke(teamHelper, player);
            return readStages(data);
        } catch (ReflectiveOperationException ignored) {
            return List.of();
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

    private boolean addGameStage(ServerPlayer player, String stage) {
        if (gameStagesAddStageMethod == null) {
            return false;
        }
        try {
            gameStagesAddStageMethod.invoke(null, player, new String[]{stage});
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private boolean removeGameStage(ServerPlayer player, String stage) {
        if (gameStagesRemoveStageMethod == null) {
            return false;
        }
        try {
            gameStagesRemoveStageMethod.invoke(null, player, new String[]{stage});
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private Collection<String> gameStages(Player player) {
        if (gameStagesGetDataMethod == null) {
            return List.of();
        }
        try {
            return readStages(gameStagesGetDataMethod.invoke(null, player));
        } catch (ReflectiveOperationException ignored) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<String> readStages(Object data) throws ReflectiveOperationException {
        if (data == null) {
            return List.of();
        }
        Method getStagesMethod = data.getClass().getMethod("getStages");
        Object stages = getStagesMethod.invoke(data);
        if (stages instanceof Collection<?>) {
            return (Collection<String>) stages;
        }
        return List.of();
    }

    private boolean hasWorldStage(Player player, String stage) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return clientFallbackStage.equals(stage);
        }
        String activeStage = RecipeStageState.get(server).getActiveStage();
        return activeStage.equals(stage);
    }

    private Set<String> stageAliases(String stage) {
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(stage);
        if (stage.startsWith("stage_")) {
            aliases.add(stage.substring("stage_".length()));
        } else {
            aliases.add("stage_" + stage);
        }
        return aliases;
    }
}
