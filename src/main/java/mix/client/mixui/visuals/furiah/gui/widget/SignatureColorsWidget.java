package mix.client.mixui.visuals.furiah.gui.widget;

import mix.client.mixui.visuals.furiah.fx.style.Signature;
import mix.client.mixui.visuals.furiah.setting.WidgetSetting;

public final class SignatureColorsWidget {
    public interface Source {
        Signature selectedSignature();
        String selectedStyleName();
    }

    public static WidgetSetting setting(Source source) {
        return new WidgetSetting("signatureColors", "Signature Colors", "Colors used by the selected signature style.") {
            @Override
            public Widget createWidget() {
                return new Widget();
            }
        };
    }
}
