package mix.client.mixui.visuals.furiah.module.targetaura;

import mix.client.mixui.visuals.furiah.module.Module;

public final class TargetAuraModule extends Module {
    public TargetAuraModule() {
        super("target_aura", "Target Aura", "", false);
    }

    public boolean hasTarget() {
        return false;
    }
}
