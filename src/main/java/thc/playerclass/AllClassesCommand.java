package thc.playerclass;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import com.mojang.brigadier.context.CommandContext;

public final class AllClassesCommand {
	private AllClassesCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("allClasses")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.executes(AllClassesCommand::execute));
		});
	}

	private static int execute(CommandContext<CommandSourceStack> context) {
		Entity entity = context.getSource().getEntity();
		if (!(entity instanceof ServerPlayer player)) {
			return 0;
		}

		boolean newState = !ClassManager.isAllClassesEnabled();
		ClassManager.setAllClasses(newState);

		if (newState) {
			player.displayClientMessage(
				Component.literal("All classes: ENABLED").withStyle(ChatFormatting.GREEN),
				true
			);
		} else {
			player.displayClientMessage(
				Component.literal("All classes: DISABLED").withStyle(ChatFormatting.RED),
				true
			);
		}

		return 1;
	}
}
