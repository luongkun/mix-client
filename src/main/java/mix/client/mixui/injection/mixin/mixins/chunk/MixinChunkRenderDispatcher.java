package mix.client.mixui.injection.mixin.mixins.chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.management.mods.impl.FPSBoostMod;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

@Mixin(ChunkRenderDispatcher.class)
public class MixinChunkRenderDispatcher {

    /** Hard ceiling for the per-task delay, so a bad setting cannot stall chunk loading. */
    private static final long MAX_DELAY_MILLIS = 250L;

    @Inject(method = "getNextChunkUpdate", at = @At("HEAD"))
    private void limitChunkUpdates(final CallbackInfoReturnable<ChunkCompileTaskGenerator> cir) {

    	FPSBoostMod mod = FPSBoostMod.getInstance();

    	if(mod != null && mod.isToggled() && mod.getChunkDelaySetting().isToggled()) {
			/* The setting is in "delay units"; the original multiplied by 15 with no
			   cap, so a large value parked the chunk workers for seconds per task and
			   the world stopped loading. Clamp it and say so once. */
			long millis = mod.getDelaySetting().getValueLong() * 15L;
			if (millis > MAX_DELAY_MILLIS) {
				if (!warnedClamp) {
					warnedClamp = true;
					MixLogger.warn("Chunk delay clamped to " + MAX_DELAY_MILLIS + " ms (requested " + millis + " ms)");
				}
				millis = MAX_DELAY_MILLIS;
			}
			if (millis > 0) {
				try {
					Thread.sleep(millis);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
    	}
    }

    private static boolean warnedClamp;
}