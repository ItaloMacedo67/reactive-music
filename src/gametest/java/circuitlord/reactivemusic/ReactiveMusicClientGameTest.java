package circuitlord.reactivemusic;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.Screen;

public class ReactiveMusicClientGameTest implements FabricClientGameTest {
	private static final int SONG_START_TIMEOUT = 20 * 30;

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

	private static boolean isPlayingWorldEntry() {
		return ReactiveMusic.currentEntry != null && !ReactiveMusic.currentEntry.eventString.startsWith("MAIN_MENU");
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
