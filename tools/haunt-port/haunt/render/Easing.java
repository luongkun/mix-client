package mix.client.mixui.integration.haunt.render;

public final class Easing {
   public static boolean instant;

   public static float clamp01(float t) {
      return t < 0.0F?0.0F:(t > 1.0F?1.0F:t);
   }

   public static float clamp01(double t) {
      return t < 0.0D?0.0F:(t > 1.0D?1.0F:(float)t);
   }

   public static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   public static float progress(double t, double start, double end) {
      return clamp01((t - start) / (end - start));
   }

   public static float inQuad(float t) {
      return t * t;
   }

   public static float outQuad(float t) {
      return 1.0F - (1.0F - t) * (1.0F - t);
   }

   public static float inCubic(float t) {
      return t * t * t;
   }

   public static float outCubic(float t) {
      float u = 1.0F - t;
      return 1.0F - u * u * u;
   }

   public static float inOutCubic(float t) {
      if(t < 0.5F) {
         return 4.0F * t * t * t;
      } else {
         float u = -2.0F * t + 2.0F;
         return 1.0F - u * u * u / 2.0F;
      }
   }

   public static float outQuart(float t) {
      float u = 1.0F - t;
      return 1.0F - u * u * u * u;
   }

   public static float outQuint(float t) {
      float u = 1.0F - t;
      return 1.0F - u * u * u * u * u;
   }

   public static float outExpo(float t) {
      return t >= 1.0F?1.0F:1.0F - (float)Math.pow(2.0D, (double)(-10.0F * t));
   }

   public static float inOutSine(float t) {
      return (float)(-(Math.cos(3.141592653589793D * (double)t) - 1.0D) / 2.0D);
   }

   public static float outBack(float t) {
      return outBack(t, 1.70158F);
   }

   public static float outBack(float t, float overshoot) {
      float u = t - 1.0F;
      return 1.0F + (overshoot + 1.0F) * u * u * u + overshoot * u * u;
   }

   public static float smoothstep(float edge0, float edge1, float x) {
      float t = clamp01((x - edge0) / (edge1 - edge0));
      return t * t * (3.0F - 2.0F * t);
   }

   public static float approach(float current, float target, float rate, float dt) {
      return instant?target:current + (target - current) * (1.0F - (float)Math.exp((double)(-rate * dt)));
   }
}
