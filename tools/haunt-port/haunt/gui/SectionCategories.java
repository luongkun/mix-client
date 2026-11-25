package mix.client.mixui.integration.haunt.gui;

import java.util.ArrayList;
import java.util.List;
import mix.client.mixui.integration.haunt.module.Module;

public final class SectionCategories {
   public static final String[] NAMES = new String[]{"Combat", "Items", "World", "HUD", "Performance", "Other"};

   public static String of(Module m) {
      String id = m.getId();
      return "fps_optimization".equals(id)?"Performance":(!"target_aura".equals(id) && !"hit_effect".equals(id) && !"kill_effects".equals(id) && !"landing_impact".equals(id) && !"trails".equals(id) && !"particles".equals(id)?(!"weapons".equals(id) && !"glow".equals(id)?(!"ambience".equals(id) && !"dynamic_skies".equals(id) && !"motion_blur".equals(id)?(!"watermark".equals(id) && !"ui".equals(id)?"Other":"HUD"):"World"):"Items"):"Combat");
   }

   public static List<Module> in(String category, List<Module> modules) {
      ArrayList<Module> out = new ArrayList();

      for(Module m : modules) {
         if(of(m).equals(category)) {
            out.add(m);
         }
      }

      return out;
   }

   public static List<Module> ordered(List<Module> modules) {
      ArrayList<Module> out = new ArrayList();

      for(String c : NAMES) {
         out.addAll(in(c, modules));
      }

      return out;
   }
}
