package circuitlord.reactivemusic;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

// The audio file behind one song of a songpack, see SongFinder for how songs name them
public final class SongFile {

	private static final String OGG_EXTENSION = ".ogg";

	// a zip or folder; null for the embedded songpack and the game's own sounds
	private final Path pack;
	private final String path;
	private final boolean embedded;
	private final Identifier gameSound;

	private SongFile(Path pack, String path, boolean embedded, Identifier gameSound) {
		this.pack = pack;
		this.path = path;
		this.embedded = embedded;
		this.gameSound = gameSound;
	}

	static SongFile inPack(Path pack, String path, boolean embedded) {
		return new SongFile(pack, path, embedded, null);
	}

	// sound is written the way sounds.json names it, like minecraft:music/game/sweden
	static SongFile gameSound(Identifier sound) {
		Identifier file = sound.withPath(soundPath -> "sounds/" + soundPath + OGG_EXTENSION);
		return new SongFile(null, file.getPath(), false, file);
	}

	public boolean isOgg() {
		return path.endsWith(OGG_EXTENSION);
	}

	// null when the file isn't there
	MusicPackResource open() throws IOException {
		return gameSound != null ? openGameSound() : RMSongpackLoader.getInputStream(pack, path, embedded);
	}

	// Only from the game's own files, so a resource pack that replaces the sound doesn't change the song
	private MusicPackResource openGameSound() throws IOException {
		IoSupplier<InputStream> file = Minecraft.getInstance().getVanillaPackResources().fullResources()
				.getResource(PackType.CLIENT_RESOURCES, gameSound);
		if (file == null) {
			return null;
		}
		MusicPackResource resource = new MusicPackResource();
		resource.inputStream = file.get();
		return resource;
	}

	@Override
	public String toString() {
		return gameSound != null ? gameSound.toString() : pack + "/" + path;
	}
}
