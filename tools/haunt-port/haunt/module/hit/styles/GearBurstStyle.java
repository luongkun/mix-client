package mix.client.mixui.integration.haunt.module.hit.styles;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Clockwork;
import mix.client.mixui.integration.haunt.fx.look.KeepVisible;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.physics.PhysicsClock;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class GearBurstStyle extends HitStyleBase {
   private static final float POP = 0.13333334F;
   private static final float SWEEP = 0.46666667F;
   private static final float BURST = 0.53333336F;
   private static final float FADE = 0.88F;
   private static final int MAX_PIECES = 40;
   private static final int MAX_SPRINGS = 6;
   private static final int POOL = 8;
   private static final double GRAVITY = 18.0D;
   private static final double BOUNCE = 0.32D;
   private static final double ROLL_FRICTION = 3.0D;
   private static final double PHYS_SPEED = 1.8D;
   private static final float RIM = 0.16F;
   private static final int ENGRAVING = -15068148;
   private static final int FLYING = 0;
   private static final int BOUNCED = 1;
   private static final int ROLLING = 2;
   private static final int RESTING = 3;
   public final ColorSetting tint;
   public final ModeSetting<Material> material;
   public final ModeSetting<Finish> finish;
   public final SliderSetting gears;
   public final ModeSetting<GearBurstStyle.Dial> dial;
   public final ModeSetting<GearBurstStyle.Sweep> sweep;
   public final SliderSetting size;
   public final BooleanSetting springs;
   private double dx;
   private double dy;
   private double dz;
   private double ux;
   private double uy;
   private double uz;
   private double rx;
   private double ry;
   private double rz;
   private double ox;
   private double oy;
   private double oz;
   private final double[] point = new double[3];
   private static final double[] AX = new double[3];
   private final ArrayDeque<GearBurstStyle.State> pool = new ArrayDeque();

   public GearBurstStyle(HitEffectModule module) {
      super(module, HitStyle.GEAR_BURST, 0.75F, new ColorSetting("gearburst.color", "Glow", "The hand\'s tip and the burst.", ColorSetting.Mode.STATIC, -6237, -15782, 1.0D));
      this.signature(this.color, -6237);
      this.material = this.materialSetting("gearburst", Material.BRASS, "What the dial and the gears are made of. Custom takes the Tint, with a finish.");
      this.finish = this.finishSetting("gearburst", Finish.METALLIC, this.material);
      this.tint = (ColorSetting)this.setting((new ColorSetting("gearburst.tint", "Tint", "The metal\'s color with the Custom material.", ColorSetting.Mode.STATIC, -3562674, -1521798, 1.0D)).secondary());
      this.tint.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return GearBurstStyle.this.material.get() == Material.CUSTOM;
         }
      });
      this.signature(this.tint, -3562674);
      this.gears = this.look((SliderSetting)this.setting(new SliderSetting("gearburst.gears", "Gears", "How many gears the dial bursts into.", 4.0D, 16.0D, 8.0D, 1.0D)));
      this.dial = (ModeSetting)this.setting((new ModeSetting("gearburst.dial", "Dial", "What is engraved round the dial.", GearBurstStyle.Dial.class, GearBurstStyle.Dial.TICKS)).style(ModeSetting.Style.CHIPS));
      this.sweep = (ModeSetting)this.setting(new ModeSetting("gearburst.sweep", "Sweep", "How the hand goes round.", GearBurstStyle.Sweep.class, GearBurstStyle.Sweep.TICKING));
      this.size = this.look((SliderSetting)this.setting((new SliderSetting("gearburst.size", "Size", "How big the dial and its gears are.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%")));
      this.springs = (BooleanSetting)this.setting(new BooleanSetting("gearburst.springs", "Springs", "A few coil springs burst out with the gears.", true));
   }

   public void start(HitFx fx) {
      GearBurstStyle.State s = (GearBurstStyle.State)this.pool.poll();
      if(s == null) {
         s = new GearBurstStyle.State();
      }

      s.burst = false;
      s.burstAt = 0.0F;
      s.seconds = 0.0D;
      s.pieces = 0;
      s.floor = fx.hit.groundY;
      fx.data = s;
   }

   public void finish(HitFx fx) {
      if(fx.data instanceof GearBurstStyle.State && this.pool.size() < 8) {
         this.pool.push((GearBurstStyle.State)fx.data);
      }

      fx.data = null;
   }

   public int passes(HitFx fx) {
      int solid = ((Material)this.material.get()).translucent((Finish)this.finish.get())?FxPass.TRANSLUCENT.bit():FxPass.SOLID.bit();
      return fx.lod?solid:solid | FxPass.GLOW.bit();
   }

   private double radius(HitFx fx) {
      return 0.24D * scale(fx) * (double)this.size.getFloat() / 100.0D;
   }

   private void plane(HitFx fx) {
      Basis b = fx.basis;
      this.dx = fx.ix() - b.fx * 0.12D;
      this.dy = fx.iy() - b.fy * 0.12D;
      this.dz = fx.iz() - b.fz * 0.12D;
      this.ux = b.ux;
      this.uy = b.uy;
      this.uz = b.uz;
      this.rx = b.rx;
      this.ry = b.ry;
      this.rz = b.rz;
      this.ox = -b.fx;
      this.oy = -b.fy;
      this.oz = -b.fz;
   }

   private float hand(HitFx fx, float p) {
      double t = (double)Easing.clamp01((p - 0.13333334F) / 0.3333333F);
      double turn = Clockwork.ratchet(t * 12.0D, 1.0D, 0.5235987755982988D, this.sweep.get() == GearBurstStyle.Sweep.SMOOTH);
      return (float)Math.min(6.283185307179586D, turn);
   }

   public void renderPass(HitFx fx, FxFrame f, FxPass pass) {
      GearBurstStyle.State s = (GearBurstStyle.State)fx.data;
      if(s != null) {
         float p = fx.progress();
         float alpha = fx.alpha() * (1.0F - Easing.smoothstep(0.88F, 1.0F, p));
         if(alpha > 0.003F) {
            this.plane(fx);
            if(pass == FxPass.GLOW) {
               this.glow(fx, f, s, p, alpha);
            } else {
               boolean custom = this.material.get() == Material.CUSTOM;
               double time = ColorSetting.clock();
               if(p >= 0.46666667F && !s.burst) {
                  this.breakLoose(fx, s, p);
               }

               if(s.burst) {
                  this.step(fx, s, (double)f.dt * 1.8D / (double)Math.max(0.25F, fx.timeScale));
               }

               MeshBatch.begin((Material)this.material.get(), (Finish)this.finish.get(), fx.light, 0.16F, -1, true, pass == FxPass.SOLID);
               float dialAlpha = p < 0.46666667F?1.0F:1.0F - Easing.clamp01((p - 0.46666667F) / 0.06666669F);
               if(dialAlpha > 0.003F) {
                  this.dials(fx, f, p, alpha * dialAlpha, custom, time);
               }

               if(s.burst) {
                  this.pieces(fx, f, s, alpha, custom, time);
               }

               MeshBatch.end();
               if(dialAlpha > 0.003F && p >= 0.06666667F) {
                  this.engraving(fx, f, p, alpha * dialAlpha);
               }
            }
         }
      }

   }

   private int metal(boolean custom, float along, double time, float a) {
      int c = custom?this.tint.getColor(along, time):-1;
      int a8 = Math.round(Easing.clamp01(a) * (float)(custom?c >>> 24:255));
      return a8 << 24 | c & 16777215;
   }

   private void dials(HitFx fx, FxFrame f, float p, float alpha, boolean custom, double time) {
      double r = this.radius(fx);
      float pop = Easing.outBack(Easing.clamp01(p / 0.060000002F), 2.2F);
      float lid = Easing.outCubic(Easing.clamp01((p - 0.040000003F) / 0.093333334F));
      double grow = p > 0.46666667F?1.0D + 0.15D * (double)Easing.clamp01((p - 0.46666667F) / 0.06666669F):1.0D;
      double sr = r * (double)pop * grow;
      if(sr > 1.0E-4D) {
         double cx = f.rx(this.dx);
         double cy = f.ry(this.dy);
         double cz = f.rz(this.dz);
         int col = this.metal(custom, 0.5F, time, alpha * fx.keep.cover(cx, cy, cz));
         float handAngle = this.hand(fx, p);
         this.dial(cx, cy, cz, sr, col, (double)handAngle, fx.crit?0.0D:1.0D);
         if(fx.crit) {
            double ir = sr * 0.55D;
            double lift = sr * 0.07D;
            this.dial(cx + this.ox * lift, cy + this.oy * lift, cz + this.oz * lift, ir, col, (double)(-handAngle), 1.0D);
         }

         double phi = Math.toRadians(150.0D) * (double)lid;
         double t = sr * 0.06D;
         double cphi = Math.cos(phi);
         double sphi = Math.sin(phi);
         double hx = cx - this.rx * sr + this.ox * sr * 0.05D;
         double hy = cy - this.ry * sr + this.oy * sr * 0.05D;
         double hz = cz - this.rz * sr + this.oz * sr * 0.05D;
         double mx = this.rx * cphi + this.ox * sphi;
         double my = this.ry * cphi + this.oy * sphi;
         double mz = this.rz * cphi + this.oz * sphi;
         double nx = this.ox * cphi - this.rx * sphi;
         double ny = this.oy * cphi - this.ry * sphi;
         double nz = this.oz * cphi - this.rz * sphi;
         double lx = hx + mx * sr + nx * t * 0.5D;
         double ly = hy + my * sr + ny * t * 0.5D;
         double lz = hz + mz * sr + nz * t * 0.5D;
         int lidCol = this.metal(custom, 0.5F, time, alpha * fx.keep.cover(lx, ly, lz));
         Clockwork.ring(lx, ly, lz, nx, ny, nz, mx, my, mz, this.ux, this.uy, this.uz, sr * 0.03D, sr * 0.98D, t, 24, lidCol, 0.15F);
         Clockwork.ring(lx + nx * t * 0.5D, ly + ny * t * 0.5D, lz + nz * t * 0.5D, nx, ny, nz, mx, my, mz, this.ux, this.uy, this.uz, sr * 0.03D, sr * 0.25D, t * 0.6D, 16, lidCol, 0.15F);
      }

   }

   private void dial(double cx, double cy, double cz, double r, int col, double angle, double full) {
      double t = r * 0.1D;
      Clockwork.ring(cx, cy, cz, this.ox, this.oy, this.oz, this.rx, this.ry, this.rz, this.ux, this.uy, this.uz, r * 0.86D, r, t, 32, col, 0.1F);
      double fx = cx - this.ox * t * 0.25D;
      double fy = cy - this.oy * t * 0.25D;
      double fz = cz - this.oz * t * 0.25D;
      Clockwork.ring(fx, fy, fz, this.ox, this.oy, this.oz, this.rx, this.ry, this.rz, this.ux, this.uy, this.uz, full > 0.5D?r * 0.03D:r * 0.55D, r * 0.88D, t * 0.4D, 32, col, 0.05F);
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      double hx = this.ux * c + this.rx * s;
      double hy = this.uy * c + this.ry * s;
      double hz = this.uz * c + this.rz * s;
      Clockwork.hand(cx + this.ox * t * 0.35D, cy + this.oy * t * 0.35D, cz + this.oz * t * 0.35D, this.ox, this.oy, this.oz, hx, hy, hz, r * 0.7D, r * 0.13D, r * 0.035D, col, 0.3F);
   }

   private void engraving(HitFx fx, FxFrame f, float p, float alpha) {
      double r = this.radius(fx) * (double)Easing.outBack(Easing.clamp01(p / 0.060000002F), 2.2F);
      if(r > 1.0E-4D) {
         double cx = f.rx(this.dx);
         double cy = f.ry(this.dy);
         double cz = f.rz(this.dz);
         float a = 0.78F * alpha * fx.keep.cover(cx, cy, cz);
         int dark = ColorUtil.withAlpha(-15068148, a);
         double lift = r * 0.1D * 0.05D + 0.001D;
         double lx = this.ox * lift;
         double ly = this.oy * lift;
         double lz = this.oz * lift;
         int face = this.faceSign();
         RenderState.decal(true);
         boolean inner = fx.crit;
         Clockwork.ticks(cx, cy, cz, this.ux, this.uy, this.uz, this.rx, this.ry, this.rz, lx, ly, lz, r * 0.75D, r * 0.2D, 0.0D, dark, (KeepVisible)null);
         GearBurstStyle.Dial d = (GearBurstStyle.Dial)this.dial.get();
         double hour = 0.5235987755982988D;
         if(d != GearBurstStyle.Dial.TICKS) {
            if(inner) {
               double ir = r * 0.55D;
               double il = r * 0.07D + lift;
               Clockwork.glyphs(cx, cy, cz, this.ux, this.uy, this.uz, this.rx, this.ry, this.rz, this.ox * il, this.oy * il, this.oz * il, ir * 0.62D, ir * 0.3D, 0.0D, hour, face, d == GearBurstStyle.Dial.ROMAN, dark, (KeepVisible)null);
            } else {
               Clockwork.glyphs(cx, cy, cz, this.ux, this.uy, this.uz, this.rx, this.ry, this.rz, lx, ly, lz, r * 0.52D, r * 0.22D, 0.0D, hour, face, d == GearBurstStyle.Dial.ROMAN, dark, (KeepVisible)null);
            }
         } else if(inner) {
            double ir = r * 0.55D;
            double il = r * 0.07D + lift;
            Clockwork.ticks(cx, cy, cz, this.ux, this.uy, this.uz, this.rx, this.ry, this.rz, this.ox * il, this.oy * il, this.oz * il, ir * 0.75D, ir * 0.2D, 0.0D, dark, (KeepVisible)null);
         }

         RenderState.decal(false);
      }

   }

   private int faceSign() {
      double nx = this.uy * this.rz - this.uz * this.ry;
      double ny = this.uz * this.rx - this.ux * this.rz;
      double nz = this.ux * this.ry - this.uy * this.rx;
      return nx * this.ox + ny * this.oy + nz * this.oz > 0.0D?-1:1;
   }

   private void breakLoose(HitFx fx, GearBurstStyle.State s, float p) {
      s.burst = true;
      s.burstAt = p;
      s.seconds = 0.0D;
      double r = this.radius(fx);
      int g = Math.min(34, Math.round((float)this.gears.getInt() * fx.density * (fx.crit?2.0F:1.0F)));
      int sp = this.springs.get()?Math.min(6, fx.crit?4:2):0;
      s.gears = Math.max(1, g);
      s.pieces = s.gears + sp;
      s.floor = fx.hit.groundY;
      Basis b = fx.basis;

      for(int i = 0; i < s.pieces; ++i) {
         boolean gear = i < s.gears;
         int id = fx.seed + i * 31;
         double a = hash(id, 1) * 3.141592653589793D * 2.0D;
         double d = r * 0.7D * Math.sqrt(hash(id, 2));
         double ca = Math.cos(a);
         double sa = Math.sin(a);
         s.x[i] = s.lx[i] = this.dx + (this.ux * ca + this.rx * sa) * d;
         s.y[i] = s.ly[i] = this.dy + (this.uy * ca + this.ry * sa) * d;
         s.z[i] = s.lz[i] = this.dz + (this.uz * ca + this.rz * sa) * d;
         double cone = Math.toRadians(45.0D) * Math.sqrt(hash(id, 3));
         double around = hash(id, 4) * 3.141592653589793D * 2.0D;
         double sc = Math.sin(cone);
         double cc = Math.cos(cone);
         double wx = b.fx * cc + (b.rx * Math.cos(around) + b.ux * Math.sin(around)) * sc;
         double wy = b.fy * cc + (b.ry * Math.cos(around) + b.uy * Math.sin(around)) * sc;
         double wz = b.fz * cc + (b.rz * Math.cos(around) + b.uz * Math.sin(around)) * sc;
         double speed = 1.0D + 0.8D * hash(id, 5);
         double out = 0.4D + 0.4D * hash(id, 6);
         s.vx[i] = wx * speed + (this.ux * ca + this.rx * sa) * out;
         s.vy[i] = wy * speed + (this.uy * ca + this.ry * sa) * out + 0.2D + 0.7D * hash(id, 7);
         s.vz[i] = wz * speed + (this.uz * ca + this.rz * sa) * out;
         double k = hash(id, 8);
         s.radius[i] = gear?r * (k < 0.25D?0.36D + 0.1D * hash(id, 9):0.18D + 0.12D * hash(id, 9)):r * 0.42D;
         s.teeth[i] = gear?(k < 0.25D?12 + 2 * (int)(hash(id, 10) * 3.0D):8 + 2 * (int)(hash(id, 10) * 2.0D)):0;
         s.spokes[i] = gear && k < 0.25D?3 + (int)(hash(id, 11) * 3.0D):0;
         s.turn[i] = hash(id, 12) * 3.141592653589793D * 2.0D;
         s.spin[i] = (double)(hash(id, 13) < 0.5D?-1:1) * (10.0D + 14.0D * hash(id, 14));
         s.axle[i * 3] = this.ox;
         s.axle[i * 3 + 1] = this.oy;
         s.axle[i * 3 + 2] = this.oz;
         double tx = hash(id, 15) - 0.5D;
         double ty = hash(id, 16) - 0.5D;
         double tz = hash(id, 17) - 0.5D;
         double tl = Math.sqrt(tx * tx + ty * ty + tz * tz) + 1.0E-9D;
         s.tumbleAxis[i * 3] = tx / tl;
         s.tumbleAxis[i * 3 + 1] = ty / tl;
         s.tumbleAxis[i * 3 + 2] = tz / tl;
         s.tumble[i] = 0.0D;
         s.tumbleRate[i] = 5.0D + 7.0D * hash(id, 18);
         s.phase[i] = 0;
         s.bounced[i] = -1.0D;
      }

   }

   private void step(HitFx fx, GearBurstStyle.State s, double dt) {
      int steps = s.clock.advance(dt);
      double h = 0.008333333333333333D;

      for(int n = 0; n < steps; ++n) {
         s.seconds += h;

         for(int i = 0; i < s.pieces; ++i) {
            s.lx[i] = s.x[i];
            s.ly[i] = s.y[i];
            s.lz[i] = s.z[i];
            switch(s.phase[i]) {
            case 0:
            case 1:
               s.vy[i] -= 18.0D * h;
               s.x[i] += s.vx[i] * h;
               s.y[i] += s.vy[i] * h;
               s.z[i] += s.vz[i] * h;
               s.turn[i] += s.spin[i] * h;
               s.tumble[i] += s.tumbleRate[i] * h;
               double low = s.floor + s.radius[i] * 0.5D;
               if(s.y[i] < low && s.vy[i] < 0.0D) {
                  s.y[i] = low;
                  if(s.phase[i] == 0) {
                     s.vy[i] = -s.vy[i] * 0.32D;
                     s.vx[i] *= 0.75D;
                     s.vz[i] *= 0.75D;
                     s.tumbleRate[i] *= 0.5D;
                     s.spin[i] *= 0.6D;
                     s.bounced[i] = s.seconds;
                     s.phase[i] = 1;
                  } else if(i < s.gears) {
                     double spx = Math.sqrt(s.vx[i] * s.vx[i] + s.vz[i] * s.vz[i]);
                     double wx = spx > 0.001D?s.vx[i] / spx:fx.basis.fx;
                     double wz = spx > 0.001D?s.vz[i] / spx:fx.basis.fz;
                     double wl = Math.sqrt(wx * wx + wz * wz) + 1.0E-9D;
                     s.roll[i * 2] = wx / wl;
                     s.roll[i * 2 + 1] = wz / wl;
                     s.vx[i] = Math.max(0.4D, spx) + 0.3D;
                     s.y[i] = s.floor + s.radius[i];
                     s.vy[i] = 0.0D;
                     s.phase[i] = 2;
                  } else {
                     s.phase[i] = 3;
                  }
               }
               break;
            case 2:
               double sp;
               s.vx[i] = sp = Math.max(0.0D, s.vx[i] - 3.0D * h);
               s.x[i] += s.roll[i * 2] * sp * h;
               s.z[i] += s.roll[i * 2 + 1] * sp * h;
               s.turn[i] -= sp / Math.max(0.001D, s.radius[i]) * h;
               if(sp <= 0.0D) {
                  s.phase[i] = 3;
               }
            }
         }
      }

   }

   private void pieces(HitFx fx, FxFrame f, GearBurstStyle.State s, float alpha, boolean custom, double time) {
      double a = s.clock.alpha();
      double thick = this.radius(fx) * 0.09D;

      for(int i = 0; i < s.pieces; ++i) {
         double x = f.rx(s.lx[i] + (s.x[i] - s.lx[i]) * a);
         double y = f.ry(s.ly[i] + (s.y[i] - s.ly[i]) * a);
         double z = f.rz(s.lz[i] + (s.z[i] - s.lz[i]) * a);
         axle(s, i);
         float k = alpha * fx.keep.solid(x, y, z);
         int col = this.metal(custom, (float)i / (float)s.pieces, time, k);
         float glint = 0.05F;
         if(s.bounced[i] >= 0.0D) {
            double since = s.seconds - s.bounced[i];
            glint += (float)(0.9D * Math.exp(-since / 0.06D));
         }

         if(i < s.gears && s.spokes[i] > 0) {
            Clockwork.gear(x, y, z, AX[0], AX[1], AX[2], s.radius[i], thick, s.turn[i], s.teeth[i], s.spokes[i], col, glint);
         } else if(i < s.gears) {
            Clockwork.kiteGear(x, y, z, AX[0], AX[1], AX[2], s.radius[i], thick * 1.2D, s.turn[i], col);
         } else {
            double half = s.radius[i] * 0.5D;
            Clockwork.spring(x - AX[0] * half, y - AX[1] * half, z - AX[2] * half, x + AX[0] * half, y + AX[1] * half, z + AX[2] * half, s.radius[i] * 0.22D, s.radius[i] * 0.05D, 5, col, glint);
         }
      }

   }

   private static void axle(GearBurstStyle.State s, int i) {
      if(s.phase[i] == 2 || s.phase[i] == 3 && i < s.gears && s.roll[i * 2] * s.roll[i * 2] + s.roll[i * 2 + 1] * s.roll[i * 2 + 1] > 0.5D) {
         AX[0] = -s.roll[i * 2 + 1];
         AX[1] = 0.0D;
         AX[2] = s.roll[i * 2];
      } else {
         double kx = s.tumbleAxis[i * 3];
         double ky = s.tumbleAxis[i * 3 + 1];
         double kz = s.tumbleAxis[i * 3 + 2];
         double x = s.axle[i * 3];
         double y = s.axle[i * 3 + 1];
         double z = s.axle[i * 3 + 2];
         double c = Math.cos(s.tumble[i]);
         double sn = Math.sin(s.tumble[i]);
         double d = kx * x + ky * y + kz * z;
         AX[0] = x * c + (ky * z - kz * y) * sn + kx * d * (1.0D - c);
         AX[1] = y * c + (kz * x - kx * z) * sn + ky * d * (1.0D - c);
         AX[2] = z * c + (kx * y - ky * x) * sn + kz * d * (1.0D - c);
      }

   }

   private void glow(HitFx fx, FxFrame f, GearBurstStyle.State s, float p, float alpha) {
      int col = this.color.getColor(0.5F);
      double r = this.radius(fx);
      double cx = f.rx(this.dx);
      double cy = f.ry(this.dy);
      double cz = f.rz(this.dz);
      boolean calm = FuriahsVisuals.instance.isReduceFlashing();
      if(p >= 0.13333334F && p < 0.46666667F) {
         float angle = this.hand(fx, p);
         double c = Math.cos((double)angle);
         double sn = Math.sin((double)angle);
         double tip = r * 0.66D;
         double lift = r * 0.06D;
         double hx = cx + (this.ux * c + this.rx * sn) * tip + this.ox * lift;
         double hy = cy + (this.uy * c + this.ry * sn) * tip + this.oy * lift;
         double hz = cz + (this.uz * c + this.rz * sn) * tip + this.oz * lift;
         double t = (double)Easing.clamp01((p - 0.13333334F) / 0.3333333F) * 12.0D;
         float tick = this.sweep.get() == GearBurstStyle.Sweep.SMOOTH?0.6F:0.45F + 0.55F * (float)Math.exp(-(t - Math.floor(t)) * 6.0D);
         Vfx.sprite(Textures.GLOW, hx, hy, hz, r * 0.5D, 0.0F, alpha(col, 0.55F * alpha * tick));
         Vfx.sprite(Textures.SPARK, hx, hy, hz, r * 0.22D, 0.0F, core(col, 0.8F * alpha * tick));
      }

      if(p >= 0.46666667F && p < 0.59333336F) {
         float b = Easing.clamp01((p - 0.46666667F) / 0.1266667F);
         float k = (1.0F - b) * alpha;
         if(calm) {
            Vfx.sprite(Textures.GLOW, cx, cy, cz, r * 2.2D, 0.0F, alpha(col, 0.3F * k));
         } else {
            Vfx.sprite(Textures.GLOW, cx, cy, cz, r * (2.4D + 1.6D * (double)b), 0.0F, alpha(col, 0.65F * k));
            Vfx.sprite(Textures.FLARE, cx, cy, cz, r * (1.6D + 2.0D * (double)b), (float)((double)b * 1.5D), core(col, 0.9F * k));
         }
      }

      if(s.burst && !fx.lod) {
         double a = s.clock.alpha();

         for(int i = 0; i < s.pieces; ++i) {
            double since;
            if(s.bounced[i] >= 0.0D && (since = s.seconds - s.bounced[i]) <= 0.12D) {
               float k = (float)(1.0D - since / 0.12D) * alpha;
               double x = f.rx(s.lx[i] + (s.x[i] - s.lx[i]) * a);
               double y = f.ry(s.floor + s.radius[i] * 0.4D);
               double z = f.rz(s.lz[i] + (s.z[i] - s.lz[i]) * a);
               Vfx.sprite(Textures.SPARK, x, y, z, s.radius[i] * 1.4D, (float)(since * 20.0D), core(col, 0.7F * k));
            }
         }
      }

   }

   public String stateForTest(HitFx fx) {
      GearBurstStyle.State s = (GearBurstStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float p = fx.progress();
         String head = String.format(Locale.ROOT, "%s, %s, %s, dial %.3f, hand %.0f deg", new Object[]{((Material)this.material.get()).displayName(), ((GearBurstStyle.Dial)this.dial.get()).displayName(), ((GearBurstStyle.Sweep)this.sweep.get()).displayName(), Double.valueOf(this.radius(fx)), Double.valueOf(Math.toDegrees((double)this.hand(fx, p)))});
         if(!s.burst) {
            return head + (p < 0.13333334F?"; popping open":"; ticking round");
         } else {
            int flying = 0;
            int bounced = 0;
            int rolling = 0;
            int resting = 0;

            for(int i = 0; i < s.pieces; ++i) {
               switch(s.phase[i]) {
               case 0:
                  ++flying;
                  break;
               case 1:
                  ++bounced;
                  break;
               case 2:
                  ++rolling;
                  break;
               default:
                  ++resting;
               }
            }

            return head + String.format(Locale.ROOT, "; burst: %d gears and %d springs: %d flying, %d bounced, %d rolling, %d resting", new Object[]{Integer.valueOf(s.gears), Integer.valueOf(s.pieces - s.gears), Integer.valueOf(flying), Integer.valueOf(bounced), Integer.valueOf(rolling), Integer.valueOf(resting)});
         }
      }
   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8 & 16777215) / 1.6777216E7D;
   }

   public static enum Dial implements Displayable {
      TICKS("Ticks", "Tick marks round the dial"),
      ROMAN("Roman numerals", "I to XII round the dial"),
      STARS("Stars", "A star at each hour");

      private final String displayName;
      private final String description;

      private Dial(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }

   private static final class State {
      final PhysicsClock clock;
      boolean burst;
      float burstAt;
      double seconds;
      int gears;
      int pieces;
      final double[] x;
      final double[] y;
      final double[] z;
      final double[] lx;
      final double[] ly;
      final double[] lz;
      final double[] vx;
      final double[] vy;
      final double[] vz;
      final double[] radius;
      final double[] turn;
      final double[] spin;
      final double[] tumble;
      final double[] tumbleRate;
      final double[] axle;
      final double[] tumbleAxis;
      final double[] roll;
      final int[] teeth;
      final int[] spokes;
      final int[] phase;
      final double[] bounced;
      double floor;

      private State() {
         this.clock = new PhysicsClock();
         this.x = new double[40];
         this.y = new double[40];
         this.z = new double[40];
         this.lx = new double[40];
         this.ly = new double[40];
         this.lz = new double[40];
         this.vx = new double[40];
         this.vy = new double[40];
         this.vz = new double[40];
         this.radius = new double[40];
         this.turn = new double[40];
         this.spin = new double[40];
         this.tumble = new double[40];
         this.tumbleRate = new double[40];
         this.axle = new double[120];
         this.tumbleAxis = new double[120];
         this.roll = new double[80];
         this.teeth = new int[40];
         this.spokes = new int[40];
         this.phase = new int[40];
         this.bounced = new double[40];
      }
   }

   public static enum Sweep implements Displayable {
      TICKING("Ticking", "The hand jumps from hour to hour"),
      SMOOTH("Smooth", "The hand glides round");

      private final String displayName;
      private final String description;

      private Sweep(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }
}
