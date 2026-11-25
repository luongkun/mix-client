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

public final class SealStampStyle extends LandingStyleBase {
   private static final int SEGMENTS = 96;
   private static final int TICKS = 36;
   public final SliderSetting spin = (SliderSetting)this.setting((new SliderSetting("seal.spin", "Spin", "How fast the seal turns.", 0.0D, 2.0D, 1.0D, 0.05D)).unit("x"));

   public SealStampStyle(LandingImpactModule module) {
      super(module, LandingStyle.SEAL_STAMP, 1.2F, 2.6F, new ColorSetting("seal.color", "Color", "Seal glow (gradient: around the rings).", -14249));
   }

   public void render(LandingFx fx, FxFrame f) {
      float p = fx.progress();
      float a = fx.alpha();
      float slam = Easing.outCubic(Easing.clamp01(p / 0.1F));
      double scale = (1.4D - 0.4D * (double)slam) * (double)fx.size;
      float appear = Easing.smoothstep(0.0F, 0.05F, p);
      double rot = (double)fx.styleTime() * 0.9D * this.spin.get() + Math.toRadians((double)fx.land.yaw);
      double r = 1.15D * scale;
      double w = 0.075D * Math.sqrt((double)fx.size);
      RenderState.decal(true);
      float flash = Easing.smoothstep(0.06F, 0.1F, p) * (1.0F - Easing.smoothstep(0.1F, 0.35F, p));
      if(flash > 0.003F) {
         RenderState.translucent(false);
         Ground.disc(f, fx.ground, Textures.GLOW, fx.x(), fx.z(), r * 1.4D, 0.0D, alpha(this.color.getColor(0.5F), 0.35F * flash * a), 3, 24);
         RenderState.additive();
         Ground.disc(f, fx.ground, Textures.GLOW, fx.x(), fx.z(), r * 1.7D, 0.0D, alpha(this.color.getColor(0.5F), 0.45F * flash * a), 3, 24);
         double shock = r * (1.0D + 0.8D * (double)Easing.outCubic(Easing.clamp01((p - 0.1F) / 0.25F)));
         Ground.ring(f, fx.ground, Textures.WAVE, fx.x(), fx.z(), shock, 0.35D * (double)fx.size, 96, this.color, 0, 0.6F * flash * a, 0.0F);
      }

      float inner;
      if((inner = a * appear * (1.0F - Easing.smoothstep(0.62F, 0.95F, p))) > 0.003F) {
         RenderState.translucent(false);
         Ground.disc(f, fx.ground, Textures.RADIAL_FILL, fx.x(), fx.z(), r * 0.84D, rot, alpha(this.color.getColor(0.5F), 0.22F * inner), 3, 32);
         RenderState.additive();
      }

      int offset = 0;
      double[] radii = new double[]{r, r * 0.8D, r * 0.52D};
      float[] detachAt = new float[]{0.46F, 0.56F, 0.66F};

      for(int k = 0; k < 3; ++k) {
         float d = Easing.clamp01((p - detachAt[k]) / 0.3F);
         double rr = radii[k] * (1.0D + 0.9D * (double)Easing.outCubic(d));
         float ra = a * appear * (1.0F - d);
         if(ra > 0.003F) {
            float tShift = (float)(rot / 6.283185307179586D) * (float)(k % 2 == 0?1:-1);
            int count = Ground.ringPoints(f, fx.ground, offset, fx.x(), fx.z(), rr, w * (k == 0?1.0D:0.8D), 96, this.color, 0, ra, tShift);
            Neon.addFlat(offset, count);
            offset += count;
         }
      }

      if(inner > 0.003F) {
         for(int i = 0; i < 36; ++i) {
            double ang = rot + 6.283185307179586D * (double)i / 36.0D;
            double len = (i % 3 == 0?0.15D:0.08D) * scale;
            double r0 = r * 0.84D;
            double r1 = r0 + len;
            int c = alpha(this.color.getColor(loop((float)i / 36.0F)), inner);
            int count = Ground.linePoints(f, fx.ground, offset, fx.x() + Math.cos(ang) * r0, fx.z() + Math.sin(ang) * r0, fx.x() + Math.cos(ang) * r1, fx.z() + Math.sin(ang) * r1, w * 0.6D, c, c, 1);
            Neon.addFlat(offset, count);
            offset += count;
         }

         double sr = r * 0.52D;

         for(int tri = 0; tri < 2; ++tri) {
            for(int kx = 0; kx < 3; ++kx) {
               double a0 = -rot * 0.6D + (double)tri * 3.141592653589793D / 3.0D + (double)kx * 6.283185307179586D / 3.0D;
               double a1 = a0 + 2.0943951023931953D;
               int c0 = alpha(this.color.getColor(loop((float)kx / 3.0F + (float)tri * 0.5F)), inner);
               int c1 = alpha(this.color.getColor(loop((float)(kx + 1) / 3.0F + (float)tri * 0.5F)), inner);
               int count = Ground.linePoints(f, fx.ground, offset, fx.x() + Math.cos(a0) * sr, fx.z() + Math.sin(a0) * sr, fx.x() + Math.cos(a1) * sr, fx.z() + Math.sin(a1) * sr, w * 0.7D, c0, c1, 6);
               Neon.addFlat(offset, count);
               offset += count;
            }
         }

         int count2 = Ground.ringPoints(f, fx.ground, offset, fx.x(), fx.z(), r * 0.2D, w * 0.6D, 40, this.color, 0, inner, 0.0F);
         Neon.addFlat(offset, count2);
      }

      Neon.flush();
      RenderState.decal(false);
      if(flash > 0.003F) {
         Vfx.sprite(Textures.FLARE, f.rx(fx.x()), f.ry(fx.y() + 0.25D), f.rz(fx.z()), 1.4D * (double)fx.size, 0.2F, alpha(Neon.tint(this.color.getColor(0.5F)), flash * a));
      }

   }
}
