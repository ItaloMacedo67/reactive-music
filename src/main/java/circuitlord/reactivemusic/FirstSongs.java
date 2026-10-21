package circuitlord.reactivemusic;

import circuitlord.reactivemusic.config.ModConfig;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import net.minecraft.SharedConstants;

// An entry's firstSong plays the first time that entry plays after the mod is installed. It only counts once it has
// been playing for a while: the game can stop the title screen music while it still loads, and an intro can too.
final class FirstSongs {

	private static final int TICKS_TO_COUNT_AS_HEARD = 10 * SharedConstants.TICKS_PER_SECOND;

	private static int ticksHeard = 0;

	private FirstSongs() {
	}

	static boolean isDue(RMRuntimeEntry entry) {
		return entry.firstSong != null && !ReactiveMusic.config.playedFirstSongs.contains(key(entry))
				&& !PlayerThread.unplayableSongs.contains(entry.firstSong);
	}

	static void tick(RMRuntimeEntry currentEntry, String currentSong, boolean playing) {
		boolean firstSongPlaying = playing && currentEntry != null && isDue(currentEntry) && currentEntry.firstSong.equals(currentSong);
		if (!firstSongPlaying) {
			ticksHeard = 0;
			return;
		}
		if (++ticksHeard >= TICKS_TO_COUNT_AS_HEARD) {
			ReactiveMusic.config.playedFirstSongs.add(key(currentEntry));
			ModConfig.saveConfig();
		}
	}

	private static String key(RMRuntimeEntry entry) {
		return entry.songpack + "/" + entry.firstSong;
	}
}
