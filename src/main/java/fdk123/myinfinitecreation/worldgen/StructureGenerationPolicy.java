package fdk123.myinfinitecreation.worldgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class StructureGenerationPolicy {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("myinfinitecreation-structures.json");
    private static volatile Map<ResourceLocation, Double> chances = Map.of();

    private StructureGenerationPolicy() {
    }

    public static void load() {
        try {
            if (Files.notExists(FILE)) {
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE, "{\n  \"structures\": {}\n}\n", StandardCharsets.UTF_8);
            }
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject entries = root.getAsJsonObject("structures");
                Map<ResourceLocation, Double> loaded = new HashMap<>();
                if (entries != null) {
                    for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
                        ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                        if (id == null || !entry.getValue().isJsonPrimitive()
                                || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                            MyInfiniteCreation.LOGGER.warn("Invalid structure generation rule '{}' in {}", entry.getKey(), FILE);
                            continue;
                        }
                        double chance = entry.getValue().getAsDouble();
                        if (!Double.isFinite(chance) || chance < 0.0 || chance > 1.0) {
                            MyInfiniteCreation.LOGGER.warn("Structure generation chance for {} must be between 0 and 1", id);
                            continue;
                        }
                        loaded.put(id, chance);
                    }
                }
                chances = Map.copyOf(loaded);
                MyInfiniteCreation.LOGGER.info("Loaded {} structure generation rule(s) from {}", loaded.size(), FILE);
            }
        } catch (IOException | RuntimeException exception) {
            MyInfiniteCreation.LOGGER.error("Failed to load structure generation rules from {}", FILE, exception);
        }
    }

    public static void clear() {
        chances = Map.of();
    }

    public static boolean allows(ResourceLocation id, long seed, int chunkX, int chunkZ) {
        Double chance = chances.get(id);
        if (chance == null || chance >= 1.0) {
            return true;
        }
        if (chance <= 0.0) {
            return false;
        }
        long hash = seed ^ ((long) chunkX * 0x9E3779B97F4A7C15L)
                ^ ((long) chunkZ * 0xC2B2AE3D27D4EB4FL) ^ id.toString().hashCode();
        hash = (hash ^ (hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ (hash >>> 27)) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return (hash >>> 11) * 0x1.0p-53 < chance;
    }
}
