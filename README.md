# Reactive Music Fork

Reactive Music trades Minecraft's default music for something dynamic, reactive, and ever-changing.

Reactive Music is based off a music pack I originally made for the Ambience mod back in 1.12. Now rebuilt to be a standalone mod for modern minecraft.

Discord: https://discord.gg/vJ6BCjPR3R

## This fork

An unofficial port of [Reactive Music](https://modrinth.com/mod/reactive-music) 1.3.5 by CircuitLord to Minecraft 26.3, Fabric only, made for the Mildly Difficult modpack. It's called Reactive Music Fork in game so it isn't taken for the original, but it keeps the original's mod id, so the two can't be installed together, and its songpacks, config and `/reactivemusic` command. Changes from the original:

- No songs come with it. The songs come from songpacks in the `resourcepacks` folder (see [Making Songpacks](docs/MAKING_SONGPACKS.md)), and the first one that loads plays until you pick another with `/reactivemusic`. With no songpack, Minecraft's own music plays.
- Music that other mods play through the game, like Naraka's boss fights and its dimension, still plays, and Reactive Music fades out under it the way it does for jukeboxes. Minecraft's own music stays off while a songpack plays.
- Discs from other mods fade it out too, not only the ones with `music_disc` in their sound name.
- The song player waits without using the processor while nothing plays. Before, it kept a whole core busy between songs.
- No more "Unsupported control type: Master Gain" errors in the log when a song starts.
- Songs can be ogg files as well as mp3. A songpack can also play Minecraft's own songs straight from the game files, where no resource pack can swap them, and songs from other packs in the `resourcepacks` folder, enabled or not (also when Prism Launcher renamed them to `.zip.disabled`).
- New events: `CREATIVE`, and `BOSSBAR=` for one boss bar at a time (the Wither, the Ender Dragon, a raid, a modded boss).
- A song that can't be read is skipped. Before, it stopped all music until the game restarted.
- An entry can have a `firstSong` that plays the first time the entry plays after the mod is installed, from any point of the song.

## Download

The latest jar is on the [releases page](https://github.com/ItaloMacedo67/reactive-music/releases/latest). It needs Fabric API.

## Credits

- Reactive Music is by CircuitLord, https://github.com/CircuitLord/ReactiveMusic
- It includes JLayer (LGPL) and SnakeYAML (Apache 2.0) under their own package names, as the original does.

## License

GPL-3.0, like the original. See [LICENSE](LICENSE).
