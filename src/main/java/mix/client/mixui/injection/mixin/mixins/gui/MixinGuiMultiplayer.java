package mix.client.mixui.injection.mixin.mixins.gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mix.client.mixui.gui.GuiFixConnecting;
import mix.client.mixui.management.mods.impl.ViaVersionMod;
import mix.client.mixui.viaversion.ViaMix;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;

@Mixin(GuiMultiplayer.class)
public class MixinGuiMultiplayer extends GuiScreen {

	@Inject(method = "initGui", at = @At("TAIL"))
    public void preInitGui(CallbackInfo ci) {
		if(ViaVersionMod.getInstance().isToggled()) {
			this.buttonList.add(ViaMix.getInstance().getAsyncVersionSlider());
		}
	}
	
	/* Vanilla's connectToServer is a single displayGuiScreen(new GuiConnecting(...)).
	   Forge patches that method, so an @Overwrite cannot bind and would take this
	   mixin -- the ViaVersion slider in the server list -- down with it. Injecting at
	   HEAD and cancelling swaps in the fix-up screen instead. */
	@Inject(method = "connectToServer", at = @At("HEAD"), cancellable = true)
    private void connectToServer(ServerData server, CallbackInfo ci) {
        mc.displayGuiScreen(new GuiFixConnecting(this, mc, server));
        ci.cancel();
    }
}