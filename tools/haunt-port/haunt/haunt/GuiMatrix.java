package mix.client.mixui.integration.haunt.haunt;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public final class GuiMatrix {
   private static final int GL_MODELVIEW_MATRIX = 2982;
   private static final FloatBuffer BUF = BufferUtils.createFloatBuffer(16);
   private static final float[] OUT = new float[3];

   public static float[] translation() {
      try {
         BUF.clear();
         GL11.glGetFloat(2982, BUF);
         float sx = BUF.get(0);
         float sy = BUF.get(5);
         float tx = BUF.get(12);
         float ty = BUF.get(13);
         if(!finite(sx) || Math.abs(sx) < 1.0E-4F) {
            sx = 1.0F;
         }

         if(!finite(sy) || Math.abs(sy) < 1.0E-4F) {
            sy = 1.0F;
         }

         OUT[0] = finite(tx)?tx / sx:0.0F;
         OUT[1] = finite(ty)?ty / sy:0.0F;
         OUT[2] = sx;
      } catch (Throwable var4) {
         OUT[0] = 0.0F;
         OUT[1] = 0.0F;
         OUT[2] = 1.0F;
      }

      return OUT;
   }

   private static boolean finite(float f) {
      return !Float.isNaN(f) && !Float.isInfinite(f);
   }
}
