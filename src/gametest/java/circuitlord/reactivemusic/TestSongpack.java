package circuitlord.reactivemusic;

import circuitlord.reactivemusic.config.ModConfig;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// This port comes without songs, so the tests bring their own songpack: one silent song for the menu and the world
final class TestSongpack {
	private static final String NAME = "Test Songpack";
	private static final String FOLDER = "ReactiveMusicTest";
	private static final String[] FILES = {"ReactiveMusic.yaml", "music/Silence.mp3"};

	private TestSongpack() {
	}

	static void load(ClientGameTestContext context) {
		context.runOnClient(client -> {
			if (isLoaded()) {
				return;
			}
			copyIntoResourcePacks();
			RMSongpackLoader.fetchAvailableSongpacks();
			SongpackZip songpack = RMSongpackLoader.availableSongpacks.stream()
					.filter(pack -> NAME.equals(pack.config.name))
					.findFirst()
					.orElseThrow(() -> new AssertionError("the test songpack wasn't found"));
			if (songpack.blockLoading || !songpack.errorString.isEmpty()) {
				throw new AssertionError("the test songpack didn't load: " + songpack.errorString);
			}
			ModConfig.setActiveSongpack(songpack);
		});
	}

	private static boolean isLoaded() {
		return ReactiveMusic.currentSongpack != null && NAME.equals(ReactiveMusic.currentSongpack.config.name);
	}

	private static void copyIntoResourcePacks() {
		Path folder = FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(FOLDER);
		for (String file : FILES) {
			Path target = folder.resolve(file);
			try (InputStream in = TestSongpack.class.getResourceAsStream("/test-songpack/" + file)) {
				Files.createDirectories(target.getParent());
				Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
}
