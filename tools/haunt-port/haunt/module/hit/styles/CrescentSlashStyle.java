package mix.client.mixui.integration.haunt.module.hit.styles;

import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class CrescentSlashStyle extends HitStyleBase {
   private static final int POINTS = 40;
   private static final double SPAN = Math.toRadians(118.0D);
   private static final float DRAW = 0.2F;
   public final SliderSetting width = (SliderSetting)this.setting((new SliderSetting("slash.width", "Blade Width", "How thick the crescent is.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   public final BooleanSetting sparks = (BooleanSetting)this.setting(new BooleanSetting("slash.sparks", "Sparks", "Sparks flying off the arc.", true));
   private final Rng rng = new Rng();
   private final Basis plane = new Basis();
   private final double[] point = new double[3];
   private int slashOffset;

   public CrescentSlashStyle(HitEffectModule module) {
      super(module, HitStyle.CRESCENT_SLASH, 0.35F, new ColorSetting("slash.color", "Color", "Blade glow (gradient: leading edge to tail).", -45715));
   }

   private void frame(HitFx fx, int k) {
      Basis b = fx.basis;
      double ux = fx.hit.swipeX;
      double uy = fx.hit.swipeY;
      double uz = fx.hit.swipeZ;
      if(k == 1) {
         double c = Math.cos(1.22D);
         double s = Math.sin(1.22D);
         double vx = b.fy * uz - b.fz * uy;
         double vy = b.fz * ux - b.fx * uz;
         double vz = b.fx * uy - b.fy * ux;
         ux = ux * c + vx * s;
         uy = uy * c + vy * s;
         uz = uz * c + vz * s;
      }

      this.plane.rx = ux;
      this.plane.ry = uy;
      this.plane.rz = uz;
      this.plane.ux = uy * b.fz - uz * b.fy;
      this.plane.uy = uz * b.fx - ux * b.fz;
      this.plane.uz = ux * b.fy - uy * b.fx;
      double vl = Math.sqrt(this.plane.ux * this.plane.ux + this.plane.uy * this.plane.uy + this.plane.uz * this.plane.uz);
      if(vl > 1.0E-9D) {
         this.plane.ux /= vl;
         this.plane.uy /= vl;
         this.plane.uz /= vl;
      }

   }

   private double radius(HitFx fx) {
      return 0.95D * (double)fx.size * (fx.crit?1.15D:1.0D);
   }

   public void start(HitFx fx) {
      if(this.sparks.get() && !fx.lod) {
         this.rng.seed(fx.seed);
         int slashes = fx.crit?2:1;

         for(int k = 0; k < slashes; ++k) {
            this.frame(fx, k);
            double r = this.radius(fx);
            int n = fx.count(6.0F, false);

            for(int i = 0; i < n; ++i) {
               float s = this.rng.range(0.15F, 0.9F);
               double theta = -SPAN / 2.0D + SPAN * (double)s;
               double[] pt = this.arcPoint(fx, r, theta);
               double tx = -this.plane.ux * Math.sin(theta) + this.plane.rx * Math.cos(theta);
               double ty = -this.plane.uy * Math.sin(theta) + this.plane.ry * Math.cos(theta);
               double tz = -this.plane.uz * Math.sin(theta) + this.plane.rz * Math.cos(theta);
               float speed = this.rng.range(2.5F, 5.0F);
               Basis b = fx.basis;
               fx.particles.spawn(2, pt[0], pt[1], pt[2], (float)(tx * (double)speed + b.fx * 1.5D), (float)(ty * (double)speed + b.fy * 1.5D) + 0.5F, (float)(tz * (double)speed + b.fz * 1.5D), fx.seconds(this.rng.range(0.22F, 0.35F)), 0.018F * fx.size).gravity(7.0F).drag(2.0F).color(this.color, 1.0F - s).hot(0.6F).alpha(fx.opacity).delay(fx.life * 0.2F * s + (float)k * fx.life * 0.12F).follow(fx.anchor, 0.5F);
            }
         }
      }

   }

   private double[] arcPoint(HitFx fx, double r, double theta) {
      Basis b = fx.basis;
      double cx = fx.ix() - b.fx * 0.14D - this.plane.ux * r * 0.82D;
      double cy = fx.iy() - b.fy * 0.14D - this.plane.uy * r * 0.82D;
      double cz = fx.iz() - b.fz * 0.14D - this.plane.uz * r * 0.82D;
      double c = Math.cos(theta);
      double s = Math.sin(theta);
      this.point[0] = cx + (this.plane.ux * c + this.plane.rx * s) * r;
      this.point[1] = cy + (this.plane.uy * c + this.plane.ry * s) * r;
      this.point[2] = cz + (this.plane.uz * c + this.plane.rz * s) * r;
      return this.point;
   }

   public void render(HitFx fx, FxFrame f) {
      int slashes = fx.crit?2:1;
      this.slashOffset = 0;

      for(int k = 0; k < slashes; ++k) {
         float p = Easing.clamp01((fx.progress() - (float)k * 0.12F) / (1.0F - (float)k * 0.12F));
         if(p > 0.0F) {
            this.frame(fx, k);
            this.drawSlash(fx, f, p, this.radius(fx) * (k == 1?0.85D:1.0D));
         }
      }

      Neon.flush();
   }

   private void drawSlash(HitFx fx, FxFrame f, float p, double r) {
      float head = Easing.outCubic(Easing.clamp01(p / 0.2F));
      float thin = 1.0F - 0.85F * Easing.smoothstep(0.25F, 1.0F, p);
      float a = fx.alpha() * (1.0F - Easing.smoothstep(0.45F, 1.0F, p));
      if(a > 0.003F && head > 0.01F) {
         double w = 0.2D * (double)fx.size * (double)this.width.getFloat() * (fx.crit?1.2D:1.0D) * (double)thin;
         int n = Math.max(2, Math.round(40.0F * head));
         int o = this.slashOffset;

         for(int i = 0; i < n; ++i) {
            float s = head * (float)i / (float)(n - 1);
            double[] pt = this.arcPoint(fx, r, -SPAN / 2.0D + SPAN * (double)s);
            Vfx.X[o + i] = f.rx(pt[0]);
            Vfx.Y[o + i] = f.ry(pt[1]);
            Vfx.Z[o + i] = f.rz(pt[2]);
            float taper = (float)Math.pow(Math.sin(3.141592653589793D * (double)s), 0.9D);
            float hot = 1.0F + 0.6F * (float)Math.exp((double)(-(head - s) * (head - s)) / 0.012D) * (1.0F - Easing.smoothstep(0.0F, 0.35F, p));
            Vfx.W[o + i] = (float)(w * (double)taper);
            Vfx.C[o + i] = alpha(this.color.getColor(1.0F - s), Math.min(1.0F, a * hot) * (0.3F + 0.7F * taper));
         }

         Neon.add(o, n, false);
         this.slashOffset += n;
      }

   }
}
