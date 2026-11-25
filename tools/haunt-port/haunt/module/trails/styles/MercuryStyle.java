package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.kit.Liquid;
import mix.client.mixui.integration.haunt.fx.look.KeepVisible;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.trail.GroundCache;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.util.MathHelper;

public final class MercuryStyle extends TrailStyleBase {
   static final int MAX_DROPS = 24;
   static final int MAX_PUDDLES = 10;
   static final int MAX_SPLASH = 12;
   private static final double DROP = 0.065D;
   private static final double GRAVITY = 12.0D;
   private static final double PER_BLOCK = 1.6D;
   private static final float PUDDLE_LIFE = 1.0F;
   private static final float SPLASH_LIFE = 0.5F;
   private static final float DROP_LIFE = 3.0F;
   private static final float PUDDLE = 2.6F;
   private static final double PUDDLE_CELL = 2.2D;
   private static final float RIM = 0.25F;
   public final ModeSetting<Material> material = this.materialSetting(Material.CHROME, "What the drops are made of. Custom takes the Tint, with a finish.");
   public final ModeSetting<Finish> finish;
   public final SliderSetting dropSize;
   public final SliderSetting mergeStrength;
   public final BooleanSetting puddles;
   public final ModeSetting<MercuryStyle.Quality> quality;
   private final GroundCache ground = new GroundCache();
   private TrailFrame groundFrame;
   private final Rng rng = new Rng();
   private int lastDrops;
   private int lastPuddles;
   private int lastSplashes;
   private int lastMode = -1;
   private int dropPoints;
   private int dropTris;
   private int floorPoints;
   private int floorTris;
   private double dropCell;
   private double floorCell;
   private double spreadX;
   private double spreadY;
   private double spreadZ;
   private float lastAlpha;

