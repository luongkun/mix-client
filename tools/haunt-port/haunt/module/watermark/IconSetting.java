package mix.client.mixui.integration.haunt.module.watermark;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.watermark.IconPickerWidget;
import mix.client.mixui.integration.haunt.module.watermark.IslandIcons;
import mix.client.mixui.integration.haunt.setting.Choices;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;

final class IconSetting extends Setting implements HasWidget, Choices {
   private final IslandIcons.Icon[] choices;
   private final IslandIcons.Icon defaultIcon;
   private final Map<IslandIcons.Icon, String> tips = new EnumMap(IslandIcons.Icon.class);
   private IslandIcons.Icon value;

   IconSetting(String id, String name, String description, IslandIcons.Icon defaultIcon, IslandIcons.Icon... choices) {
      super(id, name, description);
      this.choices = choices;
      this.defaultIcon = defaultIcon;
      this.value = defaultIcon;
   }

   IconSetting tip(IslandIcons.Icon icon, String text) {
      this.tips.put(icon, text);
      return this;
   }

   IslandIcons.Icon get() {
      return this.value;
   }

   void set(IslandIcons.Icon icon) {
      if(icon != this.value && (icon == null || this.allows(icon))) {
         this.value = icon;
         this.fireChanged();
      }

   }

   IslandIcons.Icon[] icons() {
      return this.choices;
   }

   public List<String[]> choices() {
      ArrayList<String[]> out = new ArrayList();
      out.add(new String[]{"NONE", "No icon"});

      for(IslandIcons.Icon c : this.choices) {
         out.add(new String[]{c.name(), ModeSetting.displayName(c)});
      }

      return out;
   }

   private boolean allows(IslandIcons.Icon icon) {
      for(IslandIcons.Icon c : this.choices) {
         if(c == icon) {
            return true;
         }
      }

      return false;
   }

   String describe(IslandIcons.Icon icon) {
      if(icon == null) {
         return "No icon";
      } else {
         String tip = (String)this.tips.get(icon);
         return tip != null?tip:ModeSetting.displayName(icon);
      }
   }

   public Widget createWidget() {
      return new IconPickerWidget(this);
   }

   public void reset() {
      this.set(this.defaultIcon);
   }

   public JsonElement toJson() {
      return new JsonPrimitive(this.value == null?"NONE":this.value.name());
   }

   public void fromJson(JsonElement json) {
      if(json != null && json.isJsonPrimitive()) {
         String name = json.getAsString();
         if("NONE".equalsIgnoreCase(name)) {
            this.set((IslandIcons.Icon)null);
         } else {
            for(IslandIcons.Icon c : this.choices) {
               if(c.name().equalsIgnoreCase(name)) {
                  this.set(c);
                  return;
               }
            }
         }
      }

   }
}
