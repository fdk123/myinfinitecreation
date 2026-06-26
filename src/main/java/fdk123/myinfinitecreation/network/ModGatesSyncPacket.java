package fdk123.myinfinitecreation.network;

import fdk123.myinfinitecreation.client.ClientProgressionHooks;
import fdk123.myinfinitecreation.progression.ModGateRule;
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

public record ModGatesSyncPacket(List<ModGateRule> rules) {
    public static void encode(ModGatesSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.rules.size());
        for (ModGateRule rule : packet.rules) {
            buffer.writeUtf(rule.name);
            buffer.writeUtf(rule.mode);
            writeStrings(buffer, rule.requiredStages);
            writeResourceLocations(buffer, rule.requiredResearches);
            writeStrings(buffer, rule.modids);
            writeResourceLocations(buffer, rule.items);
            writeStrings(buffer, rule.itemPatterns);
            writeResourceLocations(buffer, rule.itemTags);
            writeResourceLocations(buffer, rule.blocks);
            writeStrings(buffer, rule.blockPatterns);
            writeResourceLocations(buffer, rule.blockTags);
            writeResourceLocations(buffer, rule.exceptItems);
            writeResourceLocations(buffer, rule.exceptBlocks);
            buffer.writeBoolean(rule.hideInJei);
            buffer.writeBoolean(rule.maskName);
            buffer.writeBoolean(rule.allowPickup);
            buffer.writeBoolean(rule.preventUse);
            buffer.writeBoolean(rule.preventPlace);
            buffer.writeBoolean(rule.preventBreakBlocks);
        }
    }

    public static ModGatesSyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ModGateRule> rules = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ModGateRule rule = new ModGateRule();
            rule.name = buffer.readUtf();
            rule.mode = buffer.readUtf();
            rule.requiredStages.addAll(readStrings(buffer));
            rule.requiredResearches.addAll(readResourceLocations(buffer));
            rule.modids.addAll(readStrings(buffer));
            rule.items.addAll(readResourceLocations(buffer));
            rule.itemPatterns.addAll(readStrings(buffer));
            rule.itemTags.addAll(readResourceLocations(buffer));
            rule.blocks.addAll(readResourceLocations(buffer));
            rule.blockPatterns.addAll(readStrings(buffer));
            rule.blockTags.addAll(readResourceLocations(buffer));
            rule.exceptItems.addAll(readResourceLocations(buffer));
            rule.exceptBlocks.addAll(readResourceLocations(buffer));
            rule.hideInJei = buffer.readBoolean();
            rule.maskName = buffer.readBoolean();
            rule.allowPickup = buffer.readBoolean();
            rule.preventUse = buffer.readBoolean();
            rule.preventPlace = buffer.readBoolean();
            rule.preventBreakBlocks = buffer.readBoolean();
            rules.add(rule);
        }
        return new ModGatesSyncPacket(rules);
    }

    public static void handle(ModGatesSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientProgressionHooks.handleModGatesSync(packet.rules)
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
