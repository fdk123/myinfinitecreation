package fdk123.myinfinitecreation.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Removes configured books from a new player's inventory immediately after login. */
public final class StartupBookService {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("myinfinitecreation-starting-books.json");
    private static final String CHECKED_TAG = "myinfinitecreation_startup_books_checked";
    private static final String OLD_EMPTY_CONFIG = "{\n  \"window_ticks\": 100,\n  \"books\": []\n}";
    private static final String DEFAULT_CONFIG = """
            {
              "window_ticks": 200,
              "books": [
                { "item": "theoneprobe:probenote" },
                { "item": "prehistoricfauna:ancient_journal" },
                { "item": "tconstruct:materials_and_you" },
                { "item": "ars_nouveau:worn_notebook" },
                { "item": "patchouli:guide_book", "patchouli_book": "apotheosis:apoth_chronicle" },
                { "item": "patchouli:guide_book", "patchouli_book": "divinerpg:divine" }
              ]
            }
            """;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private List<BookRule> rules = List.of();
    private int windowTicks = 200;

    public void load() {
        sessions.clear();
        try {
            if (Files.notExists(FILE)) {
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE, DEFAULT_CONFIG, StandardCharsets.UTF_8);
            } else if (Files.readString(FILE, StandardCharsets.UTF_8).replace("\r\n", "\n").trim()
                    .equals(OLD_EMPTY_CONFIG)) {
                Files.writeString(FILE, DEFAULT_CONFIG, StandardCharsets.UTF_8);
                MyInfiniteCreation.LOGGER.info("Updated the original empty startup book config with the selected books");
            }
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int ticks = root.has("window_ticks") ? root.get("window_ticks").getAsInt() : 200;
                if (ticks < 1 || ticks > 1200) {
                    throw new IllegalArgumentException("window_ticks must be between 1 and 1200");
                }
                List<BookRule> loaded = new ArrayList<>();
                JsonArray books = root.getAsJsonArray("books");
                if (books != null) {
                    for (JsonElement element : books) {
                        try {
                            JsonObject book = element.getAsJsonObject();
                            ResourceLocation item = ResourceLocation.tryParse(book.get("item").getAsString());
                            ResourceLocation patchouliBook = book.has("patchouli_book")
                                    ? ResourceLocation.tryParse(book.get("patchouli_book").getAsString()) : null;
                            if (item == null || (book.has("patchouli_book") && patchouliBook == null)) {
                                throw new IllegalArgumentException("Invalid item or Patchouli book ID");
                            }
                            loaded.add(new BookRule(item, patchouliBook));
                        } catch (RuntimeException exception) {
                            MyInfiniteCreation.LOGGER.warn("Skipping invalid startup book rule in {}: {}", FILE, element);
                        }
                    }
                }
                rules = List.copyOf(loaded);
                windowTicks = ticks;
                MyInfiniteCreation.LOGGER.info("Loaded {} startup book rule(s) from {}", rules.size(), FILE);
            }
        } catch (Exception exception) {
            MyInfiniteCreation.LOGGER.error("Failed to load startup book rules from {}", FILE, exception);
        }
    }

    public void onLogin(ServerPlayer player) {
        if (rules.isEmpty()) {
            return;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(CHECKED_TAG)) {
            return;
        }
        persisted.putBoolean(CHECKED_TAG, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        if (player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) > 0) {
            return;
        }
        sessions.put(player.getUUID(), new Session(windowTicks));
        filter(player);
    }

    public void onTick(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            Session session = sessions.get(player.getUUID());
            if (session != null) {
                filter(player);
                if (--session.ticksLeft <= 0) {
                    sessions.remove(player.getUUID());
                }
            }
        }
    }

    public void onLogout(ServerPlayer player) {
        sessions.remove(player.getUUID());
    }

    public void clear() {
        sessions.clear();
        rules = List.of();
    }

    private void filter(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        boolean changed = false;
        for (BookRule rule : rules) {
            for (int slot = inventory.getContainerSize() - 1; slot >= 0; slot--) {
                ItemStack stack = inventory.getItem(slot);
                if (rule.matches(stack)) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                    changed = true;
                }
            }
        }
        if (changed) {
            inventory.setChanged();
            player.inventoryMenu.broadcastChanges();
        }
    }

    private record BookRule(ResourceLocation item, ResourceLocation patchouliBook) {
        boolean matches(ItemStack stack) {
            if (stack.isEmpty() || !item.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) {
                return false;
            }
            return patchouliBook == null || (stack.getTag() != null
                    && patchouliBook.toString().equals(stack.getTag().getString("patchouli:book")));
        }
    }

    private static final class Session {
        private int ticksLeft;

        private Session(int ticksLeft) {
            this.ticksLeft = ticksLeft;
        }
    }
}
