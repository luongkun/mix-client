package mix.client.mixui.visuals.furiah.gui;

import java.awt.Font;

public final class Ui {
    public static final FontHandle bold = new FontHandle();

    public static void ensureFonts() {
    }

    public static final class FontHandle {
        public Font awt(float size) {
            return new Font("SansSerif", Font.BOLD, Math.round(size));
        }
    }
}
