Reactive Music trades Minecraft's default music for something dynamic, reactive, and ever-changing. Including a collection of fantasy and celtic tracks to give an air of wonder, a whisper of the unknown, and the call of adventure.

Reactive Music is based off a music pack I originally made for the Ambience mod back in 1.12. Now rebuilt to be a standalone mod for modern minecraft.

Discord: https://discord.gg/vJ6BCjPR3R

## This fork

An unofficial port of [Reactive Music](https://modrinth.com/mod/reactive-music) 1.3.5 by CircuitLord to Minecraft 26.3, Fabric only, made for the Mildly Difficult modpack. Changes from the original:

- Music that other mods play through the game, like Naraka's boss fights and its dimension, still plays, and Reactive Music fades out under it the way it does for jukeboxes. Minecraft's own music stays off as before.
- Discs from other mods fade it out too, not only the ones with `music_disc` in their sound name.
- The song player waits without using the processor while nothing plays. Before, it kept a whole core busy between songs.
- No more "Unsupported control type: Master Gain" errors in the log when a song starts.

## Download

The latest jar is on the [releases page](https://github.com/ItaloMacedo67/reactive-music/releases/latest). It needs Fabric API.

## Credits

- Reactive Music is by CircuitLord, https://github.com/CircuitLord/ReactiveMusic
- The built-in songs are by Vindsvept, Adrian von Ziegler and Mufaya, as credited by the original mod.
- It includes JLayer (LGPL) and SnakeYAML (Apache 2.0) under their own package names, as the original does.

## License

GPL-3.0, like the original. See [LICENSE](LICENSE).
