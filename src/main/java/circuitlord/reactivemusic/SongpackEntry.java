package circuitlord.reactivemusic;

import java.util.ArrayList;
import java.util.List;

public class SongpackEntry {


    // expands out into songpack events and biometag events
    public String[] events;


    public boolean allowFallback = false;

    // OnChanged just sets both Valid and Invalid versions to true
    public boolean forceStopMusicOnChanged = false;
    public boolean forceStopMusicOnValid = false;
    public boolean forceStopMusicOnInvalid = false;

    public boolean forceStartMusicOnValid = false;

    public float forceChance = 1.0f;

    public boolean startMusicOnEventValid = false;

    // deprecated for now
    public boolean stackable = false;

    public String[] songs;

    // plays instead of a random song the first time this entry plays after the mod is installed, from firstSongStart seconds
    public String firstSong = null;
    public float firstSongStart = 0;


    // deprecated
    public boolean alwaysPlay = false;
    public boolean alwaysStop = false;


}
