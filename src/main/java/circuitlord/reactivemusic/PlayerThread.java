package circuitlord.reactivemusic;


import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundSource;
import rm_javazoom.jl.decoder.JavaLayerException;
import rm_javazoom.jl.player.AudioDevice;
import rm_javazoom.jl.player.JavaSoundAudioDevice;
import rm_javazoom.jl.player.advanced.AdvancedPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

public class PlayerThread extends Thread {

	public static final float MIN_POSSIBLE_GAIN = -80F;
	public static final float MIN_GAIN = -50F;
	public static final float MAX_GAIN = 0F;

	//public static float[] fadeGains;
	
	static {
/*		fadeGains = new float[ReactiveMusic.FADE_DURATION];
		float totaldiff = MIN_GAIN - MAX_GAIN;
		float diff = totaldiff / fadeGains.length;
		for(int i = 0; i < fadeGains.length; i++)
			fadeGains[i] = MAX_GAIN + diff * i;*/

		// Invert because we have fade ticks counting up now
		//for (int i = fadeGains.length - 1; i >= 0; i--) {
		//	fadeGains[i] = MAX_GAIN + diff * (fadeGains.length - 1 - i);
		//}
	}
	
	public volatile static float gainPercentage = 1.0f;
	public volatile static float musicDiscDuckPercentage = 1.0f;

	public static final float QUIET_VOLUME_PERCENTAGE = 0.7f;
	public static final float QUIET_VOLUME_LERP_RATE = 0.02f;
	public static float quietPercentage = 1.0f;

	public volatile static float realGain = 0;

	public volatile static String currentSong = null;
	public volatile static String currentSongChoices = null;

	// songs that failed to open or to play, so they aren't picked again every tick; cleared when a songpack loads
	static final Set<String> unplayableSongs = ConcurrentHashMap.newKeySet();

	private volatile SongFile currentSongFile = null;
	private volatile float currentSongStart = 0;
	public volatile MusicPackResource currentSongResource = null;
	
	volatile SongPlayer player;

	private volatile boolean queued = false;

	private volatile boolean kill = false;
	private volatile boolean playing = false;


	boolean notQueuedOrPlaying() {
		return !(queued || isPlaying());
	}

	boolean isPlaying() {
		return playing && !player.getComplete();
	}
	
	public PlayerThread() {
		setDaemon(true);
		setName("ReactiveMusic Player Thread");
		start();
	}

	@Override
	public void run() {
		// the game thread clears currentSong when it stops a song, so the one being played is kept here
		String song = null;

		while(!kill) {

			String queuedSong = currentSong;
			if(queued && queuedSong != null) {
				song = queuedSong;
				player = openPlayer(song, currentSongFile);
				queued = false;
			}


			if(player != null && player.getAudioDevice() != null) {

				// go to full volume
				setGainPercentage(1.0f);
				processRealGain();

				ReactiveMusic.LOGGER.info("Playing " + song);
				playing = true;
				playUntilDone(player, song);

			}
			else if (!queued) {
				// sleep until play() hands us a song, spinning here kept a whole CPU core busy between songs
				LockSupport.park(this);
			}

		}
	}

	// null when the song can't be read, another one gets picked later
	private SongPlayer openPlayer(String song, SongFile file) {
		if (file == null) {
			ReactiveMusic.LOGGER.error("No file for " + song);
			unplayableSongs.add(song);
			return null;
		}
		MusicPackResource resource = null;
		try {
			resource = file.open();
			if (resource == null || resource.inputStream == null) {
				ReactiveMusic.LOGGER.error("Couldn't find " + file);
				unplayableSongs.add(song);
				return null;
			}
			InputStream input = resource.inputStream;
			SongPlayer songPlayer = file.isOgg() ? new OggPlayer(input) : new AdvancedPlayer(input);
			songPlayer.startAt(currentSongStart);
			currentSongResource = resource;
			return songPlayer;
		} catch (IOException | JavaLayerException e) {
			ReactiveMusic.LOGGER.error("Couldn't play " + file + ": " + e);
			unplayableSongs.add(song);
			if (resource != null) {
				close(resource);
			}
			return null;
		}
	}

	// a broken song must not end this thread, or no music plays until the game restarts
	private void playUntilDone(SongPlayer songPlayer, String song) {
		try {
			songPlayer.play();
		} catch (IOException | JavaLayerException | RuntimeException e) {
			// stopping a song closes its zip, which can cut the last read short
			if (playing) {
				ReactiveMusic.LOGGER.error("Failed playing " + song + ": " + e);
				unplayableSongs.add(song);
			}
			playing = false;
			songPlayer.close();
		}
	}



