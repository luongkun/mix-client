package mix.client.mixui.integration.haunt.module.landing.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.Distortion;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class LiquidRippleStyle extends LandingStyleBase {
   private static final int SEGMENTS = 72;
   public final SliderSetting ripples = (SliderSetting)this.setting(new SliderSetting("ripple.ripples", "Ripples", "How many ripples spread out.", 3.0D, 4.0D, 3.0D, 1.0D));
   public final SliderSetting refraction = (SliderSetting)this.setting((new SliderSetting("ripple.refraction", "Refraction", "How much the ground warps under the crests.", 0.0D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final Rng rng = new Rng();

   public LiquidRippleStyle(LandingImpactModule module) {
      super(module, LandingStyle.LIQUID_RIPPLE, 1.2F, 2.8F, new ColorSetting("ripple.color", "Color", "Crest glow (gradient: around each ripple).", -11549697));
   }

   public void start(LandingFx fx) {
      if(!fx.lod) {
         this.rng.seed(fx.seed);
         int n = fx.count(8.0F);

         for(int i = 0; i < n; ++i) {
            double ang = (double)this.rng.next() * 6.283185307179586D;
            float out = this.rng.range(0.3F, 0.9F);
            fx.particles.spawn(6, fx.x() + Math.cos(ang) * 0.1D, fx.y() + 0.05D, fx.z() + Math.sin(ang) * 0.1D, (float)Math.cos(ang) * out, this.rng.range(2.4F, 4.0F), (float)Math.sin(ang) * out, fx.seconds(this.rng.range(0.45F, 0.65F)), this.rng.range(0.06F, 0.09F)).gravity(10.0F).color(this.color, this.rng.next()).alpha(fx.opacity).fade(0.02F, 0.6F).delay(fx.life * this.rng.range(0.0F, 0.2F)).ground(fx.y() + 0.03D, 0, 0.3F);
         }
      }

   }

   public void render(LandingFx fx, FxFrame f) {
      float p = fx.progress();
      int n = this.ripples.getInt();
      double maxR = 2.4D * Math.sqrt((double)fx.size);

      for(int k = 0; k < n; ++k) {
         float q = (p - (float)k * 0.12F) / 0.78F;
         if(q > 0.0F && q < 1.0F) {
            double r = 0.15D + maxR * (1.0D - Math.pow(1.0D - (double)q, 1.7D));
            float amp = (float)Math.pow((double)(1.0F - q), 1.4D) * Easing.smoothstep(0.0F, 0.06F, q) * (1.0F - 0.15F * (float)k);
            float a = fx.alpha() * amp;
            if(a > 0.003F) {
               if(!fx.lod) {
                  this.warp(fx, f, r, 0.28D * Math.sqrt((double)fx.size), a);
               }

               RenderState.decal(true);
               RenderState.translucent(false);
               Ground.band(f, fx.ground, Textures.WAVE, fx.x(), fx.z(), Math.max(0.0D, r - 0.34D * Math.sqrt((double)fx.size)), r, 0.0D, 6.283185307179586D, 72, this.color, 0, 0.45F * a, (float)k * 0.2F);
               RenderState.additive();
               Ground.band(f, fx.ground, Textures.WAVE, fx.x(), fx.z(), Math.max(0.0D, r - 0.34D * Math.sqrt((double)fx.size)), r, 0.0D, 6.283185307179586D, 72, this.color, 0, 0.3F * a, (float)k * 0.2F);
               Neon.ring(f, fx.ground, fx.x(), fx.z(), r - 0.02D, 0.05D * Math.sqrt((double)fx.size), 72, this.color, 0, 0.8F * a, (float)k * 0.2F);
               RenderState.decal(false);
            }
         }
      }

   }

   private void warp(LandingFx fx, FxFrame f, double r, double width, float a) {
      float strength = this.refraction.getFloat();
      if(strength > 0.01F) {
         Distortion.begin(f.frameId, 0.12F * strength, this.color.getColor(0.5F), 0.25F, Math.min(1.0F, a * 1.2F), true);
         double rIn = Math.max(0.0D, r - width);
         double rOut = r + width * 0.3D;

         for(int i = 0; i < 72; ++i) {
            double a0 = 6.283185307179586D * (double)i / 72.0D;
            double a1 = 6.283185307179586D * (double)(i + 1) / 72.0D;
            vertex(fx, f, rIn, a0, -0.55D);
            vertex(fx, f, rOut, a0, 0.7D);
            vertex(fx, f, rOut, a1, 0.7D);
            vertex(fx, f, rIn, a1, -0.55D);
         }

         Distortion.end();
         RenderState.additive();
      }

   }

   private static void vertex(LandingFx fx, FxFrame f, double r, double ang, double tilt) {
      double c = Math.cos(ang);
      double s = Math.sin(ang);
      double x = fx.x() + c * r;
      double z = fx.z() + s * r;
      double y = Ground.y(fx.ground, x, z) + 0.004D;
      double nx = c * tilt;
      double ny = 1.0D;
      double nz = s * tilt;
      double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
      Distortion.vertex(f.rx(x), f.ry(y), f.rz(z), nx / l, ny / l, nz / l);
   }
}
