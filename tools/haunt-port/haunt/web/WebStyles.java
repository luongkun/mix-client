package mix.client.mixui.integration.haunt.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.module.ambience.AmbienceModule;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.module.weapons.WeaponDef;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.web.WebSchema;
import mix.client.mixui.integration.haunt.web.WebValues;

public final class WebStyles {
   static ModeSetting<?> picker(Module m, Object tab, List<Setting> settings) {
      Iterator var3 = settings.iterator();

      Setting s;
      while(true) {
         if(!var3.hasNext()) {
            return null;
         }

         s = (Setting)var3.next();
         if(s instanceof ModeSetting && s.getOwner() == tab) {
            if(m instanceof AmbienceModule) {
               if(s.getId().equals("editing")) {
                  break;
               }
            } else if(((ModeSetting)s).getStyle() == ModeSetting.Style.CARDS) {
               break;
            }
         }
      }

      return (ModeSetting)s;
   }

   public static List<WebStyles.Card> cards(Module m) {
      ArrayList<WebStyles.Card> out = new ArrayList();
      if(m instanceof WeaponsModule) {
         for(WeaponDef d : ((WeaponsModule)m).cardWeapons()) {
            out.add(new WebStyles.Card(WebValues.id(d.id), d.name, (Enum)null, d.id));
         }

         return out;
      } else {
         ModeSetting<?> pick = picker(m, (Object)null, m.getSettings());
         if(pick == null) {
            return Collections.emptyList();
         } else {
            for(Enum c : pick.getValues()) {
               out.add(new WebStyles.Card(WebValues.id(c.name()), ModeSetting.displayName(c), c, (String)null));
            }

            return out;
         }
      }
   }

   public static void select(Module m, WebStyles.Card c) {
      ModeSetting<?> pick = c.choice == null?null:picker(m, (Object)null, m.getSettings());
      if(pick != null) {
         pick.setIndex(c.choice.ordinal());
         if(m instanceof TrailsModule || m instanceof AmbienceModule) {
            for(Enum v : pick.getValues()) {
               BooleanSetting sw = WebSchema.styleSwitch(m, v);
               if(sw != null) {
                  sw.set(v == c.choice);
               }
            }
         }
      }

   }

   public static final class Card {
      public final String id;
      public final String label;
      public final Enum<?> choice;
      public final String weapon;

      Card(String id, String label, Enum<?> choice, String weapon) {
         this.id = id;
         this.label = label;
         this.choice = choice;
         this.weapon = weapon;
      }
   }
}
