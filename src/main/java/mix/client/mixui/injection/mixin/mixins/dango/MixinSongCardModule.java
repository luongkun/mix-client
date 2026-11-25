package mix.client.mixui.injection.mixin.mixins.dango;

import me.sorihiko.dango.events.WorldRenderEndEvent;
import me.sorihiko.dango.module.impls.music.SongCardModule;
import mix.client.mixui.logger.MixLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SongCardModule.class)
public abstract class MixinSongCardModule {

    private static final boolean DIAG = Boolean.getBoolean("mixclient.dango.diag");
    private static boolean logged;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void mixclient$disableNativeHud(WorldRenderEndEvent event, CallbackInfo ci) {
        if (DIAG && !logged) {
            MixLogger.info("[dango] native SongCard render blocked by Mix HUD");
            logged = true;
        }
        ci.cancel();
    }
}
