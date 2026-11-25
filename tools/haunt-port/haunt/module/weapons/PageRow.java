package mix.client.mixui.integration.haunt.module.weapons;

import java.util.function.Supplier;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.setting.WidgetSetting;

final class PageRow extends WidgetSetting {
   private final Supplier<Widget> widget;

   PageRow(String id, String name, String description, Supplier<Widget> widget) {
      super(id, name, description);
      this.widget = widget;
   }

   public boolean showInMenu() {
      return true;
   }

   public Widget createWidget() {
      return (Widget)this.widget.get();
   }
}
