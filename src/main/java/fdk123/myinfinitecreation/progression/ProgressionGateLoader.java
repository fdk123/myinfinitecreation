package fdk123.myinfinitecreation.progression;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProgressionGateLoader {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "recipe_gates";

    public List<ProgressionGateRule> load(ResourceManager resourceManager) {
        List<ProgressionGateRule> rules = new ArrayList<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                DIRECTORY,
                location -> location.getPath().endsWith(".json")
        );

        resources.forEach((location, resource) -> loadFile(location, resource, rules));
        MyInfiniteCreation.LOGGER.info("Loaded {} recipe gate rule(s) from {} file(s)", rules.size(), resources.size());
        return rules;
    }

    private void loadFile(ResourceLocation location, Resource resource, List<ProgressionGateRule> rules) {
        try (BufferedReader reader = resource.openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ProgressionGateFile file = GSON.fromJson(json, ProgressionGateFile.class);
            if (file.replace) {
                rules.clear();
            }
            if (json.has("rules")) {
                for (var element : json.getAsJsonArray("rules")) {
                    ProgressionGateRule rule = ProgressionGateRule.fromJson(element.getAsJsonObject());
                    if (rule.isEmpty()) {
                        MyInfiniteCreation.LOGGER.warn("Skipping incomplete recipe gate rule '{}' in {}", rule.name, location);
                    } else {
                        rules.add(rule);
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            MyInfiniteCreation.LOGGER.error("Failed to load recipe gate file {}", location, exception);
        }
    }
}
