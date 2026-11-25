package mix.client.mixui.integration.haunt.gui;

import mix.client.mixui.integration.haunt.gui.layout.SectionLayouts;
import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.setting.SettingStyle;

public final class MenuNav {
   static MenuNav.Host host;

   public static boolean hasPage(Module m, String key) {
      return SectionLayouts.of(m).page(key) != null;
   }

   public static boolean openPage(Module m, String key) {
      return host != null && host.openPage(m, key);
   }

   public static boolean openStylePage(Module m, SettingStyle style) {
      return host != null && host.openStylePage(m, style);
   }

   interface Host {
      boolean openPage(Module var1, String var2);

      boolean openStylePage(Module var1, SettingStyle var2);
   }
}
