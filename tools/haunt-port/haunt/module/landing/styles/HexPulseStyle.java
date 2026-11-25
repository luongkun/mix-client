package mix.client.mixui.integration.haunt.module.landing.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;

public final class HexPulseStyle extends LandingStyleBase {
   public final BooleanSetting honeycomb = (BooleanSetting)this.setting(new BooleanSetting("hexpulse.honeycomb", "Honeycomb", "Six smaller hexagons light up around it.", true));
   public final ModeSetting<HexPulseStyle.Align> align = (ModeSetting)this.setting(new ModeSetting("hexpulse.align", "Align", "Which way the hexagon points.", HexPulseStyle.Align.class, HexPulseStyle.Align.FACING));
   private static final int MAX_HEXES = 7;
   private final double[] hexX = new double[7];
   private final double[] hexZ = new double[7];
   private final double[] hexR = new double[7];
   private final float[] hexA = new float[7];
   private final float[] hexT = new float[7];
   private final float[] hexW = new float[7];
   private int hexes;

   public HexPulseStyle(LandingImpactModule module) {
      super(module, LandingStyle.HEX_PULSE, 1.0F, 2.0F, new ColorSetting("hexpulse.color", "Color", "Hexagon glow (gradient: around the outline).", -16714336));
   }

   public void render(LandingFx fx, FxFrame f) {
      float p = fx.progress();
      float a = fx.alpha() * (1.0F - Easing.smoothstep(0.45F, 1.0F, p));
      if(a > 0.003F) {
         double base = this.align.get() == HexPulseStyle.Align.FACING?Math.toRadians((double)(fx.land.yaw + 90.0F)):6.283185307179586D * (double)fx.rand(1);
         double rot = base + 0.26D * (double)Easing.outCubic(p);
         float pop = Math.max(0.0F, Easing.outBack(Easing.clamp01(p / 0.22F), 1.9F));
         double r = 0.95D * (double)fx.size * (double)pop;
         RenderState.decal(true);
         this.hexes = 0;
         this.keep(fx.x(), fx.z(), r, a, 0.0F, 1.0F);
         if(this.honeycomb.get()) {
            double small = 0.95D * (double)fx.size * 0.42D;

            for(int k = 0; k < 6; ++k) {
               float q = Easing.clamp01((p - 0.12F - (float)k * 0.045F) / 0.3F);
               if(q > 0.0F) {
                  float popK = Math.max(0.0F, Easing.outBack(q, 2.2F));
                  float ak = a * Easing.smoothstep(0.0F, 0.3F, q) * (1.0F - 0.35F * Easing.smoothstep(0.4F, 1.0F, q));
                  double dir = base + 0.5235987755982988D + (double)k * 3.141592653589793D / 3.0D + 0.26D * (double)Easing.outCubic(p);
                  double dist = 0.95D * (double)fx.size + small * 0.98D;
                  this.keep(fx.x() + Math.cos(dir) * dist, fx.z() + Math.sin(dir) * dist, small * (double)popK, ak, (float)(k + 1) / 7.0F, 0.7F);
               }
            }
         }

         RenderState.translucent(false);

         for(int i = 0; i < this.hexes; ++i) {
            Ground.disc(f, fx.ground, Textures.HEX_FILL, this.hexX[i], this.hexZ[i], this.hexR[i], rot, alpha(this.color.getColor(this.hexT[i] + 0.5F), 0.36F * this.hexA[i]), 3, 24);
         }

         int offset = 0;

         for(int i = 0; i < this.hexes; ++i) {
            offset = this.outline(fx, f, i, rot, offset);
         }

         Neon.flush();
         RenderState.decal(false);
      }

   }

   private void keep(double cx, double cz, double r, float a, float t0, float width) {
      if(r > 0.01D && a > 0.003F && this.hexes < 7) {
         this.hexX[this.hexes] = cx;
         this.hexZ[this.hexes] = cz;
         this.hexR[this.hexes] = r;
         this.hexA[this.hexes] = a;
         this.hexT[this.hexes] = t0;
         this.hexW[this.hexes] = width;
         ++this.hexes;
      }

   }

   private int outline(LandingFx fx, FxFrame f, int i, double rot, int offset) {
      int per = 5;
      int n = 6 * per + 1;

      for(int k = 0; k < n; ++k) {
         int side = Math.min(5, k / per);
         float along = (float)(k - side * per) / (float)per;
         double a0 = rot + (double)side * 3.141592653589793D / 3.0D;
         double a1 = rot + (double)(side + 1) * 3.141592653589793D / 3.0D;
         double x0 = this.hexX[i] + Math.cos(a0) * this.hexR[i];
         double z0 = this.hexZ[i] + Math.sin(a0) * this.hexR[i];
         double x1 = this.hexX[i] + Math.cos(a1) * this.hexR[i];
         double z1 = this.hexZ[i] + Math.sin(a1) * this.hexR[i];
         Vfx.X[offset + k] = x0 + (x1 - x0) * (double)along;
         Vfx.Z[offset + k] = z0 + (z1 - z0) * (double)along;
         Vfx.W[offset + k] = (float)(0.1D * (double)this.hexW[i] * Math.sqrt((double)fx.size));
         Vfx.C[offset + k] = alpha(this.color.getColor(loop(this.hexT[i] + ((float)side + along) / 6.0F)), this.hexA[i]);
      }

      Ground.toRender(f, fx.ground, offset, n);
      Neon.addFlat(offset, n);
      return offset + n;
   }

   public static enum Align implements Displayable {
      FACING("Facing", "Lined up with the player"),
      RANDOM("Random", "A random angle");

      private final String displayName;
      private final String description;

      private Align(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }
}
