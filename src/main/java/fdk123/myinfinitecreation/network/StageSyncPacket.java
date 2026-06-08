package fdk123.myinfinitecreation.network;

import fdk123.myinfinitecreation.client.ClientProgressionHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StageSyncPacket(String stage) {
    public static void encode(StageSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.stage);
    }

    public static StageSyncPacket decode(FriendlyByteBuf buffer) {
        return new StageSyncPacket(buffer.readUtf());
    }

    public static void handle(StageSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientProgressionHooks.handleStageSync(packet.stage)
        ));
        context.setPacketHandled(true);
    }
}
