package fdk123.myinfinitecreation.network;

import fdk123.myinfinitecreation.client.ClientProgressionHooks;
import fdk123.myinfinitecreation.progression.ProgressionGateRule;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public record RecipeGatesSyncPacket(List<ProgressionGateRule> rules) {
    public static void encode(RecipeGatesSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.rules.size());
        for (ProgressionGateRule rule : packet.rules) {
            buffer.writeUtf(rule.name);
            writeStrings(buffer, rule.requiredStages);
            writeResourceLocations(buffer, rule.requiredResearches);
            writeResourceLocations(buffer, rule.types);
            writeResourceLocations(buffer, rule.outputs);
            writeResourceLocations(buffer, rule.outputTags);
        }
    }

    public static RecipeGatesSyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ProgressionGateRule> rules = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ProgressionGateRule rule = new ProgressionGateRule();
            rule.name = buffer.readUtf();
            rule.requiredStages.addAll(readStrings(buffer));
            rule.requiredResearches.addAll(readResourceLocations(buffer));
            rule.types.addAll(readResourceLocations(buffer));
            rule.outputs.addAll(readResourceLocations(buffer));
            rule.outputTags.addAll(readResourceLocations(buffer));
            rules.add(rule);
        }
        return new RecipeGatesSyncPacket(rules);
    }

    public static void handle(RecipeGatesSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientProgressionHooks.handleRecipeGatesSync(packet.rules)
        ));
        context.setPacketHandled(true);
    }

    private static void writeStrings(FriendlyByteBuf buffer, Set<String> values) {
        buffer.writeVarInt(values.size());
        for (String value : values) {
            buffer.writeUtf(value);
        }
    }

    private static Set<String> readStrings(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Set<String> values = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) {
            values.add(buffer.readUtf());
        }
        return values;
    }

    private static void writeResourceLocations(FriendlyByteBuf buffer, Set<ResourceLocation> values) {
        buffer.writeVarInt(values.size());
        for (ResourceLocation value : values) {
            buffer.writeResourceLocation(value);
        }
    }

    private static Set<ResourceLocation> readResourceLocations(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Set<ResourceLocation> values = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) {
            values.add(buffer.readResourceLocation());
        }
        return values;
    }
}
