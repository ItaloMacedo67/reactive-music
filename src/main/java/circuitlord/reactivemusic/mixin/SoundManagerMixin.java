package circuitlord.reactivemusic.mixin;

import circuitlord.reactivemusic.ReactiveMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play", at = @At("HEAD"))
    private void play(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {

        String path = soundInstance.getIdentifier().getPath();

        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && ReactiveMusic.printSoundEvents) {
            mc.player.sendSystemMessage(Component.literal("[ReactiveMusic]: Sound: " + path + " Attenuation: " + soundInstance.getAttenuation()));
        }

        if (path.contains("music_disc") || isJukeboxSong(mc, soundInstance)) {
            ReactiveMusic.trackSoundMuteMusic(soundInstance, false);
        }

        // Minecraft's own music doesn't start while a songpack plays (see MusicTrackerMixin), so this is
        // music from another mod, like a boss fight or a modded dimension
        else if (soundInstance.getSource() == SoundSource.MUSIC) {
            ReactiveMusic.trackSoundMuteMusic(soundInstance, true);
        }

        // cobblemon resource pack uses:
        //"battle.pvn.default"
        //"battle.pvp.default"
        //"battle.pvw.default"
        else if (path.contains("battle.pv")) {
            ReactiveMusic.trackSoundMuteMusic(soundInstance, false);

            ReactiveMusic.LOGGER.info("Detected cobblemon battle event, adding to list!");
        }

        for (String muteSound : ReactiveMusic.config.soundsMuteMusic) {
            if (path.contains(muteSound)) {
                ReactiveMusic.trackSoundMuteMusic(soundInstance, false);
                break;
            }
        }

        for (String muteSound : ReactiveMusic.config.soundsMuteMusicIgnoreDistance) {
            if (path.contains(muteSound)) {
                ReactiveMusic.trackSoundMuteMusic(soundInstance, true);
                break;
            }
        }
    }

    // Modded discs don't always have "music_disc" in their sound name
    private static boolean isJukeboxSong(Minecraft mc, SoundInstance soundInstance) {
        if (soundInstance.getSource() != SoundSource.RECORDS || mc.level == null) {
            return false;
        }
        Identifier sound = soundInstance.getIdentifier();
        return mc.level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).stream()
                .anyMatch(song -> song.soundEvent().value().location().equals(sound));
    }
}
