package circuitlord.reactivemusic;

import rm_javazoom.jl.decoder.JavaLayerException;
import rm_javazoom.jl.player.AudioDevice;

import java.io.IOException;

// Plays one song on the player thread. The game thread asks it to stop and sets the volume on its audio device.
public interface SongPlayer {

	void play() throws IOException, JavaLayerException;

	boolean getComplete();

	// null once the song is over
	AudioDevice getAudioDevice();

	void queueStop();

	// for a song that failed while playing, a finished one is already closed
	void close();
}
