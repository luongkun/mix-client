package mix.client.mixui.integration.haunt.module.landing.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.WarpShell;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class WarpDomeStyle extends LandingStyleBase {
   private static final float POP = 0.78F;
   public final SliderSetting wobble = (SliderSetting)this.setting((new SliderSetting("warpdome.wobble", "Wobble", "How much it squashes and stretches.", 0.0D, 2.0D, 1.0D, 0.05D)).unit("x"));
   public final SliderSetting refraction = (SliderSetting)this.setting((new SliderSetting("warpdome.refraction", "Refraction", "How strongly it bends what is behind it.", 0.0D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final Rng rng = new Rng();

   public WarpDomeStyle(LandingImpactModule module) {
      super(module, LandingStyle.WARP_DOME, 0.7F, 1.6F, new ColorSetting("warpdome.color", "Color", "Tint and rim of the dome.", -10690049));
   }

   private static double radius(LandingFx fx) {
      return 1.05D * (double)fx.size;
   }

   public void start(LandingFx fx) {
      if(!fx.lod) {
         this.rng.seed(fx.seed);
         double r = radius(fx);
         int n = fx.count(18.0F);
         float delay = fx.life * 0.78F;

         for(int i = 0; i < n; ++i) {
            double ang = 6.283185307179586D * ((double)i + (double)this.rng.next() * 0.7D) / (double)n;
            double cx = Math.cos(ang);
            double cz = Math.sin(ang);
            float out = this.rng.range(1.6F, 3.2F);
            double y0 = fx.y() + 0.1D + (double)this.rng.next() * r * 0.5D;
            fx.particles.spawn(6, fx.x() + cx * r * 0.95D, y0, fx.z() + cz * r * 0.95D, (float)cx * out, this.rng.range(0.6F, 2.0F), (float)cz * out, fx.seconds(this.rng.range(0.4F, 0.6F)), this.rng.range(0.07F, 0.11F) * (float)Math.sqrt((double)fx.size)).end(0.03F).gravity(8.0F).drag(1.2F).color(this.color, this.rng.next()).alpha(fx.opacity).fade(0.01F, 0.55F).delay(delay).ground(fx.y() + 0.03D, 0, 0.3F);
         }
      }

   }

   public void render(LandingFx fx, FxFrame f) {
      float p = fx.progress();
      float a = fx.alpha();
      double r = radius(fx);
      double t = (double)fx.styleTime();
      if(p < 0.84F) {
         float inflate = Math.max(0.0F, Easing.outBack(Easing.clamp01(p / 0.3F), 2.4F));
         double jelly = 0.2D * this.wobble.get() * Math.sin(t * 30.0D) * Math.exp(-t * 5.0D) * (double)Easing.smoothstep(0.12F, 0.32F, p);
         float popT = Easing.clamp01((p - 0.78F) / 0.06F);
         double swell = 1.0D + 0.1D * (double)popT;
         float shellA = a * (1.0F - popT);
         WarpShell.draw(f, fx.x(), fx.y() + 0.01D, fx.z(), r * (double)inflate * swell, 0.0D, 1.0D, 0.0D, 1.0D + jelly, 1.0D - jelly * 0.5D, 0.0D, 1.0D, 0.0D, 0.0D, 1.0D, true, this.refraction.getFloat(), this.color.getColor(0.5F), 1.1F, shellA, !fx.lod);
         RenderState.decal(true);
         Neon.ring(f, fx.ground, fx.x(), fx.z(), r * (double)inflate * swell, 0.06D * Math.sqrt((double)fx.size), 64, this.color, 0, 0.7F * shellA, 0.0F);
         RenderState.decal(false);
      }

      float flash;
      if((flash = Easing.smoothstep(0.76F, 0.79999995F, p) * (1.0F - Easing.smoothstep(0.79999995F, 1.0F, p))) > 0.003F) {
         double y = fx.y() + r * 0.45D;
         Vfx.sprite(Textures.GLOW, f.rx(fx.x()), f.ry(y), f.rz(fx.z()), r * 3.0D, 0.0F, alpha(this.color.getColor(0.5F), 0.4F * flash * a));
         Vfx.sprite(Textures.FLARE, f.rx(fx.x()), f.ry(y), f.rz(fx.z()), r * 1.6D, 0.4F, alpha(Neon.tint(this.color.getColor(0.5F)), 0.55F * flash * a));
      }

   }
}
