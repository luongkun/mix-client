package mix.client.mixui.integration.haunt.module.sky;

import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.sky.SkyCardsWidget;
import mix.client.mixui.integration.haunt.module.sky.SkyModule;
import mix.client.mixui.integration.haunt.module.sky.SkyStyle;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.ModeSetting;

public final class SkyStyleSetting extends ModeSetting<SkyStyle> implements HasWidget {
   private final SkyModule module;

   SkyStyleSetting(SkyModule module) {
      super("style", "Sky", "The sky you see: click a card (it crossfades in).", SkyStyle.class, SkyStyle.MOLTEN_MARBLE);
      this.module = module;
      this.style(ModeSetting.Style.CARDS);
   }

   public Widget createWidget() {
      return new SkyCardsWidget(this.module, this);
   }
}
