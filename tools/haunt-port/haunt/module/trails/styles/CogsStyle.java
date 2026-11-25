package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.kit.Clockwork;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.trail.GroundCache;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
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
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class CogsStyle extends TrailStyleBase {
   private static final int MAX_CLUSTERS = 96;
   private static final int PER = 3;
   private static final float GEAR = 0.15F;
   private static final float THICK = 0.28F;
   private static final double FULL_WITHIN = 5.0D;
   private static final float ROLL = 0.45F;
   private static final float FADE = 0.5F;
   private static final int[] TEETH = new int[]{10, 12, 14, 8};
   public final ModeSetting<Material> material = this.materialSetting(Material.BRASS, "What the gears are made of. Custom takes the Tint, with a finish.");
   public final ModeSetting<Finish> finish;
   public final ColorSetting glowColor;
   public final SliderSetting gearSize;
   public final BooleanSetting clusters;
   public final BooleanSetting settle;
   public final BooleanSetting glints;
   public final ModeSetting<CogsStyle.Motion> motion;
   private final Rng rng = new Rng();
   private final GroundCache ground = new GroundCache();

   public CogsStyle(TrailsModule module) {
      super(module, TrailStyle.COGS, spec().color("Tint", -3562422).signature(-993910).length(1.8D).anchor(TrailAnchor.BACK, false));
      this.finish = this.finishSetting(Finish.METALLIC, this.material);
      this.color.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CogsStyle.this.material.get() == Material.CUSTOM;
         }
      });
      this.glowColor = this.signatureColor("glowColor", "Glow", "The glints on the teeth.", -6237, -15254);
      this.doesNotApply(new Setting[]{this.size, this.glow});
      this.gearSize = (SliderSetting)this.setting((new SliderSetting("cogs.gearSize", "Gear Size", "How big the gears are.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%"));
      this.clusters = (BooleanSetting)this.setting(new BooleanSetting("cogs.clusters", "Clusters", "Two or three gears meshing together (off: single gears).", true));
      this.motion = (ModeSetting)this.setting((new ModeSetting("cogs.motion", "Motion", "Ticking round like clockwork, or turning evenly.", CogsStyle.Motion.class, CogsStyle.Motion.CLOCKWORK)).style(ModeSetting.Style.CHIPS));
      this.settle = (BooleanSetting)this.setting(new BooleanSetting("cogs.settle", "Settle on the Ground", "Gears that reach the ground roll a little and tick to a stop.", true));
      this.glints = (BooleanSetting)this.setting(new BooleanSetting("cogs.glints", "Glints", "Glints on the teeth as they turn.", true));
   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.gearSize);
   }

   protected TrailStyleBase.Store newStore() {
      return new CogsStyle.Clusters();
   }

   public int passes() {
      int body = ((Material)this.material.get()).translucent((Finish)this.finish.get())?FxPass.TRANSLUCENT.bit():FxPass.SOLID.bit();
      return this.glints.get()?body | FxPass.GLOW.bit():body;
   }

   public void renderPass(PlayerTrail p, TrailFrame f, FxPass pass) {
      if(!p.ghost && this.spawnPass(pass)) {
         int due = this.piecesDue(p, 0.55D, 2);
         if(due != 0) {
            CogsStyle.Clusters d = (CogsStyle.Clusters)this.store(f);
            double yaw = Math.toRadians((double)p.pose.bodyYaw);
            float fx = (float)(-Math.sin(yaw));
            float fz = (float)Math.cos(yaw);
            float life = this.lengthNow(p.speed);

            for(int i = 0; i < due && !this.module.piecesFull(f); ++i) {
               this.rng.seed((int)(f.now * 4441.0D) + i * 157 + p.entityId * 29);
               double x = p.headX - (double)fx * 0.25D + (double)(fz * this.rng.signed()) * 0.2D;
               double y = p.headY + (double)this.rng.signed() * 0.1D;
               double z = p.headZ - (double)fz * 0.25D - (double)(fx * this.rng.signed()) * 0.2D;
               if(f.nearFade(x, y, z) >= 0.35F) {
                  int k = d.add();
                  d.x[k] = x;
                  d.y[k] = y;
                  d.z[k] = z;
                  float back = 0.8F + this.rng.next() * 0.4F;
                  d.vx[k] = -fx * back + fz * this.rng.signed() * 0.25F;
                  d.vz[k] = -fz * back - fx * this.rng.signed() * 0.25F;
                  d.vy[k] = 0.5F + this.rng.next() * 0.4F;
                  d.born[k] = d.clock;
                  d.life[k] = life * (0.85F + this.rng.next() * 0.3F) + (this.settle.get()?0.95F:0.0F);
                  d.alpha[k] = this.opacity.getFloat() / 100.0F * p.alpha;
                  double tilt = (double)this.rng.signed() * 0.5D;
                  double swing = (double)this.rng.signed() * 0.4D;
                  double axx = (double)fz * Math.cos(swing) + (double)fx * Math.sin(swing);
                  double axz = (double)(-fx) * Math.cos(swing) + (double)fz * Math.sin(swing);
                  double axy = Math.sin(tilt);
                  double l = Math.sqrt(axx * axx * Math.cos(tilt) * Math.cos(tilt) + axy * axy + axz * axz * Math.cos(tilt) * Math.cos(tilt));
                  d.ax[k] = (float)(axx * Math.cos(tilt) / l);
                  d.ay[k] = (float)(axy / l);
                  d.az[k] = (float)(axz * Math.cos(tilt) / l);
                  d.tumble[k] = this.rng.signed() * 0.8F;
                  d.landed[k] = -1.0F;
                  d.settles[k] = this.settle.get();
                  double g = this.ground.surface(f.world, x, y, z, 0.5D, 6.0D);
                  d.groundY[k] = Double.isNaN(g)?Float.NaN:(float)g;
                  d.radius[k] = 0.15F * this.gearSize.getFloat() / 100.0F * (0.85F + this.rng.next() * 0.3F) * p.widthScale;
                  d.phase[k] = this.rng.next() * 6.283F;
                  d.spin[k] = (this.rng.next() < 0.5F?1.0F:-1.0F) * (0.9F + this.rng.next() * 0.5F);
                  d.count[k] = !p.reduced && this.clusters.get()?2 + (this.rng.next() < 0.45F?1:0):1;
                  d.tint[k] = this.tint;
                  d.seed[k] = (int)(this.rng.next() * 10000.0F);
                  d.far[k] = p.reduced;
                  d.self[k] = p.self;
               }
            }
         }
      }

   }

   private float alphaOf(CogsStyle.Clusters d, int i, TrailFrame f) {
      float age = d.clock - d.born[i];
      float a = d.alpha[i] * Easing.clamp01(age / 0.1F) * (1.0F - Easing.smoothstep(d.life[i] - 0.5F, d.life[i], age));
      return a * f.nearFade(d.x[i], d.y[i], d.z[i]) * f.fog(d.x[i], d.y[i], d.z[i]);
   }

   private double driverTurn(CogsStyle.Clusters d, int i) {
      float age = d.clock - d.born[i];
      boolean smooth = this.motion.get() == CogsStyle.Motion.SMOOTH;
      if(d.landed[i] >= 0.0F) {
         float since = d.clock - d.landed[i];
         float stop = 0.71999997F;
         float slowed = since >= stop?stop * 0.5F:since - since * since / (2.0F * stop);
         age = d.landed[i] - d.born[i] + slowed;
      }

      return (double)d.phase[i] + (double)d.spin[i] * Clockwork.ratchet((double)age, 0.22D, 0.5235987755982988D, smooth) * 1.0D;
   }

   public void renderPiecesPass(TrailFrame f, FxPass pass) {
      CogsStyle.Clusters d = (CogsStyle.Clusters)f.store(this.type);
      if(d != null && d.n != 0) {
         if(pass == FxPass.GLOW) {
            this.sparkle(f, d);
         } else {
            Material mat = (Material)this.material.get();
            this.batch(f, d, mat, pass, false);
            this.batch(f, d, Material.CUSTOM, pass, true);
         }
      }

   }

   private void batch(TrailFrame f, CogsStyle.Clusters d, Material m, FxPass pass, boolean tinted) {
      boolean any = false;

      for(int i = 0; i < d.n && !any; ++i) {
         any = d.tint[i] != 0 == tinted;
      }

      if(any) {
         MeshBatch.begin(m, tinted?Finish.METALLIC:(Finish)this.finish.get(), this.matLight, 0.2F, -1, true, pass == FxPass.SOLID);

         try {
            for(int var13 = 0; var13 < d.n; ++var13) {
               float a;
               if(d.tint[var13] != 0 == tinted && (a = this.alphaOf(d, var13, f)) > 0.01F) {
                  int col = tinted?d.tint[var13] | -16777216:(m == Material.CUSTOM?this.color.getColor(0.5F) | -16777216:-1);
                  this.cluster(f, d, var13, ColorUtil.mulAlpha(col, a));
               }
            }
         } finally {
            MeshBatch.end();
         }
      }

   }

   private void cluster(TrailFrame f, CogsStyle.Clusters d, int i, int col) {
      double ax = (double)d.ax[i];
      double ay = (double)d.ay[i];
      double az = (double)d.az[i];
      if(d.landed[i] >= 0.0F) {
         float lie = Easing.outCubic(Easing.clamp01((d.clock - d.landed[i]) / 0.45F));
         ay = ay * (double)(1.0F - lie) + (double)lie;
         double var60;
         double var62;
         double l = Math.sqrt((var60 = ax * (double)(1.0F - lie)) * var60 + ay * ay + (var62 = az * (double)(1.0F - lie)) * var62);
         ax = var60 / l;
         ay = ay / l;
         az = var62 / l;
      }

      double ey;
      double ex;
      double ez;
      if(Math.abs(ay) < 0.9D) {
         ex = -az;
         ey = 0.0D;
         ez = ax;
      } else {
         ex = 1.0D;
         ey = 0.0D;
         ez = 0.0D;
      }

      double el = Math.sqrt(ex * ex + ey * ey + ez * ez);
      double var57;
      double var58;
      double gx = ay * (var57 = ez / el) - az * (var58 = ey / el);
      double var59;
      double gy = az * (var59 = ex / el) - ax * var57;
      double gz = ax * var58 - ay * var59;
      double cx = d.x[i] - f.viewX;
      double cy = d.y[i] - f.viewY;
      double cz = d.z[i] - f.viewZ;
      boolean full = d.self[i] && !d.far[i] && cx * cx + cy * cy + cz * cz < 25.0D;
      double turn = this.driverTurn(d, i);
      int teeth0 = TEETH[d.seed[i] % 3];
      double r0 = (double)d.radius[i];
      double m = Clockwork.module(r0, teeth0);
      this.gear(cx, cy, cz, ax, ay, az, r0, turn, teeth0, full, col, d.seed[i]);

      for(int k = 1; k < d.count[i]; ++k) {
         int teeth = TEETH[(d.seed[i] / 3 + k) % 4];
         double r = Clockwork.tipRadius(m, teeth);
         double dist = Clockwork.meshDistance(m, teeth0, teeth);
         double ang = (double)(d.seed[i] % 628) / 100.0D + (double)(k - 1) * 2.3D;
         double ox = (var59 * Math.cos(ang) + gx * Math.sin(ang)) * dist;
         double oy = (var58 * Math.cos(ang) + gy * Math.sin(ang)) * dist;
         double oz = (var57 * Math.cos(ang) + gz * Math.sin(ang)) * dist;
         double towards = Clockwork.angleOf(ax, ay, az, ox, oy, oz);
         double t = Clockwork.meshed(turn, teeth0, teeth, towards);
         this.gear(cx + ox, cy + oy, cz + oz, ax, ay, az, r, t, teeth, full, col, d.seed[i] + k);
      }

   }

   private void gear(double x, double y, double z, double ax, double ay, double az, double r, double turn, int teeth, boolean full, int col, int seed) {
      if(full) {
         Clockwork.gear(x, y, z, ax, ay, az, r, r * 0.2800000011920929D, turn, teeth, teeth >= 12?4:3, col, 0.0F);
      } else {
         Clockwork.kiteGear(x, y, z, ax, ay, az, r, r * 0.2800000011920929D, turn, col);
      }

   }

   private void sparkle(TrailFrame f, CogsStyle.Clusters d) {
      RenderState.additive();
      double time = f.now;

      for(int i = 0; i < d.n; ++i) {
         float a;
         double run;
         double cyc;
         if(!d.far[i] && (cyc = (run = time * 1.7D + (double)(d.seed[i] % 97) * 0.13D) - Math.floor(run)) <= 0.18D && (a = this.alphaOf(d, i, f) * (float)Math.sin(3.141592653589793D * cyc / 0.18D)) > 0.02F) {
            double r = (double)d.radius[i];
            double turn = this.driverTurn(d, i) + (double)(d.seed[i] % 7);
            double ax = (double)d.ax[i];
            double ay = (double)d.ay[i];
            double az = (double)d.az[i];
            double ex = Math.abs(ay) < 0.9D?-az:1.0D;
            double ez = Math.abs(ay) < 0.9D?ax:0.0D;
            double el = Math.sqrt(ex * ex + ez * ez);
            double var45;
            double gx = ay * (var45 = ez / el);
            double var44;
            double gy = az * (var44 = ex / el) - ax * var45;
            double gz = -ay * var44;
            double c = Math.cos(turn) * r * 0.9D;
            double s = Math.sin(turn) * r * 0.9D;
            double x = d.x[i] - f.viewX + var44 * c + gx * s;
            double y = d.y[i] - f.viewY + gy * s;
            double z = d.z[i] - f.viewZ + var45 * c + gz * s;
            int col = this.slotColor(this.glowColor, 0.5F, f.colorTime);
            Vfx.sprite(Textures.SPARKLE, x, y, z, r * 1.1D, (float)(time * 2.0D + (double)i), ColorUtil.mulAlpha(col, Math.min(1.0F, a)));
         }
      }

   }

   public String stateForTest() {
      return String.format(Locale.ROOT, "%s, gear %.0f%%, clusters %s, %s, settle %s, glints %s", new Object[]{((Material)this.material.get()).displayName(), Float.valueOf(this.gearSize.getFloat()), this.clusters.get()?"on":"off", ((CogsStyle.Motion)this.motion.get()).displayName(), this.settle.get()?"on":"off", this.glints.get()?"on":"off"});
   }

   static final class Clusters implements TrailStyleBase.Store {
      final double[] x = new double[96];
      final double[] y = new double[96];
      final double[] z = new double[96];
      final float[] vx = new float[96];
      final float[] vy = new float[96];
      final float[] vz = new float[96];
      final float[] born = new float[96];
      final float[] life = new float[96];
      final float[] alpha = new float[96];
      final float[] ax = new float[96];
      final float[] ay = new float[96];
      final float[] az = new float[96];
      final float[] tumble = new float[96];
      final float[] landed = new float[96];
      final float[] groundY = new float[96];
      final float[] radius = new float[96];
      final float[] phase = new float[96];
      final float[] spin = new float[96];
      final int[] count = new int[96];
      final int[] tint = new int[96];
      final int[] seed = new int[96];
      final boolean[] far = new boolean[96];
      final boolean[] self = new boolean[96];
      final boolean[] settles = new boolean[96];
      int n;
      int oldest;
      float clock;

      int add() {
         if(this.n < 96) {
            return this.n++;
         } else {
            int i = this.oldest;
            this.oldest = (this.oldest + 1) % 96;
            return i;
         }
      }

      public void update(float dt) {
         this.clock += dt;
         if(dt > 0.0F) {
            float damp = (float)Math.exp(-1.4D * (double)dt);
            int w = 0;

            for(int i = 0; i < this.n; ++i) {
               if(this.clock - this.born[i] < this.life[i]) {
                  if(this.landed[i] < 0.0F) {
                     this.vx[i] *= damp;
                     this.vz[i] *= damp;
                     this.vy[i] -= 4.5F * dt;
                     this.x[i] += (double)(this.vx[i] * dt);
                     this.y[i] += (double)(this.vy[i] * dt);
                     this.z[i] += (double)(this.vz[i] * dt);
                     if(this.settles[i] && !Float.isNaN(this.groundY[i]) && this.y[i] - (double)this.radius[i] * 0.6D <= (double)this.groundY[i]) {
                        this.y[i] = (double)(this.groundY[i] + this.radius[i] * 0.6F);
                        this.landed[i] = this.clock;
                     }
                  } else {
                     float k = 1.0F - Easing.clamp01((this.clock - this.landed[i]) / 0.45F);
                     this.x[i] += (double)(this.vx[i] * dt * k);
                     this.z[i] += (double)(this.vz[i] * dt * k);
                  }

                  if(w != i) {
                     this.copy(i, w);
                  }

                  ++w;
               }
            }

            this.n = w;
            this.oldest = 0;
         }

      }

      private void copy(int a, int b) {
         this.x[b] = this.x[a];
         this.y[b] = this.y[a];
         this.z[b] = this.z[a];
         this.vx[b] = this.vx[a];
         this.vy[b] = this.vy[a];
         this.vz[b] = this.vz[a];
         this.born[b] = this.born[a];
         this.life[b] = this.life[a];
         this.alpha[b] = this.alpha[a];
         this.ax[b] = this.ax[a];
         this.ay[b] = this.ay[a];
         this.az[b] = this.az[a];
         this.tumble[b] = this.tumble[a];
         this.landed[b] = this.landed[a];
         this.groundY[b] = this.groundY[a];
         this.radius[b] = this.radius[a];
         this.phase[b] = this.phase[a];
         this.spin[b] = this.spin[a];
         this.count[b] = this.count[a];
         this.tint[b] = this.tint[a];
         this.seed[b] = this.seed[a];
         this.far[b] = this.far[a];
         this.self[b] = this.self[a];
         this.settles[b] = this.settles[a];
      }

      public int size() {
         return this.n;
      }

      public void clear() {
         this.n = 0;
         this.oldest = 0;
      }
   }

   public static enum Motion implements Displayable {
      CLOCKWORK("Clockwork", "Ticking round a tooth at a time"),
      SMOOTH("Smooth", "Turning evenly");

      private final String displayName;
      private final String description;

      private Motion(String displayName, String description) {
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
