package thc.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import thc.buckler.BucklerState;
import thc.item.THCBucklers;

public class THCBucklerGameTests {
	@GameTest
	public void bucklerParryPreventsDamage(GameTestHelper helper) {
		ServerPlayer player = spawnPlayerWithBuckler(helper);
		DamageSources sources = helper.getLevel().damageSources();
		Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 3);

		BucklerState.setPoise(player, 1.0D);
		BucklerState.setMaxPoise(player, 4.0D);
		BucklerState.setPoise(player, 2.0D);
		BucklerState.setMaxPoise(player, 4.0D);
		player.startUsingItem(InteractionHand.OFF_HAND);
		BucklerState.setRaiseTick(player, helper.getLevel().getGameTime());

		float before = player.getHealth();
		helper.hurt(player, sources.mobAttack(attacker), 4.0F);

		float after = player.getHealth();
		double poiseAfter = BucklerState.getPoise(player);
		helper.assertTrue(after == before, "Parry should prevent health loss. before=" + before + " after=" + after);
		helper.assertTrue(poiseAfter > 0.0D, "Parry should grant poise. poise=" + poiseAfter);
		helper.succeed();
	}

	@GameTest(manualOnly = true, required = false)
	public void bucklerDoesNotBlockEnvironmentalDamage(GameTestHelper helper) {
		ServerPlayer player = spawnPlayerWithBuckler(helper);
		DamageSources sources = helper.getLevel().damageSources();

		player.startUsingItem(InteractionHand.OFF_HAND);
		BucklerState.setRaiseTick(player, helper.getLevel().getGameTime());

		float before = player.getHealth();
		helper.hurt(player, sources.fall(), 4.0F);

		float after = player.getHealth();
		helper.assertTrue(after == before - 4.0F, "Fall damage should ignore bucklers. before=" + before + " after=" + after);
		helper.succeed();
	}

	@GameTest(manualOnly = true, required = false)
	public void bucklerBreaksWhenPoiseDepletes(GameTestHelper helper) {
		ServerPlayer player = spawnPlayerWithBuckler(helper);
		DamageSources sources = helper.getLevel().damageSources();
		Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 3);

		BucklerState.setPoise(player, 1.0D);
		BucklerState.setMaxPoise(player, 4.0D);
		player.startUsingItem(InteractionHand.OFF_HAND);
		BucklerState.setRaiseTick(player, helper.getLevel().getGameTime() - 100L);

		helper.hurt(player, sources.mobAttack(attacker), 4.0F);

		boolean broken = BucklerState.isBroken(player);
		double poiseAfter = BucklerState.getPoise(player);
		helper.assertTrue(broken, "Poise depletion should break the buckler. poise=" + poiseAfter + " broken=" + broken);
		helper.succeed();
	}

	@GameTest(manualOnly = true, required = false)
	public void bucklerLethalParryLeavesHalfHeart(GameTestHelper helper) {
		ServerPlayer player = spawnPlayerWithBuckler(helper);
		DamageSources sources = helper.getLevel().damageSources();
		Mob attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 3);

		player.setHealth(4.0F);
		BucklerState.setPoise(player, 1.0D);
		BucklerState.setMaxPoise(player, 4.0D);
		player.startUsingItem(InteractionHand.OFF_HAND);
		BucklerState.setRaiseTick(player, helper.getLevel().getGameTime());

		helper.hurt(player, sources.mobAttack(attacker), 20.0F);

		float after = player.getHealth();
		boolean offhandEmpty = player.getOffhandItem().isEmpty();
		helper.assertTrue(after == 1.0F, "Lethal parry should leave half a heart. health=" + after);
		helper.assertTrue(offhandEmpty, "Buckler should break on lethal parry. offhandEmpty=" + offhandEmpty);
		helper.succeed();
	}

	private static ServerPlayer spawnPlayerWithBuckler(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
		player.teleportTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		player.setYRot(0.0F);
		player.setYHeadRot(0.0F);
		player.setXRot(0.0F);
		player.setGameMode(GameType.SURVIVAL);
		player.getAbilities().invulnerable = false;
		player.getAbilities().instabuild = false;
		player.getAbilities().flying = false;
		player.onUpdateAbilities();
		player.setInvulnerable(false);
		player.setHealth(20.0F);

		ItemStack buckler = new ItemStack(THCBucklers.STONE_BUCKLER);
		player.setItemSlot(EquipmentSlot.OFFHAND, buckler);
		return player;
	}
}
