package mix.client.mixui.visuals.landing;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.visuals.FuriahVisualMod;
import mix.client.mixui.visuals.furiah.core.FuriahRuntime;

public class LandingImpactMod extends FuriahVisualMod {
    public LandingImpactMod() {
        super(TranslateText.LANDING_IMPACT, TranslateText.LANDING_IMPACT_DESCRIPTION, FuriahRuntime.landing());
    }
}
