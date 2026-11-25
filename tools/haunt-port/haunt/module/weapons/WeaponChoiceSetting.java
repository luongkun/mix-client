package mix.client.mixui.integration.haunt.module.weapons;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.weapons.WeaponChoiceWidget;
import mix.client.mixui.integration.haunt.module.weapons.WeaponDef;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.setting.Choices;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.Setting;

public final class WeaponChoiceSetting extends Setting implements HasWidget, Choices {
   public static final String SAME = "same";
   private final String defaultValue;
   private String value;
   WeaponsModule module;
   WeaponDef def;

   WeaponChoiceSetting(String id, String name, String description, String defaultValue) {
      super(id, name, description);
      this.defaultValue = defaultValue;
      this.value = defaultValue;
   }

   public String get() {
      return this.value;
   }

   public void set(String v) {
      if(v != null && !v.equals(this.value)) {
         this.value = v;
         this.fireChanged();
      }

   }

   public List<String[]> choices() {
      ArrayList<String[]> out = new ArrayList();
      out.add(new String[]{"same", "Same as the right hand"});
      if(this.module != null) {
         for(WeaponDef d : this.module.cardWeapons()) {
            if(!d.twoHanded) {
               out.add(new String[]{d.id, d.name});
            }
         }
      }

      return out;
   }

   public boolean showInMenu() {
      return true;
   }

   public Widget createWidget() {
      return new WeaponChoiceWidget(this.module, this, this.def);
   }

   public void reset() {
      this.set(this.defaultValue);
   }

   public JsonElement toJson() {
      return new JsonPrimitive(this.value);
   }

   public void fromJson(JsonElement json) {
      if(json != null && json.isJsonPrimitive()) {
         this.set(json.getAsString());
      }

   }
}
