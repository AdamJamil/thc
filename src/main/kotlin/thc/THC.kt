package thc

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.GameType
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.animal.cow.Cow
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.gamerules.GameRules
import org.slf4j.LoggerFactory
import thc.base.BasePermissions
import thc.bell.BellHandler
import thc.buckler.BucklerState
import thc.buckler.BucklerStatsRegistry
import thc.item.BucklerItem
import thc.item.THCArrows
import thc.item.THCBucklers
import thc.item.THCItems
import thc.network.BucklerSync
import thc.network.BucklerStatePayload
import thc.network.RevivalStatePayload
import thc.network.RevivalSync
import thc.armor.ArmorRebalancing
import thc.food.FoodStatsModifier
import thc.monster.DamageRebalancing
import thc.monster.GhastModifications
import thc.monster.MonsterModifications
import thc.monster.SimpleEntityBehaviors
import thc.playerclass.ClassManager
import thc.playerclass.SelectClassCommand
import thc.stage.AdvanceStageCommand
import thc.stage.StageManager
import thc.world.MiningFatigue
import thc.world.VillageProtection
import thc.world.WorldRestrictions
import thc.enchant.EnchantmentEnforcement
import thc.lectern.LecternEnchanting
import thc.downed.DownedManager
import thc.downed.DownedState
import thc.downed.RevivalState
import thc.playerclass.PlayerClass
import thc.villager.JobBlockAssignment
import thc.villager.VillagerInteraction

object THC : ModInitializer {
	private val logger = LoggerFactory.getLogger("thc")
	private const val SMOKE_TEST_PROPERTY = "thc.smokeTest"
	private const val SMOKE_TEST_TICKS = 100

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		logger.info("Hello Fabric world!")
		THCAttachments.init()
		THCArrows.init()
		THCBucklers.init()
		thc.entity.THCEntities.init()
		THCItems.init()
		THCSounds.init()
		BellHandler.register()
		LecternEnchanting.register()
		BasePermissions.register()
		WorldRestrictions.register()
		VillageProtection.register()
		MiningFatigue.register()
		ArmorRebalancing.register()
		FoodStatsModifier.register()
		SelectClassCommand.register()
		AdvanceStageCommand.register()
		MonsterModifications.register()
		GhastModifications.register()
		SimpleEntityBehaviors.register()
		DamageRebalancing.register()
		VillagerInteraction.register()
		JobBlockAssignment.register()
		DownedManager.register()
		PayloadTypeRegistry.playS2C().register(BucklerStatePayload.TYPE, BucklerStatePayload.STREAM_CODEC)
		PayloadTypeRegistry.playS2C().register(RevivalStatePayload.TYPE, RevivalStatePayload.STREAM_CODEC)

		// Cow milking with copper bucket
		UseEntityCallback.EVENT.register { player, level, hand, entity, _ ->
			val stack = player.getItemInHand(hand)

			// Check if player is holding empty copper bucket and targeting an adult cow
			if (stack.item == THCItems.COPPER_BUCKET && entity is Cow && !entity.isBaby) {
				if (!level.isClientSide()) {
					// Play milking sound
					player.playSound(SoundEvents.COW_MILK, 1.0f, 1.0f)

					// Replace copper bucket with copper milk bucket
					stack.shrink(1)
					val milkBucket = ItemStack(THCItems.COPPER_BUCKET_OF_MILK)
					if (!player.inventory.add(milkBucket)) {
						player.drop(milkBucket, false)
					}
				}

				return@register InteractionResult.SUCCESS
			}

			InteractionResult.PASS
		}

		ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
			updateBucklerState(server)
			processRevival(server)

