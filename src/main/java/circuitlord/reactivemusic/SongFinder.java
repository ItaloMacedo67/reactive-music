package circuitlord.reactivemusic;

import circuitlord.reactivemusic.platform.PlatformHelper;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// Finds the file behind each song a songpack lists. A song is written one of three ways:
//   "MySong"                          music/MySong.ogg or music/MySong.mp3 in the songpack
//   "minecraft:music/game/sweden"     one of the game's own sounds, never swapped by a resource pack
//   "Other Pack.zip/assets/.../x.ogg" a file in another zip in the resourcepacks folder, enabled or not,
//                                     also found as Other Pack.zip.disabled
// Each zip is listed only once, however many songs come from it.
final class SongFinder {

	private static final String GAME_SOUND_PREFIX = Identifier.DEFAULT_NAMESPACE + ":";
	private static final String ZIP_FOLDER = ".zip/";
	// Prism Launcher turns a pack off by adding this to its file name, and its songs are still wanted then
	private static final String PRISM_DISABLED_SUFFIX = ".disabled";
	private static final String SONGPACK_MUSIC_FOLDER = "music/";
	private static final String[] SONGPACK_EXTENSIONS = {".ogg", ".mp3"};

	private final SongpackZip songpack;
	private final Path resourcePacks = PlatformHelper.INSTANCE.getGameDir().resolve("resourcepacks");
	private final Map<Path, Set<String>> zipContents = new HashMap<>();
	// pack name -> the songs that wanted a file from it
	final Map<String, Set<String>> missingPacks = new LinkedHashMap<>();

	SongFinder(SongpackZip songpack) {
		this.songpack = songpack;
	}

	// null when there is no file for the song
	SongFile find(String song) {
		if (song.startsWith(GAME_SOUND_PREFIX)) {
			return findGameSound(song);
		}
		String packName = otherPackName(song);
		if (packName != null) {
			return findInOtherPack(packName, song.substring(packName.length() + 1), song);
		}
		return findInSongpack(song);
	}

	boolean isFromMissingPack(String song) {
		String packName = otherPackName(song);
		return packName != null && missingPacks.containsKey(packName);
	}

	// "Other Pack.zip" for "Other Pack.zip/assets/.../x.ogg", null for songs that aren't in another pack
	private static String otherPackName(String song) {
		int packNameEnd = song.indexOf(ZIP_FOLDER);
		return packNameEnd < 0 ? null : song.substring(0, packNameEnd + ZIP_FOLDER.length() - 1);
	}

	// The game's files can't be read yet while mods load, so a missing one only shows up in the log when it should play
	private static SongFile findGameSound(String song) {
		Identifier sound = Identifier.tryParse(song);
		return sound == null ? null : SongFile.gameSound(sound);
	}

	private SongFile findInOtherPack(String packName, String path, String song) {
		Path pack = otherPack(packName);
		if (pack == null) {
			missingPacks.computeIfAbsent(packName, name -> new HashSet<>()).add(song);
			return null;
		}
		return zipContains(pack, path) ? SongFile.inPack(pack, path, false) : null;
	}

	private Path otherPack(String packName) {
		for (String fileName : List.of(packName, packName + PRISM_DISABLED_SUFFIX)) {
			Path pack = resourcePacks.resolve(fileName);
			if (Files.isRegularFile(pack)) {
				return pack;
			}
		}
		return null;
	}

	private SongFile findInSongpack(String song) {
		for (String extension : SONGPACK_EXTENSIONS) {
			String path = SONGPACK_MUSIC_FOLDER + song + extension;
			if (songpackContains(path)) {
				return SongFile.inPack(songpack.path, path, songpack.embedded);
			}
		}
		return null;
	}

	private boolean songpackContains(String path) {
		if (songpack.embedded) {
			return SongFinder.class.getResource(RMSongpackLoader.EMBEDDED_SONGPACK_FOLDER + path) != null;
		}
		if (Files.isDirectory(songpack.path)) {
			return Files.isRegularFile(songpack.path.resolve(path));
		}
		return zipContains(songpack.path, path);
	}

	private boolean zipContains(Path zip, String path) {
		return zipContents.computeIfAbsent(zip, SongFinder::listZip).contains(path);
	}

	private static Set<String> listZip(Path zip) {
		try (ZipFile file = new ZipFile(zip.toFile())) {
			return file.stream().map(ZipEntry::getName).collect(Collectors.toSet());
		} catch (IOException e) {
			ReactiveMusic.LOGGER.error("Failed reading zip " + zip + ": " + e);
			return Set.of();
		}
	}
}
