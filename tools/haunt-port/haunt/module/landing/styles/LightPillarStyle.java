package mix.client.mixui.integration.haunt.module.landing.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.fx.Rng;
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
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class LightPillarStyle extends LandingStyleBase {
   private static final int POINTS = 14;
   public final SliderSetting height = (SliderSetting)this.setting((new SliderSetting("pillar.height", "Height", "How tall the beam is.", 3.0D, 6.0D, 4.5D, 0.1D)).unit(" blocks"));
   public final SliderSetting width = (SliderSetting)this.setting((new SliderSetting("pillar.width", "Width", "How wide the beam is.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   public final BooleanSetting sparks = (BooleanSetting)this.setting(new BooleanSetting("pillar.sparks", "Sparks", "Sparks rising along the beam.", true));
   private final Rng rng = new Rng();

   public LightPillarStyle(LandingImpactModule module) {
      super(module, LandingStyle.LIGHT_PILLAR, 0.8F, 1.2F, new ColorSetting("pillar.color", "Color", "Beam glow (gradient: bottom to top).", -8399617));
   }

   public void start(LandingFx fx) {
      if(this.sparks.get() && !fx.lod) {
         this.rng.seed(fx.seed);
         int n = fx.count(12.0F);

         for(int i = 0; i < n; ++i) {
            double ang = (double)this.rng.next() * 6.283185307179586D;
            double rad = (double)(this.rng.range(0.05F, 0.3F) * fx.size);
            fx.particles.spawn(1, fx.x() + Math.cos(ang) * rad, fx.y() + (double)this.rng.range(0.1F, 1.2F), fx.z() + Math.sin(ang) * rad, 0.0F, this.rng.range(2.2F, 5.0F), 0.0F, fx.seconds(this.rng.range(0.3F, 0.5F)), this.rng.range(0.04F, 0.07F)).drag(1.2F).color(this.color, this.rng.next()).alpha(fx.opacity).hot(0.5F).delay(fx.life * this.rng.range(0.02F, 0.5F));
         }
      }

   }

   public void render(LandingFx fx, FxFrame f) {
      float p = fx.progress();
      float a = fx.alpha() * (1.0F - Easing.smoothstep(0.8F, 1.0F, p));
      if(a > 0.003F) {
         double h = this.height.get() * Math.sqrt((double)fx.size) * (double)Easing.outCubic(Easing.clamp01(p / 0.14F));
         float thin = 1.0F - 0.92F * Easing.smoothstep(0.5F, 0.95F, p);
         double w = 0.55D * this.width.get() * Math.sqrt((double)fx.size) * (double)thin * (double)Easing.smoothstep(0.0F, 0.06F, p);
         double x = fx.x();
         double y = fx.y();
         double z = fx.z();
         RenderState.decal(true);
         float base = a * (1.0F - Easing.smoothstep(0.35F, 0.9F, p));
         RenderState.translucent(false);
         Ground.disc(f, fx.ground, Textures.GLOW, x, z, 0.9D * (double)fx.size, 0.0D, alpha(this.color.getColor(0.0F), 0.35F * base), 3, 20);
         RenderState.additive();
         Ground.disc(f, fx.ground, Textures.GLOW, x, z, 1.1D * (double)fx.size, 0.0D, alpha(this.color.getColor(0.0F), 0.4F * base), 3, 20);
         double ringR = (0.35D + 0.5D * (double)Easing.outCubic(Easing.clamp01(p / 0.4F))) * (double)fx.size;
         Neon.ring(f, fx.ground, x, z, ringR, 0.07D * (double)fx.size, 64, this.color, 0, base, 0.0F);
         RenderState.decal(false);
         if(h > 0.02D && w > 0.002D) {
            for(int i = 0; i < 14; ++i) {
               float s = (float)i / 13.0F;
               double py = y + h * (double)s;
               Vfx.X[i] = f.rx(x);
               Vfx.Y[i] = f.ry(py);
               Vfx.Z[i] = f.rz(z);
               float taper = 1.0F - 0.75F * (float)Math.pow((double)s, 1.4D);
               float near = f.nearCameraFade(x, py, z, 0.45F, 1.3F);
               float top = 1.0F - Easing.smoothstep(0.75F, 1.0F, s);
               Vfx.W[i] = (float)(w * 1.1D * (double)taper);
               Vfx.C[i] = alpha(this.color.getColor(s), a * top * near);
            }

            Neon.add(0, 14, false);
            Neon.flush(1.2F, 0.8F, 1.0F);
         }
      }

   }
}
