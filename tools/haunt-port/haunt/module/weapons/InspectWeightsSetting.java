package mix.client.mixui.integration.haunt.module.weapons;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.weapons.InspectWeightsWidget;
import mix.client.mixui.integration.haunt.module.weapons.WeaponDef;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.Setting;

public final class InspectWeightsSetting extends Setting implements HasWidget {
   public static final int MAX_WEIGHT = 3;
   private final List<String> inspects;
   private final Map<String, Integer> weights = new LinkedHashMap();
   WeaponsModule module;
   WeaponDef def;

   InspectWeightsSetting(String id, String name, String description, List<String> inspects) {
      super(id, name, description);
      this.inspects = Collections.unmodifiableList(new ArrayList(inspects));

      for(String s : inspects) {
         this.weights.put(s, Integer.valueOf(1));
      }

   }

   public List<String> inspects() {
      return this.inspects;
   }

   public int weight(String inspect) {
      Integer w = (Integer)this.weights.get(inspect);
      return w == null?0:w.intValue();
   }

   public void setWeight(String inspect, int weight) {
      if(this.weights.containsKey(inspect)) {
         int w = Math.max(0, Math.min(3, weight));
         if(w != ((Integer)this.weights.get(inspect)).intValue()) {
            this.weights.put(inspect, Integer.valueOf(w));
            this.fireChanged();
         }
      }

   }

   public List<String> enabled() {
      ArrayList<String> out = new ArrayList();

      for(String s : this.inspects) {
         if(this.weight(s) > 0) {
            out.add(s);
         }
      }

      return out;
   }

   public boolean showInMenu() {
      return true;
   }

   public Widget createWidget() {
      return new InspectWeightsWidget(this.module, this, this.def);
   }

   public void reset() {
      boolean changed = false;

      for(String s : this.inspects) {
         if(((Integer)this.weights.put(s, Integer.valueOf(1))).intValue() != 1) {
            changed = true;
         }
      }

      if(changed) {
         this.fireChanged();
      }

   }

   public JsonElement toJson() {
      JsonObject o = new JsonObject();

      for(String s : this.inspects) {
         o.addProperty(s, Integer.valueOf(this.weight(s)));
      }

      return o;
   }

   public void fromJson(JsonElement json) {
      if(json != null && json.isJsonObject()) {
         boolean changed = false;

         for(Entry e : json.getAsJsonObject().entrySet()) {
            if(this.weights.containsKey(e.getKey())) {
               try {
                  int w = Math.max(0, Math.min(3, ((JsonElement)e.getValue()).getAsInt()));
                  if(w != ((Integer)this.weights.get(e.getKey())).intValue()) {
                     this.weights.put((String)e.getKey(), Integer.valueOf(w));
                     changed = true;
                  }
               } catch (RuntimeException var6) {
                  ;
               }
            }
         }

         if(changed) {
            this.fireChanged();
         }
      }

   }
}
