package mix.client.mixui.integration.haunt.module.hit.styles;

import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class ShockwaveConeStyle extends HitStyleBase {
   private static final int SEGMENTS = 56;
   private static final int MAX_RINGS = 3;
   public final SliderSetting rings = (SliderSetting)this.setting(new SliderSetting("shockwave.rings", "Rings", "How many rings burst out.", 2.0D, 3.0D, 3.0D, 1.0D));
   public final SliderSetting thickness = (SliderSetting)this.setting((new SliderSetting("shockwave.thickness", "Thickness", "Width of the rings.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final double[] ringX = new double[3];
   private final double[] ringY = new double[3];
   private final double[] ringZ = new double[3];
   private final double[] ringR = new double[3];
   private final float[] ringA = new float[3];

   public ShockwaveConeStyle(HitEffectModule module) {
      super(module, HitStyle.SHOCKWAVE_CONE, 0.35F, new ColorSetting("shockwave.color", "Color", "Ring color (gradient: around each ring).", -10307329));
   }

   public void render(HitFx fx, FxFrame f) {
      float p = fx.progress();
      float a = fx.alpha();
      Basis b = fx.basis;
      double ix = fx.ix();
      double iy = fx.iy();
      double iz = fx.iz();
      float size = fx.size;
      float flash = 1.0F - Easing.smoothstep(0.0F, 0.3F, p);
      if(flash > 0.003F) {
         int c = this.color.getColor(0.0F);
         double x = f.rx(ix - b.fx * 0.08D);
         double y = f.ry(iy - b.fy * 0.08D);
         double z = f.rz(iz - b.fz * 0.08D);
         RenderState.translucent(false);
         Vfx.sprite(Textures.FLARE, x, y, z, 0.9D * (double)size * (double)(0.7F + 0.3F * flash), 0.2F, alpha(c, 0.75F * flash * a));
         RenderState.additive();
         Vfx.sprite(Textures.GLOW, x, y, z, 1.0D * (double)size, 0.0F, alpha(c, 0.5F * flash * a));
         Vfx.sprite(Textures.FLARE, x, y, z, 0.6D * (double)size * (double)(0.7F + 0.3F * flash), 0.2F, alpha(Neon.tint(c), flash * a));
      }

      int n = this.rings.getInt();
      float width = this.thickness.getFloat() * (fx.crit?1.7F:1.0F);

      for(int k = 0; k < n; ++k) {
         float q = (p - (float)k * 0.13F) / 0.62F;
         if(q > 0.0F && q < 1.0F) {
            double d = 1.75D * (double)size * (double)Easing.outCubic(q) - 0.15D * (double)size;
            this.ringX[k] = ix + b.fx * d;
            this.ringY[k] = iy + b.fy * d;
            this.ringZ[k] = iz + b.fz * d;
            this.ringR[k] = (0.16D + 0.78D * (double)Easing.outQuad(q)) * (double)size * (fx.crit?1.15D:1.0D);
            this.ringA[k] = (float)Math.pow((double)(1.0F - q), 1.3D) * Easing.smoothstep(0.0F, 0.08F, q) * a;
            int o = k * 56;

            for(int i = 0; i < 56; ++i) {
               double phi = 6.283185307179586D * (double)i / 56.0D;
               double cos = Math.cos(phi) * this.ringR[k];
               double sin = Math.sin(phi) * this.ringR[k];
               Vfx.X[o + i] = f.rx(this.ringX[k] + b.rx * cos + b.ux * sin);
               Vfx.Y[o + i] = f.ry(this.ringY[k] + b.ry * cos + b.uy * sin);
               Vfx.Z[o + i] = f.rz(this.ringZ[k] + b.rz * cos + b.uz * sin);
               Vfx.W[o + i] = (float)(0.07D * (double)size * (double)width * (double)(1.0F - 0.3F * q));
               Vfx.C[o + i] = alpha(this.color.getColor(loop((float)i / 56.0F + (float)k * 0.17F)), this.ringA[k]);
            }

            Neon.add(o, 56, true);
         } else {
            this.ringA[k] = 0.0F;
         }
      }

      Neon.flush();
      if(fx.crit) {
         RenderState.translucent(false);

         for(int var28 = 0; var28 + 1 < n; ++var28) {
            if(this.ringA[var28] > 0.0F && this.ringA[var28 + 1] > 0.0F) {
               for(int i = 0; i < 56; ++i) {
                  double p0 = 6.283185307179586D * (double)i / 56.0D;
                  double p1 = 6.283185307179586D * (double)(i + 1) / 56.0D;
                  int c0 = alpha(this.color.getColor(loop((float)i / 56.0F)), 0.12F * this.ringA[var28]);
                  int c1 = alpha(this.color.getColor(loop((float)i / 56.0F)), 0.12F * this.ringA[var28 + 1]);
                  Vfx.quad(f.rx(this.ringX[var28] + (b.rx * Math.cos(p0) + b.ux * Math.sin(p0)) * this.ringR[var28]), f.ry(this.ringY[var28] + (b.ry * Math.cos(p0) + b.uy * Math.sin(p0)) * this.ringR[var28]), f.rz(this.ringZ[var28] + (b.rz * Math.cos(p0) + b.uz * Math.sin(p0)) * this.ringR[var28]), f.rx(this.ringX[var28] + (b.rx * Math.cos(p1) + b.ux * Math.sin(p1)) * this.ringR[var28]), f.ry(this.ringY[var28] + (b.ry * Math.cos(p1) + b.uy * Math.sin(p1)) * this.ringR[var28]), f.rz(this.ringZ[var28] + (b.rz * Math.cos(p1) + b.uz * Math.sin(p1)) * this.ringR[var28]), f.rx(this.ringX[var28 + 1] + (b.rx * Math.cos(p1) + b.ux * Math.sin(p1)) * this.ringR[var28 + 1]), f.ry(this.ringY[var28 + 1] + (b.ry * Math.cos(p1) + b.uy * Math.sin(p1)) * this.ringR[var28 + 1]), f.rz(this.ringZ[var28 + 1] + (b.rz * Math.cos(p1) + b.uz * Math.sin(p1)) * this.ringR[var28 + 1]), f.rx(this.ringX[var28 + 1] + (b.rx * Math.cos(p0) + b.ux * Math.sin(p0)) * this.ringR[var28 + 1]), f.ry(this.ringY[var28 + 1] + (b.ry * Math.cos(p0) + b.uy * Math.sin(p0)) * this.ringR[var28 + 1]), f.rz(this.ringZ[var28 + 1] + (b.rz * Math.cos(p0) + b.uz * Math.sin(p0)) * this.ringR[var28 + 1]), c0, c0, c1, c1);
               }
            }
         }
      }

   }
}
