package mix.client.mixui.integration.haunt.module.weapons;

import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.weapons.StylePickerWidget;
import mix.client.mixui.integration.haunt.module.weapons.WeaponOptions;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.ModeSetting;

public final class WeaponStyleSetting extends ModeSetting<WeaponOptions.WeaponStyle> implements HasWidget {
   private final WeaponsModule module;

   WeaponStyleSetting(WeaponsModule module, String id, String name, String description) {
      super(id, name, description, WeaponOptions.WeaponStyle.class, WeaponOptions.WeaponStyle.HD);
      this.module = module;
   }

   public Widget createWidget() {
      return new StylePickerWidget(this.module, this);
   }
}
