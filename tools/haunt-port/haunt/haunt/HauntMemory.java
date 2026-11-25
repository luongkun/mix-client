package mix.client.mixui.integration.haunt.haunt;

import java.util.concurrent.Semaphore;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GLContext;

public final class HauntMemory {
   public static final Semaphore HEAVY_DECODE = new Semaphore(1, true);
   private static volatile int glMax;
   private static volatile int degrade;

   public static int textureCap(int wanted) {
      long max = Runtime.getRuntime().maxMemory();
      int cap = wanted;
      if(max < 1153433600L) {
         cap = Math.min(wanted, 512);
      } else if(max < 1887436800L) {
         cap = Math.min(wanted, 1024);
      }

      cap = cap >> Math.min(2, degrade);
      int g = glMax;
      if(g > 0) {
         cap = Math.min(cap, g);
      }

      return Math.max(256, cap);
   }

   public static void degrade() {
      if(degrade < 2) {
         ++degrade;
      }

   }

   public static int degradeLevel() {
      return degrade;
   }

   public static void safeUnpackState() {
      try {
         if(glMax == 0) {
            int m = GL11.glGetInteger(3379);
            glMax = m > 0?m:4096;
         }

         GL11.glPixelStorei(3314, 0);
         GL11.glPixelStorei(3315, 0);
         GL11.glPixelStorei(3316, 0);
         GL11.glPixelStorei(3317, 4);
         if(GLContext.getCapabilities().OpenGL21) {
            GL15.glBindBuffer('裬', 0);
         }
      } catch (Throwable var1) {
         ;
      }

   }
}
