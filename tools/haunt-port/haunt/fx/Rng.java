package mix.client.mixui.integration.haunt.fx;

public final class Rng {
   private int state = 1;

   public Rng seed(int seed) {
      this.state = mix(seed);
      if(this.state == 0) {
         this.state = 1831565813;
      }

      return this;
   }

   public Rng seed(int a, int b) {
      return this.seed(a * -1640531535 + b * -2048144789);
   }

   public int nextInt() {
      int x = this.state;
      x = x ^ x << 13;
      x = x ^ x >>> 17;
      x = x ^ x << 5;
      this.state = x;
      return x;
   }

   public float next() {
      return (float)(this.nextInt() >>> 8) / 1.6777216E7F;
   }

   public float range(float min, float max) {
      return min + (max - min) * this.next();
   }

   public float signed() {
      return this.next() * 2.0F - 1.0F;
   }

   public int nextInt(int bound) {
      return Math.min(bound - 1, (int)(this.next() * (float)bound));
   }

   public int between(int min, int max) {
      return min + this.nextInt(max - min + 1);
   }

   public static int mix(int h) {
      h = h ^ h >>> 16;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      h = h * -1028477387;
      return h ^ h >>> 16;
   }

   public static float hash(int a, int b) {
      return (float)(mix(a * 668265261 ^ (b + 374761393) * -1640531535) >>> 8) / 1.6777216E7F;
   }
}
