package circuitlord.reactivemusic;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.util.Mth;
import rm_javazoom.jl.decoder.JavaLayerException;
import rm_javazoom.jl.player.AudioDevice;
import rm_javazoom.jl.player.JavaSoundAudioDevice;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

// Plays an Ogg Vorbis song with the decoder Minecraft uses for its own sounds, on the same kind of audio line as the mp3 songs
public class OggPlayer implements SongPlayer, FloatConsumer {

	// one decoded Vorbis packet of a stereo song fits without growing
	private static final int INITIAL_CHUNK_SAMPLES = 8192;

	private final JOrbisAudioStream stream;
	private volatile JavaSoundAudioDevice audio;
	private short[] chunk = new short[INITIAL_CHUNK_SAMPLES];
	private int chunkLength = 0;
	// decoded and thrown away to start the song further in
	private long samplesToSkip = 0;
	private volatile boolean stopQueued = false;
	private volatile boolean complete = false;

	public OggPlayer(InputStream input) throws IOException, JavaLayerException {
		stream = new JOrbisAudioStream(input);
		JavaSoundAudioDevice device = new JavaSoundAudioDevice();
		device.open(stream.getFormat());
		audio = device;
	}

	@Override
	public void startAt(float seconds) {
		AudioFormat format = stream.getFormat();
		samplesToSkip = (long) (seconds * format.getSampleRate()) * format.getChannels();
	}

	@Override
	public void play() throws IOException, JavaLayerException {
		try {
			boolean moreAudio = true;
			while (moreAudio && !stopQueued) {
				moreAudio = stream.readChunk(this);
				audio.write(chunk, 0, chunkLength);
				chunkLength = 0;
			}
			if (!stopQueued) {
				// the line still holds the last half second or so
				audio.flush();
			}
		} finally {
			complete = true;
			close();
		}
	}

	@Override
	public void accept(float sample) {
		if (samplesToSkip > 0) {
			samplesToSkip--;
			return;
		}
		if (chunkLength == chunk.length) {
			chunk = Arrays.copyOf(chunk, chunk.length * 2);
		}
		chunk[chunkLength++] = toPcm16(sample);
	}

	// the same rounding as Minecraft's ChunkedSampleByteBuf
	private static short toPcm16(float sample) {
		return (short) Mth.clamp((int) (sample * 32767.5F - 0.5F), Short.MIN_VALUE, Short.MAX_VALUE);
	}

	@Override
	public synchronized void close() {
		if (audio == null) {
			return;
		}
		audio.close();
		audio = null;
		try {
			stream.close();
		} catch (IOException e) {
			ReactiveMusic.LOGGER.error("Failed closing a song: " + e);
		}
	}

	public AudioFormat getFormat() {
		return stream.getFormat();
	}

	@Override
	public boolean getComplete() {
		return complete;
	}

	@Override
	public AudioDevice getAudioDevice() {
		return audio;
	}

	@Override
	public void queueStop() {
		stopQueued = true;
	}
}
