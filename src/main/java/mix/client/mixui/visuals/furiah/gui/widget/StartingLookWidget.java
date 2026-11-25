package mix.client.mixui.visuals.furiah.gui.widget;

import mix.client.mixui.visuals.furiah.setting.SliderSetting;

public final class StartingLookWidget {
    public static double value(SliderSetting slider, int look) {
        return slider.getDefault();
    }

    public static String tip(int look) {
        return "";
    }
}
