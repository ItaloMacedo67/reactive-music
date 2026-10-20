package circuitlord.reactivemusic;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.Screen;

import java.lang.management.ManagementFactory;

public class ReactiveMusicClientGameTest implements FabricClientGameTest {
	private static final int SONG_START_TIMEOUT = 20 * 30;
	private static final int IDLE_MEASURE_TICKS = 100;
	private static final double MAX_IDLE_CPU_SHARE = 0.05;
	private static final String OVERWORLD = "minecraft:overworld";

	@Override
	public void runTest(ClientGameTestContext context) {
		boolean debugModeBefore = context.computeOnClient(client -> ReactiveMusic.config.debugModeEnabled);
		try {
			// debug mode drops the delay between songs, so the world music starts right away
			context.runOnClient(client -> ReactiveMusic.config.debugModeEnabled = true);

			titleScreenPlaysMusic(context);
			try (TestSingleplayerContext world = context.worldBuilder().create()) {
				world.getConnection().waitForChunksRender();
				worldPlaysItsOwnMusic(context);
				commandOpensConfigScreen(context);
				idlePlayerThreadSleeps(context);
			}
		} finally {
			context.runOnClient(client -> ReactiveMusic.config.debugModeEnabled = debugModeBefore);
		}
	}

	private static void titleScreenPlaysMusic(ClientGameTestContext context) {
		context.waitFor(client -> ReactiveMusic.thread.isPlaying(), SONG_START_TIMEOUT);
		assertEvent(SongpackEventType.MAIN_MENU, true);
		if (PlayerThread.currentSong == null) {
			throw new AssertionError("nothing is playing on the title screen");
		}
	}

	private static void worldPlaysItsOwnMusic(ClientGameTestContext context) {
		context.waitFor(client -> !SongPicker.songpackEventMap.get(SongpackEventType.MAIN_MENU));
		assertEvent(SongpackEventType.OVERWORLD, true);
		context.waitFor(client -> ReactiveMusic.thread.isPlaying() && isPlayingWorldEntry(), SONG_START_TIMEOUT);
		ReactiveMusic.LOGGER.info("In-game song {} for {}", PlayerThread.currentSong, ReactiveMusic.currentEntry.eventString);
		context.takeScreenshot("reactivemusic-in-world");
	}

	private static void commandOpensConfigScreen(ClientGameTestContext context) {
		context.runOnClient(client -> client.player.connection.sendCommand("reactivemusic"));
		context.waitFor(client -> isConfigScreen(client.gui.screen()));
		context.takeScreenshot("reactivemusic-config-screen");
		context.runOnClient(client -> client.gui.screen().onClose());
	}

	// No music in a blacklisted dimension, so the player thread has nothing to do
	private static void idlePlayerThreadSleeps(ClientGameTestContext context) {
		context.runOnClient(client -> ReactiveMusic.config.blacklistedDimensions.add(OVERWORLD));
		try {
			context.waitFor(client -> ReactiveMusic.thread.notQueuedOrPlaying(), SONG_START_TIMEOUT);
			context.waitTicks(20);
			long cpuBefore = playerThreadCpuNanos();
			long wallBefore = System.nanoTime();
			context.waitTicks(IDLE_MEASURE_TICKS);
			long cpu = playerThreadCpuNanos() - cpuBefore;
			long wall = System.nanoTime() - wallBefore;
			if (cpu > wall * MAX_IDLE_CPU_SHARE) {
				throw new AssertionError("the idle player thread used " + cpu / 1_000_000 + " ms of CPU in " + wall / 1_000_000 + " ms");
			}
		} finally {
			context.runOnClient(client -> ReactiveMusic.config.blacklistedDimensions.remove(OVERWORLD));
		}
	}

	private static boolean isPlayingWorldEntry() {
		return ReactiveMusic.currentEntry != null && !ReactiveMusic.currentEntry.eventString.startsWith("MAIN_MENU");
	}

	private static long playerThreadCpuNanos() {
		return ManagementFactory.getThreadMXBean().getThreadCpuTime(ReactiveMusic.thread.threadId());
	}

	private static boolean isConfigScreen(Screen screen) {
		return screen != null && screen.getClass().getName().startsWith("circuitlord.reactivemusic.config.ModConfig");
	}

	private static void assertEvent(SongpackEventType event, boolean expected) {
		if (SongPicker.songpackEventMap.get(event) != expected) {
			throw new AssertionError(event + " should be " + expected);
		}
	}
}
