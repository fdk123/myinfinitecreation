package fdk123.myinfinitecreation.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import fdk123.myinfinitecreation.MyInfiniteCreation;
import fdk123.myinfinitecreation.network.MicNetwork;
import fdk123.myinfinitecreation.progression.MineColoniesResearchAccess;
import fdk123.myinfinitecreation.progression.StageAccess;
import fdk123.myinfinitecreation.recipe.RecipePolicyService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Set;

public class MicCommands {
    private final RecipePolicyService recipePolicyService;
    private final StageAccess stageAccess = new StageAccess();
    private final MineColoniesResearchAccess mineColoniesResearchAccess = new MineColoniesResearchAccess();

    public MicCommands(RecipePolicyService recipePolicyService) {
        this.recipePolicyService = recipePolicyService;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mic")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("stage")
                        .then(Commands.literal("get")
                                .executes(context -> listPlayerStages(context.getSource())))
                        .then(Commands.literal("list")
                                .executes(context -> listPlayerStages(context.getSource())))
                        .then(Commands.literal("check")
                                .then(Commands.argument("stage", StringArgumentType.word())
                                        .executes(context -> checkPlayerStage(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "stage")
                                        ))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("stage", StringArgumentType.word())
                                        .executes(context -> addPlayerStage(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "stage")
                                        ))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("stage", StringArgumentType.word())
                                        .executes(context -> removePlayerStage(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "stage")
                                        ))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("stage", StringArgumentType.word())
                                        .executes(context -> addPlayerStage(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "stage")
                                        )))))
                .then(Commands.literal("recipe-stage")
                        .then(Commands.literal("get")
                                .executes(context -> getRecipeStage(context.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.argument("stage", StringArgumentType.word())
                                        .executes(context -> setRecipeStage(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "stage")
                                        )))))
                .then(Commands.literal("research")
                        .then(Commands.literal("list")
                                .executes(context -> listResearches(context.getSource()))))
                .then(Commands.literal("research-stage")
                        .then(Commands.literal("status")
                                .executes(context -> researchStageStatus(context.getSource())))
                        .then(Commands.literal("apply")
                                .executes(context -> applyResearchStages(context.getSource()))))
                .then(Commands.literal("recipes")
                        .then(Commands.literal("reapply")
                                .executes(context -> reapplyRecipes(context.getSource())))));
    }

    private int listPlayerStages(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Collection<String> stages = stageAccess.stages(player);
        String text = stages.isEmpty() ? "<none>" : String.join(", ", stages);
        source.sendSuccess(
                () -> Component.literal("MIC progression stages via " + stageAccess.primaryBackend() + ": " + text),
                false
        );
        return 1;
    }

    private int checkPlayerStage(CommandSourceStack source, String stage) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean hasStage = stageAccess.hasStage(player, stage);
        source.sendSuccess(
                () -> Component.literal("MIC progression stage '" + stage + "': " + (hasStage ? "present" : "missing")),
                false
        );
        return hasStage ? 1 : 0;
    }

    private int addPlayerStage(CommandSourceStack source, String stage) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!stageAccess.addStage(player, stage)) {
            source.sendFailure(Component.literal("No writable stage backend found. Install TeamStages or GameStages."));
            return 0;
        }
        MicNetwork.syncStage(player.server);
        source.sendSuccess(
                () -> Component.literal("Added MIC progression stage via " + stageAccess.primaryBackend() + ": " + stage),
                true
        );
        return 1;
    }

    private int removePlayerStage(CommandSourceStack source, String stage) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!stageAccess.removeStage(player, stage)) {
            source.sendFailure(Component.literal("No writable stage backend found. Install TeamStages or GameStages."));
            return 0;
        }
        MicNetwork.syncStage(player.server);
        source.sendSuccess(
                () -> Component.literal("Removed MIC progression stage via " + stageAccess.primaryBackend() + ": " + stage),
                true
        );
        return 1;
    }

    private int getRecipeStage(CommandSourceStack source) {
        String activeStage = recipePolicyService.activeStage(source.getServer());
        source.sendSuccess(() -> Component.literal("Active MIC global recipe stage: " + activeStage), false);
        return 1;
    }

    private int setRecipeStage(CommandSourceStack source, String stage) {
        MinecraftServer server = source.getServer();
        recipePolicyService.setStage(server, stage);
        MicNetwork.syncStage(server);
        source.sendSuccess(() -> Component.literal("Set MIC global recipe stage to: " + stage), true);
        return 1;
    }

    private int listResearches(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Set<net.minecraft.resources.ResourceLocation> researches = mineColoniesResearchAccess.completedResearches(player);
        String text = researches.isEmpty() ? "<none>" : String.join(", ", researches.stream().map(Object::toString).toList());
        source.sendSuccess(() -> Component.literal("MIC visible MineColonies researches: " + text), false);
        return 1;
    }

    private int researchStageStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var lines = MyInfiniteCreation.RESEARCH_STAGE_UNLOCKS.status(player);
        if (lines.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No MIC research stage unlock rules loaded"), false);
            return 1;
        }
        for (String line : lines) {
            source.sendSuccess(() -> Component.literal("MIC research stage: " + line), false);
        }
        return lines.size();
    }

    private int applyResearchStages(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int unlocked = MyInfiniteCreation.RESEARCH_STAGE_UNLOCKS.applyAndCount(player);
        if (unlocked > 0) {
            MicNetwork.syncStage(player.server);
        }
        source.sendSuccess(() -> Component.literal("Applied MIC research stage unlocks: " + unlocked), true);
        return Math.max(1, unlocked);
    }

    private int reapplyRecipes(CommandSourceStack source) {
        recipePolicyService.reapply(source.getServer());
        source.sendSuccess(() -> Component.literal("Reapplied MIC recipe policies"), true);
        return 1;
    }
}
