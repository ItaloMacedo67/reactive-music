package circuitlord.reactivemusic;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;

// Naraka plays its boss music through the game's music manager and fades or switches phases from its tick,
// and its dimension has music of its own. Both keep working, with our songs quiet meanwhile.
public class NarakaMusicClientGameTest implements FabricClientGameTest {
	// Naraka fades a phase out by 0.02 a tick before the next one starts
	private static final int PHASE_SWITCH_TIMEOUT = 20 * 10;
	private static final int DUCK_TIMEOUT = 20 * 30;
	private static final float DUCKED = 0.9f;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!FabricLoader.getInstance().isModLoaded("naraka")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			ReactiveMusicClientGameTest.withAudibleMusic(context, () -> {
				bossMusicSwitchesPhasesAndStops(context);
				narakaDimensionKeepsItsMusic(context, world);
			});
		}
	}

	private static void bossMusicSwitchesPhasesAndStops(ClientGameTestContext context) {
		context.runOnClient(client -> playBossMusic(client, "HEROBRINE_PHASE_1"));
		context.waitFor(client -> isPlaying(client, "HEROBRINE_PHASE_1") && PlayerThread.musicDiscDuckPercentage < DUCKED, DUCK_TIMEOUT);

		context.runOnClient(client -> playBossMusic(client, "HEROBRINE_PHASE_2"));
		context.waitFor(client -> isPlaying(client, "HEROBRINE_PHASE_2"), PHASE_SWITCH_TIMEOUT);

		context.runOnClient(NarakaMusicClientGameTest::stopBossMusic);
		context.waitFor(client -> client.getMusicManager().getCurrentMusicTranslationKey() == null, PHASE_SWITCH_TIMEOUT);
		context.waitFor(client -> PlayerThread.musicDiscDuckPercentage == 1.0f, DUCK_TIMEOUT);
	}

	private static void narakaDimensionKeepsItsMusic(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("execute in naraka:naraka run tp @a 0 100 0");
		context.waitFor(client -> client.level.dimension().identifier().toString().equals("naraka:naraka"));
		context.waitFor(client -> isPlaying(client, "HEROBRINE_PHASE_4") && PlayerThread.musicDiscDuckPercentage < DUCKED, DUCK_TIMEOUT);
		context.takeScreenshot("reactivemusic-naraka-dimension");
	}

	private static boolean isPlaying(Minecraft client, String narakaMusic) {
		return client.getMusicManager().isPlayingMusic(narakaMusic(narakaMusic));
	}

	private static Music narakaMusic(String name) {
		try {
			return (Music) Class.forName("com.yummy.naraka.sounds.NarakaMusics").getField(name).get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void playBossMusic(Minecraft client, String narakaMusic) {
		try {
			bossMusicPlayer().getMethod("naraka$playBossMusic", Music.class).invoke(client.getMusicManager(), narakaMusic(narakaMusic));
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void stopBossMusic(Minecraft client) {
		try {
			bossMusicPlayer().getMethod("naraka$stopBossMusic").invoke(client.getMusicManager());
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static Class<?> bossMusicPlayer() throws ClassNotFoundException {
		return Class.forName("com.yummy.naraka.client.sound.BossMusicPlayer");
	}
}
