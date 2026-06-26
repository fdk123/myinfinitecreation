package fdk123.myinfinitecreation.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

public class RecipePolicyRule {
    public String name = "unnamed";
    public Set<ResourceLocation> ids = new LinkedHashSet<>();
    public Set<String> namespaces = new LinkedHashSet<>();
    public Set<ResourceLocation> types = new LinkedHashSet<>();
    public Set<ResourceLocation> outputs = new LinkedHashSet<>();
    public Set<ResourceLocation> outputTags = new LinkedHashSet<>();
    public Set<ResourceLocation> inputs = new LinkedHashSet<>();
    public Set<ResourceLocation> inputTags = new LinkedHashSet<>();
    public Set<ResourceLocation> exceptIds = new LinkedHashSet<>();
    public Set<ResourceLocation> exceptTypes = new LinkedHashSet<>();
    public Set<ResourceLocation> exceptOutputs = new LinkedHashSet<>();

    public static RecipePolicyRule fromJson(JsonObject object) {
        RecipePolicyRule rule = new RecipePolicyRule();
        if (object.has("name")) {
            rule.name = object.get("name").getAsString();
        }
        readResourceLocations(object, "id", rule.ids);
        readStrings(object, "namespace", rule.namespaces);
        readResourceLocations(object, "type", rule.types);
        readResourceLocations(object, "output", rule.outputs);
        readResourceLocations(object, "output_tag", rule.outputTags);
        readResourceLocations(object, "output_tags", rule.outputTags);
        readResourceLocations(object, "input", rule.inputs);
        readResourceLocations(object, "inputs", rule.inputs);
        readResourceLocations(object, "input_tag", rule.inputTags);
        readResourceLocations(object, "input_tags", rule.inputTags);
        readResourceLocations(object, "except_id", rule.exceptIds);
        readResourceLocations(object, "except_ids", rule.exceptIds);
        readResourceLocations(object, "except_type", rule.exceptTypes);
        readResourceLocations(object, "except_types", rule.exceptTypes);
        readResourceLocations(object, "except_output", rule.exceptOutputs);
        readResourceLocations(object, "except_outputs", rule.exceptOutputs);
        return rule;
    }

    public boolean isEmpty() {
        return ids.isEmpty() && namespaces.isEmpty() && types.isEmpty() && outputs.isEmpty()
                && outputTags.isEmpty() && inputs.isEmpty() && inputTags.isEmpty();
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
