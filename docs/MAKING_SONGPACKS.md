
# Making Songpacks

Songpacks are constructed as folders with a yaml configuration file and a folder with ogg or mp3 music files.

They're loaded from the `resourcepacks` folder, although they're not actually resource packs and are instead selected from the configuration UI. This is purely to make including songpacks easier. Until one is selected, the first songpack that loads plays.

**Start from the [songpack template](songpack-template) folder!** Copy it into `resourcepacks` and add your own ogg or mp3 files to its `music` folder. This port comes without songs, so only use songs whose license lets you share them.

You can find a video version of this wiki page [here](https://www.youtube.com/watch?v=YvEap8IUS-c)!
> Highly recommended to use VSCode with the YAML extension as shown in the video, or else you will have issues making properly formatted YAML files!


## Testing Songpacks

Open the songpack menu using `/reactivemusic`. Click on the button for your songpack name and it'll start playing.
This also works for reloading a songpack if you've made changes you want to try without restarting your game by clicking another songpack and back onto yours.

I also highly recommend turning debug mode on in the Debug section,
this will make songs switch whenever their events become valid and removes silence gaps entirely to make it easier to test.

You can also run `/reactivemusic toggleLogging` to display what the mod is doing in real-time.

<br>

## Songpack Configuration

`ReactiveMusic.yaml` is where all the configuration for your songpack takes place. Let's go over all the properties available in a songpack config.

### Global configuration:
```
name: "My Awesome Songpack"
version: "1.0"
author: "CircuitLord"
description: "A really good songpack that I made"
credits: "Cool people here"

musicSwitchSpeed: NORMAL
musicDelayLength: NORMAL
```

- `(String) name` The unique name/identifier for your songpack.
-  `musicSwitchSpeed` (INSTANT, SHORT, NORMAL, LONG) Defines the default for how fast music will stop when it's event becomes invalid
-  `musicDelayLength` (NONE, SHORT, NORMAL, LONG) Defines the default for how much silence there should be before a new song starts playing again

<br><br>

## Songpack Entries:

Songpack entries define what songs play in specific events. You can have as many entries as you want, and they're prioritized internally based on what order you put them in.

Here's an example songpack entry:

```
- events: [ "MAIN_MENU" ]
  songs:
    - "DuNock-Street-TitleEdit"
```

- `(SongpackEventType) events` The specific events that need to be valid for this entry to play. (see details below)
- `(Boolean) allowFallback` (default false) If we've played all songs from this event, should  we "fallback" to other events that are also valid? (good for one-off events you don't want to play over and over)
- `(String[]) songs` The list of song files to pick from when this event plays. These are picked from the `music` sub-folder, `MySong` is `music/MySong.ogg` or `music/MySong.mp3`. Songs can also come from the game or from another pack, see [Songs From Elsewhere](#songs-from-elsewhere).

<br><br>

There are also a couple of other advanced params you can use:

`(Boolean) forceStopMusicOnChanged` (default false) Force stop the current music when this event becomes valid/invalid. Good for when you definitely want the music to switch from whatever's currently playing (boss, nether, etc)
- this also has variations as `forceStopMusicOnValid` and `forceStopMusicOnInvalid` if you want specific behavior

`(Boolean) forceStartMusicOnValid` (default false) If this event becomes valid, and the music stops naturally or because of forceStop, then play this event immediately.

`(float) forceChance` (default 1.0f) If forceStop/Start is enabled, what is the chance it happens? Good for events where you only want the music to switch sometimes.

`(String) firstSong` Plays instead of a random song the first time this entry plays after the mod is installed, like a theme for the first time the game opens. It counts as played once it has been heard for 10 seconds, so a game closed right away or an intro that stops the music doesn't use it up. `(float) firstSongStart` (default 0) starts it that many seconds in.

```
  - events: [ "MAIN_MENU" ]
    firstSong: "MainTheme"
    firstSongStart: 95
    songs:
      - "MainTheme"
      - "Clarity"
```

<br><br>


### IMPORTANT: Spacing matters! Use tabs to properly indent the entries and their properties.
A proper entry looks like this!
```
(tab) - events: [ "DAY" ]
(tab)(tab)alwaysPlay: true
(tab)(tab)songs:
(tab)(tab)(tab) - "MyAwesomeSong"
(tab)(tab)(tab) - "MyOtherCoolSong"
```

<br><br>

You can also combine multiple events for more specific songs, or do an "OR" condition

```
  - events: [ "DAY", "BIOME=MOUNTAIN" ]
    songs:
      - "ForTheKing"
      - "Freedom"

  - events: [ "BIOME=ocean || UNDERWATER" ]
    songs:
      - "Freedom"
```

<br><br>

## Events (SongpackEventTypes)

This lists all the available songpack events you have available.

### Special
- `MAIN_MENU`
- `CREDITS`
- `HOME` (within 45 blocks of bed)
- `CREATIVE` (creative mode, the same test Minecraft uses for its creative music)

### Time
- `DAY`
- `NIGHT`
- `SUNRISE`
- `SUNSET`

### Weather
- `RAIN`
- `SNOW`
- `STORM`

### World Height
- `UNDERWATER`
- `UNDERGROUND`
- `DEEP_UNDERGROUND`
- `HIGH_UP`

### Entities
- `NEARBY_MOBS` (nearby >= 1 monster, may not work that well and may be replaced with combat events soon)
- `MINECART`
- `BOAT`
- `HORSE`
- `PIG`

### Actions
- `FISHING`
- `DYING`

### Location
- `VILLAGE` (nearby villagers)

### Combat
- `BOSS` (whenever a boss bar is on-screen)
- `BOSSBAR=name` (a specific boss bar, see below)

<br><br>

## Biome Events

You can search for any biome by using `BIOME=biomename`. This can be the full biome name or just a part of it if you want to soft-search.

```
  # any biome with cherry in the name
  - events: [ "BIOME=cherry" ]
    songs:
      - "Storm"
```

<br><br>

Biome tags can be used by specifying `BIOMETAG=BIOME_TAG`. They're good for automatically having compatibility with modded biomes. You can find the full list of tags [here](https://maven.fabricmc.net/docs/fabric-api-0.100.3+1.21/net/fabricmc/fabric/api/tag/convention/v2/ConventionalBiomeTags.html)

NOTE: putting the "IS_" in front of the tag is optional ("BIOMETAG=IS_MOUNTAIN" and "BIOMETAG=MOUNTAIN" are both valid

```
  - events: [ "DAY", "BIOMETAG=IS_HOT" ]
    songs:
      - "ForTheKing"
```

<br><br>


## Boss Bar Events

`BOSSBAR=name` is true while a boss bar with that name is on-screen. The name is the translation key the bar's title uses, written in full: `entity.minecraft.wither`, `entity.minecraft.ender_dragon`, `event.minecraft.raid`, or `entity.modname.boss` for a modded boss. A boss renamed with a name tag can be matched by its name in lowercase, without spaces.

```
  - events: [ "BOSSBAR=entity.minecraft.wither" ]
    forceStopMusicOnChanged: true
    forceStartMusicOnValid: true
    songs:
      - "WitherFight"

  # the raid bar reads event.minecraft.raid.victory.full once the raid is won, so this stops with the fight
  - events: [ "BOSSBAR=event.minecraft.raid" ]
    forceStopMusicOnChanged: true
    forceStartMusicOnValid: true
    songs:
      - "RaidFight"
```

<br><br>


## Dimension Events

Dimensions can be specified by doing `DIM=dimname`. Similar to biome events, this can be the fully typed name or just a subset of it.

```
  # we add forceStopMusicOnChanged here so that the song always changes upon going in/out of the nether, since that music is pretty different than anything else
  - events: [ "DIM=NETHER" ]
    forceStopMusicOnChanged: true
    songs:
      - "Storm"
```

<br><br>


## Block Events

In version 1.2.0 and above, you can detect nearby blocks in a square 25 block radius.

Use `/reactivemusic logBlockCounter` to print the nearby block counts to get an idea of what numbers to use.

Fortress detection example:
```
# 1000 nether bricks and 2 brick fences within 25 blocks
[ "DIM=NETHER", "BLOCK=nether_bricks,1000", "BLOCK=nether_brick_fence,2" ]
```


<br><br>


## Songs From Elsewhere

A song doesn't have to be in the songpack's `music` folder:

- `minecraft:music/game/sweden` is one of Minecraft's own songs, named the way `sounds.json` names it. It plays from the game's own files, so a resource pack that replaces the song doesn't change it.
- `Some Pack.zip/assets/minecraft/sounds/music/game/track.ogg` is a file inside another zip in the `resourcepacks` folder. That pack doesn't need to be enabled, it only has to be there, and it's also found as `Some Pack.zip.disabled`, the name Prism Launcher gives a pack it turns off. Songs from a pack that isn't there are skipped, with one warning for the whole pack.

This lets a songpack use music it can't share itself.

```
  - events: [ "BIOME=minecraft:forest" ]
    songs:
      - "minecraft:music/game/sweden"
      - "Other Music Pack.zip/assets/minecraft/sounds/music/game/forest_theme.ogg"
      - "MyOwnSong"
```

An entry whose songs are all missing is skipped, so the entries below it still play. An entry written with `songs: []` plays silence on purpose.

<br><br>


# Mod Configuration

## Making Sounds Mute Music

In-game, do `/reactivemusic toggleSoundEventLogging` to start printing the nearby sound events happening into chat.

In the ReactiveMusic.json5 config, you can edit the `soundsMuteMusic` field with the names/partial names of any sound events that you want to mute RM when they play. This respects positional sound distance. This requires a game restart to be re-loaded.

If a mod incorrectly tags non-positional music as positional, use `soundsMuteMusicIgnoreDistance` instead.

Example:
```
  # Mute RM when near a beacon block
	soundsMuteMusic: [
		"block.beacon.ambient",
		"othermod.xy"
	],

  # Mute RM whenever this sound plays, even if the sound position is wrong
	soundsMuteMusicIgnoreDistance: [
		"boss.theme",
		"partial-event-name"
	],
```

<br><br>

