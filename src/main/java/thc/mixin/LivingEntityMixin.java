package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import thc.buckler.BucklerState;
import thc.buckler.BucklerStats;
import thc.buckler.BucklerStatsRegistry;
import thc.item.BucklerItem;
import thc.THCSounds;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Unique
	private static final int THC_LETHAL_PARRY_TICKS = 3;

	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float thc$applyBucklerReduction(float amount, ServerLevel level, DamageSource source, float amountArg) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof ServerPlayer player)) {
			return amount;
		}
		if (amount <= 0.0F) {
			return amount;
		}
		if (!BucklerItem.isBucklerRaised(self)) {
			return amount;
		}
		if (BucklerState.isBroken(player) || BucklerState.getPoise(player) <= 0.0D) {
			return amount;
		}
		if (thc$isEnvironmentalDamage(source, player)) {
			return amount;
		}
		if (!thc$canBucklerBlock(player, source)) {
			return amount;
		}

		BucklerStats stats = BucklerStatsRegistry.INSTANCE.forStack(player.getOffhandItem());
		if (stats == null) {
			return amount;
		}

		long tick = level.getGameTime();
		boolean parry = thc$isParryWindow(player, stats, tick);
		double shieldPowerHearts = stats.getShieldPowerHearts();
		double reductionHearts = shieldPowerHearts * (parry ? 2.5D : 1.0D);
		double reductionDamage = reductionHearts * 2.0D;
		double appliedReduction = Math.min(reductionDamage, (double) amount);
		if (appliedReduction <= 0.0D) {
			return amount;
		}
		float adjustedAmount = (float) Math.max(0.0D, (double) amount - appliedReduction);

		BucklerState.setMaxPoise(player, stats.getMaxPoiseHearts());
		double maxPoise = stats.getMaxPoiseHearts();
		if (parry) {
			double poise = BucklerState.getPoise(player) + shieldPowerHearts;
			if (maxPoise > 0.0D) {
				poise = Math.min(maxPoise, poise);
			}
			BucklerState.setPoise(player, poise);
			if (poise >= maxPoise) {
				BucklerState.setLastFullTick(player, tick);
			}
			BucklerState.setRaiseTick(player, tick);
			thc$stunNearby(level, player, stats);
			level.playSound(null, player.blockPosition(), THCSounds.BUCKLER_PARRY, SoundSource.PLAYERS, 1.0F, 1.0F);
		} else {
			double reducedHearts = appliedReduction / 2.0D;
			double poise = BucklerState.getPoise(player) - reducedHearts;
			if (poise <= 0.0D) {
				poise = 0.0D;
				BucklerState.setBroken(player, true);
				player.stopUsingItem();
			}
			BucklerState.setPoise(player, poise);
			level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
		}

		if (!parry) {
			thc$applyDurabilityLoss(player, appliedReduction);
		}
		return adjustedAmount;
	}

	@Inject(method = "hurtServer", at = @At("TAIL"))
	private void thc$applyLethalParry(
		ServerLevel level,
		DamageSource source,
		float amount,
		CallbackInfoReturnable<Boolean> cir
	) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof ServerPlayer player)) {
			return;
		}
		if (thc$isEnvironmentalDamage(source, player)) {
			return;
		}
		if (!BucklerItem.isBucklerRaised(self)) {
			return;
		}
		if (!thc$canBucklerBlock(player, source)) {
			return;
		}
		long raiseTick = BucklerState.getRaiseTick(player);
		if (raiseTick < 0L || level.getGameTime() - raiseTick > THC_LETHAL_PARRY_TICKS) {
			return;
		}
		if (player.getHealth() > 0.0F) {
			return;
		}
		thc$breakBuckler(player);
		player.setHealth(1.0F);
	}

	@Unique
	private static boolean thc$isParryWindow(ServerPlayer player, BucklerStats stats, long tick) {
		long raiseTick = BucklerState.getRaiseTick(player);
		if (raiseTick < 0L) {
			return false;
		}
		long delta = tick - raiseTick;
		int parryTicks = (int) Math.round(stats.getParryWindowSeconds() * 20.0D);
		if (parryTicks < 1) {
			parryTicks = 1;
		}
		return delta <= parryTicks;
	}

	@Unique
	private static boolean thc$isEnvironmentalDamage(DamageSource source, LivingEntity target) {
		if (source.is(DamageTypes.IN_FIRE)
			|| source.is(DamageTypes.ON_FIRE)
			|| source.is(DamageTypes.CAMPFIRE)
			|| source.is(DamageTypes.LAVA)
			|| source.is(DamageTypes.HOT_FLOOR)
			|| source.is(DamageTypes.IN_WALL)
			|| source.is(DamageTypes.DROWN)
			|| source.is(DamageTypes.LIGHTNING_BOLT)
			|| source.is(DamageTypes.FALLING_BLOCK)
			|| source.is(DamageTypes.FALLING_ANVIL)
			|| source.is(DamageTypes.FALLING_STALACTITE)
			|| source.is(DamageTypes.CRAMMING)
			|| source.is(DamageTypes.FREEZE)
			|| source.is(DamageTypes.FALL)
			|| source.is(DamageTypes.WITHER)) {
			return true;
		}
		if (source.is(DamageTypes.MAGIC)) {
			return target.hasEffect(MobEffects.POISON);
		}
		return false;
	}

	@Unique
	private static boolean thc$canBucklerBlock(LivingEntity target, DamageSource source) {
		if (source.is(DamageTypeTags.BYPASSES_SHIELD)) {
			return false;
		}
		Vec3 sourcePos = source.getSourcePosition();
		if (sourcePos == null) {
			return false;
		}
		Vec3 view = target.getViewVector(1.0F);
		Vec3 delta = target.position().subtract(sourcePos).normalize();
		delta = new Vec3(delta.x, 0.0D, delta.z);
		return delta.dot(view) < 0.0D;
	}

	@Unique
	private static void thc$stunNearby(ServerLevel level, LivingEntity player, BucklerStats stats) {
		int durationTicks = (int) Math.round(stats.getStunDurationSeconds() * 20.0D);
		if (durationTicks <= 0) {
			return;
		}
		MobEffectInstance slow = new MobEffectInstance(MobEffects.SLOWNESS, durationTicks, 5);
		MobEffectInstance weak = new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 19);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(3.0D),
			entity -> entity.getType().getCategory() == MobCategory.MONSTER)) {
			mob.addEffect(slow, player);
			mob.addEffect(weak, player);
		}
	}

	@Unique
	private static void thc$applyDurabilityLoss(ServerPlayer player, double blockedDamage) {
		if (blockedDamage <= 0.0D) {
			return;
		}
		ItemStack buckler = player.getOffhandItem();
		if (buckler.isEmpty()) {
			return;
		}
		int loss = (int) Math.floor(blockedDamage / 5.0D);
		double remainder = blockedDamage - (loss * 5.0D);
		if (remainder > 0.0D) {
			int roll = player.getRandom().nextInt(5) + 1;
			if (roll <= remainder) {
				loss += 1;
			}
		}
		if (loss > 0) {
			buckler.hurtAndBreak(loss, player, EquipmentSlot.OFFHAND);
		}
	}

	@Unique
	private static void thc$breakBuckler(ServerPlayer player) {
		ItemStack buckler = player.getOffhandItem();
		if (buckler.isEmpty()) {
			return;
		}
		buckler.hurtAndBreak(buckler.getMaxDamage(), player, EquipmentSlot.OFFHAND);
	}
}
