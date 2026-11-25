package mix.client.mixui.visuals.furiah.mixin;

import mix.client.mixui.injection.interfaces.IMixinMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

public final class AccessorMinecraft {
    public static boolean available() {
        return true;
    }

    public static Timer getFvTimer(Minecraft mc) {
        return ((IMixinMinecraft) mc).getTimer();
    }
}
