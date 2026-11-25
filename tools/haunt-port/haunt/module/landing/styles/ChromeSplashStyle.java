package mix.client.mixui.integration.haunt.module.landing.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.kit.Liquid;
import mix.client.mixui.integration.haunt.fx.look.KeepVisible;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
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

public final class ChromeSplashStyle extends LandingStyleBase {
   private static final float RISE = 0.2F;
   private static final float BREAK = 0.4F;
   private static final float MERGED = 0.8F;
   private static final float END = 1.3F;
   private static final float JUMP_END = 0.8F;
   private static final int MAX_DROPLETS = 16;
   private static final int MAX_BALLS = 36;
   private static final int ROWS = 6;
   private static final int MAX_SEGS = 128;
   private static final double CROWN_R = 0.4D;
   private static final double CROWN_H = 0.5D;
   private static final double FLARE = 0.35D;
   private static final double WALL = 0.03D;
   private static final double DROP = 0.065D;
   private static final double PUDDLE = 0.13D;
   private static final double POOL = 0.5D;
   private static final double POOL_CELL = 0.17D;
   private static final double GRAVITY = 12.0D;
   private static final float RIM = 0.22F;
   public final ModeSetting<Material> material = this.materialSetting("chromesplash", Material.CHROME, "What the liquid is made of. Custom takes the Tint, with a finish.");
   public final ModeSetting<Finish> finish;
   public final SliderSetting crownHeight;
   public final SliderSetting droplets;
   public final BooleanSetting puddles;
   public final SliderSetting merge;
   public final ModeSetting<ChromeSplashStyle.Quality> quality;
   private final double[] ringX = new double[128];
   private final double[] ringZ = new double[128];
   private final double[] ringBase = new double[128];

