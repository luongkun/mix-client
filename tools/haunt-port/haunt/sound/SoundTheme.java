package mix.client.mixui.integration.haunt.sound;

import java.util.Locale;
import mix.client.mixui.integration.haunt.setting.Displayable;

public enum SoundTheme implements Displayable {
   SOFT("Soft", "Warm, rounded plops and chimes"),
   GLASS("Glass", "Bright crystal pings and bells"),
   MINIMAL("Minimal", "Tiny dry clicks"),
   CREAMY("Creamy", "Deep, soft keystrokes, like creamy mechanical switches"),
   HAUNT("Haunt", "Music-box chimes, low bells and a ghostly breath: HAUN’T\'s own sounds");

   private final String displayName;
   private final String description;

   private SoundTheme(String displayName, String description) {
      this.displayName = displayName;
      this.description = description;
   }

   public String displayName() {
      return this.displayName;
   }

   public String description() {
      return this.description;
   }

   public String dir() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   // $FF: synthetic method
   private static SoundTheme[] $values() {
      return new SoundTheme[]{SOFT, GLASS, MINIMAL, CREAMY, HAUNT};
   }
}
