package fdk123.myinfinitecreation.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

public class ProgressionGateRule {
    public String name = "unnamed";
    public Set<String> requiredStages = new LinkedHashSet<>();
    public Set<ResourceLocation> requiredResearches = new LinkedHashSet<>();
    public Set<ResourceLocation> types = new LinkedHashSet<>();
    public Set<ResourceLocation> outputs = new LinkedHashSet<>();
    public Set<ResourceLocation> outputTags = new LinkedHashSet<>();

    public static ProgressionGateRule fromJson(JsonObject object) {
        ProgressionGateRule rule = new ProgressionGateRule();
        if (object.has("name")) {
            rule.name = object.get("name").getAsString();
        }
        readStrings(object, "required_stage", rule.requiredStages);
        readStrings(object, "required_stages", rule.requiredStages);
        readResourceLocations(object, "required_research", rule.requiredResearches);
        readResourceLocations(object, "required_researches", rule.requiredResearches);
        readResourceLocations(object, "type", rule.types);
        readResourceLocations(object, "types", rule.types);
        readResourceLocations(object, "output", rule.outputs);
        readResourceLocations(object, "outputs", rule.outputs);
        readResourceLocations(object, "output_tag", rule.outputTags);
        readResourceLocations(object, "output_tags", rule.outputTags);
        return rule;
    }

    public boolean isEmpty() {
        return requiredStages.isEmpty() && requiredResearches.isEmpty() || outputs.isEmpty() && outputTags.isEmpty();
    }

    private static void readResourceLocations(JsonObject object, String key, Set<ResourceLocation> target) {
        if (!object.has(key)) {
            return;
        }
        for (String value : readStringValues(object.get(key))) {
            ResourceLocation location = ResourceLocation.tryParse(value);
            if (location == null) {
                throw new IllegalArgumentException("Invalid resource location '" + value + "' for key '" + key + "'");
            }
            target.add(location);
        }
    }

    private static void readStrings(JsonObject object, String key, Set<String> target) {
        if (!object.has(key)) {
            return;
        }
        target.addAll(readStringValues(object.get(key)));
    }

    private static Set<String> readStringValues(JsonElement element) {
        Set<String> values = new LinkedHashSet<>();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                values.add(item.getAsString());
            }
        } else {
            values.add(element.getAsString());
        }
        return values;
    }
}
