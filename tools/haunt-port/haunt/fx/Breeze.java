package mix.client.mixui.integration.haunt.fx;

import mix.client.mixui.integration.haunt.core.ClientClock;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.core.GameClock;
import mix.client.mixui.integration.haunt.module.ambience.AmbienceModule;

public final class Breeze {
   public static final float GENTLE = 0.55F;

   public static float[] at(double x, double y, double z, boolean preview, float[] out) {
      if(!preview) {
         AmbienceModule ambience = FuriahsVisuals.instance == null?null:FuriahsVisuals.instance.getAmbience();
         if(ambience != null && ambience.windAt(x, y, z, out)) {
            return out;
         }
      }

      gentle(preview?ClientClock.seconds():GameClock.seconds(), out);
      return out;
   }

   public static void gentle(float[] out) {
      gentle(GameClock.seconds(), out);
   }

   private static void gentle(double t, float[] out) {
      double heading = 0.6D + 0.5D * Math.sin(t * 0.019D) + 0.25D * Math.sin(t * 0.053D + 1.3D);
      double gust = 1.0D + 0.25D * Math.sin(t * 0.41D) * Math.sin(t * 0.17D + 0.7D);
      out[0] = (float)(Math.cos(heading) * 0.550000011920929D * gust);
      out[1] = 0.0F;
      out[2] = (float)(Math.sin(heading) * 0.550000011920929D * gust);
   }
}
