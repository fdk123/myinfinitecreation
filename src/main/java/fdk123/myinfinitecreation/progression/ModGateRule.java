package fdk123.myinfinitecreation.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

public class ModGateRule {
    public String name = "unnamed";
    public Set<String> requiredStages = new LinkedHashSet<>();
    public Set<ResourceLocation> requiredResearches = new LinkedHashSet<>();
    public Set<String> modids = new LinkedHashSet<>();
    public Set<ResourceLocation> items = new LinkedHashSet<>();
    public Set<ResourceLocation> itemTags = new LinkedHashSet<>();
    public Set<ResourceLocation> blocks = new LinkedHashSet<>();
    public Set<ResourceLocation> blockTags = new LinkedHashSet<>();
    public Set<ResourceLocation> exceptItems = new LinkedHashSet<>();
    public Set<ResourceLocation> exceptBlocks = new LinkedHashSet<>();
    public boolean hideInJei = true;
    public boolean maskName = true;
    public boolean allowPickup = true;
    public boolean preventUse = true;
    public boolean preventPlace = true;
    public boolean preventBreakBlocks = true;

    public static ModGateRule fromJson(JsonObject object) {
        ModGateRule rule = new ModGateRule();
        if (object.has("name")) {
            rule.name = object.get("name").getAsString();
        }
        readStrings(object, "required_stage", rule.requiredStages);
        readStrings(object, "required_stages", rule.requiredStages);
        readResourceLocations(object, "required_research", rule.requiredResearches);
        readResourceLocations(object, "required_researches", rule.requiredResearches);
        readStrings(object, "modid", rule.modids);
        readStrings(object, "modids", rule.modids);
        readResourceLocations(object, "item", rule.items);
        readResourceLocations(object, "items", rule.items);
        readResourceLocations(object, "item_tag", rule.itemTags);
        readResourceLocations(object, "item_tags", rule.itemTags);
        readResourceLocations(object, "block", rule.blocks);
        readResourceLocations(object, "blocks", rule.blocks);
        readResourceLocations(object, "block_tag", rule.blockTags);
        readResourceLocations(object, "block_tags", rule.blockTags);
        readResourceLocations(object, "except_item", rule.exceptItems);
        readResourceLocations(object, "except_items", rule.exceptItems);
        readResourceLocations(object, "except_block", rule.exceptBlocks);
        readResourceLocations(object, "except_blocks", rule.exceptBlocks);
        rule.hideInJei = readBoolean(object, "hide_in_jei", rule.hideInJei);
        rule.maskName = readBoolean(object, "mask_name", rule.maskName);
        rule.allowPickup = readBoolean(object, "allow_pickup", rule.allowPickup);
        rule.preventUse = readBoolean(object, "prevent_use", rule.preventUse);
        rule.preventPlace = readBoolean(object, "prevent_place", rule.preventPlace);
        rule.preventBreakBlocks = readBoolean(object, "prevent_break_blocks", rule.preventBreakBlocks);
        return rule;
    }

    public boolean isEmpty() {
        return requiredStages.isEmpty() && requiredResearches.isEmpty()
                || modids.isEmpty() && items.isEmpty() && itemTags.isEmpty() && blocks.isEmpty() && blockTags.isEmpty();
    }

    private static boolean readBoolean(JsonObject object, String key, boolean fallback) {
        return object.has(key) ? object.get(key).getAsBoolean() : fallback;
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
