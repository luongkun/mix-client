package mix.client.mixui.integration.haunt.fx.physics;

public final class PhysicsClock {
   public static final double STEP = 0.008333333333333333D;
   private static final int MAX_STEPS = 12;
   private double acc;

   public int advance(double dt) {
      this.acc += Math.max(0.0D, Math.min(0.25D, dt));
      int n = (int)(this.acc / 0.008333333333333333D);
      if(n > 12) {
         n = 12;
         this.acc = 0.0D;
      } else {
         this.acc -= (double)n * 0.008333333333333333D;
      }

      return n;
   }

   public double alpha() {
      return Math.max(0.0D, Math.min(1.0D, this.acc / 0.008333333333333333D));
   }
}
