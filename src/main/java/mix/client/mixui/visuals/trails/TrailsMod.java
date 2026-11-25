package mix.client.mixui.visuals.trails;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.visuals.FuriahVisualMod;
import mix.client.mixui.visuals.furiah.core.FuriahRuntime;

public class TrailsMod extends FuriahVisualMod {
    public TrailsMod() {
        super(TranslateText.TRAILS, TranslateText.TRAILS_DESCRIPTION, FuriahRuntime.trails());
    }
}
