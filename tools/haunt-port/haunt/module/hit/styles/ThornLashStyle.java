package mix.client.mixui.integration.haunt.module.hit.styles;

import java.util.ArrayDeque;
import java.util.Locale;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Chains;
import mix.client.mixui.integration.haunt.fx.kit.Vines;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.ambience.AmbienceModule;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class ThornLashStyle extends HitStyleBase {
   private static final float WHIP = 0.13333334F;
   private static final float COIL = 0.35555556F;
   private static final float HOLD = 0.44444448F;
   private static final float SNAP = 0.5555556F;
   private static final int POINTS = 36;
   private static final int MAX_VINES = 4;
   private static final int MAX_PETALS = 60;
   private static final int MAX_SPLINTERS = 28;
   private static final int POOL = 8;
   private static final int NIGHT_RIM = -3549953;
   private static final double SPLINTER_GRAVITY = 12.0D;
   private static final double PETAL_GRAVITY = 2.6D;
   private static final double PETAL_DRAG = 1.4D;
   public final ColorSetting tipColor;
   public final ColorSetting petalColor;
   public final SliderSetting vines;
   public final SliderSetting thornDensity;
   public final SliderSetting coilTurns;
   public final SliderSetting petals;
   public final SliderSetting drift;
   private final Vines kit = new Vines(144);
   private final Basis coil = new Basis();
   private final float[] wind = new float[3];
   private final double[] point = new double[3];
   private final ArrayDeque<ThornLashStyle.State> pool = new ArrayDeque();

   public ThornLashStyle(HitEffectModule module) {
      super(module, HitStyle.THORN_LASH, 0.9F, new ColorSetting("thornlash.color", "Vines", "The bramble\'s bark.", ColorSetting.Mode.STATIC, -14734816, -12965348, 1.0D));
      this.signature(this.color, -14734816);
      this.tipColor = (ColorSetting)this.setting((new ColorSetting("thornlash.tips", "Thorn Tips", "The thorns\' pale tips.", ColorSetting.Mode.STATIC, -1452088, -2840, 1.0D)).secondary());
      this.signature(this.tipColor, -1452088);
      this.petalColor = (ColorSetting)this.setting((new ColorSetting("thornlash.petals", "Petals", "The rose petals (and a crit\'s rose).", ColorSetting.Mode.STATIC, -5041618, -10614246, 1.0D)).secondary());
      this.signature(this.petalColor, -5041618);
      this.vines = this.look((SliderSetting)this.setting(new SliderSetting("thornlash.vines", "Vines", "How many vines lash out.", 1.0D, 4.0D, 3.0D, 1.0D)));
      this.thornDensity = (SliderSetting)this.setting((new SliderSetting("thornlash.thorns", "Thorn Density", "How thick with thorns the vines are.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%"));
      this.coilTurns = (SliderSetting)this.setting((new SliderSetting("thornlash.turns", "Coil Turns", "How many times the vines wind round.", 0.5D, 2.0D, 1.0D, 0.1D)).unit("x"));
      this.petals = this.look((SliderSetting)this.setting(new SliderSetting("thornlash.petalCount", "Petals", "How many petals burst out as it snaps.", 0.0D, 30.0D, 14.0D, 1.0D)));
      this.drift = (SliderSetting)this.setting((new SliderSetting("thornlash.drift", "Petal Drift", "How much the petals drift on the wind (Ambience\'s, when it\'s on).", 0.0D, 100.0D, 40.0D, 5.0D)).unit("%"));
   }

   public void start(HitFx fx) {
      ThornLashStyle.State s = (ThornLashStyle.State)this.pool.poll();
      if(s == null) {
         s = new ThornLashStyle.State();
      }

      s.snapped = false;
      s.snapAt = 0.0F;
      s.splinters = 0;
      s.petals = 0;
      s.ground = fx.hit.groundY;
      fx.data = s;
   }

   public void finish(HitFx fx) {
      if(fx.data instanceof ThornLashStyle.State && this.pool.size() < 8) {
         this.pool.push((ThornLashStyle.State)fx.data);
      }

      fx.data = null;
   }

   public int passes(HitFx fx) {
      return FxPass.SOLID.bit();
   }

   private void frame(HitFx fx) {
      Basis b = fx.basis;
      double tip = 0.28D + Math.max(0.0D, -b.fy) * 0.6D;
      double st = Math.sin(tip);
      double nx = b.fx * st;
      double ny = Math.cos(tip) + b.fy * st;
      double nz = b.fz * st;
      double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
      this.coil.fx = nx / nl;
      this.coil.fy = ny / nl;
      this.coil.fz = nz / nl;
      double tx = -b.fx;
      double ty = -b.fy;
      double tz = -b.fz;
      double d;
      double tl;
      if((tl = Math.sqrt((tx = tx - this.coil.fx * (d = tx * this.coil.fx + ty * this.coil.fy + tz * this.coil.fz)) * tx + (ty = ty - this.coil.fy * d) * ty + (tz = tz - this.coil.fz * d) * tz)) < 1.0E-6D) {
         tx = -b.rx;
         ty = -b.ry;
         tz = -b.rz;
         tl = 1.0D;
      }

      this.coil.rx = tx / tl;
      this.coil.ry = ty / tl;
      this.coil.rz = tz / tl;
      this.coil.ux = this.coil.fy * this.coil.rz - this.coil.fz * this.coil.ry;
      this.coil.uy = this.coil.fz * this.coil.rx - this.coil.fx * this.coil.rz;
      this.coil.uz = this.coil.fx * this.coil.ry - this.coil.fy * this.coil.rx;
   }

   private static double height(HitFx fx) {
      return Math.max((double)fx.hit.height * 0.38D, Math.min((double)fx.hit.height * 0.72D, fx.hit.offY));
   }

   private static double radius(HitFx fx) {
      return (double)fx.hit.width * 0.5D + 0.06D;
   }

   private void vinePoint(HitFx fx, int k, int count, double u, double loose, double pull) {
      double turns = (double)this.coilTurns.getFloat();
      double dir = (k & 1) == 0?1.0D:-1.0D;
      double a = ((double)k - (double)(count - 1) * 0.5D) * 0.35D + dir * u * turns * 6.283185307179586D;
      double r = radius(fx) * loose * (1.0D + 0.05D * Math.sin(u * 19.0D + (double)k * 2.3D));
      double lift = (((double)k - (double)(count - 1) * 0.5D) * 0.11D + dir * 0.09D * (u - 0.5D)) * scale(fx);
      double c = Math.cos(a);
      double s = Math.sin(a);
      double x = fx.cx() + (this.coil.rx * c + this.coil.ux * s) * r + this.coil.fx * lift;
      double y = fx.anchor.feetY() + height(fx) + (this.coil.ry * c + this.coil.uy * s) * r + this.coil.fy * lift;
      double z = fx.cz() + (this.coil.rz * c + this.coil.uz * s) * r + this.coil.fz * lift;
      double w = pull * Math.max(0.0D, 1.0D - u / 0.22D);
      this.point[0] = x + (fx.ix() - x) * w;
      this.point[1] = y + (fx.iy() - y) * w;
      this.point[2] = z + (fx.iz() - z) * w;
   }

   public void renderPass(HitFx fx, FxFrame f, FxPass pass) {
      ThornLashStyle.State s = (ThornLashStyle.State)fx.data;
      if(s != null) {
         float p = fx.progress();
         float alpha = fx.alpha() * (1.0F - Easing.smoothstep(0.85F, 1.0F, p));
         if(alpha > 0.003F) {
            this.frame(fx);
            Camera cam = Vfx.camera();
            int count = Math.min(4, this.vines.getInt());
            if(p >= 0.5355556F && !s.snapped) {
               this.snap(fx, s, p, count);
            }

            double dt = (double)(f.dt / Math.max(0.25F, fx.timeScale));
            float rim = 0.1F + 0.55F * fx.night;
            MeshBatch.begin(Material.CUSTOM, Finish.MATTE, fx.light, rim, -3549953, true, true);
            float vineAlpha = p < 0.44444448F?1.0F:1.0F - Easing.clamp01((p - 0.44444448F) / 0.111111104F);
            if(vineAlpha > 0.003F) {
               for(int k = 0; k < count; ++k) {
                  this.vine(fx, f, cam, p, k, count, alpha * vineAlpha);
               }
            }

            if(s.snapped) {
               this.splinters(fx, f, s, p, alpha, dt);
            }

            MeshBatch.end();
            MeshBatch.begin(Material.CUSTOM, Finish.SATIN, fx.light, 0.1F + 0.25F * fx.night, -3549953, false, true);
            if(vineAlpha > 0.003F) {
               this.thorns(fx, p, count, alpha * vineAlpha);
            }

            if(fx.crit) {
               this.rose(fx, f, p, alpha);
            }

            if(s.snapped) {
               this.petals(fx, f, s, p, alpha, dt);
            }

            MeshBatch.end();
         }
      }

   }

   private static double grown(float p) {
      return (double)Easing.outCubic(Easing.clamp01(p / 0.13333334F));
   }

   private static double looseness(float p) {
      return p < 0.13333334F?1.35D:(p < 0.35555556F?1.0D + 0.35D * (1.0D - (double)Easing.outBack(Easing.clamp01((p - 0.13333334F) / 0.22222222F), 2.6F)):(p < 0.44444448F?1.0D:1.0D + 0.3D * (double)Easing.outCubic(Easing.clamp01((p - 0.44444448F) / 0.111111104F))));
   }

   private void vine(HitFx fx, FxFrame f, Camera cam, float p, int k, int count, float alpha) {
      double g = grown(p);
      double loose = looseness(p);
      double pull = p < 0.13333334F?1.0D - g:0.0D;
      double thick = 0.036D * scale(fx);
      int at = k * 36;
      int bark = this.color.getColor((float)k / (float)Math.max(1, count - 1));

      for(int j = 0; j < 36; ++j) {
         double u = g * (double)j / 35.0D;
         this.vinePoint(fx, k, count, u, loose, pull);
         int i = at + j;
         this.kit.px[i] = f.rx(this.point[0]);
         this.kit.py[i] = f.ry(this.point[1]);
         this.kit.pz[i] = f.rz(this.point[2]);
         double tip = (double)Easing.clamp01((g - u) / 0.1D + (g >= 1.0D?0.35D:0.0D));
         double knots = Math.sin(u * 41.0D + (double)k * 3.1D) * Math.sin(u * 17.0D + (double)k * 1.7D);
         this.kit.rad[i] = thick * (1.0D - 0.35D * u) * (1.0D + 0.18D * knots) * Math.max(0.15D, tip);
         int c = ColorUtil.shade(bark, (float)(0.88D + 0.16D * Math.sin(u * 29.0D + (double)k * 2.3D)));
         this.kit.col[i] = -16777216 | c & 16777215;
         this.kit.keep[i] = fx.keep.cover(this.kit.px[i], this.kit.py[i], this.kit.pz[i]);
      }

      this.kit.frames(at, 36, false, f.rx(fx.cx()), f.rz(fx.cz()));
      this.kit.tube(at, 36, false, cam, alpha);
   }

   private void thorns(HitFx fx, float p, int count, float alpha) {
      float density = this.thornDensity.getFloat() / 100.0F;
      if(density > 0.0F && p >= 0.06666667F) {
         double flare = p < 0.35555556F?0.35D + 0.65D * (double)Easing.outBack(Easing.clamp01((p - 0.13333334F) / 0.22222222F), 3.0F):1.0D;
         int tip = this.tipColor.getColor(0.5F);
         double g = grown(p);

         for(int k = 0; k < count; ++k) {
            int n = Math.round(density * 16.0F * this.coilTurns.getFloat());

            for(int q = 0; q < n; ++q) {
               double u = ((double)q + 0.5D + 0.6D * (hash(k * 31 + q, 3) - 0.5D)) / (double)n;
               if(u >= 0.06D && u <= g - 0.04D) {
                  double at = u / Math.max(1.0E-6D, g) * 35.0D;
                  double len = 0.075D * scale(fx) * flare * (0.8D + 0.4D * hash(k * 17 + q, 23));
                  this.kit.thorn(k * 36, 36, at, 6.283185307179586D * hash(k * 13 + q, 7), len, tip, 0.0D, alpha);
               }
            }
         }
      }

   }

   private void rose(HitFx fx, FxFrame f, float p, float alpha) {
      float open = Easing.outCubic(Easing.clamp01((p - 0.38444448F) / 0.1411111F));
      if(open > 0.0F && p <= 0.57555556F) {
         Basis b = fx.basis;
         double x = f.rx(fx.ix() - b.fx * 0.08D);
         double y = f.ry(fx.iy() - b.fy * 0.08D);
         double z = f.rz(fx.iz() - b.fz * 0.08D);
         double size = 0.11D * scale(fx) * (0.4D + 0.6D * (double)open);
         float a = alpha * fx.keep.cover(x, y, z) * (1.0F - Easing.clamp01((p - 0.5555556F) / 0.02F));
         this.kit.rose(fx.seed & 7, x, y, z, -b.fx, -b.fy, -b.fz, size, (double)open, this.petalColor.getColor(0.5F) | -16777216, a);
      }

   }

   private void snap(HitFx fx, ThornLashStyle.State s, float p, int count) {
      s.snapped = true;
      s.snapAt = p;
      Basis b = fx.basis;
      s.splinters = fx.lod?0:Math.min(28, 6 + 5 * count);

      for(int i = 0; i < s.splinters; ++i) {
         int id = fx.seed + i * 13;
         int k = i % count;
         this.vinePoint(fx, k, count, 0.08D + 0.9D * hash(id, 1), 1.15D, 0.0D);
         s.sx[i] = this.point[0];
         s.sy[i] = this.point[1];
         s.sz[i] = this.point[2];
         double ox = this.point[0] - fx.cx();
         double oz = this.point[2] - fx.cz();
         double ol = Math.sqrt(ox * ox + oz * oz) + 1.0E-9D;
         double out = 1.2D + 1.4D * hash(id, 2);
         double along = 0.8D + 1.2D * hash(id, 3);
         s.svx[i] = ox / ol * out + b.fx * along;
         s.svy[i] = 1.0D + 1.5D * hash(id, 4) + b.fy * along;
         s.svz[i] = oz / ol * out + b.fz * along;
         s.slen[i] = (0.04D + 0.05D * hash(id, 5)) * scale(fx);
         s.sspin[i] = 10.0D + 14.0D * hash(id, 6);
         double ax = hash(id, 7) - 0.5D;
         double ay = hash(id, 8) - 0.5D;
         double az = hash(id, 9) - 0.5D;
         double al = Math.sqrt(ax * ax + ay * ay + az * az) + 1.0E-9D;
         s.saxis[i * 3] = ax / al;
         s.saxis[i * 3 + 1] = ay / al;
         s.saxis[i * 3 + 2] = az / al;
      }

      s.petals = Math.min(60, Math.round((float)this.petals.getInt() * fx.density * (fx.crit?2.0F:1.0F)));

      for(int var28 = 0; var28 < s.petals; ++var28) {
         int id = fx.seed + var28 * 29 + 7;
         boolean fromRose = fx.crit && var28 < s.petals / 2;
         if(fromRose) {
            this.point[0] = fx.ix() - b.fx * 0.08D;
            this.point[1] = fx.iy() - b.fy * 0.08D;
            this.point[2] = fx.iz() - b.fz * 0.08D;
         } else {
            this.vinePoint(fx, var28 % count, count, 0.1D + 0.85D * hash(id, 1), 1.1D, 0.0D);
         }

         s.px[var28] = this.point[0];
         s.py[var28] = this.point[1];
         s.pz[var28] = this.point[2];
         double out = 1.6D + 1.2D * hash(id, 3);
         double along = 0.5D + 0.6D * hash(id, 4);
         double up = 0.6D + 0.9D * hash(id, 8);
         double cy = fx.anchor.feetY() + height(fx);
         double sx = (this.point[0] - fx.cx()) * b.rx + (this.point[1] - cy) * b.ry + (this.point[2] - fx.cz()) * b.rz;
         double sy = (this.point[0] - fx.cx()) * b.ux + (this.point[1] - cy) * b.uy + (this.point[2] - fx.cz()) * b.uz;
         double sl = Math.sqrt(sx * sx + sy * sy);
         if(fromRose || sl < 0.12D * radius(fx)) {
            double around = hash(id, 2) * 6.283185307179586D;
            sx = Math.cos(around);
            sy = Math.sin(around);
            sl = 1.0D;
         }

         double var30;
         double var31;
         s.pvx[var28] = (b.rx * (var30 = sx / sl) + b.ux * (var31 = sy / sl)) * out + b.fx * along;
         s.pvy[var28] = (b.ry * var30 + b.uy * var31) * out + b.fy * along + up;
         s.pvz[var28] = (b.rz * var30 + b.uz * var31) * out + b.fz * along;
         s.pspin[var28] = hash(id, 5) * 6.283185307179586D;
         s.ptumble[var28] = hash(id, 6) * 6.283185307179586D;
         s.psize[var28] = (0.05D + 0.03D * hash(id, 7)) * scale(fx);
         s.landed[var28] = false;
      }

   }

   private void splinters(HitFx fx, FxFrame f, ThornLashStyle.State s, float p, float alpha, double dt) {
      double t = (double)((p - s.snapAt) * fx.life / Math.max(0.25F, fx.timeScale));
      float fade = 1.0F - Easing.smoothstep(0.12F, 0.3F, (float)t);
      if(fade > 0.003F) {
         int bark = ColorUtil.shade(this.color.getColor(0.5F), 0.8F);
         double w = 0.011D * scale(fx);

         for(int i = 0; i < s.splinters; ++i) {
            s.svy[i] -= 12.0D * dt;
            s.sx[i] += s.svx[i] * dt;
            s.sy[i] += s.svy[i] * dt;
            s.sz[i] += s.svz[i] * dt;
            if(s.sy[i] < s.ground + w) {
               s.sy[i] = s.ground + w;
               s.svy[i] = Math.abs(s.svy[i]) * 0.3D;
               s.svx[i] *= 0.6D;
               s.svz[i] *= 0.6D;
            }

            double x = f.rx(s.sx[i]);
            double y = f.ry(s.sy[i]);
            double z = f.rz(s.sz[i]);
            double ang = s.sspin[i] * t;
            double kx = s.saxis[i * 3];
            double ky = s.saxis[i * 3 + 1];
            double kz = s.saxis[i * 3 + 2];
            double ex = Math.abs(ky) < 0.9D?-kz:0.0D;
            double ey = Math.abs(ky) < 0.9D?0.0D:kz;
            double ez = Math.abs(ky) < 0.9D?kx:-ky;
            double el = Math.sqrt(ex * ex + ey * ey + ez * ez) + 1.0E-9D;
            double var66;
            double var67;
            double fx2 = ky * (var67 = ez / el) - kz * (var66 = ey / el);
            double var65;
            double fy2 = kz * (var65 = ex / el) - kx * var67;
            double fz2 = kx * var66 - ky * var65;
            double c = Math.cos(ang);
            double sn = Math.sin(ang);
            double ax = var65 * c + fx2 * sn;
            double ay = var66 * c + fy2 * sn;
            double az = var67 * c + fz2 * sn;
            double qx = ky * az - kz * ay;
            double qy = kz * ax - kx * az;
            double qz = kx * ay - ky * ax;
            float a = alpha * fade * fx.keep.cover(x, y, z);
            int col = Math.round(255.0F * Easing.clamp01(a)) << 24 | bark & 16777215;
            Chains.box(x, y, z, ax, ay, az, kx, ky, kz, qx, qy, qz, s.slen[i] * 0.5D, w, w * 0.7D, col, 0.0F);
         }
      }

   }

   private void petals(HitFx fx, FxFrame f, ThornLashStyle.State s, float p, float alpha, double dt) {
      if(s.petals != 0) {
         double t = (double)((p - s.snapAt) * fx.life / Math.max(0.25F, fx.timeScale));
         double wx = fx.basis.fx * 0.6D;
         double wz = fx.basis.fz * 0.6D;
         AmbienceModule ambience = !fx.preview && FuriahsVisuals.instance != null?FuriahsVisuals.instance.getAmbience():null;
         if(ambience != null && ambience.windAt(fx.cx(), fx.cy(), fx.cz(), this.wind)) {
            wx = (double)this.wind[0];
            wz = (double)this.wind[2];
         }

         double amount = (double)this.drift.getFloat() / 100.0D;
         int petal = this.petalColor.getColor(0.5F) | -16777216;

         for(int i = 0; i < s.petals; ++i) {
            if(!s.landed[i]) {
               double flutter = Math.sin(t * 9.0D + (double)i * 1.7D) * 0.9D;
               s.pvx[i] += ((wx * amount - s.pvx[i]) * 1.4D + flutter * Math.cos(s.pspin[i])) * dt;
               s.pvz[i] += ((wz * amount - s.pvz[i]) * 1.4D + flutter * Math.sin(s.pspin[i])) * dt;
               s.pvy[i] += (-2.6D - s.pvy[i] * 1.4D * 0.6D) * dt;
               s.px[i] += s.pvx[i] * dt;
               s.py[i] += s.pvy[i] * dt;
               s.pz[i] += s.pvz[i] * dt;
               s.pspin[i] += 3.0D * dt;
               s.ptumble[i] += (5.0D + 3.0D * Math.sin((double)i * 2.1D)) * dt;
               if(s.py[i] < s.ground + 0.01D) {
                  s.py[i] = s.ground + 0.01D;
                  s.landed[i] = true;
                  s.ptumble[i] = 0.0D;
               }
            }

            double x = f.rx(s.px[i]);
            double y = f.ry(s.py[i]);
            double z = f.rz(s.pz[i]);
            float a = alpha * Easing.smoothstep(0.0F, 0.04F, (float)t) * fx.keep.cover(x, y, z);
            this.kit.tumblingPetal(x, y, z, s.pspin[i], s.landed[i]?1.5707963D:s.ptumble[i], s.psize[i], petal, a);
         }
      }

   }

   public String stateForTest(HitFx fx) {
      ThornLashStyle.State s = (ThornLashStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float p = fx.progress();
         String phase = p < 0.13333334F?"whipping out":(p < 0.35555556F?"coiling":(p < 0.44444448F?"holding":(p < 0.5555556F?"snapping":"petals")));
         int landed = 0;

         for(int i = 0; i < s.petals; ++i) {
            landed += s.landed[i]?1:0;
         }

         return String.format(Locale.ROOT, "%s: %d vine(s) %.1f turn(s), grown %.2f, coil x%.2f; %d splinters, %d petals (%d landed); light %.2f, night %.2f", new Object[]{phase, Integer.valueOf(Math.min(4, this.vines.getInt())), Float.valueOf(this.coilTurns.getFloat()), Double.valueOf(grown(p)), Double.valueOf(looseness(p)), Integer.valueOf(s.splinters), Integer.valueOf(s.petals), Integer.valueOf(landed), Float.valueOf(fx.light), Float.valueOf(fx.night)});
      }
   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8 & 16777215) / 1.6777216E7D;
   }

   private static final class State {
      boolean snapped;
      float snapAt;
      int splinters;
      int petals;
      final double[] sx;
      final double[] sy;
      final double[] sz;
      final double[] svx;
      final double[] svy;
      final double[] svz;
      final double[] slen;
      final double[] sspin;
      final double[] saxis;
      final double[] px;
      final double[] py;
      final double[] pz;
      final double[] pvx;
      final double[] pvy;
      final double[] pvz;
      final double[] pspin;
      final double[] ptumble;
      final double[] psize;
      final boolean[] landed;
      double ground;

      private State() {
         this.sx = new double[28];
         this.sy = new double[28];
         this.sz = new double[28];
         this.svx = new double[28];
         this.svy = new double[28];
         this.svz = new double[28];
         this.slen = new double[28];
         this.sspin = new double[28];
         this.saxis = new double[84];
         this.px = new double[60];
         this.py = new double[60];
         this.pz = new double[60];
         this.pvx = new double[60];
         this.pvy = new double[60];
         this.pvz = new double[60];
         this.pspin = new double[60];
         this.ptumble = new double[60];
         this.psize = new double[60];
         this.landed = new boolean[60];
      }
   }
}
