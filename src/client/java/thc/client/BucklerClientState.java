package thc.client;

public final class BucklerClientState {
	private static double poise;
	private static double maxPoise;
	private static boolean broken;
	private static long lastFullTick = -1L;

	private BucklerClientState() {
	}

	public static void update(double newPoise, double newMaxPoise, boolean newBroken, long newLastFullTick) {
		poise = newPoise;
		maxPoise = newMaxPoise;
		broken = newBroken;
		lastFullTick = newLastFullTick;
	}

	public static double getPoise() {
		return poise;
	}

	public static double getMaxPoise() {
		return maxPoise;
	}

	public static boolean isBroken() {
		return broken;
	}

	public static long getLastFullTick() {
		return lastFullTick;
	}
}
