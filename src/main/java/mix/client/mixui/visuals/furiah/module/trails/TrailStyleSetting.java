package mix.client.mixui.visuals.furiah.module.trails;

import mix.client.mixui.visuals.furiah.gui.widget.Widget;
import mix.client.mixui.visuals.furiah.setting.HasWidget;
import mix.client.mixui.visuals.furiah.setting.ModeSetting;

final class TrailStyleSetting extends ModeSetting<TrailStyle> implements HasWidget {
    private final TrailsModule module;

    TrailStyleSetting(TrailsModule module) {
        super("style", "Style", "The look of the trail: click a card for its settings (with Combine Styles, its switch runs it).",
                TrailStyle.class, TrailStyle.RIBBON);
        this.module = module;
        style(ModeSetting.Style.CARDS);
    }

    @Override
    public Widget createWidget() {
        return new TrailCardsWidget(module);
    }
}
