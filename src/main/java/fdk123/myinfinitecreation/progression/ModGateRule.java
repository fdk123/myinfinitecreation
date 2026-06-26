package fdk123.myinfinitecreation.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

public class ModGateRule {
    public static final String MODE_RESTRICT = "restrict";
    public static final String MODE_ALLOW = "allow";

    public String name = "unnamed";
    public String mode = MODE_RESTRICT;
    public Set<String> requiredStages = new LinkedHashSet<>();
    public Set<ResourceLocation> requiredResearches = new LinkedHashSet<>();
    public Set<String> modids = new LinkedHashSet<>();
    public Set<ResourceLocation> items = new LinkedHashSet<>();
    public Set<String> itemPatterns = new LinkedHashSet<>();
    public Set<ResourceLocation> itemTags = new LinkedHashSet<>();
    public Set<ResourceLocation> blocks = new LinkedHashSet<>();
    public Set<String> blockPatterns = new LinkedHashSet<>();
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
        if (object.has("mode")) {
            rule.mode = object.get("mode").getAsString();
            if (!MODE_RESTRICT.equals(rule.mode) && !MODE_ALLOW.equals(rule.mode)) {
                throw new IllegalArgumentException("Invalid mod gate mode '" + rule.mode + "' in rule '" + rule.name + "'");
            }
        }
        readStrings(object, "required_stage", rule.requiredStages);
        readStrings(object, "required_stages", rule.requiredStages);
        readResourceLocations(object, "required_research", rule.requiredResearches);
        readResourceLocations(object, "required_researches", rule.requiredResearches);
        readStrings(object, "modid", rule.modids);
        readStrings(object, "modids", rule.modids);
        readResourceLocations(object, "item", rule.items);
        readResourceLocations(object, "items", rule.items);
        readStrings(object, "item_pattern", rule.itemPatterns);
        readStrings(object, "item_patterns", rule.itemPatterns);
        readResourceLocations(object, "item_tag", rule.itemTags);
        readResourceLocations(object, "item_tags", rule.itemTags);
        readResourceLocations(object, "block", rule.blocks);
        readResourceLocations(object, "blocks", rule.blocks);
        readStrings(object, "block_pattern", rule.blockPatterns);
        readStrings(object, "block_patterns", rule.blockPatterns);
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
        boolean hasNoTargets = modids.isEmpty()
                && items.isEmpty()
                && itemPatterns.isEmpty()
                && itemTags.isEmpty()
                && blocks.isEmpty()
                && blockPatterns.isEmpty()
                && blockTags.isEmpty();
        if (hasNoTargets) {
            return true;
        }
        return isRestrict() && requiredStages.isEmpty() && requiredResearches.isEmpty();
    }

    public static boolean matchesLocationPattern(Set<String> patterns, ResourceLocation location) {
        if (location == null || patterns.isEmpty()) {
            return false;
        }
        String value = location.toString();
        return patterns.stream().anyMatch(pattern -> wildcardMatches(pattern, value));
    }

    public boolean isAllow() {
        return MODE_ALLOW.equals(mode);
    }

    public boolean isRestrict() {
        return MODE_RESTRICT.equals(mode);
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

    private static boolean wildcardMatches(String pattern, String value) {
        int patternIndex = 0;
        int valueIndex = 0;
        int starIndex = -1;
        int matchIndex = 0;

        while (valueIndex < value.length()) {
            if (patternIndex < pattern.length()
                    && (pattern.charAt(patternIndex) == '?' || pattern.charAt(patternIndex) == value.charAt(valueIndex))) {
                patternIndex++;
                valueIndex++;
            } else if (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
                starIndex = patternIndex;
                matchIndex = valueIndex;
                patternIndex++;
            } else if (starIndex != -1) {
                patternIndex = starIndex + 1;
                matchIndex++;
                valueIndex = matchIndex;
            } else {
                return false;
            }
        }

        while (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
            patternIndex++;
        }
        return patternIndex == pattern.length();
    }
}
