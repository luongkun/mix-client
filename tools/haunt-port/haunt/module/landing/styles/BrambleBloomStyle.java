package mix.client.mixui.integration.haunt.module.landing.styles;

import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.Terrain;
import mix.client.mixui.integration.haunt.fx.kit.Surface;
import mix.client.mixui.integration.haunt.fx.kit.Vines;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class BrambleBloomStyle extends LandingStyleBase {
   private static final float GROW = 0.25F;
   private static final float BLOOM = 0.45F;
   private static final float HOLD = 0.75F;
   private static final float SINK = 1.25F;
   private static final float END = 1.5F;
   private static final float JUMP_GROW = 0.15F;
   private static final float JUMP_WITHER = 0.2F;
   private static final float JUMP_GONE = 0.55F;
   private static final int MAX_RUNNERS = 14;
   private static final int POINTS = 30;
   private static final int MAX_PETALS = 42;
   private static final int NIGHT_RIM = -3549953;
   private static final double THICK = 0.045D;
   private static final double ROSE = 0.13D;
   private static final double START = 0.06D;
   public final ColorSetting tipColor;
   public final ColorSetting roseColor;
   public final SliderSetting runners;
   public final SliderSetting length;
   public final BooleanSetting roses;
   public final SliderSetting thornDensity;
   public final SliderSetting curl;
   private final Vines kit = new Vines(420);
   private final BrambleBloomStyle.Floor floor = new BrambleBloomStyle.Floor();

   public BrambleBloomStyle(LandingImpactModule module) {
      super(module, LandingStyle.BRAMBLE_BLOOM, 1.5F, 4.3F, new ColorSetting("bramblebloom.color", "Vines", "The brambles\' bark.", ColorSetting.Mode.STATIC, -14734816, -12965348, 1.0D));
      this.signature(this.color, -14734816);
      this.tipColor = (ColorSetting)this.setting((new ColorSetting("bramblebloom.tips", "Thorn Tips", "The thorns\' pale tips.", ColorSetting.Mode.STATIC, -1452088, -2840, 1.0D)).secondary());
      this.signature(this.tipColor, -1452088);
      this.roseColor = (ColorSetting)this.setting((new ColorSetting("bramblebloom.roses", "Roses", "The roses at the runners\' tips, and their petals.", ColorSetting.Mode.STATIC, -5041618, -10614246, 1.0D)).secondary());
      this.signature(this.roseColor, -5041618);
      this.runners = this.look((SliderSetting)this.setting(new SliderSetting("bramblebloom.runners", "Runners", "How many vines burst out.", 3.0D, 10.0D, 6.0D, 1.0D)));
      this.length = (SliderSetting)this.setting((new SliderSetting("bramblebloom.length", "Length", "How far the runners reach.", 1.0D, 4.0D, 2.0D, 0.1D)).unit(" blocks"));
      this.roses = (BooleanSetting)this.setting(new BooleanSetting("bramblebloom.rosesOn", "Roses", "Roses bloom at the runners\' tips.", true));
      this.thornDensity = this.look((SliderSetting)this.setting((new SliderSetting("bramblebloom.thorns", "Thorn Density", "How thick with thorns the runners are.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%")));
      this.curl = (SliderSetting)this.setting((new SliderSetting("bramblebloom.curl", "Curl", "How much the runners curl as they go.", 0.0D, 100.0D, 50.0D, 5.0D)).unit("%"));
   }

   public int passes(LandingFx fx) {
      return FxPass.SOLID.bit();
   }

   public void start(LandingFx fx) {
      BrambleBloomStyle.State s = new BrambleBloomStyle.State();
      s.jump = fx.land.jump;
      s.scale = (double)fx.size;
      float k = fx.land.fallScale;
      int n = s.jump?Math.max(3, Math.round((float)this.runners.getInt() * 0.6F)):Math.round((float)this.runners.getInt() * (0.8F + 0.25F * k));
      s.n = Math.max(3, Math.min(14, n));
      double base = (double)this.length.getFloat() * Math.sqrt((double)fx.size) * (s.jump?0.45D:1.0D);
      double turn = (double)fx.rand(1) * 6.283185307179586D;
      double c = (double)this.curl.getFloat() / 100.0D;

      for(int r = 0; r < s.n; ++r) {
         double a = turn + 6.283185307179586D * ((double)r + 0.35D * ((double)fx.rand(10 + r) - 0.5D)) / (double)s.n;
         double dir = (r & 1) == 0?1.0D:-1.0D;
         double len = base * (0.75D + 0.4D * (double)fx.rand(30 + r));
         double ph = (double)fx.rand(50 + r) * 6.283185307179586D;
         double x = fx.x() + Math.cos(a) * 0.06D * s.scale;
         double z = fx.z() + Math.sin(a) * 0.06D * s.scale;
         double step = len / 29.0D;

         for(int j = 0; j < 30; ++j) {
            int i = r * 30 + j;
            s.wx[i] = x;
            s.wz[i] = z;
            double u = (double)j / 29.0D;
            double th = a + dir * c * (0.45D * u + 2.8D * u * u * u) + 0.22D * Math.sin(u * 7.0D + ph);
            x += Math.cos(th) * step;
            z += Math.sin(th) * step;
         }

         s.delay[r] = 0.04F * fx.rand(70 + r);
         s.rx[r] = Double.NaN;
      }

      fx.data = s;
   }

   private static double grown(LandingFx fx, BrambleBloomStyle.State s, int r) {
      float t = fx.styleTime() - s.delay[r];
      return (double)Easing.outCubic(Easing.clamp01(t / (s.jump?0.15F:0.25F)));
   }

   private static float wither(LandingFx fx, BrambleBloomStyle.State s) {
      float t = fx.styleTime();
      return s.jump?Easing.smoothstep(0.2F, 0.55F, t):Easing.smoothstep(0.75F, 1.25F, t);
   }

   private static float sunk(LandingFx fx, BrambleBloomStyle.State s) {
      float t = fx.styleTime();
      return s.jump?Easing.smoothstep(0.3F, 0.55F, t):Easing.inCubic(Easing.clamp01((t - 0.75F - 0.1F) / 0.4F));
   }

   private static double bloom(LandingFx fx, BrambleBloomStyle.State s, int r) {
      float t = fx.styleTime() - s.delay[r];
      return (double)Easing.outBack(Easing.clamp01((t - 0.25F) / 0.19999999F), 1.6F);
   }

   public void renderPass(LandingFx fx, FxFrame f, FxPass pass) {
      BrambleBloomStyle.State s = (BrambleBloomStyle.State)fx.data;
      if(s != null) {
         float t = fx.styleTime();
         float alpha = fx.alpha();
         if((!s.jump || t < 0.55F) && alpha > 0.003F) {
            Camera cam = Vfx.camera();
            this.floor.hf = fx.ground;
            float w = wither(fx, s);
            float sink = sunk(fx, s);
            boolean vines = sink < 0.999F;
            int points = fx.lod?15:30;
            if(vines) {
               MeshBatch.begin(Material.CUSTOM, Finish.MATTE, fx.light, 0.1F + 0.55F * fx.night, -3549953, true, true);

               for(int r = 0; r < s.n; ++r) {
                  this.runner(fx, f, s, cam, r, points, w, sink, alpha);
               }

               MeshBatch.end();
            }

            MeshBatch.begin(Material.CUSTOM, Finish.SATIN, fx.light, 0.1F + 0.25F * fx.night, -3549953, false, true);
            if(vines) {
               for(int r = 0; r < s.n; ++r) {
                  if(!fx.lod) {
                     this.thorns(fx, f, s, cam, r, points, w, alpha);
                  }

                  if(this.roses.get() && !s.jump) {
                     this.rose(fx, f, s, cam, r, w, sink, alpha);
                  }
               }
            }

            if(this.roses.get() && !s.jump && !fx.lod) {
               if(!s.shed && t >= 0.83F) {
                  this.shed(fx, s);
               }

               this.petals(fx, f, s, cam, t, alpha);
            }

            MeshBatch.end();
         }
      }

   }

   private void runner(LandingFx fx, FxFrame f, BrambleBloomStyle.State s, Camera cam, int r, int points, float w, float sink, float alpha) {
      double g = grown(fx, s, r);
      if(g <= 0.01D) {
         s.rx[r] = Double.NaN;
      } else {
         int at = r * 30;
         int bark = ColorUtil.lerp(this.color.getColor((float)r / (float)Math.max(1, s.n - 1)), -15856630, 0.6F * w);
         double thick = 0.045D * Math.sqrt(s.scale) * (s.jump?0.75D:1.0D) * (1.0D - 0.45D * (double)w);
         boolean mine = fx.land.self && f.firstPerson;

         for(int j = 0; j < points; ++j) {
            double u = g * (double)j / ((double)points - 1.0D);
            double idx = u * 29.0D;
            int j0 = Math.min(28, (int)idx);
            double fr = idx - (double)j0;
            int i0 = at + j0;
            double x = s.wx[i0] + (s.wx[i0 + 1] - s.wx[i0]) * fr;
            double z = s.wz[i0] + (s.wz[i0 + 1] - s.wz[i0]) * fr;
            int i = at + j;
            this.kit.px[i] = f.rx(x);
            this.kit.py[i] = f.ry(fx.y());
            this.kit.pz[i] = f.rz(z);
            double tip = (double)Easing.clamp01((g - u) / 0.08D + (g >= 0.999D?0.4D:0.0D));
            double knots = Math.sin(u * 37.0D + (double)r * 3.1D) * Math.sin(u * 13.0D + (double)r * 1.7D);
            this.kit.rad[i] = thick * (1.0D - 0.55D * u) * (1.0D + 0.15D * knots) * Math.max(0.18D, tip);
            int c = ColorUtil.shade(bark, (float)(0.86D + 0.16D * Math.sin(u * 23.0D + (double)r * 2.3D)));
            this.kit.col[i] = -16777216 | c & 16777215;
            this.kit.keep[i] = 1.0F;
         }

         double lift = -(thick * 2.3D + 0.012D) * (double)sink;
         int n = this.kit.followGround(at, points, this.floor, f.viewX, f.viewY, f.viewZ, lift);
         if(n < 2) {
            s.rx[r] = Double.NaN;
         } else {
            if(mine) {
               for(int j = 0; j < n; ++j) {
                  int i = at + j;
                  this.kit.keep[i] = NearCamera.dark(cam, this.kit.px[i], this.kit.py[i], this.kit.pz[i], 1.0F);
               }
            }

            this.kit.groundFrames(at, n);
            this.kit.tube(at, n, false, cam, alpha);
            int e = at + n - 1;
            s.rx[r] = this.kit.px[e] + f.viewX;
            s.ry[r] = this.kit.py[e] + f.viewY;
            s.rz[r] = this.kit.pz[e] + f.viewZ;
            s.rax[r] = this.kit.tx[e];
            s.raz[r] = this.kit.tz[e];
            s.reached[r] = n;
         }
      }

   }

   private void thorns(LandingFx fx, FxFrame f, BrambleBloomStyle.State s, Camera cam, int r, int points, float w, float alpha) {
      float density = this.thornDensity.getFloat() / 100.0F;
      double g = grown(fx, s, r);
      if(density > 0.0F && g > 0.05D && !Double.isNaN(s.rx[r])) {
         int n = s.reached[r];
         double reach = (double)this.length.getFloat() * Math.sqrt(s.scale) * (s.jump?0.45D:1.0D);
         int count = Math.max(1, Math.round(density * 6.0F * (float)reach));
         int tip = this.tipColor.getColor(0.5F);
         double flare = 0.35D + 0.65D * g;

         for(int q = 0; q < count; ++q) {
            double u = ((double)q + 0.5D + 0.6D * (hash(r * 31 + q, 3) - 0.5D)) / (double)count;
            double atPoint;
            if(u >= 0.06D && u <= g - 0.04D && (atPoint = u / g * (double)(points - 1)) <= (double)n - 1.05D) {
               double around = (double)(q % 2 == 0?1:-1) * (0.35D + 0.9D * hash(r * 13 + q, 7));
               double len = 0.085D * Math.sqrt(s.scale) * flare * (0.8D + 0.4D * hash(r * 17 + q, 23)) * (1.0D - 0.4D * (double)w);
               this.kit.thorn(r * 30, n, atPoint, around, len, tip, (double)w, alpha);
            }
         }
      }

   }

   private void rose(LandingFx fx, FxFrame f, BrambleBloomStyle.State s, Camera cam, int r, float w, float sink, float alpha) {
      float bud = Easing.smoothstep(0.55F, 0.95F, (float)grown(fx, s, r));
      if(bud > 0.01F && !Double.isNaN(s.rx[r])) {
         double open = Math.max(0.0D, bloom(fx, s, r));
         double size = 0.13D * s.scale * (0.85D + 0.3D * hash(r, 41)) * (0.3D + 0.7D * Math.min(1.0D, open)) * (double)bud * (1.0D - 0.3D * (double)sink);
         double ax = s.rax[r] * 0.4D;
         double ay = 1.0D;
         double az = s.raz[r] * 0.4D;
         double al = Math.sqrt(ax * ax + ay * ay + az * az);
         int col = ColorUtil.lerp(this.roseColor.getColor(0.5F), -15856630, 0.7F * w) | -16777216;
         double x = f.rx(s.rx[r]);
         double y = f.ry(s.ry[r]) - size * 0.3D * (double)sink;
         double z = f.rz(s.rz[r]);
         float a = alpha;
         if(fx.land.self && f.firstPerson) {
            a = NearCamera.dark(cam, x, y, z, alpha);
         }

         this.kit.rose(fx.seed + r & 7, x, y, z, ax / al, ay / al, az / al, size, Math.min(1.1D, open) * (1.0D - 0.35D * (double)w), col, a);
      }

   }

   private void shed(LandingFx fx, BrambleBloomStyle.State s) {
      s.shed = true;

      for(int r = 0; r < s.n && s.petals < 42; ++r) {
         if(!Double.isNaN(s.rx[r])) {
            int count = 2 + (hash(r, 61) > 0.5D?1:0);

            for(int q = 0; q < count && s.petals < 42; ++q) {
               double a = 6.283185307179586D * hash(r * 7 + q, 67);
               double d = (0.08D + 0.22D * hash(r * 7 + q, 71)) * Math.sqrt(s.scale);
               double x = s.rx[r] + Math.cos(a) * d;
               double z = s.rz[r] + Math.sin(a) * d;
               double y = Ground.y(fx.ground, x, z);
               if(Ground.lastValid()) {
                  int i = s.petals++;
                  s.px[i] = x;
                  s.py[i] = y + 0.012D;
                  s.pz[i] = z;
                  s.spin[i] = 6.283185307179586D * hash(r * 7 + q, 73);
                  s.tumble[i] = 0.08D + 0.25D * hash(r * 7 + q, 79);
                  s.psize[i] = 0.13D * s.scale * (0.45D + 0.2D * hash(r * 7 + q, 83));
               }
            }
         }
      }

   }

   private void petals(LandingFx fx, FxFrame f, BrambleBloomStyle.State s, Camera cam, float t, float alpha) {
      if(s.petals != 0) {
         float a = alpha * Easing.smoothstep(0.83F, 0.95F, t) * (1.0F - Easing.smoothstep(1.25F, 1.5F, t));
         if(a > 0.003F) {
            int petal = ColorUtil.lerp(this.roseColor.getColor(0.5F), -15856630, 0.3F) | -16777216;
            boolean mine = fx.land.self && f.firstPerson;

            for(int i = 0; i < s.petals; ++i) {
               double x = f.rx(s.px[i]);
               double y = f.ry(s.py[i]);
               double z = f.rz(s.pz[i]);
               this.kit.tumblingPetal(x, y, z, s.spin[i], s.tumble[i], s.psize[i], petal, mine?NearCamera.dark(cam, x, y, z, a):a);
            }
         }
      }

   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8) / 1.6777216E7D;
   }

   public String stateForTest(LandingFx fx) {
      BrambleBloomStyle.State s = (BrambleBloomStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.styleTime();
         String phase = s.jump?(t < 0.15F?"whipping out":(t < 0.2F?"out":(t < 0.55F?"withering":"gone"))):(t < 0.25F?"runners shooting out":(t < 0.45F?"roses blooming":(t < 0.75F?"holding":(t < 1.25F?"withering, sinking":(t < 1.5F?"the petals fading":"gone")))));
         int roseCount = 0;

         for(int r = 0; r < s.n; ++r) {
            if(!Double.isNaN(s.rx[r])) {
               ++roseCount;
            }
         }

         return String.format(Locale.ROOT, "%s; %d runners (%d reached), grown %.2f, roses %s (open %.2f), wither %.2f, sunk %.2f, %d petals, curl %.0f%%; fall %.1f (x%.2f)%s; %s, light %.2f, night %.2f", new Object[]{phase, Integer.valueOf(s.n), Integer.valueOf(roseCount), Double.valueOf(grown(fx, s, 0)), this.roses.get() && !s.jump?"on":"off", Double.valueOf(bloom(fx, s, 0)), Float.valueOf(wither(fx, s)), Float.valueOf(sunk(fx, s)), Integer.valueOf(s.petals), Float.valueOf(this.curl.getFloat()), Float.valueOf(fx.land.fallHeight), Float.valueOf(fx.land.fallScale), s.jump?", a jump":"", fx.lod?"far":"near", Float.valueOf(fx.light), Float.valueOf(fx.night)});
      }
   }

   private static final class Floor implements Surface {
      Terrain.HeightField hf;

      private Floor() {
      }

      public double at(double x, double y, double z) {
         return this.hf.at(x, z);
      }
   }

   private static final class State {
      int n;
      int petals;
      boolean jump;
      boolean shed;
      double scale;
      final double[] wx;
      final double[] wz;
      final float[] delay;
      final int[] reached;
      final double[] rx;
      final double[] ry;
      final double[] rz;
      final double[] rax;
      final double[] raz;
      final double[] px;
      final double[] py;
      final double[] pz;
      final double[] spin;
      final double[] tumble;
      final double[] psize;

      private State() {
         this.wx = new double[420];
         this.wz = new double[420];
         this.delay = new float[14];
         this.reached = new int[14];
         this.rx = new double[14];
         this.ry = new double[14];
         this.rz = new double[14];
         this.rax = new double[14];
         this.raz = new double[14];
         this.px = new double[42];
         this.py = new double[42];
         this.pz = new double[42];
         this.spin = new double[42];
         this.tumble = new double[42];
         this.psize = new double[42];
      }
   }
}
