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
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class SparkleBurstStyle extends LandingStyleBase {
   public final SliderSetting sparkles = (SliderSetting)this.setting(new SliderSetting("sparkle.count", "Sparkles", "How many sparkles shoot out.", 4.0D, 16.0D, 8.0D, 1.0D));

   public SparkleBurstStyle(LandingImpactModule module) {
      super(module, LandingStyle.SPARKLE_BURST, 0.9F, 2.2F, new ColorSetting("sparkle.color", "Color", "Sparkles and flash (gradient: a mix per sparkle).", -7558));
   }

   public void render(LandingFx fx, FxFrame f) {
      float p = fx.progress();
      float a = fx.alpha();
      double cy = fx.y() + 0.2D;
      float flash = 1.0F - Easing.smoothstep(0.0F, 0.35F, p);
      if(flash > 0.003F) {
         int c = this.color.getColor(0.5F);
         double x = f.rx(fx.x());
         double y = f.ry(cy);
         double z = f.rz(fx.z());
         RenderState.translucent(false);
         Vfx.sprite(Textures.SPARKLE, x, y, z, 0.9D * (double)fx.size * (double)(1.1F - 0.3F * flash), 0.0F, alpha(c, 0.8F * flash * a));
         RenderState.additive();
         Vfx.sprite(Textures.GLOW, x, y, z, 1.4D * (double)fx.size, 0.0F, alpha(c, 0.5F * flash * a));
         Vfx.sprite(Textures.STREAK, x, y, z, 1.6D * (double)fx.size * (double)(1.3F - 0.3F * flash), 0.09D * (double)fx.size, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, alpha(Neon.tint(c), flash * a));
         Vfx.sprite(Textures.SPARKLE, x, y, z, 0.55D * (double)fx.size * (double)(1.1F - 0.3F * flash), 0.0F, alpha(-1, flash * a));
      }

      int n = Math.max(1, Math.round(this.sparkles.getFloat() * fx.density));
      float travel = Easing.outCubic(Easing.clamp01(p / 0.75F));
      float fade = a * Easing.smoothstep(0.0F, 0.04F, p) * (1.0F - Easing.smoothstep(0.55F, 1.0F, p));
      if(fade > 0.003F) {
         double reach = 1.9D * Math.sqrt((double)fx.size);
         RenderState.decal(true);
         int offset = 0;

         for(int i = 0; i < n; ++i) {
            double ang = 6.283185307179586D * ((double)i + 0.4D * (double)fx.srand(i)) / (double)n;
            double d = reach * (0.75D + 0.35D * (double)fx.rand(i + 30)) * (double)travel;
            int c = this.color.getColor(fx.rand(i + 60));
            int count = Ground.linePoints(f, fx.ground, offset, fx.x() + Math.cos(ang) * d * 0.3D, fx.z() + Math.sin(ang) * d * 0.3D, fx.x() + Math.cos(ang) * d, fx.z() + Math.sin(ang) * d, 0.06D * Math.sqrt((double)fx.size), c & 16777215, alpha(c, 0.55F * fade), 4);
            Neon.addFlat(offset, count);
            offset += count;
         }

         Neon.flush();
         RenderState.decal(false);

         for(int pass = 0; pass < 3; ++pass) {
            if(pass == 0) {
               RenderState.translucent(false);
            } else if(pass == 1) {
               RenderState.additive();
            }

            for(int i = 0; i < n; ++i) {
               double ang = 6.283185307179586D * ((double)i + 0.4D * (double)fx.srand(i)) / (double)n;
               double d = reach * (0.75D + 0.35D * (double)fx.rand(i + 30)) * (double)travel;
               double x = fx.x() + Math.cos(ang) * d;
               double z = fx.z() + Math.sin(ang) * d;
               double y = Ground.y(fx.ground, x, z) + 0.2D + 0.14D * Math.sin(3.141592653589793D * (double)travel);
               float twinkle = 0.65F + 0.35F * (float)Math.sin((double)(fx.age * 26.0F + (float)i * 2.1F));
               int c = this.color.getColor(fx.rand(i + 60));
               float s = (float)(0.55D * Math.sqrt((double)fx.size) * (double)twinkle);
               float spin = (float)((double)fx.age * 1.5D + (double)i);
               if(pass == 0) {
                  Vfx.sprite(Textures.SPARKLE, f.rx(x), f.ry(y), f.rz(z), (double)s, spin, alpha(c, 0.9F * fade));
               } else if(pass == 1) {
                  Vfx.sprite(Textures.GLOW, f.rx(x), f.ry(y), f.rz(z), (double)(s * 2.0F), 0.0F, alpha(c, 0.35F * fade));
               } else {
                  Vfx.sprite(Textures.SPARKLE, f.rx(x), f.ry(y), f.rz(z), (double)(s * 0.55F), spin, alpha(Neon.tint(c), fade * twinkle));
               }
            }
         }
      }

   }
}