   public MercuryStyle(TrailsModule module) {
      super(module, TrailStyle.MERCURY, spec().color("Tint", -2300950).signature(-4668208).length(1.2D).anchor(TrailAnchor.WAIST, false));
      this.finish = this.finishSetting(Finish.METALLIC, this.material);
      this.color.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return MercuryStyle.this.material.get() == Material.CUSTOM;
         }
      });
      this.doesNotApply(new Setting[]{this.size, this.glow});
      this.dropSize = (SliderSetting)this.setting((new SliderSetting("mercury.dropSize", "Drop Size", "How big the drops are.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%"));
      this.mergeStrength = (SliderSetting)this.setting((new SliderSetting("mercury.mergeStrength", "Merge Strength", "How readily nearby drops flow into one.", 0.0D, 100.0D, 60.0D, 1.0D)).unit("%"));
      this.puddles = (BooleanSetting)this.setting(new BooleanSetting("mercury.puddles", "Puddles", "Landing drops splash into small mirror puddles that ripple and shrink away.", true));
      this.quality = (ModeSetting)this.setting((new ModeSetting("mercury.quality", "Quality", "How fine the liquid\'s surface is.", MercuryStyle.Quality.class, MercuryStyle.Quality.MEDIUM)).style(ModeSetting.Style.CHIPS));
   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.dropSize);
   }

   protected TrailStyleBase.Store newStore() {
      return new MercuryStyle.Pools(this);
   }

   public int passes() {
      return ((Material)this.material.get()).translucent((Finish)this.finish.get())?FxPass.TRANSLUCENT.bit():FxPass.SOLID.bit();
   }

   public void update(TrailFrame f) {
      this.groundFrame = f;
      super.update(f);
   }

   public void renderPass(PlayerTrail p, TrailFrame f, FxPass pass) {
      if(!p.ghost && this.spawnPass(pass)) {
         this.groundFrame = f;
         MercuryStyle.Pools all = (MercuryStyle.Pools)this.store(f);
         MercuryStyle.Pool pool = all.of(p);
         pool.seen = all.clock;
         pool.alpha = this.opacity.getFloat() / 100.0F * p.alpha;
         pool.tint = this.tint;
         pool.light = this.matLight;
         pool.reduced = p.reduced;
         pool.self = p.self;
         pool.anchorX = Math.floor(p.headX / 4.0D) * 4.0D;
         pool.anchorY = Math.floor(p.headY / 4.0D) * 4.0D;
         pool.anchorZ = Math.floor(p.headZ / 4.0D) * 4.0D;
         this.drip(p, f, pool);
      }

   }

   public void renderPiecesPass(TrailFrame f, FxPass pass) {
      MercuryStyle.Pools all = (MercuryStyle.Pools)f.store(this.type);
      if(all != null) {
         for(int i = 0; i < all.count; ++i) {
            this.draw(f, all.list[i], pass);
         }
      }

   }

   private void drip(PlayerTrail p, TrailFrame f, MercuryStyle.Pool pool) {
      int due = this.piecesDue(p, 1.6D, 2);
      if(due != 0) {
         double yaw = Math.toRadians((double)p.pose.bodyYaw);
         double fx = -Math.sin(yaw);
         double fz = Math.cos(yaw);
         double r0 = 0.065D * (double)this.dropSize.getFloat() / 100.0D * (double)p.widthScale;
         pool.merge = this.mergeStrength.getFloat() / 100.0F;
         pool.makesPuddles = this.puddles.get();

         for(int i = 0; i < due && pool.drops < 24 && !this.module.piecesFull(f); ++i) {
            this.rng.seed((int)(f.now * 6151.0D) + i * 97 + p.entityId * 41);
            double side = (double)this.rng.signed() * 0.2D;
            int k = pool.drops++;
            pool.dx[k] = p.headX - fx * 0.15D + fz * side;
            pool.dy[k] = p.headY - 0.05D + (double)this.rng.signed() * 0.08D;
            pool.dz[k] = p.headZ - fz * 0.15D - fx * side;
            pool.dvx[k] = -fx * (0.3D + (double)this.rng.next() * 0.4D) + fz * (double)this.rng.signed() * 0.25D;
            pool.dvy[k] = (double)this.rng.next() * 0.6D;
            pool.dvz[k] = -fz * (0.3D + (double)this.rng.next() * 0.4D) - fx * (double)this.rng.signed() * 0.25D;
            pool.dr[k] = r0 * (0.75D + (double)this.rng.next() * 0.5D);
            pool.dage[k] = 0.0F;
         }
      }

   }

   void step(MercuryStyle.Pool p, float dt) {
      float pull = p.merge * 3.0F;
      if(pull > 0.0F) {
         for(int a = 0; a < p.drops; ++a) {
            for(int b = a + 1; b < p.drops; ++b) {
               double ex = p.dx[b] - p.dx[a];
               double ey = p.dy[b] - p.dy[a];
               double ez = p.dz[b] - p.dz[a];
               double d2 = ex * ex + ey * ey + ez * ez;
               double reach = (p.dr[a] + p.dr[b]) * 2.2D;
               if(d2 <= reach * reach && d2 >= 1.0E-8D) {
                  double d = Math.sqrt(d2);
                  double k = (double)pull * (1.0D - d / reach) * (double)dt / d;
                  p.dvx[a] += ex * k;
                  p.dvy[a] += ey * k;
                  p.dvz[a] += ez * k;
                  p.dvx[b] -= ex * k;
                  p.dvy[b] -= ey * k;
                  p.dvz[b] -= ez * k;
               }
            }
         }
      }

      double drag = Math.exp(-0.8D * (double)dt);
      int i = 0;

      while(i < p.drops) {
         p.dage[i] += dt;
         p.dvx[i] *= drag;
         p.dvz[i] *= drag;
         p.dvy[i] -= 12.0D * (double)dt;
         p.dx[i] += p.dvx[i] * (double)dt;
         p.dy[i] += p.dvy[i] * (double)dt;
         p.dz[i] += p.dvz[i] * (double)dt;
         double g = this.groundUnder(p.dx[i], p.dy[i] + 0.5D, p.dz[i]);
         boolean landed = !Double.isNaN(g) && p.dy[i] - p.dr[i] * 0.6D <= g;
         if(!landed && p.dage[i] <= 3.0F) {
            ++i;
         } else {
            if(landed) {
               this.splash(p, i, g);
            }

            removeDrop(p, i);
         }
      }

      i = 0;

      while(i < p.puddles) {
         p.page[i] += dt;
         if(p.page[i] >= 1.0F) {
            int last = --p.puddles;
            p.px[i] = p.px[last];
            p.py[i] = p.py[last];
            p.pz[i] = p.pz[last];
            p.pr[i] = p.pr[last];
            p.page[i] = p.page[last];
            p.pphase[i] = p.pphase[last];
         } else {
            ++i;
         }
      }

      i = 0;

      while(i < p.splashes) {
         p.sage[i] += dt;
         p.svy[i] -= 12.0D * (double)dt;
         p.sx[i] += p.svx[i] * (double)dt;
         p.sy[i] += p.svy[i] * (double)dt;
         p.sz[i] += p.svz[i] * (double)dt;
         double g = p.svy[i] < 0.0D?this.groundUnder(p.sx[i], p.sy[i] + 0.3D, p.sz[i]):Double.NaN;
         if(p.sage[i] < 0.5F && (Double.isNaN(g) || p.sy[i] > g)) {
            ++i;
         } else {
            int last = --p.splashes;
            p.sx[i] = p.sx[last];
            p.sy[i] = p.sy[last];
            p.sz[i] = p.sz[last];
            p.svx[i] = p.svx[last];
            p.svy[i] = p.svy[last];
            p.svz[i] = p.svz[last];
            p.sr[i] = p.sr[last];
            p.sage[i] = p.sage[last];
         }
      }

   }

   private double groundUnder(double x, double y, double z) {
      return this.ground.surface(this.groundFrame == null?null:this.groundFrame.world, x, y, z, 0.6D, 4.0D);
   }

   private void splash(MercuryStyle.Pool p, int i, double g) {
      double r = p.dr[i];
      if(p.makesPuddles) {
         int into = -1;

         for(int k = 0; k < p.puddles && into < 0; ++k) {
            double ex = p.px[k] - p.dx[i];
            double ez = p.pz[k] - p.dz[i];
            if(ex * ex + ez * ez < p.pr[k] * p.pr[k] * 1.5D && Math.abs(p.py[k] - g) < 0.2D) {
               into = k;
            }
         }

         if(into >= 0) {
            p.pr[into] = Math.min(p.pr[into] * 1.15D, r * 2.5999999046325684D * 2.0D);
            p.page[into] = Math.min(p.page[into], 0.25F);
         } else if(p.puddles < 10) {
            int var13 = p.puddles++;
            p.px[var13] = p.dx[i];
            p.py[var13] = g;
            p.pz[var13] = p.dz[i];
            p.pr[var13] = r * 2.5999999046325684D;
            p.page[var13] = 0.0F;
            p.pphase[var13] = (float)((double)p.dage[i] * 31.0D % 6.283D);
         }
      }

      int n = p.self?2 + ((int)((double)p.dage[i] * 97.0D) & 1):1;

      for(int kx = 0; kx < n && p.splashes < 12; ++kx) {
         int s = p.splashes++;
         double a = (double)kx * 2.4D + (double)p.dage[i] * 13.0D;
         p.sx[s] = p.dx[i];
         p.sy[s] = g + r * 0.5D;
         p.sz[s] = p.dz[i];
         p.svx[s] = (double)MathHelper.cos((float)a) * 0.7D;
         p.svy[s] = 1.3D + 0.4D * (double)kx;
         p.svz[s] = (double)MathHelper.sin((float)a) * 0.7D;
         p.sr[s] = r * (0.35D + 0.1D * (double)kx);
         p.sage[s] = 0.0F;
      }

   }

   private static void removeDrop(MercuryStyle.Pool p, int i) {
      int last = --p.drops;
      p.dx[i] = p.dx[last];
      p.dy[i] = p.dy[last];
      p.dz[i] = p.dz[last];
      p.dvx[i] = p.dvx[last];
      p.dvy[i] = p.dvy[last];
      p.dvz[i] = p.dvz[last];
      p.dr[i] = p.dr[last];
      p.dage[i] = p.dage[last];
   }

   private void draw(TrailFrame f, MercuryStyle.Pool pool, FxPass pass) {
      boolean mine = pool.self && !f.preview;
      if(mine) {
         this.lastDrops = pool.drops;
         this.lastPuddles = pool.puddles;
         this.lastSplashes = pool.splashes;
      }

      if(pool.size() != 0 && pool.alpha > 0.002F) {
         Liquid.Field fl = pool.field;
         Liquid.Field floor = pool.floor;
         fl.plain = pool.reduced;
         floor.plain = pool.reduced;
         fl.begin(1.5D + 0.9D * (double)pool.merge);
         floor.begin(1.5D + 0.9D * (double)pool.merge);
         float alpha = pool.alpha;
         double vx = f.viewX;
         double vy = f.viewY;
         double vz = f.viewZ;

         for(int i = 0; i < pool.drops; ++i) {
            double grow = (double)Easing.outCubic(Easing.clamp01(pool.dage[i] / 0.12F));
            double speed = Math.sqrt(pool.dvx[i] * pool.dvx[i] + pool.dvy[i] * pool.dvy[i] + pool.dvz[i] * pool.dvz[i]);
            double stretch = 1.0D / (1.0D + Math.min(0.6D, speed * 0.08D));
            fl.ball(pool.dx[i] - vx, pool.dy[i] - vy, pool.dz[i] - vz, pool.dr[i] * (0.4D + 0.6D * grow), 1.0D, pool.dvx[i], pool.dvy[i], pool.dvz[i], stretch);
         }

         for(int var49 = 0; var49 < pool.puddles; ++var49) {
            float age = pool.page[var49];
            double spread = (double)Easing.outCubic(Easing.clamp01(age / 0.15F));
            double shrink = 1.0D - (double)Easing.smoothstep(0.45F, 1.0F, age);
            double ripple = 1.0D + 0.09D * (double)MathHelper.sin(age * 22.0F + pool.pphase[var49]) * Math.exp((double)(-age) * 3.0D);
            double r = pool.pr[var49] * (0.4D + 0.6D * spread) * ripple * shrink;
            if(r > 0.01D) {
               floor.ball(pool.px[var49] - vx, pool.py[var49] + r * 0.06D - vy, pool.pz[var49] - vz, r, 1.0D, 0.0D, 1.0D, 0.0D, 4.5D);
            }
         }

         for(int var50 = 0; var50 < pool.splashes; ++var50) {
            fl.ball(pool.sx[var50] - vx, pool.sy[var50] - vy, pool.sz[var50] - vz, pool.sr[var50] * (1.0D - 0.5D * (double)pool.sage[var50] / 0.5D), 1.0D, pool.svx[var50], pool.svy[var50], pool.svz[var50], 0.85D);
         }

         if(fl.balls() != 0 || floor.balls() != 0) {
            Material m = pool.tint != 0?Material.CUSTOM:(Material)this.material.get();
            int tintCol = pool.tint != 0?pool.tint | -16777216:(m == Material.CUSTOM?this.color.getColor(0.5F) | -16777216:-1);
            double ax = pool.anchorX - vx;
            double ay = pool.anchorY - vy;
            double az = pool.anchorZ - vz;
            double ex = pool.anchorX + 2.0D - f.eyeX;
            double ey = pool.anchorY + 2.0D - f.eyeY;
            double ez = pool.anchorZ + 2.0D - f.eyeZ;
            double cell = 0.065D * (double)this.dropSize.getFloat() / 100.0D * (pool.self?((MercuryStyle.Quality)this.quality.get()).cell:Math.max(((MercuryStyle.Quality)this.quality.get()).cell, MercuryStyle.Quality.LOW.cell));
            MeshBatch.begin(m, pool.tint != 0?Finish.METALLIC:(Finish)this.finish.get(), pool.light, 0.25F, -1, false, pass == FxPass.SOLID);

            try {
               double d2 = ex * ex + ey * ey + ez * ez;
               if(fl.balls() > 0) {
                  fl.mesh(cell, ax, ay, az, d2, alpha, tintCol, (KeepVisible)null);
               }

               if(floor.balls() > 0) {
                  floor.mesh(cell * 2.2D, ax, ay, az, d2, alpha, tintCol, (KeepVisible)null);
               }
            } finally {
               MeshBatch.end();
            }

            if(mine) {
               this.lastMode = fl.balls() > 0?fl.lastMode:floor.lastMode;
               this.dropPoints = fl.balls() > 0?fl.lastPoints:0;
               this.dropCell = fl.balls() > 0?fl.lastCell:0.0D;
               this.dropTris = fl.balls() > 0?fl.lastTriangles:0;
               this.floorPoints = floor.balls() > 0?floor.lastPoints:0;
               this.floorCell = floor.balls() > 0?floor.lastCell:0.0D;
               this.floorTris = floor.balls() > 0?floor.lastTriangles:0;
               this.lastAlpha = alpha;
               double x0 = Double.MAX_VALUE;
               double y0 = Double.MAX_VALUE;
               double z0 = Double.MAX_VALUE;
               double x1 = -1.7976931348623157E308D;
               double y1 = -1.7976931348623157E308D;
               double z1 = -1.7976931348623157E308D;

               for(int i2 = 0; i2 < pool.drops; ++i2) {
                  x0 = Math.min(x0, pool.dx[i2]);
                  x1 = Math.max(x1, pool.dx[i2]);
                  y0 = Math.min(y0, pool.dy[i2]);
                  y1 = Math.max(y1, pool.dy[i2]);
                  z0 = Math.min(z0, pool.dz[i2]);
                  z1 = Math.max(z1, pool.dz[i2]);
               }

               for(int var56 = 0; var56 < pool.splashes; ++var56) {
                  x0 = Math.min(x0, pool.sx[var56]);
                  x1 = Math.max(x1, pool.sx[var56]);
                  y0 = Math.min(y0, pool.sy[var56]);
                  y1 = Math.max(y1, pool.sy[var56]);
                  z0 = Math.min(z0, pool.sz[var56]);
                  z1 = Math.max(z1, pool.sz[var56]);
               }

               boolean any = pool.drops + pool.splashes > 0;
               this.spreadX = any?x1 - x0:0.0D;
               this.spreadY = any?y1 - y0:0.0D;
               this.spreadZ = any?z1 - z0:0.0D;
            }
         }
      }

   }

   public String stateForTest() {
      String how = this.lastMode == 0?"meshed":(this.lastMode == 1?"its last mesh":(this.lastMode == 2?"spheres":"nothing"));
      return String.format(Locale.ROOT, "%s, drops %.0f%%, merge %.0f%%, puddles %s, %s quality; %d drop(s), %d puddle(s), %d splash(es), %s (drops: %d points, cell %.3f, %d triangles, spread %.2f x %.2f x %.2f; puddles: %d points, cell %.3f, %d triangles; opacity %.2f)", new Object[]{((Material)this.material.get()).displayName(), Float.valueOf(this.dropSize.getFloat()), Float.valueOf(this.mergeStrength.getFloat()), this.puddles.get()?"on":"off", ((MercuryStyle.Quality)this.quality.get()).displayName(), Integer.valueOf(this.lastDrops), Integer.valueOf(this.lastPuddles), Integer.valueOf(this.lastSplashes), how, Integer.valueOf(this.dropPoints), Double.valueOf(this.dropCell), Integer.valueOf(this.dropTris), Double.valueOf(this.spreadX), Double.valueOf(this.spreadY), Double.valueOf(this.spreadZ), Integer.valueOf(this.floorPoints), Double.valueOf(this.floorCell), Integer.valueOf(this.floorTris), Float.valueOf(this.lastAlpha)});
   }

   private static final class Pool {
      final PlayerTrail owner;
      final Liquid.Field field = new Liquid.Field(36);
      final Liquid.Field floor = new Liquid.Field(10);
      double seen;
      int drops;
      int puddles;
      int splashes;
      float alpha = 1.0F;
      float light = 1.0F;
      int tint;
      boolean reduced;
      double anchorX;
      double anchorY;
      double anchorZ;
      final double[] dx = new double[24];
      final double[] dy = new double[24];
      final double[] dz = new double[24];
      final double[] dvx = new double[24];
      final double[] dvy = new double[24];
      final double[] dvz = new double[24];
      final double[] dr = new double[24];
      final float[] dage = new float[24];
      final double[] px = new double[10];
      final double[] py = new double[10];
      final double[] pz = new double[10];
      final double[] pr = new double[10];
      final float[] page = new float[10];
      final float[] pphase = new float[10];
      final double[] sx = new double[12];
      final double[] sy = new double[12];
      final double[] sz = new double[12];
      final double[] svx = new double[12];
      final double[] svy = new double[12];
      final double[] svz = new double[12];
      final double[] sr = new double[12];
      final float[] sage = new float[12];
      float merge = 0.6F;
      boolean makesPuddles = true;
      boolean self;

      Pool(PlayerTrail owner) {
         this.owner = owner;
      }

      int size() {
         return this.drops + this.puddles + this.splashes;
      }
   }

   private static final class Pools implements TrailStyleBase.Store {
      MercuryStyle.Pool[] list = new MercuryStyle.Pool[8];
      int count;
      final MercuryStyle style;
      double clock;

      Pools(MercuryStyle style) {
         this.style = style;
      }

      MercuryStyle.Pool of(PlayerTrail t) {
         for(int i = 0; i < this.count; ++i) {
            if(this.list[i].owner == t) {
               return this.list[i];
            }
         }

         if(this.count == this.list.length) {
            this.list = (MercuryStyle.Pool[])Arrays.copyOf(this.list, this.count * 2);
         }

         MercuryStyle.Pool pool = new MercuryStyle.Pool(t);
         this.list[this.count++] = pool;
         pool.seen = this.clock;
         return pool;
      }

      public void update(float dt) {
         this.clock += (double)dt;
         int i = 0;

         while(i < this.count) {
            MercuryStyle.Pool p = this.list[i];
            if(this.clock - p.seen > 2.0D && p.size() == 0) {
               this.list[i] = this.list[--this.count];
               this.list[this.count] = null;
            } else {
               if(dt > 0.0F) {
                  this.style.step(p, Math.min(dt, 0.1F));
               }

               ++i;
            }
         }

      }

      public int size() {
         int n = 0;

         for(int i = 0; i < this.count; ++i) {
            n += this.list[i].size();
         }

         return n;
      }

      public void clear() {
         Arrays.fill(this.list, 0, this.count, (Object)null);
         this.count = 0;
      }
   }

   public static enum Quality implements Displayable {
      LOW("Low", "Coarse drops, the least work", 0.9D),
      MEDIUM("Medium", "Round drops", 0.65D),
      HIGH("High", "The smoothest surface", 0.5D);

      private final String displayName;
      private final String description;
      final double cell;

      private Quality(String displayName, String description, double cell) {
         this.displayName = displayName;
         this.description = description;
         this.cell = cell;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }
}
