package mix.client.mixui.visuals.furiah.module.watermark;

import mix.client.mixui.visuals.furiah.module.Module;

public final class WaterMarkModule extends Module {
    public WaterMarkModule() {
        super("watermark", "Watermark", "", false);
    }

    public float glintSweep() { return 0.9F; }
    public float glintEvery() { return 4.0F; }
}
