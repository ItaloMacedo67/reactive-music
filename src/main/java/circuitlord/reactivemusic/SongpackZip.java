package circuitlord.reactivemusic;

import circuitlord.reactivemusic.entries.RMRuntimeEntry;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SongpackZip {

    public SongpackConfig config;


    public List<RMRuntimeEntry> runtimeEntries;

    // song name from the yaml -> its audio file, for every song that was found
    public Map<String, SongFile> songFiles = new HashMap<>();


    public Path path;

    public String errorString = "";
    public boolean blockLoading = false;

    // backwards compat
    public boolean convertBiomeToBiomeTag = false;

    public boolean isv05OldSongpack = false;

    public boolean embedded = false;

}
