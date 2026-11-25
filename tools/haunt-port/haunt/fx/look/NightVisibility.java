package mix.client.mixui.integration.haunt.fx.look;

import mix.client.mixui.integration.haunt.setting.Displayable;

public enum NightVisibility implements Displayable {
   AUTO("Auto", "Only where it\'s dark"),
   ON("On", "Always"),
   OFF("Off", "Never");

   private final String displayName;
   private final String description;

   private NightVisibility(String displayName, String description) {
      this.displayName = displayName;
      this.description = description;
   }

   public String displayName() {
      return this.displayName;
   }

   public String description() {
      return this.description;
   }
}
