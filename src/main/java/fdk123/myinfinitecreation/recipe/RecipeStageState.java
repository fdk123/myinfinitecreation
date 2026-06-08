package fdk123.myinfinitecreation.recipe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class RecipeStageState extends SavedData {
    private static final String DATA_NAME = "myinfinitecreation_recipe_stage";
    private static final String ACTIVE_STAGE_KEY = "active_stage";
    public static final String DEFAULT_STAGE = "stage_1";

    private String activeStage = DEFAULT_STAGE;

    public static RecipeStageState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(RecipeStageState::load, RecipeStageState::new, DATA_NAME);
    }

    public static RecipeStageState load(CompoundTag tag) {
        RecipeStageState state = new RecipeStageState();
        if (tag.contains(ACTIVE_STAGE_KEY)) {
            state.activeStage = tag.getString(ACTIVE_STAGE_KEY);
        }
        return state;
    }

    public String getActiveStage() {
        return activeStage;
    }

    public void setActiveStage(String activeStage) {
        this.activeStage = activeStage;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString(ACTIVE_STAGE_KEY, activeStage);
        return tag;
    }
}
