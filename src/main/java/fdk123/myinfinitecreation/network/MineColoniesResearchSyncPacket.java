package fdk123.myinfinitecreation.network;

import fdk123.myinfinitecreation.client.ClientProgressionHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

public record MineColoniesResearchSyncPacket(Set<ResourceLocation> completedResearches) {
    public static void encode(MineColoniesResearchSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.completedResearches.size());
        for (ResourceLocation research : packet.completedResearches) {
            buffer.writeResourceLocation(research);
        }
    }

    public static MineColoniesResearchSyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Set<ResourceLocation> completedResearches = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) {
            completedResearches.add(buffer.readResourceLocation());
        }
        return new MineColoniesResearchSyncPacket(completedResearches);
    }

    public static void handle(MineColoniesResearchSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientProgressionHooks.handleMineColoniesResearchSync(packet.completedResearches)
        ));
        context.setPacketHandled(true);
    }
}
