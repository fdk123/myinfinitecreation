package fdk123.myinfinitecreation.progression;

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

public class ResearchStageUnlockLoader {
    private static final String DIRECTORY = "research_stage_unlocks";

    public List<ResearchStageUnlockRule> load(ResourceManager resourceManager) {
        List<ResearchStageUnlockRule> rules = new ArrayList<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                DIRECTORY,
                location -> location.getPath().endsWith(".json")
        );

        resources.forEach((location, resource) -> loadFile(location, resource, rules));
        MyInfiniteCreation.LOGGER.info("Loaded {} research stage unlock(s) from {} file(s)", rules.size(), resources.size());
        return rules;
    }

    private void loadFile(ResourceLocation location, Resource resource, List<ResearchStageUnlockRule> rules) {
        try (BufferedReader reader = resource.openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("replace") && json.get("replace").getAsBoolean()) {
                rules.clear();
            }
            if (json.has("unlocks")) {
                for (var element : json.getAsJsonArray("unlocks")) {
                    ResearchStageUnlockRule rule = ResearchStageUnlockRule.fromJson(element.getAsJsonObject());
                    if (rule.isComplete()) {
                        rules.add(rule);
                    } else {
                        MyInfiniteCreation.LOGGER.warn("Skipping incomplete research stage unlock '{}' in {}", rule.name, location);
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            MyInfiniteCreation.LOGGER.error("Failed to load research stage unlock file {}", location, exception);
        }
    }
}
