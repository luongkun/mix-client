package mix.client.mixui.integration.haunt.setting;

import mix.client.mixui.integration.haunt.gui.widget.NoteWidget;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.setting.WidgetSetting;

public final class NoteSetting extends WidgetSetting {
   public NoteSetting(String id, String text) {
      super(id, "", text);
   }

   public Widget createWidget() {
      return new NoteWidget(this);
   }
}
