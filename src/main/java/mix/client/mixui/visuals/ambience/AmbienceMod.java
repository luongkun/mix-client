package mix.client.mixui.visuals.ambience;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.visuals.FuriahVisualMod;
import mix.client.mixui.visuals.furiah.core.FuriahRuntime;

public class AmbienceMod extends FuriahVisualMod {
    public AmbienceMod() {
        super(TranslateText.AMBIENCE, TranslateText.AMBIENCE_DESCRIPTION, FuriahRuntime.ambience());
    }
}
