package mix.client.mixui.integration.haunt.module.targetaura;

import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.targetaura.AuraCardsWidget;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.ModeSetting;

final class AuraEffectSetting extends ModeSetting<EffectType> implements HasWidget {
   private final TargetAuraModule module;

   AuraEffectSetting(TargetAuraModule module) {
      super("effect", "Effect", "The look of the aura.", EffectType.class, EffectType.ORBIT_RINGS);
      this.module = module;
      this.style(ModeSetting.Style.CARDS);
   }

   public Widget createWidget() {
      return new AuraCardsWidget(this.module);
   }
}
