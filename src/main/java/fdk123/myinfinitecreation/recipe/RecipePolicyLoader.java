package fdk123.myinfinitecreation.recipe;

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

public class RecipePolicyLoader {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "recipe_policies";
    public static final String GLOBAL_STAGE = "global";

    public List<RecipePolicyRule> load(ResourceManager resourceManager, String activeStage) {
        List<RecipePolicyRule> rules = new ArrayList<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                DIRECTORY,
                location -> location.getPath().endsWith(".json")
        );

        resources.forEach((location, resource) -> loadFile(location, resource, activeStage, rules));
        MyInfiniteCreation.LOGGER.info(
                "Loaded {} recipe policy rule(s) for stage '{}' from {} file(s)",
                rules.size(),
                activeStage,
                resources.size()
        );
        return rules;
    }

    private void loadFile(ResourceLocation location, Resource resource, String activeStage, List<RecipePolicyRule> rules) {
        try (BufferedReader reader = resource.openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            RecipePolicyFile file = GSON.fromJson(json, RecipePolicyFile.class);
            String policyStage = policyStage(location, file);
            if (!GLOBAL_STAGE.equals(policyStage) && !activeStage.equals(policyStage)) {
                return;
            }
            if (file.replace) {
                rules.clear();
            }
            if (json.has("rules")) {
                for (var element : json.getAsJsonArray("rules")) {
                    RecipePolicyRule rule = RecipePolicyRule.fromJson(element.getAsJsonObject());
                    if (rule.isEmpty()) {
                        MyInfiniteCreation.LOGGER.warn("Skipping empty recipe policy rule '{}' in {}", rule.name, location);
                    } else {
                        rules.add(rule);
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            MyInfiniteCreation.LOGGER.error("Failed to load recipe policy file {}", location, exception);
        }
    }

    private String policyStage(ResourceLocation location, RecipePolicyFile file) {
        if (file.policyStage != null && !file.policyStage.isBlank()) {
            return file.policyStage;
        }
        if (file.stage != null && !file.stage.isBlank()) {
            return file.stage;
        }

        String prefix = DIRECTORY + "/";
        String path = location.getPath();
        if (!path.startsWith(prefix)) {
            return GLOBAL_STAGE;
        }

        String relativePath = path.substring(prefix.length());
        int slash = relativePath.indexOf('/');
        if (slash < 0) {
            return GLOBAL_STAGE;
        }
        return relativePath.substring(0, slash);
    }
}
