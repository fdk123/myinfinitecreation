package fdk123.myinfinitecreation;

import fdk123.myinfinitecreation.command.MicCommands;
import fdk123.myinfinitecreation.network.MicNetwork;
import fdk123.myinfinitecreation.progression.ProgressionGateService;
import fdk123.myinfinitecreation.recipe.RecipePolicyService;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MyInfiniteCreation.MOD_ID)
public class MyInfiniteCreation {
    public static final String MOD_ID = "myinfinitecreation";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ProgressionGateService PROGRESSION_GATES = new ProgressionGateService();

    private final RecipePolicyService recipePolicyService = new RecipePolicyService();
    private final MicCommands commands = new MicCommands(recipePolicyService);
    private int researchSyncTick = 0;

    public MyInfiniteCreation() {
        MicNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        recipePolicyService.apply(event.getServer());
        PROGRESSION_GATES.load(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        commands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            MicNetwork.syncRecipeGates(player, PROGRESSION_GATES);
            MicNetwork.syncStage(player);
            MicNetwork.syncMineColoniesResearch(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++researchSyncTick < 100) {
            return;
        }
        researchSyncTick = 0;
        for (net.minecraft.server.level.ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            MicNetwork.syncMineColoniesResearch(player);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        recipePolicyService.clear();
        PROGRESSION_GATES.clear();
    }
}
