package circuitlord.reactivemusic;

import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import circuitlord.reactivemusic.platform.PlatformHelper;
import org.rm_yaml.snakeyaml.Yaml;
import org.rm_yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class RMSongpackLoader {

    static final String EMBEDDED_SONGPACK_FOLDER = "/musicpack/";

    public static List<SongpackZip> availableSongpacks = new ArrayList<>();

    public static MusicPackResource getInputStream(Path dirPath, String fileName, boolean embedded) {
        MusicPackResource resource = new MusicPackResource();

        // quick-path for embedded files
        if (embedded) {
            resource.inputStream = RMSongpackLoader.class.getResourceAsStream(EMBEDDED_SONGPACK_FOLDER + fileName);
            return resource;
        }

        if (dirPath == null) {
            ReactiveMusic.LOGGER.error("dirpath was null");
            return null;
        }

        // Check if path is a zip file
        if (Files.isRegularFile(dirPath) && dirPath.toString().endsWith(".zip")) {
            Map<String, String> env = new HashMap<>();
            env.put("create", "false");

            try {
                FileSystem fs = FileSystems.newFileSystem(dirPath, env);
                Path filePath = fs.getPath(fileName);
                if (Files.exists(filePath)) {
                    resource.inputStream = Files.newInputStream(filePath);
                    resource.fileSystem = fs;
                    return resource;
                }
                fs.close();
            } catch (IOException e) {
                ReactiveMusic.LOGGER.error("Failed while loading file from zip " + e.getMessage());
                return null;
            }
        }

        // handle normal directories
        else {
            Path filePath = dirPath.resolve(fileName);
            if (Files.exists(filePath)) {
                try {
                    resource.inputStream = Files.newInputStream(filePath);
                    return resource;
                } catch (IOException e) {
                    ReactiveMusic.LOGGER.error(e.toString());
                }
            } else {
                ReactiveMusic.LOGGER.error("Couldn't find file! " + filePath);
            }
        }

        return null;
    }

    public static void fetchAvailableSongpacks() {
        long startTime = System.currentTimeMillis();
        availableSongpacks.clear();

        // Load embedded songpack, this port ships without one
        SongpackZip embeddedSongpack = loadSongpack(null, true, "ReactiveMusic.yaml");
        if (embeddedSongpack != null) {
            availableSongpacks.add(embeddedSongpack);
        }

        // Load user songpacks
        Path resourcePacksPath = PlatformHelper.INSTANCE.getGameDir().resolve("resourcepacks");
        List<Path> potentialPacks = new ArrayList<>();

        try {
            Files.walkFileTree(resourcePacksPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
                    potentialPacks.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            ReactiveMusic.LOGGER.error("Failed while visiting potential packs " + e.getMessage());
        }

        for (Path packPath : potentialPacks) {
            List<String> yamlFileNames = new ArrayList<>();

            // zip files
            if (Files.isRegularFile(packPath) && packPath.toString().endsWith(".zip")) {
                try (FileSystem fs = FileSystems.newFileSystem(packPath, (ClassLoader) null)) {

                    Path root = fs.getPath("/");

                    yamlFileNames = getYamlFiles(Files.list(root).toList());

                } catch (IOException e) {
                    ReactiveMusic.LOGGER.error("Failed reading zip: " + e);
                    continue;
                }
            }

            // normal directories
            else if (Files.isDirectory(packPath)) {
                try {

                    yamlFileNames = getYamlFiles(Files.list(packPath).toList());

                } catch (IOException e) {
                    ReactiveMusic.LOGGER.error("Failed reading directory: " + e);
                    continue;
                }
            }

            for (String yamlFile : yamlFileNames) {
                SongpackZip songpackZip = loadSongpack(packPath, false, yamlFile);
                if (songpackZip != null) {
                    availableSongpacks.add(songpackZip);
                }
            }
        }

        ReactiveMusic.LOGGER.info("Took " + (System.currentTimeMillis() - startTime) + "ms to parse available songpacks, found " + availableSongpacks.size() + "!");
    }

    public static List<String> getYamlFiles(List<Path> paths) {

        List<String> found = new ArrayList<>();

        for (Path path : paths) {

            if (Files.isRegularFile(path)) {
                String filename = path.getFileName().toString();

                if (filename.toLowerCase().endsWith(".yaml")) {
                    found.add(filename);
                }
            }
        }

        return found;
    }


    // New version of loadSongpack with YAML file name
    public static SongpackZip loadSongpack(Path songpackPath, boolean embedded, String yamlFileName) {
        SongpackZip songpackZip = new SongpackZip();
        songpackZip.path = songpackPath;
        songpackZip.embedded = embedded;

        var configResource = getInputStream(songpackPath, yamlFileName, embedded);
        if (configResource == null || configResource.inputStream == null) {
            return null;
        }

        Yaml yaml = new Yaml();

        try {
            songpackZip.config = yaml.loadAs(configResource.inputStream, SongpackConfig.class);
        } catch (Exception e) {
            songpackZip.config = new SongpackConfig();
            songpackZip.config.name = songpackPath != null ? songpackPath.getFileName().toString() : "Embedded";
            songpackZip.errorString = e.toString() + "\n\n";
            songpackZip.blockLoading = true;

            ReactiveMusic.LOGGER.error("Failed to load properties! Embedded=" + embedded + " Exception:" + e.toString());
        }

        // yaml.loadAs can return null for empty/invalid YAML without throwing
        if (songpackZip.config == null) {
            songpackZip.config = new SongpackConfig();
            songpackZip.config.name = songpackPath != null ? songpackPath.getFileName().toString() : "Embedded";
            songpackZip.errorString += "YAML config was empty or invalid.\n\n";
            songpackZip.blockLoading = true;

            ReactiveMusic.LOGGER.error("YAML config was null after parsing! Embedded=" + embedded);
        }

        if (!Constructor.errorString.isEmpty()) {
            songpackZip.errorString += Constructor.errorString;
        }

        if (Constructor.blockLoading) {
            songpackZip.blockLoading = true;
        }

        verifySongpackZip(songpackZip);

        if (!songpackZip.blockLoading) {
            songpackZip.runtimeEntries = getRuntimeEntries(songpackZip);
        }

        return songpackZip;
    }

    // Legacy call for default "ReactiveMusic.yaml"
    public static SongpackZip loadSongpack(Path songpackPath, boolean embedded) {
        return loadSongpack(songpackPath, embedded, "ReactiveMusic.yaml");
    }

    private static List<RMRuntimeEntry> getRuntimeEntries(SongpackZip songpackZip) {
        List<RMRuntimeEntry> runtimeEntries = new ArrayList<>();

        for (var entry : songpackZip.config.entries) {
            if (entry == null) continue;

            RMRuntimeEntry runtimeEntry = RMRuntimeEntry.create(songpackZip, entry);

            if (!runtimeEntry.errorString.isEmpty()) {
                songpackZip.errorString += runtimeEntry.errorString;

                // bail out, we don't want to potentially have a half valid event that takes more priority than it should
                continue;
            }

            if (runtimeEntry.conditions.isEmpty()) continue;

            runtimeEntries.add(runtimeEntry);
        }

        return runtimeEntries;
    }

    public static void verifySongpackZip(SongpackZip songpackZip) {
        if (songpackZip.config == null || songpackZip.config.entries == null) {
            songpackZip.errorString += "Entries are null or not formatted correctly! Make sure you.\n\n";
            songpackZip.blockLoading = true;
            return;
        }

        for (int i = 0; i < songpackZip.config.entries.length; i++) {
            if (songpackZip.config.entries[i] == null) continue;

            if (songpackZip.config.entries[i].alwaysPlay || songpackZip.config.entries[i].alwaysStop) {
                songpackZip.errorString += "WARNING! You are using a songpack made for Reactive Music v0.5 and older, things may not work well!\n\n";
                songpackZip.convertBiomeToBiomeTag = true;
                songpackZip.isv05OldSongpack = true;
                break;
            }
        }

        // Find the file of every song, drop the songs without one and the entries left with none
        SongFinder finder = new SongFinder(songpackZip);
        for (int i = 0; i < songpackZip.config.entries.length; i++) {
            SongpackEntry entry = songpackZip.config.entries[i];
            if (entry == null || entry.songs == null) continue;

            List<String> foundSongs = new ArrayList<>();
            for (String song : entry.songs) {
                SongFile file = finder.find(song);
                if (file != null) {
                    foundSongs.add(song);
                    songpackZip.songFiles.put(song, file);
                } else if (!finder.isFromMissingPack(song)) {
                    songpackZip.errorString += "Failed finding song: \"" + song + "\" for event: \"" + String.join("", entry.events) + "\"\n\n";
                }
            }

            // an entry written with no songs is a silent one, but one whose songs are all missing would silence the entries below it
            if (foundSongs.isEmpty() && entry.songs.length > 0) {
                songpackZip.errorString += "None of the songs for event \"" + String.join("", entry.events) + "\" were found, it is skipped\n\n";
                songpackZip.config.entries[i] = null;
                continue;
            }
            entry.songs = foundSongs.toArray(new String[0]);
        }

        finder.missingPacks.forEach((pack, songs) -> songpackZip.errorString +=
                "Couldn't find \"" + pack + "\" in the resourcepacks folder, " + songs.size() + " songs from it won't play\n\n");
    }

}
