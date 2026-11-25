package mix.client.mixui.integration.haunt.module.trails;

import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.trails.TrailCardsWidget;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.ModeSetting;

final class TrailStyleSetting extends ModeSetting<TrailStyle> implements HasWidget {
   private final TrailsModule module;

   TrailStyleSetting(TrailsModule module) {
      super("style", "Style", "The look of the trail: click a card for its settings (with Combine Styles, its switch runs it).", TrailStyle.class, TrailStyle.RIBBON);
      this.module = module;
      this.style(ModeSetting.Style.CARDS);
   }

   public Widget createWidget() {
      return new TrailCardsWidget(this.module);
   }
}
