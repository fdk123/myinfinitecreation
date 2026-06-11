package fdk123.myinfinitecreation.progression;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public class ResearchStageUnlockRule {
    public String name = "Unnamed research stage unlock";
    public ResourceLocation research;
    public String stage;

    public static ResearchStageUnlockRule fromJson(JsonObject object) {
        ResearchStageUnlockRule rule = new ResearchStageUnlockRule();
        if (object.has("name")) {
            rule.name = object.get("name").getAsString();
        }
        if (object.has("research")) {
            rule.research = ResourceLocation.parse(object.get("research").getAsString());
        }
        if (object.has("stage")) {
            rule.stage = object.get("stage").getAsString();
        }
        return rule;
    }

    public boolean isComplete() {
        return research != null && stage != null && !stage.isBlank();
    }
}
