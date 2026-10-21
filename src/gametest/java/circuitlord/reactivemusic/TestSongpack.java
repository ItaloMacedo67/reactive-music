package circuitlord.reactivemusic;

import circuitlord.reactivemusic.config.ModConfig;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// This port comes without songs, so the tests bring their own: silent songs in the songpack, in another pack it
// borrows from, and in a resource pack that swaps one of the game's own songs
final class TestSongpack {
	static final String NAME = "Test Songpack";
	static final String WITH_MISSING_FILES = "Test Songpack With Missing Files";
	static final String QUIET_SONG = "Quiet";
	static final String OTHER_PACK_SONG = "Other Test Pack.zip/assets/minecraft/sounds/music/quiet.ogg";
	// the songpack names it Disabled Test Pack.zip, the file is turned off the way Prism Launcher does it
	static final String DISABLED_PACK_SONG = "Disabled Test Pack.zip/assets/minecraft/sounds/music/quiet.ogg";
	static final String BROKEN_SONG = "Broken";
	static final String GAME_SONG = "minecraft:music/menu/moog_city_2";
	static final String SWAPPING_PACK_ID = "file/Swapped Test Music";

	private static final String FOLDER = "ReactiveMusicTest";
	private static final String[] FILES = {"ReactiveMusic.yaml", "ReactiveMusicMissing.yaml", "music/Silence.mp3", "music/Quiet.ogg", "music/Broken.ogg"};
	private static final String QUIET_OGG = "/test-songpack/music/Quiet.ogg";
	private static final String PACK_MCMETA = """
			{"pack": {"description": "Reactive Music test", "min_format": 1, "max_format": 999}}""";

	private TestSongpack() {
	}

	static void load(ClientGameTestContext context) {
		context.runOnClient(client -> {
			if (isLoaded()) {
				return;
			}
			copyIntoResourcePacks();
			RMSongpackLoader.fetchAvailableSongpacks();
			SongpackZip songpack = find(NAME);
			if (songpack.blockLoading || !songpack.errorString.isEmpty()) {
				throw new AssertionError("the test songpack didn't load: " + songpack.errorString);
			}
			ModConfig.setActiveSongpack(songpack);
		});
	}

	static SongpackZip find(String name) {
		return RMSongpackLoader.availableSongpacks.stream()
				.filter(pack -> name.equals(pack.config.name))
				.findFirst()
				.orElseThrow(() -> new AssertionError("the songpack " + name + " wasn't found"));
	}

	private static boolean isLoaded() {
		return ReactiveMusic.currentSongpack != null && NAME.equals(ReactiveMusic.currentSongpack.config.name);
	}

	private static void copyIntoResourcePacks() {
		Path resourcePacks = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
		try {
			Path folder = resourcePacks.resolve(FOLDER);
			for (String file : FILES) {
				copyResource("/test-songpack/" + file, folder.resolve(file));
			}
			writeOtherPack(resourcePacks.resolve(OTHER_PACK_SONG.substring(0, OTHER_PACK_SONG.indexOf('/'))));
			writeOtherPack(resourcePacks.resolve(DISABLED_PACK_SONG.substring(0, DISABLED_PACK_SONG.indexOf('/')) + ".disabled"));
			writeSwappingPack(resourcePacks.resolve(SWAPPING_PACK_ID.substring("file/".length())));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void writeOtherPack(Path zip) throws IOException {
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
			out.putNextEntry(new ZipEntry("pack.mcmeta"));
			out.write(PACK_MCMETA.getBytes(StandardCharsets.UTF_8));
			out.putNextEntry(new ZipEntry(OTHER_PACK_SONG.substring(OTHER_PACK_SONG.indexOf('/') + 1)));
			copyResource(QUIET_OGG, out);
		}
	}

	// a resource pack that replaces GAME_SONG with the 8000 Hz mono silence
	private static void writeSwappingPack(Path folder) throws IOException {
		Files.createDirectories(folder);
		Files.writeString(folder.resolve("pack.mcmeta"), PACK_MCMETA);
		String gameSongPath = GAME_SONG.substring(GAME_SONG.indexOf(':') + 1);
		copyResource(QUIET_OGG, folder.resolve("assets/minecraft/sounds/" + gameSongPath + ".ogg"));
	}

	private static void copyResource(String resource, Path target) throws IOException {
		try (InputStream in = TestSongpack.class.getResourceAsStream(resource)) {
			Files.createDirectories(target.getParent());
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void copyResource(String resource, OutputStream out) throws IOException {
		try (InputStream in = TestSongpack.class.getResourceAsStream(resource)) {
			in.transferTo(out);
		}
	}
}
