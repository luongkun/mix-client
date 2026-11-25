package mix.client.mixui.visuals.particles;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.visuals.FuriahVisualMod;
import mix.client.mixui.visuals.furiah.core.FuriahRuntime;

public class ParticlesMod extends FuriahVisualMod {
    public ParticlesMod() {
        super(TranslateText.VISUAL_PARTICLES, TranslateText.VISUAL_PARTICLES_DESCRIPTION, FuriahRuntime.particles());
    }
}
