package fdk123.myinfinitecreation.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fdk123.myinfinitecreation.network.MicNetwork;
import fdk123.myinfinitecreation.recipe.RecipePolicyService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class MicCommands {
    private final RecipePolicyService recipePolicyService;

    public MicCommands(RecipePolicyService recipePolicyService) {
        this.recipePolicyService = recipePolicyService;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mic")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("stage")
                        .then(Commands.literal("get")
                                .executes(context -> getStage(context.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.argument("stage", StringArgumentType.word())
                                        .executes(context -> setStage(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "stage")
                                        )))))
                .then(Commands.literal("recipes")
                        .then(Commands.literal("reapply")
                                .executes(context -> reapplyRecipes(context.getSource())))));
    }

    private int getStage(CommandSourceStack source) {
        String activeStage = recipePolicyService.activeStage(source.getServer());
        source.sendSuccess(() -> Component.literal("Active MIC recipe stage: " + activeStage), false);
        return 1;
    }

    private int setStage(CommandSourceStack source, String stage) {
        MinecraftServer server = source.getServer();
        recipePolicyService.setStage(server, stage);
        MicNetwork.syncStage(server);
        source.sendSuccess(() -> Component.literal("Set MIC recipe stage to: " + stage), true);
        return 1;
    }

    private int reapplyRecipes(CommandSourceStack source) {
        recipePolicyService.reapply(source.getServer());
        source.sendSuccess(() -> Component.literal("Reapplied MIC recipe policies"), true);
        return 1;
    }
}
