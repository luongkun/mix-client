package mix.client.mixui.visuals.furiah.module.hit.styles;

import mix.client.mixui.visuals.furiah.setting.Displayable;

public final class CorruptStyle {
    public enum GlyphSet implements Displayable {
        ERRORS("Errors", "ERR, NULL, 404..."),
        HEX("Hex", "0x and a byte"),
        BLOCKS("Blocks", "Shade blocks"),
        CUSTOM("Custom text", "Your own text, up to 12 characters");

        private final String displayName;
        private final String description;

        GlyphSet(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        @Override
        public String displayName() {
            return displayName;
        }

        @Override
        public String description() {
            return description;
        }
    }
}
