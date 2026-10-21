package circuitlord.reactivemusic;

import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import rm_javazoom.jl.player.JavaSoundAudioDevice;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// Where songs come from (the songpack's own ogg files, another pack's zip, the game's own files) and the events
// that choose them: boss bars by name and creative mode
public class SongSourcesClientGameTest implements FabricClientGameTest {
	private static final int SONG_SWITCH_TIMEOUT = 20 * 30;
	private static final int PLAYBACK_CHECK_TICKS = 40;
	private static final int MIN_PLAYED_MILLIS = 1000;
	private static final String BOSS_BAR = "reactivemusic:test";
	private static final String GENERIC_SONG = "Silence";

	@Override
	public void runTest(ClientGameTestContext context) {
		boolean debugModeBefore = context.computeOnClient(client -> ReactiveMusic.config.debugModeEnabled);
		try {
			// debug mode drops the delay between songs, so each switch happens right away
			context.runOnClient(client -> ReactiveMusic.config.debugModeEnabled = true);
			TestSongpack.load(context);
			missingFilesAreReported(context);
			try (TestSingleplayerContext world = context.worldBuilder().create()) {
				world.getConnection().waitForChunksRender();
				world.getServer().runCommand("gamemode survival @a");
				waitForSong(context, GENERIC_SONG);
				bossBarsPickSongsByName(context, world);
				creativePlaysTheGameOwnSong(context, world);
			}
		} finally {
			context.runOnClient(client -> ReactiveMusic.config.debugModeEnabled = debugModeBefore);
		}
	}

	private static void missingFilesAreReported(ClientGameTestContext context) {
		context.runOnClient(client -> {
			SongpackZip songpack = TestSongpack.find(TestSongpack.WITH_MISSING_FILES);
			assertContains(songpack.errorString, "Couldn't find \"Not Installed Pack.zip\" in the resourcepacks folder, 2 songs from it won't play");
			assertContains(songpack.errorString, "Failed finding song: \"NoSuchSong\"");
			if (songpack.errorString.contains("Failed finding song: \"Not Installed Pack.zip")) {
				throw new AssertionError("the songs of a missing pack should be reported once for the pack: " + songpack.errorString);
			}

			List<String> events = songpack.runtimeEntries.stream().map(entry -> entry.eventString).toList();
			if (!events.equals(List.of("CREATIVE_", "GENERIC_"))) {
				throw new AssertionError("only the entry left without songs should be skipped, the silent one stays: " + events);
			}
			RMRuntimeEntry creative = songpack.runtimeEntries.getFirst();
			if (!creative.songs.equals(List.of(TestSongpack.QUIET_SONG))) {
				throw new AssertionError("the missing song should be dropped: " + creative.songs);
			}
		});
	}

	private static void bossBarsPickSongsByName(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("bossbar add " + BOSS_BAR + " {translate:\"entity.minecraft.wither\"}");
		world.getServer().runCommand("bossbar set " + BOSS_BAR + " players @a");
		waitForOggSong(context, TestSongpack.QUIET_SONG);

		setBossBarName(world, "event.minecraft.raid");
		waitForOggSong(context, TestSongpack.OTHER_PACK_SONG);

		// once the raid is won its bar reads event.minecraft.raid.victory.full, and the fight music ends with the fight
		setBossBarName(world, "event.minecraft.raid.victory.full");
		waitForSong(context, GENERIC_SONG);

		world.getServer().runCommand("bossbar remove " + BOSS_BAR);
	}

	private static void setBossBarName(TestSingleplayerContext world, String translationKey) {
		world.getServer().runCommand("bossbar set " + BOSS_BAR + " name {translate:\"" + translationKey + "\"}");
	}

	// With a resource pack swapping the song in, the game's own file still plays
	private static void creativePlaysTheGameOwnSong(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> packsBefore = context.computeOnClient(client -> List.copyOf(client.getResourcePackRepository().getSelectedIds()));
		List<String> packsWithSwap = new ArrayList<>(packsBefore);
		packsWithSwap.add(TestSongpack.SWAPPING_PACK_ID);
		selectResourcePacks(context, packsWithSwap);
		try {
			context.runOnClient(client -> {
				String source = client.getResourceManager().getResource(gameSongFile()).orElseThrow().sourcePackId();
				if (!source.equals(TestSongpack.SWAPPING_PACK_ID)) {
					throw new AssertionError("the test pack didn't swap the song, it comes from " + source);
				}
			});

			world.getServer().runCommand("gamemode creative @a");
			waitForOggSong(context, TestSongpack.GAME_SONG);
			float playedRate = context.computeOnClient(client -> ((OggPlayer) ReactiveMusic.thread.player).getFormat().getSampleRate());
			float gameRate = context.computeOnClient(SongSourcesClientGameTest::gameSongSampleRate);
			if (playedRate != gameRate) {
				throw new AssertionError("played the resource pack's copy at " + playedRate + " Hz, the game's own song is " + gameRate + " Hz");
			}

			world.getServer().runCommand("gamemode survival @a");
			waitForSong(context, GENERIC_SONG);
		} finally {
			selectResourcePacks(context, packsBefore);
		}
	}

	private static Identifier gameSongFile() {
		return Identifier.parse(TestSongpack.GAME_SONG).withPath(path -> "sounds/" + path + ".ogg");
	}

	private static float gameSongSampleRate(Minecraft client) {
		try (InputStream file = client.getVanillaPackResources().fullResources().getResource(PackType.CLIENT_RESOURCES, gameSongFile()).get()) {
			return new JOrbisAudioStream(file).getFormat().getSampleRate();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void selectResourcePacks(ClientGameTestContext context, Collection<String> packIds) {
		CompletableFuture<Void> reload = context.computeOnClient(client -> {
			PackRepository repository = client.getResourcePackRepository();
			repository.reload();
			repository.setSelected(packIds);
			return client.reloadResourcePacks();
		});
		context.waitFor(client -> reload.isDone() && client.gui.overlay() == null);
	}

	private static void waitForSong(ClientGameTestContext context, String song) {
		context.waitFor(client -> ReactiveMusic.thread.isPlaying() && song.equals(PlayerThread.currentSong), SONG_SWITCH_TIMEOUT);
	}

	// the song keeps going and its samples reach the audio line
	private static void waitForOggSong(ClientGameTestContext context, String song) {
		waitForSong(context, song);
		SongPlayer player = context.computeOnClient(client -> ReactiveMusic.thread.player);
		if (!(player instanceof OggPlayer)) {
			throw new AssertionError(song + " should play as ogg, not with " + player);
		}
		context.waitTicks(PLAYBACK_CHECK_TICKS);
		int playedMillis = context.computeOnClient(client -> {
			if (ReactiveMusic.thread.player != player || !ReactiveMusic.thread.isPlaying()) {
				throw new AssertionError(song + " stopped right after it started");
			}
			return ((JavaSoundAudioDevice) player.getAudioDevice()).getPosition();
		});
		if (playedMillis < MIN_PLAYED_MILLIS) {
			throw new AssertionError(song + " played only " + playedMillis + " ms in " + PLAYBACK_CHECK_TICKS + " ticks");
		}
	}

	private static void assertContains(String text, String expected) {
		if (!text.contains(expected)) {
			throw new AssertionError("expected \"" + expected + "\" in: " + text);
		}
	}
}
