package fdk123.myinfinitecreation;

import fdk123.myinfinitecreation.command.MicCommands;
import fdk123.myinfinitecreation.network.MicNetwork;
import fdk123.myinfinitecreation.progression.ModGateService;
import fdk123.myinfinitecreation.progression.ProgressionGateService;
import fdk123.myinfinitecreation.progression.ResearchStageUnlockService;
import fdk123.myinfinitecreation.recipe.ModRecipeSerializers;
import fdk123.myinfinitecreation.recipe.RecipePolicyService;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MyInfiniteCreation.MOD_ID)
public class MyInfiniteCreation {
    public static final String MOD_ID = "myinfinitecreation";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ProgressionGateService PROGRESSION_GATES = new ProgressionGateService();
    public static final ModGateService MOD_GATES = new ModGateService();
    public static final ResearchStageUnlockService RESEARCH_STAGE_UNLOCKS = new ResearchStageUnlockService();

    private final RecipePolicyService recipePolicyService = new RecipePolicyService();
    private final MicCommands commands = new MicCommands(recipePolicyService);
    private int researchSyncTick = 0;

    public MyInfiniteCreation() {
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MicNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        recipePolicyService.apply(event.getServer());
        loadDatapackBackedServices(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        commands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            if (RESEARCH_STAGE_UNLOCKS.apply(player)) {
                MicNetwork.syncStage(player.server);
            }
            syncProgressionData(player);
        }
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            recipePolicyService.reload(event.getPlayerList().getServer());
            loadDatapackBackedServices(event.getPlayerList().getServer());
        }
        event.getPlayers().forEach(this::syncProgressionData);
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!MOD_GATES.mayUseItem(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!MOD_GATES.mayUseItem(event.getEntity(), event.getItemStack())
                || !MOD_GATES.mayUseBlock(event.getEntity(), event.getLevel().getBlockState(event.getPos()))) {
            event.setCanceled(true);
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && !MOD_GATES.mayPlaceBlock(player, event.getPlacedBlock())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!MOD_GATES.mayBreakBlock(event.getPlayer(), event.getState())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++researchSyncTick < 100) {
            return;
        }
        researchSyncTick = 0;
        for (net.minecraft.server.level.ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (RESEARCH_STAGE_UNLOCKS.apply(player)) {
                MicNetwork.syncStage(player.server);
            }
            MicNetwork.syncMineColoniesResearch(player);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        recipePolicyService.clear();
        PROGRESSION_GATES.clear();
        MOD_GATES.clear();
        RESEARCH_STAGE_UNLOCKS.clear();
    }

    private void loadDatapackBackedServices(net.minecraft.server.MinecraftServer server) {
        PROGRESSION_GATES.load(server);
        MOD_GATES.load(server);
        RESEARCH_STAGE_UNLOCKS.load(server);
    }

    private void syncProgressionData(net.minecraft.server.level.ServerPlayer player) {
        MicNetwork.syncRecipeGates(player, PROGRESSION_GATES);
        MicNetwork.syncModGates(player, MOD_GATES);
        MicNetwork.syncStage(player);
        MicNetwork.syncMineColoniesResearch(player);
    }
}
