package circuitlord.reactivemusic.mixin;

import circuitlord.reactivemusic.ReactiveMusic;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(MusicManager.class)
public class MusicTrackerMixin {

    // A songpack's songs replace Minecraft's own music. Music that other mods give their biomes, dimensions or
    // screens still starts, and the rest of the tick still runs for mods that drive their boss music from it.
    // ReactiveMusic steps aside while that music plays (see SoundManagerMixin). Without a songpack, Minecraft's
    // music plays as usual.
    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/MusicManager;startPlaying(Lnet/minecraft/sounds/Music;)V"))
    private boolean reactivemusic$onlyOtherModsMusic(MusicManager musicManager, Music music) {
        return ReactiveMusic.currentSongpack == null || !music.sound().value().location().getNamespace().equals(Identifier.DEFAULT_NAMESPACE);
    }

}
