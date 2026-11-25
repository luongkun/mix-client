package mix.client.mixui.integration.haunt.module.ui.crosshair;

import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.ui.crosshair.CrosshairCardsWidget;
import mix.client.mixui.integration.haunt.module.ui.crosshair.CrosshairElement;
import mix.client.mixui.integration.haunt.module.ui.crosshair.CrosshairStyle;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.ModeSetting;

final class CrosshairStyleSetting extends ModeSetting<CrosshairStyle> implements HasWidget {
   private final CrosshairElement element;

   CrosshairStyleSetting(CrosshairElement element) {
      super("crosshair.style", "Style", "What the crosshair looks like: click a card.", CrosshairStyle.class, CrosshairStyle.CLASSIC);
      this.element = element;
      this.style(ModeSetting.Style.CARDS);
   }

   public Widget createWidget() {
      return new CrosshairCardsWidget(this.element, this);
   }
}