   public ChromeSplashStyle(LandingImpactModule module) {
      super(module, LandingStyle.CHROME_SPLASH, 1.3F, 2.0F, new ColorSetting("chromesplash.color", "Tint", "The metal\'s color with the Custom material.", ColorSetting.Mode.STATIC, -2300950, -6310712, 1.0D));
      this.finish = this.finishSetting("chromesplash", Finish.METALLIC, this.material);
      this.color.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return ChromeSplashStyle.this.material.get() == Material.CUSTOM;
         }
      });
      this.signature(this.color, -2300950);
      this.crownHeight = this.look((SliderSetting)this.setting((new SliderSetting("chromesplash.crownHeight", "Crown Height", "How tall the crown rises.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%")));
      this.droplets = this.look((SliderSetting)this.setting(new SliderSetting("chromesplash.droplets", "Droplets", "How many droplet-tipped spikes the rim breaks into.", 4.0D, 16.0D, 10.0D, 1.0D)));
      this.puddles = (BooleanSetting)this.setting(new BooleanSetting("chromesplash.puddles", "Puddles", "The droplets fall back into mirror puddles that ripple and shrink away.", true));
      this.merge = (SliderSetting)this.setting((new SliderSetting("chromesplash.merge", "Merge Strength", "How far apart two drops start to flow into one.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%"));
      this.quality = (ModeSetting)this.setting(new ModeSetting("chromesplash.quality", "Quality", "How fine the liquid\'s surface is: Low, Medium or High (a little more work each frame).", ChromeSplashStyle.Quality.class, ChromeSplashStyle.Quality.MEDIUM));
   }

   public int passes(LandingFx fx) {
      int body = ((Material)this.material.get()).translucent((Finish)this.finish.get())?FxPass.TRANSLUCENT.bit():FxPass.SOLID.bit();
      return fx.lod?body:body | FxPass.GLOW.bit();
   }

   public void start(LandingFx fx) {
      ChromeSplashStyle.State s = new ChromeSplashStyle.State();
      s.jump = fx.land.jump;
      s.scale = (double)fx.size;
      s.crown = 0.5D * (double)this.crownHeight.getFloat() / 100.0D * Math.sqrt((double)fx.size) * (double)Math.min(1.25F, 0.8F + 0.25F * fx.land.fallScale);
      int n = s.jump?3:Math.round((float)this.droplets.getInt() * Math.min(1.3F, 0.85F + 0.2F * fx.land.fallScale));
      s.n = Math.max(3, Math.min(16, n));
      s.turn = (double)fx.rand(1) * 6.283185307179586D;
      double r = 0.4D * s.scale * (s.jump?0.6D:1.0D);

      for(int i = 0; i < s.n; ++i) {
         s.angle[i] = s.turn + 6.283185307179586D * (double)i / (double)s.n;
         s.breakAt[i] = s.jump?0.05F + 0.05F * fx.rand(10 + i):0.2F + 0.2F * (0.15F + 0.85F * fx.rand(10 + i));
         s.vOut[i] = (s.jump?0.6D:0.9D + 0.6D * (double)fx.rand(30 + i)) * Math.sqrt(s.scale);
         s.vUp[i] = (s.jump?1.3D:1.1D + 0.6D * (double)fx.rand(50 + i)) * Math.sqrt(s.scale);
         s.size[i] = 0.065D * Math.sqrt(s.scale) * (0.8D + 0.4D * (double)fx.rand(70 + i));
         s.phase[i] = (double)fx.rand(90 + i) * 6.283185307179586D;
         double tipR = r + 0.35D * s.crown;
         double tipH = s.jump?0.05D:s.crown * 1.1D;
         double ca = Math.cos(s.angle[i]);
         double sa = Math.sin(s.angle[i]);
         s.tipX[i] = fx.x() + ca * tipR;
         s.tipZ[i] = fx.z() + sa * tipR;
         s.tipY[i] = fx.y() + tipH;
         double tFall = flightTime(s.vUp[i], tipH);
         double lx = s.tipX[i] + ca * s.vOut[i] * tFall;
         double lz = s.tipZ[i] + sa * s.vOut[i] * tFall;
         double gy = Ground.y(fx.ground, lx, lz);
         s.landX[i] = lx;
         s.landZ[i] = lz;
         s.landY[i] = Ground.lastValid()?gy:Double.NaN;
         s.landAt[i] = s.breakAt[i] + (float)tFall;
      }

      fx.data = s;
   }

   private static double flightTime(double vUp, double h) {
      return (vUp + Math.sqrt(vUp * vUp + 24.0D * Math.max(0.0D, h))) / 12.0D;
   }

   private static double crownUp(float t) {
      return t < 0.2F?(double)Easing.outBack(Easing.clamp01(t / 0.2F), 1.4F):1.0D - (double)Easing.inOutCubic(Easing.clamp01((t - 0.2F - 0.05F) / 0.55F));
   }

   private static double shrink(float t) {
      return 1.0D - (double)Easing.inCubic(Easing.clamp01((t - 0.8F) / 0.49999994F));
   }

   public void renderPass(LandingFx fx, FxFrame f, FxPass pass) {
      ChromeSplashStyle.State s = (ChromeSplashStyle.State)fx.data;
      if(s != null) {
         float t = fx.styleTime();
         float alpha = fx.alpha();
         if(alpha > 0.003F && (!s.jump || t < 0.8F)) {
            if(pass == FxPass.GLOW) {
               this.glints(fx, f, s, t, alpha);
            } else {
               boolean custom = this.material.get() == Material.CUSTOM;
               int tint = custom?this.color.getColor(0.5F) | -16777216:-1;
               MeshBatch.begin((Material)this.material.get(), (Finish)this.finish.get(), fx.light, 0.22F, -1, false, pass == FxPass.SOLID);

               try {
                  if(!s.jump) {
                     this.crown(fx, f, s, t, tint, alpha);
                  }

                  this.liquid(fx, f, s, t, tint, alpha);
               } finally {
                  MeshBatch.end();
               }
            }
         }
      }

   }

   private void crown(LandingFx fx, FxFrame f, ChromeSplashStyle.State s, float t, int tint, float alpha) {
      double up = crownUp(t);
      if(up > 0.01D) {
         int per = fx.lod?3:((ChromeSplashStyle.Quality)this.quality.get()).perSpike;
         int segs = Math.min(128, s.n * per);
         double r = 0.4D * s.scale * (1.0D + 0.25D * (double)Easing.clamp01((t - 0.2F) / 0.6F));
         double h = s.crown * up;

         for(int k2 = 0; k2 < segs; ++k2) {
            double th = s.turn + 6.283185307179586D * (double)k2 / (double)segs;
            this.ringX[k2] = Math.cos(th);
            this.ringZ[k2] = Math.sin(th);
            double gy = Ground.y(fx.ground, fx.x() + this.ringX[k2] * r, fx.z() + this.ringZ[k2] * r);
            this.ringBase[k2] = Ground.lastValid()?gy:fx.y();
         }

         int vc = Math.round(255.0F * Easing.clamp01(alpha * ((float)(tint >>> 24) / 255.0F))) << 24 | tint & 16777215;
         int base = MeshBatch.stage(segs * 6 * 2);

         for(int k = 0; k < segs; ++k) {
            double th = 6.283185307179586D * (double)k / (double)segs;
            double c = 0.5D + 0.5D * Math.cos((double)s.n * th);
            double spike = c * c * c * c * c * c * c * c;
            double top = h * (0.6D + 0.55D * spike);

            for(int j = 0; j < 6; ++j) {
               double v = (double)j / 5.0D;
               double y = top * v;
               double rr = r + 0.35D * s.crown * up * v * v;
               double slope = 0.7D * s.crown * up * v / Math.max(0.001D, top);
               double nl = Math.sqrt(1.0D + slope * slope);
               double nx = this.ringX[k] / nl;
               double ny = -slope / nl;
               double nz = this.ringZ[k] / nl;
               double wall = 0.03D * Math.sqrt(s.scale) * (1.0D - 0.6D * v);
               double wx = fx.x() + this.ringX[k] * rr;
               double wz = fx.z() + this.ringZ[k] * rr;
               double wy = this.ringBase[k] + y;
               int o = base + (k * 6 + j) * 2;
               MeshBatch.vertex(o, f.rx(wx), f.ry(wy), f.rz(wz), nx, ny, nz, vc, 0.0F);
               MeshBatch.vertex(o + 1, f.rx(wx - this.ringX[k] * wall), f.ry(wy), f.rz(wz - this.ringZ[k] * wall), -nx, -ny, -nz, vc, 0.0F);
            }
         }

         for(int var52 = 0; var52 < segs; ++var52) {
            int k2 = (var52 + 1) % segs;

            for(int j = 0; j < 5; ++j) {
               int a = base + (var52 * 6 + j) * 2;
               int b = base + (k2 * 6 + j) * 2;
               int a2 = a + 2;
               int b2 = b + 2;
               MeshBatch.quad(a, b, b2, a2);
               MeshBatch.quad(a + 1, a2 + 1, b2 + 1, b + 1);
            }

            int a = base + (var52 * 6 + 6 - 1) * 2;
            int b = base + (k2 * 6 + 6 - 1) * 2;
            MeshBatch.quad(a, a + 1, b + 1, b);
         }
      }

   }

   private void liquid(LandingFx fx, FxFrame f, ChromeSplashStyle.State s, float t, int tint, float alpha) {
      Liquid.Field fl = s.drops;
      Liquid.Field pl = s.pool;
      fl.plain = fx.lod;
      pl.plain = fx.lod;
      double reach = 1.5D + 0.9D * (double)this.merge.getFloat() / 100.0D;
      fl.begin(reach);
      pl.begin(reach);
      double up = crownUp(t);
      double shrink = shrink(t);
      boolean pud = this.puddles.get();
      double r = 0.4D * s.scale;

      for(int i = 0; i < s.n; ++i) {
         double ca = Math.cos(s.angle[i]);
         double sa = Math.sin(s.angle[i]);
         if(t < s.breakAt[i]) {
            if(!s.jump && up > 0.05D) {
               double tipR = r + 0.35D * s.crown * up;
               double tipH = s.crown * up * 1.15D;
               double x = fx.x() + ca * tipR;
               double z = fx.z() + sa * tipR;
               double gy2 = Ground.y(fx.ground, x, z);
               double grow = (double)Easing.clamp01((t - 0.120000005F) / 0.120000005F);
               fl.ball(f.rx(x), f.ry(gy2 + tipH + s.size[i] * 0.6D), f.rz(z), s.size[i] * grow, 1.0D, 0.0D, 1.0D, 0.0D, 0.8D);
            }
         } else if(t < s.landAt[i]) {
            double dt = (double)(t - s.breakAt[i]);
            double x = s.tipX[i] + ca * s.vOut[i] * dt;
            double z = s.tipZ[i] + sa * s.vOut[i] * dt;
            double y = s.tipY[i] + s.vUp[i] * dt - 6.0D * dt * dt;
            double vy = s.vUp[i] - 12.0D * dt;
            fl.ball(f.rx(x), f.ry(y), f.rz(z), s.size[i], 1.0D, ca * s.vOut[i], vy, sa * s.vOut[i], 0.8D);
         } else if(pud && !Double.isNaN(s.landY[i])) {
            double since = (double)(t - s.landAt[i]);
            double spread = (double)Easing.outCubic(Easing.clamp01((float)(since / 0.12D)));
            double ripple = 1.0D + 0.08D * Math.sin(since * 22.0D + s.phase[i]) * Math.exp(-since * 3.0D);
            double pr = 0.13D * Math.sqrt(s.scale) * (s.jump?0.6D:1.0D) * (0.5D + 0.5D * spread) * ripple * shrink;
            if(pr > 0.01D) {
               pl.ball(f.rx(s.landX[i]), f.ry(s.landY[i] + pr * 0.08D), f.rz(s.landZ[i]), pr, 1.0D, 0.0D, 1.0D, 0.0D, 4.0D);
            }
         }
      }

      if(pud || !s.jump) {
         double pool = s.jump?0.0D:(double)Easing.smoothstep(0.2F, 0.8F, t) * shrink;
         if(pool > 0.02D) {
            double pr = 0.5D * s.scale * (0.6D + 0.4D * pool) * pool;
            double gy = Ground.y(fx.ground, fx.x(), fx.z());
            if(Ground.lastValid()) {
               pl.ball(f.rx(fx.x()), f.ry(gy + pr * 0.06D), f.rz(fx.z()), pr, 1.0D, 0.0D, 1.0D, 0.0D, 5.0D);
            }
         }
      }

      double ringK = s.jump?1.0D - (double)Easing.smoothstep(0.3F, 0.8F, t):(double)Easing.smoothstep(0.7F, 0.90000004F, t) * shrink;
      if(ringK > 0.02D) {
         double rr = s.jump?(0.15D + 0.5D * (double)Easing.outCubic(Easing.clamp01(t / 0.5F))) * s.scale:0.5D * s.scale * (0.9D + 0.5D * (double)Easing.clamp01((t - 0.8F) / 0.49999994F));
         double gy = Ground.y(fx.ground, fx.x(), fx.z());
         pl.ring(f.rx(fx.x()), f.ry(gy + 0.01D), f.rz(fx.z()), rr, 0.035D * Math.sqrt(s.scale), ringK, 0.012D, Math.cos((double)t * 9.0D), Math.sin((double)t * 9.0D));
      }

      double q = ((ChromeSplashStyle.Quality)this.quality.get()).cell;
      double root = Math.sqrt(s.scale);
      double distSq = f.distanceSqToEye(fx.x(), fx.y(), fx.z());
      if(fl.balls() > 0) {
         fl.mesh(0.065D * root * q, f.rx(fx.x()), f.ry(fx.y()), f.rz(fx.z()), distSq, alpha, tint, (KeepVisible)null);
      }

      if(pl.balls() > 0 || ringK > 0.02D) {
         pl.mesh(0.17D * root * q, f.rx(fx.x()), f.ry(fx.y()), f.rz(fx.z()), distSq, alpha, tint, (KeepVisible)null);
      }

   }

   private void glints(LandingFx fx, FxFrame f, ChromeSplashStyle.State s, float t, float alpha) {
      if(!s.jump) {
         float peak = Easing.smoothstep(0.14F, 0.2F, t) * (1.0F - Easing.smoothstep(0.4F, 0.5F, t));
         if(peak > 0.01F) {
            RenderState.additive();
            double up = crownUp(t);
            double r = 0.4D * s.scale + 0.35D * s.crown * up;

            for(int i = 0; i < s.n; ++i) {
               if(t < s.breakAt[i] + 0.06F) {
                  double x = fx.x() + Math.cos(s.angle[i]) * r;
                  double z = fx.z() + Math.sin(s.angle[i]) * r;
                  double y = Ground.y(fx.ground, x, z) + s.crown * up * 1.15D + s.size[i];
                  float tw = 0.6F + 0.4F * (float)Math.sin((double)t * 40.0D + s.phase[i]);
                  Vfx.sprite(Textures.SPARKLE, f.rx(x), f.ry(y), f.rz(z), 0.22D * Math.sqrt(s.scale), (float)s.phase[i], ColorUtil.mulAlpha(-1, alpha * peak * tw * 0.8F));
               }
            }
         }
      }

   }

   public String stateForTest(LandingFx fx) {
      ChromeSplashStyle.State s = (ChromeSplashStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.styleTime();
         int flying = 0;
         int landed = 0;
         int lost = 0;

         for(int i = 0; i < s.n; ++i) {
            if(t >= s.breakAt[i] && t < s.landAt[i]) {
               ++flying;
            } else if(t >= s.landAt[i]) {
               if(Double.isNaN(s.landY[i])) {
                  ++lost;
               } else {
                  ++landed;
               }
            }
         }

         String phase = s.jump?(t < 0.8F?"a ripple ring and a few droplets":"gone"):(t < 0.2F?"the crown rising":(t < 0.4F?"the droplets breaking off":(t < 0.8F?"falling, merging":(t < 1.3F?"the puddles shrinking":"gone"))));
         return String.format(Locale.ROOT, "%s; crown %.2f of %.2f, %d droplets (%d flying, %d puddles, %d over no ground), drops %s (%d triangles), pool %s (%d triangles), %s, %s, quality %s; fall %.1f (x%.2f)%s; %s, light %.2f", new Object[]{phase, Double.valueOf(s.crown * crownUp(t)), Double.valueOf(s.crown), Integer.valueOf(s.n), Integer.valueOf(flying), Integer.valueOf(landed), Integer.valueOf(lost), mode(s.drops), Integer.valueOf(s.drops.lastTriangles), mode(s.pool), Integer.valueOf(s.pool.lastTriangles), ((Material)this.material.get()).displayName(), ((Finish)this.finish.get()).displayName(), ((ChromeSplashStyle.Quality)this.quality.get()).displayName(), Float.valueOf(fx.land.fallHeight), Float.valueOf(fx.land.fallScale), s.jump?", a jump":"", fx.lod?"far":"near", Float.valueOf(fx.light)});
      }
   }

   private static String mode(Liquid.Field fl) {
      return fl.lastMode == 0?"meshed":(fl.lastMode == 1?"reused":(fl.lastMode == 2?"spheres":"none"));
   }

   public static enum Quality implements Displayable {
      LOW("Low", "Coarse drops, the least work", 0.9D, 4),
      MEDIUM("Medium", "Round drops", 0.65D, 6),
      HIGH("High", "The smoothest surface", 0.5D, 8);

      private final String displayName;
      private final String description;
      final double cell;
      final int perSpike;

      private Quality(String displayName, String description, double cell, int perSpike) {
         this.displayName = displayName;
         this.description = description;
         this.cell = cell;
         this.perSpike = perSpike;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }

   private static final class State {
      final Liquid.Field drops;
      final Liquid.Field pool;
      boolean jump;
      int n;
      double scale;
      double crown;
      double turn;
      final double[] angle;
      final double[] vOut;
      final double[] vUp;
      final float[] breakAt;
      final float[] landAt;
      final double[] landX;
      final double[] landY;
      final double[] landZ;
      final double[] tipX;
      final double[] tipY;
      final double[] tipZ;
      final double[] phase;
      final double[] size;

      private State() {
         this.drops = new Liquid.Field(18);
         this.pool = new Liquid.Field(36);
         this.angle = new double[16];
         this.vOut = new double[16];
         this.vUp = new double[16];
         this.breakAt = new float[16];
         this.landAt = new float[16];
         this.landX = new double[16];
         this.landY = new double[16];
         this.landZ = new double[16];
         this.tipX = new double[16];
         this.tipY = new double[16];
         this.tipZ = new double[16];
         this.phase = new double[16];
         this.size = new double[16];
      }
   }
}
