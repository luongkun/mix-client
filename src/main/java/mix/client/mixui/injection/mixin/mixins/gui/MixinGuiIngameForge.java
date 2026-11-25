package mix.client.mixui.injection.mixin.mixins.gui;

import mix.client.mixui.gui.GuiEditHUD;
import mix.client.mixui.gui.modmenu.GuiModMenu;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.event.impl.EventRenderDamageTint;
import mix.client.mixui.management.event.impl.EventRenderNotification;
import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.utils.buffer.ScreenTarget;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameForge.class)
public abstract class MixinGuiIngameForge {

    private static final boolean LOG_TARGET = Boolean.getBoolean("mixclient.uicapture.target");
    private static int lastTarget = -1;

    @Inject(method = "renderGameOverlay", at = @At("TAIL"))
    public void postRenderGameOverlay(float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        int target = ScreenTarget.capture();
        if (LOG_TARGET && target != lastTarget) {
            MixLogger.info("[uicapture] Forge HUD target=" + target);
            lastTarget = target;
        }
        new EventRenderDamageTint(partialTicks).call();

        if (!(mc.currentScreen instanceof GuiEditHUD)) {
            new EventRender2D(partialTicks).call();
            if (!(mc.currentScreen instanceof GuiModMenu)) {
                new EventRenderNotification().call();
            }
        }
    }
}
