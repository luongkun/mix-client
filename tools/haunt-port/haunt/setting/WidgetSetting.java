package mix.client.mixui.integration.haunt.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.Setting;

public abstract class WidgetSetting extends Setting implements HasWidget {
   protected WidgetSetting(String id, String name, String description) {
      super(id, name, description);
   }

   public boolean isSaved() {
      return false;
   }

   public void reset() {
   }

   public JsonElement toJson() {
      return JsonNull.INSTANCE;
   }

   public void fromJson(JsonElement json) {
   }
}
