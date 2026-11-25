package mix.client.mixui.integration.haunt.setting;

public interface Displayable {
   String displayName();

   String description();

   default String tag() {
      return null;
   }

   default String[] keywords() {
      return null;
   }

   default boolean isNew() {
      return false;
   }
}
