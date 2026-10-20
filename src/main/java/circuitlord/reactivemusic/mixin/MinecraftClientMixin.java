package circuitlord.reactivemusic.mixin;


import circuitlord.reactivemusic.ReactiveMusic;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void reactivemusic$tick(CallbackInfo ci) {

        ReactiveMusic.newTick();

    }



}
