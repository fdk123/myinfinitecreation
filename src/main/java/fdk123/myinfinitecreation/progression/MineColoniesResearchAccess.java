package fdk123.myinfinitecreation.progression;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MineColoniesResearchAccess {
    private Method apiGetInstanceMethod;
    private Method apiGetColonyManagerMethod;
    private Method getColonyByOwnerMethod;
    private Method getColonyByPosFromWorldMethod;
    private Method colonyGetResearchManagerMethod;
    private Method researchManagerGetResearchTreeMethod;
    private Method researchTreeHasCompletedResearchMethod;
    private Method researchTreeGetCompletedListMethod;
    private boolean initialized = false;

    public boolean hasCompletedResearch(Player player, ResourceLocation research) {
        return completedResearches(player).contains(research);
    }

    public Set<ResourceLocation> completedResearches(Player player) {
        initialize();
        Set<ResourceLocation> completed = new LinkedHashSet<>();
        Object colony = colonyFor(player);
        if (colony == null || researchManagerGetResearchTreeMethod == null || researchTreeGetCompletedListMethod == null) {
            return completed;
        }

        try {
            Object researchManager = colonyGetResearchManagerMethod.invoke(colony);
            Object researchTree = researchManagerGetResearchTreeMethod.invoke(researchManager);
            Object completedList = researchTreeGetCompletedListMethod.invoke(researchTree);
            if (completedList instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry instanceof ResourceLocation research) {
                        completed.add(research);
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            return Set.of();
        }
        return completed;
    }

    private Object colonyFor(Player player) {
        if (!ModList.get().isLoaded("minecolonies") || player.level().isClientSide || !initialized) {
            return null;
        }

        try {
            Object api = apiGetInstanceMethod.invoke(null);
            Object colonyManager = apiGetColonyManagerMethod.invoke(api);
            Object colony = getColonyByOwnerMethod.invoke(colonyManager, player.level(), player);
            if (colony != null) {
                return colony;
            }
            return getColonyByPosFromWorldMethod.invoke(colonyManager, player.level(), player.blockPosition());
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!ModList.get().isLoaded("minecolonies")) {
            return;
        }

        try {
            Class<?> apiClass = Class.forName("com.minecolonies.api.IMinecoloniesAPI");
            apiGetInstanceMethod = apiClass.getMethod("getInstance");
            apiGetColonyManagerMethod = apiClass.getMethod("getColonyManager");

            Class<?> colonyManagerClass = Class.forName("com.minecolonies.api.colony.IColonyManager");
            getColonyByOwnerMethod = colonyManagerClass.getMethod("getIColonyByOwner", Level.class, Player.class);
            getColonyByPosFromWorldMethod = colonyManagerClass.getMethod("getColonyByPosFromWorld", Level.class, net.minecraft.core.BlockPos.class);

            Class<?> colonyClass = Class.forName("com.minecolonies.api.colony.IColony");
            colonyGetResearchManagerMethod = colonyClass.getMethod("getResearchManager");

            Class<?> researchManagerClass = Class.forName("com.minecolonies.api.research.IResearchManager");
            researchManagerGetResearchTreeMethod = researchManagerClass.getMethod("getResearchTree");

            Class<?> researchTreeClass = Class.forName("com.minecolonies.api.research.ILocalResearchTree");
            researchTreeHasCompletedResearchMethod = researchTreeClass.getMethod("hasCompletedResearch", ResourceLocation.class);
            researchTreeGetCompletedListMethod = researchTreeClass.getMethod("getCompletedList");
        } catch (ReflectiveOperationException ignored) {
            apiGetInstanceMethod = null;
            apiGetColonyManagerMethod = null;
            getColonyByOwnerMethod = null;
            getColonyByPosFromWorldMethod = null;
            colonyGetResearchManagerMethod = null;
            researchManagerGetResearchTreeMethod = null;
            researchTreeHasCompletedResearchMethod = null;
            researchTreeGetCompletedListMethod = null;
        }
    }
}
