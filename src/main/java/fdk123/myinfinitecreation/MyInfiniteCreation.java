package fdk123.myinfinitecreation;

import fdk123.myinfinitecreation.command.MicCommands;
import fdk123.myinfinitecreation.divinerpg.DivineProcessingItems;
import fdk123.myinfinitecreation.divinerpg.DivineMoltenFluids;
import fdk123.myinfinitecreation.divinerpg.DivineCompatModifiers;
import fdk123.myinfinitecreation.network.MicNetwork;
import fdk123.myinfinitecreation.progression.ModGateService;
import fdk123.myinfinitecreation.progression.ProgressionGateService;
import fdk123.myinfinitecreation.progression.ResearchStageUnlockService;
import fdk123.myinfinitecreation.progression.StartupBookService;
import fdk123.myinfinitecreation.recipe.ModRecipeSerializers;
import fdk123.myinfinitecreation.recipe.RecipePolicyService;
import fdk123.myinfinitecreation.twilight.CastleKeeperTrophy;
import fdk123.myinfinitecreation.reliquified.HydraHeart;
import fdk123.myinfinitecreation.reliquified.HydraFireEntities;
import fdk123.myinfinitecreation.reliquified.InvisibilityCloak;
import fdk123.myinfinitecreation.reliquified.FireflyQueen;
import fdk123.myinfinitecreation.reliquified.TwilightRelicLoot;
import fdk123.myinfinitecreation.reliquified.MapleSyrupBottle;
import fdk123.myinfinitecreation.reliquified.SteelCape;
import fdk123.myinfinitecreation.reliquified.MinotaurHoof;
import fdk123.myinfinitecreation.reliquified.TwilightFeather;
import fdk123.myinfinitecreation.reliquified.GiantGlove;
import fdk123.myinfinitecreation.reliquified.CicadaBottle;
import fdk123.myinfinitecreation.reliquified.ScaledCloak;
import fdk123.myinfinitecreation.reliquified.DeerAntler;
import fdk123.myinfinitecreation.reliquified.Parasite115;
import fdk123.myinfinitecreation.reliquified.BrokenCharm;
import fdk123.myinfinitecreation.reliquified.CharmBackpack;
import fdk123.myinfinitecreation.reliquified.RelicStorageItem;
import fdk123.myinfinitecreation.reliquified.ChromaticCloak;
import fdk123.myinfinitecreation.reliquified.GoblinNose;
import fdk123.myinfinitecreation.reliquified.LichGem;
import fdk123.myinfinitecreation.reliquified.LichCrown;
import fdk123.myinfinitecreation.reliquified.LichCrownEffects;
import fdk123.myinfinitecreation.reliquified.EchoGlove;
import fdk123.myinfinitecreation.reliquified.ThornCrown;
import fdk123.myinfinitecreation.reliquified.CastleKeeperSigil;
import fdk123.myinfinitecreation.reliquified.IronRelics;
import fdk123.myinfinitecreation.worldgen.StructureGenerationPolicy;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

@Mod(MyInfiniteCreation.MOD_ID)
public class MyInfiniteCreation {
    public static final String MOD_ID = "myinfinitecreation";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ProgressionGateService PROGRESSION_GATES = new ProgressionGateService();
    public static final ModGateService MOD_GATES = new ModGateService();
    public static final ResearchStageUnlockService RESEARCH_STAGE_UNLOCKS = new ResearchStageUnlockService();

    private final RecipePolicyService recipePolicyService = new RecipePolicyService();
    private final StartupBookService startupBookService = new StartupBookService();
    private final MicCommands commands = new MicCommands(recipePolicyService);
    private int researchSyncTick = 0;

    public MyInfiniteCreation() {
        HydraFireEntities.register(FMLJavaModLoadingContext.get().getModEventBus());
        HydraHeart.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(HydraHeart.class);
        InvisibilityCloak.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        FireflyQueen.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MapleSyrupBottle.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(MapleSyrupBottle.class);
        SteelCape.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(SteelCape.class);
        MinotaurHoof.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(MinotaurHoof.class);
        TwilightFeather.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(TwilightFeather.class);
        GiantGlove.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(GiantGlove.class);
        CicadaBottle.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        CicadaBottle.EFFECTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(CicadaBottle.class);
        ScaledCloak.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(ScaledCloak.class);
        DeerAntler.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(DeerAntler.class);
        Parasite115.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(Parasite115.class);
        BrokenCharm.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        CharmBackpack.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ChromaticCloak.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        GoblinNose.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(GoblinNose.class);
        LichGem.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        LichCrown.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(LichCrownEffects.class);
        MinecraftForge.EVENT_BUS.register(RelicStorageItem.class);
        MinecraftForge.EVENT_BUS.register(TwilightRelicLoot.class);
        EchoGlove.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(EchoGlove.class);
        ThornCrown.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(ThornCrown.class);
        CastleKeeperSigil.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(CastleKeeperSigil.class);
        IronRelics.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(IronRelics.class);
        CastleKeeperTrophy.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        CastleKeeperTrophy.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CastleKeeperTrophy::addToCreativeTab);
        DivineProcessingItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        DivineMoltenFluids.register(FMLJavaModLoadingContext.get().getModEventBus());
        DivineCompatModifiers.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MicNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        StructureGenerationPolicy.load();
        startupBookService.load();
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
            startupBookService.onLogin(player);
            if (RESEARCH_STAGE_UNLOCKS.apply(player)) {
                MicNetwork.syncStage(player.server);
            }
            syncProgressionData(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            startupBookService.onLogout(player);
        }
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            StructureGenerationPolicy.load();
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
        if (event.phase == TickEvent.Phase.END) {
            startupBookService.onTick(event.getServer().getPlayerList().getPlayers());
        }
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
        startupBookService.clear();
        StructureGenerationPolicy.clear();
        recipePolicyService.clear();
        PROGRESSION_GATES.clear();
        MOD_GATES.clear();
        RESEARCH_STAGE_UNLOCKS.clear();
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) {
            return;
        }
        ModifierId id = new ModifierId(MOD_ID, "divine_intervention");
        ItemStack selected = ItemStack.EMPTY;
        int level = 0;
        for (ItemStack stack : player.getHandSlots()) {
            if (stack.getItem() instanceof IModifiable) {
                ToolStack tool = ToolStack.from(stack);
                int candidate = tool.getModifierLevel(id);
                int candidateUsed = tool.getPersistentData().getInt(new ResourceLocation(MOD_ID, "divine_intervention_uses"));
                if (candidate > candidateUsed && candidate > level) {
                    level = candidate;
                    selected = stack;
                }
            }
        }
        if (level <= 0 || selected.isEmpty()) {
            return;
        }
        ToolStack tool = ToolStack.from(selected);
        ResourceLocation key = new ResourceLocation(MOD_ID, "divine_intervention_uses");
        int used = tool.getPersistentData().getInt(key);
        if (used >= level) {
            return;
        }
        tool.getPersistentData().putInt(key, used + 1);
        player.setHealth(Math.max(1.0f, player.getMaxHealth() * 0.25f));
        player.clearFire();
        event.setCanceled(true);
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
