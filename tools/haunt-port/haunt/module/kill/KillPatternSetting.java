package mix.client.mixui.integration.haunt.module.kill;

import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.kill.KillPatternWidget;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.StringListSetting;

final class KillPatternSetting extends StringListSetting implements HasWidget {
   static final int MAX_LENGTH = 120;

   KillPatternSetting(String id, String name, String description, String... defaults) {
      super(id, name, description, defaults);
   }

   public boolean showInMenu() {
      return true;
   }

   public Widget createWidget() {
      return new KillPatternWidget(this);
   }

   static String problem(String pattern) {
      String p = pattern.trim();
      return p.isEmpty()?"Type a message first":(!p.contains("{victim}")?"A pattern needs {victim} where the name of the player who died appears":(p.indexOf("{victim}") != p.lastIndexOf("{victim}")?"Use {victim} only once":(p.replace("{victim}", "").replace("{me}", "").replace("*", "").trim().length() < 3?"Add some of the message\'s words, or it matches everything":null)));
   }
}