	public void resetPlayer() {
		playing = false;

		if(player != null)
			player.queueStop();

		queued = false;
		currentSong = null;
		currentSongFile = null;

		// the player closes the song's own stream, a zip has to be closed here
		MusicPackResource resource = currentSongResource;
		if (resource != null && resource.fileSystem != null) {
			close(resource);
		}

		currentSongResource = null;
	}

	private static void close(MusicPackResource resource) {
		try {
			resource.close();
		} catch (Exception e) {
			ReactiveMusic.LOGGER.error("Failed to close file system/input stream " + e.getMessage());
		}
	}

	public void play(String song, SongFile file, float startSeconds) {
		resetPlayer();

		currentSong = song;
		currentSongFile = file;
		currentSongStart = startSeconds;
		queued = true;
		LockSupport.unpark(this);
	}
	
/*	public float getGain() {
		if(player == null)
			return gain;
		
		AudioDevice device = player.getAudioDevice();
		if(device != null && device instanceof JavaSoundAudioDevice)
			return ((JavaSoundAudioDevice) device).getGain();
		return gain;
	}*/
	
/*	public void addGain(float gain) {
		setGain(getGain() + gain);
	}*/
	
	public void setGainPercentage(float newGain) {
		gainPercentage = Math.min(1.0f, Math.max(0.0f, newGain));
	}

	public void setMusicDiscDuckPercentage(float newGain) {
		musicDiscDuckPercentage = newGain;
	}
	
	public void processRealGain() {

		var client = Minecraft.getInstance();

		Options options = Minecraft.getInstance().options;

		boolean musicOptionsOpen = false;

		// Try to find the music options menu
		TranslatableContents ScreenTitleContent = null;
		var screen = client.gui.screen();
		if (screen != null && screen.getTitle() != null && screen.getTitle().getContents() != null
			&& screen.getTitle().getContents() instanceof TranslatableContents) {

			ScreenTitleContent = (TranslatableContents) screen.getTitle().getContents();

			if (ScreenTitleContent != null) {
				musicOptionsOpen = ScreenTitleContent.getKey().equals("options.sounds.title");
			}
		}


		boolean doQuietMusic =  client.isPaused()
				&& client.level != null
				&& !musicOptionsOpen;


		float targetQuietMusicPercentage = doQuietMusic ? QUIET_VOLUME_PERCENTAGE : 1.0f;
        quietPercentage = MyMath.lerpConstant(quietPercentage, targetQuietMusicPercentage, QUIET_VOLUME_LERP_RATE);

		
		float minecraftGain = options.getSoundSourceVolume(SoundSource.MUSIC) * options.getSoundSourceVolume(SoundSource.MASTER);

		// my jank way of changing the volume curve to be less drastic
		float minecraftDistFromMax = 1.0f - minecraftGain;
		float minecraftGainAddScalar = (minecraftDistFromMax * 1.0f) * minecraftGain;
		// cap to 1.0
		minecraftGain = Math.min(minecraftGain + minecraftGainAddScalar, 1.0f);

		//ReactiveMusic.LOGGER.info("minecraft fake gain: " + minecraftGain);


		float newRealGain = MIN_GAIN + (MAX_GAIN - MIN_GAIN) * minecraftGain * gainPercentage * quietPercentage * musicDiscDuckPercentage;

		// Force to basically off if the user sets their volume off
		if (minecraftGain <= 0) {
			newRealGain = MIN_POSSIBLE_GAIN;
		}

		//ReactiveMusic.LOGGER.info("Current gain: " + newRealGain);

		realGain = newRealGain;
		if(player != null) {
			AudioDevice device = player.getAudioDevice();
			if(device != null && device instanceof JavaSoundAudioDevice) {
				try {
					((JavaSoundAudioDevice) device).setGain(newRealGain);
				} catch(IllegalArgumentException e) {
					ReactiveMusic.LOGGER.error(e.toString());
				}
			}
		}
		
		//if(musicGain == 0)
		//	play(null);
	}

	
/*	public float getRelativeVolume() {
		return getRelativeVolume(getGain());
	}*/
	
/*	public float getRelativeVolume(float gain) {
		float width = MAX_GAIN - MIN_GAIN;
		float rel = Math.abs(gain - MIN_GAIN);
		return rel / Math.abs(width);
	}*/

/*	public int getFramesPlayed() {
		return player == null ? 0 : player.getFrames();
	}*/
	
	public void forceKill() {
		try {
			resetPlayer();
			interrupt();

			finalize();
			kill = true;
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
}
