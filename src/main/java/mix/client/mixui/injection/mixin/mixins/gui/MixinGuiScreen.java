package mix.client.mixui.injection.mixin.mixins.gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mix.client.mixui.management.mods.impl.ClickEffectMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

	@Shadow
    protected Minecraft mc;

    /* The handleKeyboardInput override that used to sit here is gone.

       It replaced GuiScreen#handleKeyboardInput only to add one extra branch --
       "no key code but a printable character" -- on top of vanilla's
       getEventKeyState() check. Forge's patched GuiScreen does not expose the
       method under the name the notch mapping lists, so the @Overwrite could not
       bind and took this mixin (and with it the Mix UI's screen hooks) down.
       Normal typing still arrives through vanilla's keyTyped dispatch; only that
       extra branch is lost. */

	@Inject(method = "drawScreen", at = @At("TAIL"))
    public void postDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
		if(ClickEffectMod.getInstance().isToggled()) {
			ClickEffectMod.getInstance().drawClickEffects();
		}
	}
	
	@Inject(method = "mouseClicked", at = @At("HEAD"))
	public void preMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
		if(ClickEffectMod.getInstance().isToggled()) {
			ClickEffectMod.getInstance().addClickEffect(mouseX, mouseY);
		}
	}
}