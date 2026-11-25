package mix.client.mixui.injection.mixin.mixins.gui;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.mainmenu.GuiMixMainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu {

    @Inject(method = "initGui", at = @At("TAIL"))
    private void mixclient$openMixMenu(CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != (GuiScreen) (Object) this) {
            return;
        }
        Mix.getInstance().start();
        mc.displayGuiScreen(new GuiMixMainMenu());
    }
}
