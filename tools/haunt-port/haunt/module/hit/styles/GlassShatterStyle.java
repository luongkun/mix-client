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
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class GlassShatterStyle extends HitStyleBase {
   private static final int RADIALS = 8;
   private static final int RINGS = 3;
   private static final float BREAK = 0.17F;
   public final SliderSetting shards = (SliderSetting)this.setting(new SliderSetting("glass.shards", "Shards", "How many pieces the pane breaks into.", 8.0D, 24.0D, 13.0D, 1.0D));
   private final Rng rng = new Rng();
   private final double[] angle = new double[8];
   private final double[] reach = new double[8];
   private final double[][] ringR = new double[3][8];

   public GlassShatterStyle(HitEffectModule module) {
      super(module, HitStyle.GLASS_SHATTER, 0.7F, new ColorSetting("glass.color", "Color", "Cracks and shards (gradient: centre to edge).", -4721409));
   }

   private double paneSize(HitFx fx) {
      return 0.62D * (double)fx.size * (fx.crit?1.35D:1.0D);
   }

   private void web(HitFx fx) {
      this.rng.seed(fx.seed, 11);
      double size = this.paneSize(fx);

      for(int k = 0; k < 8; ++k) {
         this.angle[k] = 6.283185307179586D * ((double)k + (double)this.rng.signed() * 0.3D) / 8.0D;
         this.reach[k] = size * (double)this.rng.range(0.85F, 1.1F);

         for(int j = 0; j < 3; ++j) {
            this.ringR[j][k] = size * (0.26D + 0.3D * (double)j) * (double)this.rng.range(0.85F, 1.12F);
         }
      }

   }

   private double px(HitFx fx, double lx, double ly) {
      Basis b = fx.basis;
      return fx.ix() - b.fx * 0.08D + b.rx * lx + b.ux * ly;
   }

   private double py(HitFx fx, double lx, double ly) {
      Basis b = fx.basis;
      return fx.iy() - b.fy * 0.08D + b.ry * lx + b.uy * ly;
   }

   private double pz(HitFx fx, double lx, double ly) {
      Basis b = fx.basis;
      return fx.iz() - b.fz * 0.08D + b.rz * lx + b.uz * ly;
   }

   public void start(HitFx fx) {
      this.web(fx);
      Basis b = fx.basis;
      int want = fx.count(this.shards.getFloat() * (fx.lod?0.5F:1.0F), true);
      int cells = 24;
      this.rng.seed(fx.seed, 29);
      float delay = fx.life * 0.17F;

      for(int i = 0; i < Math.min(want, cells + 8); ++i) {
         int cell = (i * 7 + (int)(this.rng.next() * 3.0F)) % cells;
         int k = cell % 8;
         int j = cell / 8;
         int k2 = (k + 1) % 8;
         double r0 = j == 0?0.02D:this.ringR[j - 1][k];
         double r1 = this.ringR[j][k];
         double r0b = j == 0?0.02D:this.ringR[j - 1][k2];
         double r1b = this.ringR[j][k2];
         double a0 = this.angle[k];
         double a1 = this.angle[k2] + (k2 == 0?6.283185307179586D:0.0D);
         double[] cx = new double[]{Math.cos(a0) * r0, Math.cos(a0) * r1, Math.cos(a1) * (i % 2 == 0?r1b:r0b)};
         double[] cy = new double[]{Math.sin(a0) * r0, Math.sin(a0) * r1, Math.sin(a1) * (i % 2 == 0?r1b:r0b)};
         if(i % 2 == 1) {
            cx[0] = Math.cos(a1) * r1b;
            cy[0] = Math.sin(a1) * r1b;
         }

         double mx = (cx[0] + cx[1] + cx[2]) / 3.0D;
         double my = (cy[0] + cy[1] + cy[2]) / 3.0D;
         double ml = Math.max(1.0E-4D, Math.sqrt(mx * mx + my * my));
         float spread = this.rng.range(0.8F, 2.0F);
         float away = this.rng.range(1.8F, 3.6F);
         double vx = b.fx * (double)away + (b.rx * mx + b.ux * my) / ml * (double)spread;
         double vy = b.fy * (double)away + (b.ry * mx + b.uy * my) / ml * (double)spread + 0.6D;
         double vz = b.fz * (double)away + (b.rz * mx + b.uz * my) / ml * (double)spread;
         fx.particles.spawn(11, this.px(fx, mx, my), this.py(fx, mx, my), this.pz(fx, mx, my), (float)vx, (float)vy, (float)vz, fx.life * 0.83F * this.rng.range(0.85F, 1.0F), 1.0F).end(0.75F).gravity(6.0F).drag(1.2F).axis(this.rng.signed(), this.rng.signed(), this.rng.signed()).spin(0.0F, this.rng.range(7.0F, 15.0F) * (float)(this.rng.next() < 0.5F?-1:1)).shard((float)(cx[0] - mx), (float)(cy[0] - my), (float)(cx[1] - mx), (float)(cy[1] - my), (float)(cx[2] - mx), (float)(cy[2] - my), b.rx, b.ry, b.rz, b.ux, b.uy, b.uz).color(this.color, (float)(ml / this.paneSize(fx))).alpha(fx.opacity).fade(0.01F, 0.55F).delay(delay).follow(fx.anchor, 0.5F);
      }

   }

   public void render(HitFx fx, FxFrame f) {
      float p = fx.progress();
      float a = fx.alpha();
      float snap = Easing.outQuint(Easing.clamp01(p / 0.06F));
      float crackA = a * (1.0F - Easing.smoothstep(0.16F, 0.2F, p));
      if(crackA > 0.003F) {
         this.web(fx);
         double cx = this.px(fx, 0.0D, 0.0D);
         double cy = this.py(fx, 0.0D, 0.0D);
         double cz = this.pz(fx, 0.0D, 0.0D);
         Vfx.sprite(Textures.GLOW, f.rx(cx), f.ry(cy), f.rz(cz), this.paneSize(fx) * 2.2D * (double)snap, 0.0F, alpha(this.color.getColor(0.3F), 0.14F * crackA));
         double w = 0.024D * (double)fx.size;
         int o = 0;

         for(int k = 0; k < 8; ++k) {
            double len = this.reach[k] * (double)snap;

            for(int s = 0; s <= 4; ++s) {
               double r = len * (double)s / 4.0D;
               double jitter = s == 0?0.0D:((double)Rng.hash(fx.seed + k * 13, s) - 0.5D) * 0.18D;
               this.point(fx, f, o + s, Math.cos(this.angle[k] + jitter) * r, Math.sin(this.angle[k] + jitter) * r, w * (1.0D - 0.5D * (double)s / 4.0D), alpha(this.color.getColor((float)s / 4.0F), crackA));
            }

            Neon.add(o, 5, false);
            o += 5;
         }

         float ringSnap = Easing.smoothstep(0.02F, 0.07F, p);

         for(int j = 0; j < 3 && ringSnap > 0.0F; ++j) {
            for(int k = 0; k < 8; ++k) {
               if(Rng.hash(fx.seed + j * 31, k) >= 0.18F) {
                  int k2 = (k + 1) % 8;
                  double a0 = this.angle[k];
                  double a1 = this.angle[k2] + (k2 == 0?6.283185307179586D:0.0D);
                  double r0 = this.ringR[j][k] * (double)snap;
                  double r1 = this.ringR[j][k2] * (double)snap;
                  double mid = (a0 + a1) / 2.0D;
                  double rm = (r0 + r1) / 2.0D * 0.86D;
                  int c = alpha(this.color.getColor((float)(j + 1) / 4.0F), crackA * ringSnap * 0.9F);
                  this.point(fx, f, o, Math.cos(a0) * r0, Math.sin(a0) * r0, w * 0.7D, c);
                  this.point(fx, f, o + 1, Math.cos(mid) * rm, Math.sin(mid) * rm, w * 0.7D, c);
                  this.point(fx, f, o + 2, Math.cos(a1) * r1, Math.sin(a1) * r1, w * 0.7D, c);
                  Neon.add(o, 3, false);
                  o += 3;
               }
            }
         }

         Neon.flush();
      }

      float flash;
      if((flash = Easing.smoothstep(0.14F, 0.17F, p) * (1.0F - Easing.smoothstep(0.17F, 0.37F, p))) > 0.003F) {
         double cx = this.px(fx, 0.0D, 0.0D);
         double cy = this.py(fx, 0.0D, 0.0D);
         double cz = this.pz(fx, 0.0D, 0.0D);
         Vfx.sprite(Textures.GLOW, f.rx(cx), f.ry(cy), f.rz(cz), this.paneSize(fx) * 2.8D, 0.0F, alpha(this.color.getColor(0.5F), 0.5F * flash * a));
         Vfx.sprite(Textures.FLARE, f.rx(cx), f.ry(cy), f.rz(cz), this.paneSize(fx) * 2.0D, 0.3F, alpha(Neon.tint(this.color.getColor(0.5F)), flash * a));
      }

   }

   private void point(HitFx fx, FxFrame f, int i, double lx, double ly, double w, int c) {
      Vfx.X[i] = f.rx(this.px(fx, lx, ly));
      Vfx.Y[i] = f.ry(this.py(fx, lx, ly));
      Vfx.Z[i] = f.rz(this.pz(fx, lx, ly));
      Vfx.W[i] = (float)w;
      Vfx.C[i] = c;
   }
}
