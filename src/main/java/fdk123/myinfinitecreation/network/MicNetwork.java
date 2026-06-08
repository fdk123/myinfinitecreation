package fdk123.myinfinitecreation.network;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import fdk123.myinfinitecreation.progression.MineColoniesResearchAccess;
import fdk123.myinfinitecreation.progression.ProgressionGateService;
import fdk123.myinfinitecreation.recipe.RecipeStageState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class MicNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static int nextPacketId = 0;
    private static final MineColoniesResearchAccess MINECOLONIES_RESEARCH = new MineColoniesResearchAccess();
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MyInfiniteCreation.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.registerMessage(
                nextPacketId++,
                StageSyncPacket.class,
                StageSyncPacket::encode,
                StageSyncPacket::decode,
                StageSyncPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                RecipeGatesSyncPacket.class,
                RecipeGatesSyncPacket::encode,
                RecipeGatesSyncPacket::decode,
                RecipeGatesSyncPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                MineColoniesResearchSyncPacket.class,
                MineColoniesResearchSyncPacket::encode,
                MineColoniesResearchSyncPacket::decode,
                MineColoniesResearchSyncPacket::handle
        );
    }

    public static void syncStage(ServerPlayer player) {
        String activeStage = RecipeStageState.get(player.server).getActiveStage();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StageSyncPacket(activeStage));
    }

    public static void syncStage(MinecraftServer server) {
        String activeStage = RecipeStageState.get(server).getActiveStage();
        CHANNEL.send(PacketDistributor.ALL.noArg(), new StageSyncPacket(activeStage));
    }

    public static void syncRecipeGates(ServerPlayer player, ProgressionGateService progressionGateService) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RecipeGatesSyncPacket(progressionGateService.rules()));
    }

    public static void syncMineColoniesResearch(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new MineColoniesResearchSyncPacket(MINECOLONIES_RESEARCH.completedResearches(player))
        );
    }
}
