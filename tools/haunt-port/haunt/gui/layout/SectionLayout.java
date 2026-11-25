package mix.client.mixui.integration.haunt.gui.layout;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mix.client.mixui.integration.haunt.gui.layout.LayoutEntry;
import mix.client.mixui.integration.haunt.gui.layout.LayoutGroup;
import mix.client.mixui.integration.haunt.gui.layout.LayoutPage;
import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SettingStyle;

public final class SectionLayout {
   public final Module module;
   public final List<LayoutPage> pages = new ArrayList();
   final Map<Setting, LayoutEntry> entries = new IdentityHashMap();
   public final Map<Setting, String> noRow = new LinkedHashMap();
   public final List<Setting> unplanned = new ArrayList();
   final Map<Setting, String> renamed = new LinkedHashMap();

   SectionLayout(Module module) {
      this.module = module;
   }

   public LayoutPage sectionPage() {
      return (LayoutPage)this.pages.get(0);
   }

   public LayoutPage page(String key) {
      for(LayoutPage p : this.pages) {
         if(p.key.equals(key)) {
            return p;
         }
      }

      return null;
   }

   public LayoutPage stylePage(SettingStyle style) {
      for(LayoutPage p : this.pages) {
         if(style != null && (p.style == style || p.tab == style)) {
            return p;
         }
      }

      return null;
   }

   public List<LayoutPage> tabPages() {
      ArrayList<LayoutPage> out = new ArrayList();

      for(LayoutPage p : this.pages) {
         if(p.tab != null) {
            out.add(p);
         }
      }

      return out;
   }

   public boolean tabbed() {
      for(LayoutPage p : this.pages) {
         if(p.tab != null) {
            return true;
         }
      }

      return false;
   }

   public LayoutPage firstPage() {
      for(LayoutPage p : this.pages) {
         if(p.tab != null) {
            return p;
         }
      }

      return (LayoutPage)this.pages.get(0);
   }

   public LayoutEntry entry(Setting s) {
      return (LayoutEntry)this.entries.get(s);
   }

   public List<LayoutEntry> allEntries() {
      ArrayList<LayoutEntry> out = new ArrayList();

      for(LayoutPage p : this.pages) {
         for(LayoutGroup g : p.groups) {
            out.addAll(g.entries);
         }
      }

      return out;
   }

   public Map<Setting, String> renamed() {
      return this.renamed;
   }
}
