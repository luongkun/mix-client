package mix.client.mixui.integration.haunt.gui.layout;

import java.util.ArrayList;
import java.util.List;
import mix.client.mixui.integration.haunt.gui.layout.LayoutGroup;
import mix.client.mixui.integration.haunt.module.MiniSection;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SettingStyle;

public final class LayoutPage {
   public final String key;
   public final String title;
   public final LayoutPage parent;
   public final SettingStyle style;
   public final BooleanSetting styleSwitch;
   public final String description;
   public final MiniSection tab;
   public final List<LayoutGroup> groups;
   boolean headerless;

   LayoutPage(String key, String title, LayoutPage parent, SettingStyle style, BooleanSetting styleSwitch, String description) {
      this(key, title, parent, style, styleSwitch, description, (MiniSection)null);
   }

   LayoutPage(String key, String title, LayoutPage parent, SettingStyle style, BooleanSetting styleSwitch, String description, MiniSection tab) {
      this.groups = new ArrayList();
      this.key = key;
      this.title = title;
      this.parent = parent;
      this.style = style;
      this.styleSwitch = styleSwitch;
      this.description = description;
      this.tab = tab;
   }

   public boolean isSection() {
      return this.parent == null;
   }

   public boolean isTab() {
      return this.tab != null;
   }

   public LayoutPage tabPage() {
      for(LayoutPage p = this; p != null; p = p.parent) {
         if(p.tab != null) {
            return p;
         }
      }

      return null;
   }

   public String path() {
      if(this.parent == null) {
         return null;
      } else {
         String up = this.parent.path();
         return up == null?this.title:up + " › " + this.title;
      }
   }

   public List<Setting> settings() {
      ArrayList<Setting> out = new ArrayList();

      for(LayoutGroup g : this.groups) {
         out.addAll(g.members());
      }

      return out;
   }

   public int count() {
      return this.settings().size();
   }

   public String toString() {
      return this.key.isEmpty()?"(section)":this.key;
   }
}
