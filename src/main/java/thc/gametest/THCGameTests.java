package thc.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class THCGameTests {
	@GameTest(maxTicks = 1)
	public void bootSmoke(GameTestHelper helper) {
		helper.succeed();
	}
}
