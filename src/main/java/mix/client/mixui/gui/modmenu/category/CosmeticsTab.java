package mix.client.mixui.gui.modmenu.category;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.nanovg.font.Icon;

public enum CosmeticsTab {
    CAPE(TranslateText.COSMETICS_TAB_CAPE, Icon.SHOPPING),
    KILL_EFFECT(TranslateText.COSMETICS_TAB_KILL_EFFECT, Icon.SHIELD_FILL),
    HIT_EFFECT(TranslateText.COSMETICS_TAB_HIT_EFFECT, Icon.ACTIVITY),
    TRAIL(TranslateText.COSMETICS_TAB_TRAIL, Icon.MOVE),
    LANDING(TranslateText.COSMETICS_TAB_LANDING, Icon.DOWNLOAD),
    AMBIENCE(TranslateText.COSMETICS_TAB_AMBIENCE, Icon.CLOUD_LIGHTING),
    PARTICLES(TranslateText.COSMETICS_TAB_PARTICLES, Icon.STAR);

    private final TranslateText name;
    private final String icon;

    CosmeticsTab(TranslateText name, String icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name.getText();
    }

    public String getIcon() {
        return icon;
    }
}
