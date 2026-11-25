package mix.client.mixui.visuals.furiah.module.sky;

import mix.client.mixui.visuals.furiah.module.Module;
import net.minecraft.util.Vec3;

public final class SkyModule extends Module {
    public SkyModule() {
        super("sky", "Sky", "", false);
    }

    public Vec3 tintClouds(Vec3 clouds) {
        return clouds;
    }
}
