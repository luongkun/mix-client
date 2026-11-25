package mix.client.mixui.integration.haunt.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import mix.client.mixui.integration.haunt.mixin.Access;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

public final class AccessorMinecraft {
   private static final Field TIMER = timerField();

   private static Field timerField() {
      Field f = Access.field(Minecraft.class, "timer", "timer");
      if(f != null) {
         return f;
      } else {
         for(Field c : Minecraft.class.getDeclaredFields()) {
            if(c.getType() == Timer.class && !Modifier.isStatic(c.getModifiers())) {
               c.setAccessible(true);
               return c;
            }
         }

         return null;
      }
   }

   public static boolean available() {
      return TIMER != null;
   }

   public static Timer getFvTimer(Minecraft mc) {
      return TIMER == null?null:(Timer)Access.get(TIMER, mc);
   }
}
