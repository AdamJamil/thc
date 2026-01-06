package thc.gametest;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.npc.villager.VillagerTrades;

public class THCShieldGameTests {
	@GameTest
	public void shieldRecipeRemoved(GameTestHelper helper) {
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("shield"));
		boolean present = helper.getLevel().getServer().getRecipeManager().byKey(key).isPresent();
		helper.assertTrue(!present, "Shield recipe should be removed.");
		helper.succeed();
	}

	@GameTest
	public void shieldLootRemoved(GameTestHelper helper) {
		ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("thc", "tests/shield"));
		LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
		LootParams params = new LootParams.Builder(helper.getLevel())
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(BlockPos.ZERO))
			.create(LootContextParamSets.CHEST);
		List<ItemStack> drops = table.getRandomItems(params);
		boolean hasShield = drops.stream().anyMatch(stack -> stack.is(Items.SHIELD));
		helper.assertTrue(!hasShield, "Shield should be removed from loot drops.");
		helper.succeed();
	}

	@GameTest
	public void shieldTradeRemoved(GameTestHelper helper) {
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 1, factories -> {
			factories.add(new VillagerTrades.ItemsForEmeralds(Items.SHIELD, 1, 1, 1));
		});

		Villager villager = helper.spawn(EntityType.VILLAGER, 1, 2, 1);
		VillagerData data = villager.getVillagerData().withProfession(
			helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.FARMER)
		);
		villager.setVillagerData(data);

		MerchantOffers offers = villager.getOffers();
		boolean hasShield = offers.stream().map(MerchantOffer::getResult).anyMatch(stack -> stack.is(Items.SHIELD));
		helper.assertTrue(!hasShield, "Shield should be removed from villager trades.");
		helper.succeed();
	}
}
