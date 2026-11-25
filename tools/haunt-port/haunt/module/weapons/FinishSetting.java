package mix.client.mixui.integration.haunt.module.weapons;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.weapons.FinishWidget;
import mix.client.mixui.integration.haunt.module.weapons.WeaponOptions;
import mix.client.mixui.integration.haunt.setting.Choices;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;

public final class FinishSetting extends Setting implements HasWidget, Choices {
   private final List<WeaponOptions.Finish> options;
   private final WeaponOptions.Finish defaultValue;
   private WeaponOptions.Finish value;

   FinishSetting(String id, String name, String description, List<String> finishes, String start) {
      super(id, name, description);
      ArrayList<WeaponOptions.Finish> list = new ArrayList();

      for(String s : finishes) {
         WeaponOptions.Finish f = WeaponOptions.Finish.of(s);
         if(f != null && f != WeaponOptions.Finish.CUSTOM && !list.contains(f)) {
            list.add(f);
         }
      }

      if(list.isEmpty()) {
         list.add(WeaponOptions.Finish.STEEL_POLISHED);
      }

      list.add(WeaponOptions.Finish.CUSTOM);
      this.options = Collections.unmodifiableList(list);
      WeaponOptions.Finish d = WeaponOptions.Finish.of(start);
      this.value = this.defaultValue = d != null && list.contains(d)?d:(WeaponOptions.Finish)list.get(0);
   }

   public List<WeaponOptions.Finish> options() {
      return this.options;
   }

   public List<String[]> choices() {
      ArrayList<String[]> out = new ArrayList();

      for(WeaponOptions.Finish f : this.options) {
         out.add(new String[]{f.name(), ModeSetting.displayName(f)});
      }

      return out;
   }

   public WeaponOptions.Finish get() {
      return this.value;
   }

   public WeaponOptions.Finish getDefault() {
      return this.defaultValue;
   }

   public void set(WeaponOptions.Finish f) {
      if(f != null && f != this.value && this.options.contains(f)) {
         this.value = f;
         this.fireChanged();
      }

   }

   public boolean showInMenu() {
      return true;
   }

   public Widget createWidget() {
      return new FinishWidget(this);
   }

   public void reset() {
      this.set(this.defaultValue);
   }

   public JsonElement toJson() {
      return new JsonPrimitive(this.value.name());
   }

   public void fromJson(JsonElement json) {
      if(json != null && json.isJsonPrimitive()) {
         this.set(WeaponOptions.Finish.of(json.getAsString()));
      }

   }
}
