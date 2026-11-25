package mix.client.mixui.integration.haunt.mixin;

import java.lang.reflect.Field;
import mix.client.mixui.integration.haunt.hook.Hooks;

final class Access {
   static Field field(Class<?> owner, String mcp, String srg) {
      return Hooks.field(owner, new String[]{mcp, srg});
   }

   static int getInt(Field f, Object o) {
      try {
         return f.getInt(o);
      } catch (Exception var3) {
         return 0;
      }
   }

   static void setInt(Field f, Object o, int v) {
      try {
         f.setInt(o, v);
      } catch (Exception var4) {
         ;
      }

   }

   static Object get(Field f, Object o) {
      try {
         return f.get(o);
      } catch (Exception var3) {
         return null;
      }
   }
}
