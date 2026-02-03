package thc.playerclass;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import thc.claim.ClaimManager;

public final class SelectClassCommand {
	private SelectClassCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("selectClass")
				.then(Commands.argument("class", StringArgumentType.string())
					.suggests((context, builder) -> {
						builder.suggest("bastion");
						builder.suggest("melee");
						builder.suggest("ranged");
						builder.suggest("support");
						return builder.buildFuture();
					})
					.executes(SelectClassCommand::execute)));
		});
	}

	private static int execute(CommandContext<CommandSourceStack> context) {
		// Get player (command requires player source)
		Entity entity = context.getSource().getEntity();
		if (!(entity instanceof ServerPlayer player)) {
			return 0;
		}

		String className = StringArgumentType.getString(context, "class");

		// Validate class name
		PlayerClass playerClass = PlayerClass.fromString(className);
		if (playerClass == null) {
			// Actionbar: invalid class
			player.displayClientMessage(
				Component.literal("Invalid class: " + className).withStyle(ChatFormatting.RED),
				true  // actionbar
			);
			return 0;
		}

		// Check if already has class
		if (ClassManager.hasClass(player)) {
			// Actionbar: already has class
			player.displayClientMessage(
				Component.literal("You already have a class!").withStyle(ChatFormatting.RED),
				true
			);
			return 0;
		}

		// Check if in base
		MinecraftServer server = player.level().getServer();
		if (server == null || !ClaimManager.INSTANCE.isInBase(server, player.blockPosition())) {
			// Actionbar: not in base
			player.displayClientMessage(
				Component.literal("You must be in a base to select a class!").withStyle(ChatFormatting.RED),
				true
			);
			return 0;
		}

		// Set class
		ClassManager.setClass(player, playerClass);

		// Success: Title + chat
		player.sendSystemMessage(
			Component.literal("You are now a " + playerClass.name().toLowerCase() + "!")
				.withStyle(ChatFormatting.GREEN)
		);

		// Title announcement
		player.connection.send(new ClientboundSetTitleTextPacket(
			Component.literal(playerClass.name()).withStyle(ChatFormatting.GOLD)
		));
		player.connection.send(new ClientboundSetSubtitleTextPacket(
			Component.literal("Class Selected").withStyle(ChatFormatting.YELLOW)
		));
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 10)); // 0.5s fade in, 3s stay, 0.5s fade out

		return 1;
	}
}
