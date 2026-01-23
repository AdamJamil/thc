package thc.stage;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;

/**
 * Operator-only command to advance the server to the next stage.
 * Requires permission level 2 (operator).
 */
public final class AdvanceStageCommand {
	private AdvanceStageCommand() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("advanceStage")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.executes(AdvanceStageCommand::execute));
		});
	}

	private static int execute(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();

		int currentStage = StageManager.getCurrentStage(server);
		if (currentStage >= 5) {
			source.sendFailure(
				Component.literal("Already at maximum stage (5)!").withStyle(ChatFormatting.RED)
			);
			return 0;
		}

		if (StageManager.advanceStage(server)) {
			source.sendSuccess(
				() -> Component.literal("Advanced to Stage " + StageManager.getCurrentStage(server))
					.withStyle(ChatFormatting.GREEN),
				true // broadcast to ops
			);
			return 1;
		}

		// Should not reach here, but handle gracefully
		source.sendFailure(
			Component.literal("Failed to advance stage").withStyle(ChatFormatting.RED)
		);
		return 0;
	}
}
