package mix.client.mixui.integration.haunt.gui.layout;

public enum Slot {
   MAIN("Main"),
   COLORS("Colors"),
   SIZE("Size & strength"),
   MOTION("Motion"),
   WHEN("When it shows"),
   OPTIONS("Style options"),
   FEATURE((String)null),
   EXTRAS("Extras"),
   ADVANCED("Advanced");

   final String title;

   private Slot(String title) {
      this.title = title;
   }
}
