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

public class ModGateLoader {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "mod_gates";

    public List<ModGateRule> load(ResourceManager resourceManager) {
        List<ModGateRule> rules = new ArrayList<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                DIRECTORY,
                location -> location.getPath().endsWith(".json")
        );

        resources.forEach((location, resource) -> loadFile(location, resource, rules));
        MyInfiniteCreation.LOGGER.info("Loaded {} mod gate rule(s) from {} file(s)", rules.size(), resources.size());
        return rules;
    }

    private void loadFile(ResourceLocation location, Resource resource, List<ModGateRule> rules) {
        try (BufferedReader reader = resource.openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ModGateFile file = GSON.fromJson(json, ModGateFile.class);
            if (file.replace) {
                rules.clear();
            }
            if (json.has("rules")) {
                for (var element : json.getAsJsonArray("rules")) {
                    ModGateRule rule = ModGateRule.fromJson(element.getAsJsonObject());
                    if (rule.isEmpty()) {
                        MyInfiniteCreation.LOGGER.warn("Skipping incomplete mod gate rule '{}' in {}", rule.name, location);
                    } else {
                        rules.add(rule);
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            MyInfiniteCreation.LOGGER.error("Failed to load mod gate file {}", location, exception);
        }
    }
}
