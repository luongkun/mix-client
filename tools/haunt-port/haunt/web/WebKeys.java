package mix.client.mixui.integration.haunt.web;

import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.web.WebValues;

public final class WebKeys {
   public static final String GENERAL = "settings.";
   public static final String ACTIVE = "active";

   public static String section(Module m) {
      return WebValues.id(m.getId());
   }

   public static String key(Module m, Setting s) {
      return (m == null?"settings.":section(m) + ".") + s.getId();
   }
}
