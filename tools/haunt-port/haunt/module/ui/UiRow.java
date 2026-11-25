package mix.client.mixui.integration.haunt.module.ui;

import java.util.function.Supplier;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.ui.RowWidget;
import mix.client.mixui.integration.haunt.setting.WidgetSetting;

final class UiRow extends WidgetSetting {
   private final Supplier<Widget> widget;

   UiRow(String id, String name, String description, Supplier<Widget> widget) {
      super(id, name, description);
      this.widget = widget;
   }

   public Widget createWidget() {
      Widget w = (Widget)this.widget.get();
      if(w instanceof RowWidget) {
         ((RowWidget)w).row = this;
      }

      return w;
   }
}
