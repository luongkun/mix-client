package mix.client.mixui.integration.haunt.module.hit.styles;

import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class VortexStyle extends HitStyleBase {
   private static final int ARM_POINTS = 18;
   private static final int RIM_POINTS = 56;
   private static final int INNER_OFFSET = 144;
   private static final int RIM_OFFSET = 216;
   public final SliderSetting arms = (SliderSetting)this.setting(new SliderSetting("vortex.arms", "Arms", "How many spiral arms.", 5.0D, 8.0D, 6.0D, 1.0D));
   public final SliderSetting spin = (SliderSetting)this.setting((new SliderSetting("vortex.spin", "Spin", "How fast it turns.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   public final BooleanSetting rays = (BooleanSetting)this.setting(new BooleanSetting("vortex.rays", "Light Rays", "Soft wedges of light fanning out behind the target.", true));

   public VortexStyle(HitEffectModule module) {
      super(module, HitStyle.VORTEX, 0.5F, new ColorSetting("vortex.color", "Color", "Arms, rim and glow (gradient: centre to edge).", -6595329));
   }

   public void render(HitFx fx, FxFrame f) {
      float p = fx.progress();
      float a = fx.alpha();
      if(a > 0.003F) {
         Basis b = fx.basis;
         float size = fx.size * (fx.crit?1.25F:1.0F);
         double maxR = 0.95D * (double)size;
         double r = p < 0.12F?maxR * 0.35D * (double)Math.max(0.0F, Easing.outBack(p / 0.12F, 2.2F)):(p < 0.58F?maxR * (0.35D + 0.65D * (double)Easing.outCubic((p - 0.12F) / 0.46F)):maxR * (1.0D - (double)Easing.inCubic(Easing.clamp01((p - 0.58F) / 0.32F))));
         double t = (double)fx.styleTime();
         int dir = fx.hit.side;
         double angle = (double)dir * this.spin.get() * (13.0D * t + 22.0D * t * t);
         double twist = 1.7D + 3.4D * (double)Easing.smoothstep(0.55F, 0.92F, p);
         double cx = fx.ix() - b.fx * 0.06D;
         double cy = fx.iy() - b.fy * 0.06D;
         double cz = fx.iz() - b.fz * 0.06D;
         float live = 1.0F - Easing.smoothstep(0.86F, 0.93F, p);
         if(this.rays.get() && !fx.lod) {
            this.drawRays(fx, f, p, a, size);
         }

         if(r > 0.005D && live > 0.0F) {
            float la = a * live;
            RenderState.translucent(false);
            Vfx.sprite(Textures.GLOW, f.rx(cx), f.ry(cy), f.rz(cz), r * 1.9D, 0.0F, ColorUtil.withAlpha(ColorUtil.shade(this.color.getColor(0.0F), 0.12F), 0.62F * la));
            RenderState.additive();
            Vfx.sprite(Textures.GLOW, f.rx(cx), f.ry(cy), f.rz(cz), r * 3.0D, 0.0F, alpha(this.color.getColor(0.2F), 0.22F * la));
            int n = this.arms.getInt();
            this.spiral(f, b, cx, cy, cz, r, angle, twist, dir, n, 0.075D * (double)size, la, 0);
            if(fx.crit) {
               this.spiral(f, b, cx, cy, cz, r * 0.55D, -angle * 1.3D, twist * 0.8D, -dir, 4, 0.055D * (double)size, la, 144);
            }

            this.rim(f, b, cx, cy, cz, r, angle, la, size);
            Neon.flush();
            this.flakes(fx, f, b, cx, cy, cz, r, angle, la);
         }

         float flash;
         if((flash = Easing.smoothstep(0.84F, 0.9F, p) * (1.0F - Easing.smoothstep(0.9F, 1.0F, p))) > 0.003F) {
            int c = this.color.getColor(0.5F);
            RenderState.translucent(false);
            Vfx.sprite(Textures.FLARE, f.rx(cx), f.ry(cy), f.rz(cz), 1.1D * (double)size * (double)(0.6F + 0.4F * flash), (float)angle, alpha(c, 0.7F * flash * a));
            RenderState.additive();
            Vfx.sprite(Textures.GLOW, f.rx(cx), f.ry(cy), f.rz(cz), 1.3D * (double)size, 0.0F, alpha(c, 0.6F * flash * a));
            Vfx.sprite(Textures.FLARE, f.rx(cx), f.ry(cy), f.rz(cz), 0.8D * (double)size * (double)(0.6F + 0.4F * flash), (float)angle, core(c, flash * a));
         }
      }

   }

   private void spiral(FxFrame f, Basis b, double cx, double cy, double cz, double r, double angle, double twist, int dir, int n, double width, float a, int offset) {
      for(int arm = 0; arm < n; ++arm) {
         double base = 6.283185307179586D * (double)arm / (double)n + angle;
         int o = offset + arm * 18;

         for(int k = 0; k < 18; ++k) {
            float s = (float)k / 17.0F;
            double rho = r * (0.1D + 0.9D * (double)s);
            double phi = base - (double)dir * twist * (double)s;
            double lx = Math.cos(phi) * rho;
            double ly = Math.sin(phi) * rho;
            Vfx.X[o + k] = f.rx(cx + b.rx * lx + b.ux * ly);
            Vfx.Y[o + k] = f.ry(cy + b.ry * lx + b.uy * ly);
            Vfx.Z[o + k] = f.rz(cz + b.rz * lx + b.uz * ly);
            float taper = (float)Math.pow(Math.sin(3.141592653589793D * (double)s), 0.8D);
            Vfx.W[o + k] = (float)(width * (0.25D + 0.75D * (double)taper));
            Vfx.C[o + k] = alpha(this.color.getColor(s), a * (0.25F + 0.75F * taper));
         }

         Neon.add(o, 18, false);
      }

   }

   private void rim(FxFrame f, Basis b, double cx, double cy, double cz, double r, double angle, float a, float size) {
      for(int k = 0; k < 56; ++k) {
         double phi = 6.283185307179586D * (double)k / 56.0D;
         double lx = Math.cos(phi) * r;
         double ly = Math.sin(phi) * r;
         int o = 216 + k;
         Vfx.X[o] = f.rx(cx + b.rx * lx + b.ux * ly);
         Vfx.Y[o] = f.ry(cy + b.ry * lx + b.uy * ly);
         Vfx.Z[o] = f.rz(cz + b.rz * lx + b.uz * ly);
         float wave = 0.55F + 0.45F * (float)Math.sin(phi * 3.0D + angle * 1.5D);
         Vfx.W[o] = (float)(0.05D * (double)size);
         Vfx.C[o] = alpha(this.color.getColor(loop((float)k / 56.0F * 0.5F + 0.5F)), a * wave);
      }

      Neon.add(216, 56, true);
   }

   private void flakes(HitFx fx, FxFrame f, Basis b, double cx, double cy, double cz, double r, double angle, float a) {
      for(int pass = 0; pass < 2; ++pass) {
         for(int k = 0; k < 9; ++k) {
            float appear = Easing.smoothstep(0.06F + fx.rand(k) * 0.1F, 0.2F + fx.rand(k) * 0.1F, fx.progress());
            if(appear > 0.0F) {
               double phi = 6.283185307179586D * (double)fx.rand(k + 20) + angle * (1.1D + 0.4D * (double)fx.rand(k + 40));
               double rho = r * (1.05D + 0.4D * (double)fx.rand(k + 60));
               double lx = Math.cos(phi) * rho;
               double ly = Math.sin(phi) * rho;
               double px = cx + b.rx * lx + b.ux * ly;
               double py = cy + b.ry * lx + b.uy * ly;
               double pz = cz + b.rz * lx + b.uz * ly;
               float s = (0.035F + 0.03F * fx.rand(k + 80)) * fx.size;
               int c = this.color.getColor(fx.rand(k + 90));
               if(pass == 0) {
                  Vfx.sprite(Textures.GLOW, f.rx(px), f.ry(py), f.rz(pz), (double)(s * 3.5F), 0.0F, alpha(c, 0.35F * a * appear));
               } else {
                  Vfx.sprite(Textures.SPARK, f.rx(px), f.ry(py), f.rz(pz), (double)s, 0.0F, alpha(Neon.tint(c), a * appear));
               }
            }
         }
      }

   }

   private void drawRays(HitFx fx, FxFrame f, float p, float a, float size) {
      float ra = Easing.smoothstep(0.0F, 0.06F, p) * (1.0F - Easing.smoothstep(0.14F, 0.62F, p));
      if(ra > 0.003F) {
         Basis b = fx.basis;
         double ox = fx.cx() + b.fx * (double)fx.hit.width * 0.35D;
         double oy = fx.cy() + b.fy * (double)fx.hit.width * 0.35D;
         double oz = fx.cz() + b.fz * (double)fx.hit.width * 0.35D;
         float grow = Easing.outCubic(Easing.clamp01(p / 0.3F));
         int count = 6 + Math.round(4.0F * fx.rand(500));

         for(int pass = 0; pass < 2; ++pass) {
            if(pass == 0) {
               RenderState.translucent(false);
            } else {
               RenderState.additive();
            }

            for(int k = 0; k < count; ++k) {
               double phi = 6.283185307179586D * ((double)k + 0.5D * (double)fx.rand(k + 510)) / (double)count;
               double spread = 0.8D + 0.4D * (double)fx.rand(k + 520);
               double dx = b.fx * 0.6D + (b.rx * Math.cos(phi) + b.ux * Math.sin(phi)) * spread;
               double dy = b.fy * 0.6D + (b.ry * Math.cos(phi) + b.uy * Math.sin(phi)) * spread;
               double dz = b.fz * 0.6D + (b.rz * Math.cos(phi) + b.uz * Math.sin(phi)) * spread;
               double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
               double length = (1.3D + 0.9D * (double)fx.rand(k + 530)) * (double)size * (double)grow;
               int c = this.color.getColor(0.3F + 0.7F * fx.rand(k + 540));
               wedge(f, ox, oy, oz, dx / len, dy / len, dz / len, length, 0.03D * (double)size, 0.45D * length, pass == 0?alpha(c, 0.3F * ra * a):alpha(Neon.tint(c), 0.3F * ra * a));
            }
         }
      }

   }

   static void wedge(FxFrame f, double ox, double oy, double oz, double dx, double dy, double dz, double length, double baseWidth, double endWidth, int color) {
      Camera cam = Vfx.camera();
      double bx = f.rx(ox);
      double by = f.ry(oy);
      double bz = f.rz(oz);
      double ez;
      double mz = (bz + (ez = bz + dz * length)) * 0.5D - cam.posZ;
      double ey;
      double my;
      double sx = dy * mz - dz * (my = (by + (ey = by + dy * length)) * 0.5D - cam.posY);
      double sz;
      double ex;
      double mx;
      double sy;
      double sl = Math.sqrt(sx * sx + (sy = dz * (mx = (bx + (ex = bx + dx * length)) * 0.5D - cam.posX) - dx * mz) * sy + (sz = dx * my - dy * mx) * sz);
      if(sl >= 1.0E-9D) {
         double h0 = baseWidth * 0.5D;
         double h1 = endWidth * 0.5D;
         double var51;
         double var52;
         double var53;
         Vfx.quadUV(Textures.RAY, bx - (var53 = sx / sl) * h0, by - (var52 = sy / sl) * h0, bz - (var51 = sz / sl) * h0, 0.0D, 0.0D, color, bx + var53 * h0, by + var52 * h0, bz + var51 * h0, 1.0D, 0.0D, color, ex + var53 * h1, ey + var52 * h1, ez + var51 * h1, 1.0D, 1.0D, color, ex - var53 * h1, ey - var52 * h1, ez - var51 * h1, 0.0D, 1.0D, color);
      }

   }
}