			// Sync revival state to all players for HUD rendering
			val players = server.playerList.players
			for (player in players) {
				RevivalSync.sync(player, players)
			}
		})

		ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
			BucklerSync.clear(handler.player)
			RevivalSync.clear(handler.player)
		})

		ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler, sender, server ->
			val player = handler.player

			// New players (no class yet) get boon level matching current stage
			// Returning players (have class) keep their accumulated boon level and restore health
			if (!ClassManager.hasClass(player)) {
				val currentStage = StageManager.getCurrentStage(server)
				StageManager.setBoonLevel(player, currentStage)
			} else {
				// Returning player - restore health modifier from their class
				ClassManager.restoreHealthModifier(player)
			}
		})

		ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEvents.ServerStarted { server ->
			// Set server reference for village deregistration in claimed chunks
			thc.village.ServerHolder.setServer(server)

			server.allLevels.forEach { world ->
				world.gameRules.set(GameRules.MOB_GRIEFING, false, server)
			}
		})

		val removedItems = setOf(
			Items.SHIELD,
			Items.WOODEN_SPEAR,
			Items.STONE_SPEAR,
			Items.COPPER_SPEAR,
			Items.IRON_SPEAR,
			Items.GOLDEN_SPEAR,
			Items.DIAMOND_SPEAR,
			Items.NETHERITE_SPEAR,
			Items.BOW,
			Items.CROSSBOW,
			Items.TOTEM_OF_UNDYING,
			// FR-02: Equipment drops (armor)
			Items.LEATHER_HELMET,
			Items.LEATHER_CHESTPLATE,
			Items.LEATHER_LEGGINGS,
			Items.LEATHER_BOOTS,
			Items.CHAINMAIL_HELMET,
			Items.CHAINMAIL_CHESTPLATE,
			Items.CHAINMAIL_LEGGINGS,
			Items.CHAINMAIL_BOOTS,
			Items.IRON_HELMET,
			Items.IRON_CHESTPLATE,
			Items.IRON_LEGGINGS,
			Items.IRON_BOOTS,
			Items.GOLDEN_HELMET,
			Items.GOLDEN_CHESTPLATE,
			Items.GOLDEN_LEGGINGS,
			Items.GOLDEN_BOOTS,
			Items.DIAMOND_HELMET,
			Items.DIAMOND_CHESTPLATE,
			Items.DIAMOND_LEGGINGS,
			Items.DIAMOND_BOOTS,
			// FR-02: Equipment drops (weapons)
			Items.WOODEN_SWORD,
			Items.STONE_SWORD,
			Items.IRON_SWORD,
			Items.GOLDEN_SWORD,
			Items.DIAMOND_SWORD,
			// FR-05: Iron ingot from zombies/husks
			Items.IRON_INGOT
		)
		LootTableEvents.MODIFY_DROPS.register(LootTableEvents.ModifyDrops { key, _, drops ->
			val hadTotem = drops.any { it.`is`(Items.TOTEM_OF_UNDYING) }
			drops.removeIf { stack -> removedItems.any { stack.`is`(it) } }
			if (hadTotem) {
				drops.add(THCItems.BLAST_TOTEM.defaultInstance)
			}

			// Filter stage 3+ enchanted books and items from chests/fishing only
			// Skip entity loot tables - mob drops are the intended acquisition path
			val keyString = key.toString()
			if (!keyString.contains("entities/")) {
				drops.removeIf { stack ->
					if (stack.`is`(Items.ENCHANTED_BOOK)) {
						val stored = stack.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS)
						EnchantmentEnforcement.hasStage3PlusEnchantment(stored)
					} else {
						// Filter items (armor, weapons) with stage 3+ enchantments
						val enchants = stack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS)
						EnchantmentEnforcement.hasStage3PlusEnchantment(enchants)
					}
				}
			}

			// Enchantment enforcement: strip removed enchants, normalize levels
			drops.forEach { stack ->
				EnchantmentEnforcement.correctStack(stack)
			}
		})

		if (java.lang.Boolean.getBoolean(SMOKE_TEST_PROPERTY)) {
			var tickCount = 0
			var stopRequested = false
			ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
				if (stopRequested) {
					return@EndTick
				}

				tickCount++
				if (tickCount >= SMOKE_TEST_TICKS) {
					stopRequested = true
					logger.info("Smoke test complete, stopping server.")
					server.halt(false)
				}
			})
		}
	}

	private fun updateBucklerState(server: MinecraftServer) {
		for (player in server.playerList.players) {
			val tick = player.level().gameTime
			val isRaised = BucklerItem.isBucklerRaised(player)
			val raiseTick = BucklerState.getRaiseTick(player)
			if (isRaised) {
				if (raiseTick < 0L) {
					BucklerState.setRaiseTick(player, tick)
				}
			} else if (raiseTick >= 0L) {
				BucklerState.setRaiseTick(player, -1L)
			}

			val stats = BucklerStatsRegistry.forStack(player.offhandItem)
			if (stats != null) {
				BucklerState.setMaxPoise(player, stats.maxPoiseHearts)
			}
			val maxPoise = if (stats != null) stats.maxPoiseHearts else BucklerState.getMaxPoise(player)
			if (maxPoise <= 0.0) {
				BucklerSync.sync(player)
				continue
			}

			var poise = BucklerState.getPoise(player)
			if (poise > maxPoise) {
				poise = maxPoise
				BucklerState.setPoise(player, poise)
			}

			if (isRaised) {
				val drainPerTick = 0.5 / 10.0
				if (drainPerTick > 0.0 && poise > 0.0) {
					val updatedPoise = kotlin.math.max(0.0, poise - drainPerTick)
					BucklerState.setPoise(player, updatedPoise)
					if (updatedPoise <= 0.0) {
						BucklerState.setBroken(player, true)
						player.stopUsingItem()
					}
				}
			} else {
				val regenPerTick = maxPoise / 4.0 / 20.0
				if (regenPerTick > 0.0 && poise < maxPoise) {
					val updatedPoise = kotlin.math.min(maxPoise, poise + regenPerTick)
					BucklerState.setPoise(player, updatedPoise)
					if (updatedPoise >= maxPoise) {
						BucklerState.setLastFullTick(player, tick)
						if (BucklerState.isBroken(player)) {
							BucklerState.setBroken(player, false)
						}
					}
				}
			}

			BucklerSync.sync(player)
		}
	}

	private fun processRevival(server: MinecraftServer) {
		val players = server.playerList.players

		// Find all downed players first
		val downedPlayers = players.filter { DownedState.isDowned(it) }
		if (downedPlayers.isEmpty()) return

		// For each alive player who is sneaking, check proximity to downed players
		for (reviver in players) {
			// Must be alive (not downed) and sneaking
			if (DownedState.isDowned(reviver)) continue
			if (!reviver.isShiftKeyDown) continue

			// Determine progress rate based on class
			val playerClass = ClassManager.getClass(reviver)
			val progressRate = if (playerClass == PlayerClass.SUPPORT) {
				1.0 / 100.0  // 100 ticks = 5 seconds to reach 1.0
			} else {
				0.5 / 100.0  // 200 ticks = 10 seconds (0.5 per tick scaled to 0-1 range)
			}

			// Check each downed player
			for (downed in downedPlayers) {
				val downedLoc = DownedState.getDownedLocation(downed) ?: continue

				// 2 block radius = 4.0 squared distance
				val distSq = reviver.position().distanceToSqr(downedLoc)
				if (distSq > 4.0) continue

				// Accumulate progress on the downed player
				RevivalState.addProgress(downed, progressRate)
			}
		}

		// Check for revival completion (separate pass to avoid concurrent modification)
		for (downed in downedPlayers) {
			if (RevivalState.getProgress(downed) >= 1.0) {
				val downedLoc = DownedState.getDownedLocation(downed)
				if (downedLoc != null) {
					completeRevival(downed, downedLoc)
				}
			}
		}
	}

	private fun completeRevival(player: net.minecraft.server.level.ServerPlayer, downedLocation: net.minecraft.world.phys.Vec3) {
		// Clear downed state (also clears revival progress)
		DownedState.clearDowned(player)

		// Restore to survival mode
		player.setGameMode(GameType.SURVIVAL)

		// Teleport to downed location
		player.teleportTo(downedLocation.x, downedLocation.y, downedLocation.z)

		// Set health to 50% of max
		val maxHealth = player.maxHealth
		player.health = maxHealth * 0.5f

		// Set food level to 6 (CONTEXT.md override: not 0)
		player.foodData.foodLevel = 6

		// Spawn green particles (HAPPY_VILLAGER)
		val level = player.level() as ServerLevel
		level.sendParticles(
			ParticleTypes.HAPPY_VILLAGER,
			downedLocation.x,
			downedLocation.y + 1.0,
			downedLocation.z,
			30,           // count
			0.5, 0.5, 0.5, // spread (x, y, z)
			0.0           // speed
		)
	}

}
