package mix.client.mixui.integration.haunt.fx.look;

public final class LightBoost {
   private static volatile float amount;

   public static void set(float a) {
      amount = Math.max(0.0F, Math.min(1.0F, a));
   }

   public static float amount() {
      return amount;
   }

   public static float lift(float light) {
      float a = amount;
      return a <= 0.0F?light:light + (1.0F - light) * a;
   }
}
