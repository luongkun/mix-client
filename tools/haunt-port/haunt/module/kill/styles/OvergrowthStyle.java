package mix.client.mixui.integration.haunt.module.kill.styles;

import java.util.Locale;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Vines;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.ambience.AmbienceModule;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.look.BodyLook;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class OvergrowthStyle extends KillStyleBase {
   private static final float GROWN = 0.7F;
   private static final float BLOOMED = 1.0F;
   private static final float WITHERED = 1.7F;
   private static final float COLLAPSED = 2.4F;
   private static final int POINTS = 26;
   private static final int MAX_VINES = 8;
   private static final int MAX_ROSES = 10;
   private static final int MAX_PETALS = 48;
   private static final double TURNS = 2.0D;
   private static final double R_FOOT = 0.5D;
   private static final double R_HEAD = 0.3D;
   private static final double TOP = 2.05D;
   private static final double LEAN = 0.18D;
   private static final double PETAL_GRAVITY = 1.6D;
   private static final double PETAL_DRAG = 2.4D;
   private static final int NIGHT_RIM = -6309633;
   public final ColorSetting thornTips;
   public final ColorSetting roses;
   public final SliderSetting vineCount;
   public final SliderSetting roseCount;
   public final SliderSetting thornDensity;
   public final ModeSetting<OvergrowthStyle.Ending> ending;
   public final BooleanSetting petals;
   private final Vines kit = new Vines(208);
   private final double[] point = new double[3];
   private final float[] wind = new float[3];

   public OvergrowthStyle(KillEffectsModule module) {
      super(module, KillStyle.OVERGROWTH, new ColorSetting("overgrowth.color", "Vines", "The brambles\' bark.", ColorSetting.Mode.STATIC, -14734816, -12965346, 1.0D));
      this.signature(this.color, -14734816);
      this.thornTips = (ColorSetting)this.setting((new ColorSetting("overgrowth.thornTips", "Thorn Tips", "The thorns\' pale tips.", ColorSetting.Mode.STATIC, -1452088, -1, 1.0D)).secondary());
      this.signature(this.thornTips, -1452088);
      this.roses = (ColorSetting)this.setting((new ColorSetting("overgrowth.roses", "Roses", "The roses and their petals.", ColorSetting.Mode.STATIC, -5041618, -9760982, 1.0D)).secondary());
      this.signature(this.roses, -5041618);
      this.vineCount = this.look((SliderSetting)this.setting(new SliderSetting("overgrowth.vines", "Vines", "How many brambles climb the body.", 3.0D, 8.0D, 5.0D, 1.0D)));
      this.roseCount = (SliderSetting)this.setting(new SliderSetting("overgrowth.roseCount", "Roses", "How many roses bloom on the vines.", 0.0D, 10.0D, 6.0D, 1.0D));
      this.thornDensity = (SliderSetting)this.setting((new SliderSetting("overgrowth.thornDensity", "Thorn Density", "How thick with thorns the vines are.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%"));
      this.ending = (ModeSetting)this.setting((new ModeSetting("overgrowth.ending", "Ending", "Withering into petals, or sinking into the ground.", OvergrowthStyle.Ending.class, OvergrowthStyle.Ending.WITHER)).style(ModeSetting.Style.CHIPS));
      this.petals = (BooleanSetting)this.setting(new BooleanSetting("overgrowth.petals", "Petals", "Dark petals blow away as it collapses.", true));
   }

   public float seconds(KillFx fx) {
      return 2.4F;
   }

   public int passes(KillFx fx) {
      return FxPass.SOLID.bit() | (fx.night > 0.01F?FxPass.GLOW.bit():0);
   }

   public void start(KillFx fx) {
      OvergrowthStyle.State s = new OvergrowthStyle.State();
      double ax = fx.ctx.dirX;
      double az = fx.ctx.dirZ;
      double l = Math.sqrt(ax * ax + az * az);
      if(l < 1.0E-4D) {
         ax = 0.0D;
         az = 1.0D;
         l = 1.0D;
      }

      s.awayX = ax / l;
      s.awayZ = az / l;
      int n = this.vines(fx);

      for(int k = 0; k < n; ++k) {
         s.start[k] = 6.283185307179586D * ((double)k + 0.35D * hash(fx.seed, k * 3 + 1)) / (double)n;
         s.delay[k] = 0.12D * hash(fx.seed, k * 3 + 2);
      }

      s.roses = Math.min(10, fx.lod?this.roseCount.getInt() / 2:this.roseCount.getInt());

      for(int i = 0; i < s.roses; ++i) {
         s.roseVine[i] = i % n;
         s.roseAt[i] = 0.25D + 0.65D * hash(fx.seed, 40 + i * 3);
         s.roseSize[i] = (0.1D + 0.05D * hash(fx.seed, 41 + i * 3)) * (double)fx.size;
      }

      fx.data = s;
   }

   private int vines(KillFx fx) {
      int n = Math.min(8, this.vineCount.getInt());
      return fx.lod?Math.max(2, n / 2):n;
   }

   private static double grown(KillFx fx, OvergrowthStyle.State s, int k) {
      return (double)Easing.outCubic(Easing.clamp01(((double)fx.t - s.delay[k]) / (0.699999988079071D - s.delay[k])));
   }

   private static float withered(KillFx fx) {
      return Easing.inOutCubic(fx.phase(1.0F, 1.7F));
   }

   private static float collapsed(KillFx fx) {
      return fx.phase(1.7F, 2.4F);
   }

   private float sunk(KillFx fx) {
      return this.ending.get() == OvergrowthStyle.Ending.SINK?Easing.inCubic(fx.phase(1.2F, 2.4F)):0.0F;
   }

   private void vinePoint(KillFx fx, OvergrowthStyle.State s, int k, double u, double sink) {
      double size = (double)fx.size;
      double a = s.start[k] + 12.566370614359172D * u * (double)(k % 2 == 0?1:-1);
      double slump = this.ending.get() == OvergrowthStyle.Ending.WITHER?(double)Easing.smoothstep(0.0F, 0.7F, collapsed(fx)):0.0D;
      double r = (0.5D + -0.2D * u) * size * (1.0D + 0.6D * slump);
      double lean = 0.18D * u * size;
      this.point[0] = fx.ctx.feetX + Math.cos(a) * r + s.awayX * lean;
      this.point[1] = fx.ctx.groundY + 2.05D * u * size * (1.0D - 0.9D * slump) - sink;
      this.point[2] = fx.ctx.feetZ + Math.sin(a) * r + s.awayZ * lean;
   }

   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      OvergrowthStyle.State s = (OvergrowthStyle.State)fx.data;
      if(s != null) {
         if(pass == FxPass.GLOW) {
            this.night(fx, f);
         } else {
            this.body(fx, f);
            Camera cam = Vfx.camera();
            float alpha = fx.alpha();
            float c = collapsed(fx);
            double sink = (double)this.sunk(fx) * 2.05D * (double)fx.size;
            int n = this.vines(fx);
            float vineAlpha = alpha * (this.ending.get() == OvergrowthStyle.Ending.SINK?1.0F:1.0F - Easing.smoothstep(0.45F, 0.95F, c));
            MeshBatch.begin(Material.CUSTOM, Finish.MATTE, fx.light, 0.08F + 0.5F * fx.night, -6309633, true, true);
            if(vineAlpha > 0.003F) {
               for(int k = 0; k < n; ++k) {
                  this.vine(fx, f, s, cam, k, n, sink, vineAlpha);
               }
            }

            MeshBatch.end();
            MeshBatch.begin(Material.CUSTOM, Finish.SATIN, fx.light, 0.1F + 0.25F * fx.night, -6309633, false, true);
            if(vineAlpha > 0.003F) {
               this.thorns(fx, s, n, vineAlpha);
               this.roses(fx, f, s, sink, vineAlpha);
            }

            if(this.petals.get() && this.ending.get() == OvergrowthStyle.Ending.WITHER) {
               this.petals(fx, f, s, alpha);
            }

            MeshBatch.end();
            RenderState.translucent(true);
         }
      }

   }

   private void vine(KillFx fx, FxFrame f, OvergrowthStyle.State s, Camera cam, int k, int n, double sink, float alpha) {
      double g = grown(fx, s, k);
      if(g > 0.01D) {
         float w = withered(fx);
         double thick = 0.046D * (double)fx.size * (1.0D - 0.35D * (double)w);
         int bark = ColorUtil.lerp(this.color.getColor((float)k / (float)Math.max(1, n - 1)), -15856630, 0.6F * w);
         int at = k * 26;

         for(int j = 0; j < 26; ++j) {
            double u = g * (double)j / 25.0D;
            this.vinePoint(fx, s, k, u, sink);
            int i = at + j;
            this.kit.px[i] = f.rx(this.point[0]);
            this.kit.py[i] = f.ry(this.point[1]);
            this.kit.pz[i] = f.rz(this.point[2]);
            double tip = (double)Easing.clamp01((g - u) / 0.08D + (g >= 1.0D?0.35D:0.0D));
            double knots = Math.sin(u * 37.0D + (double)k * 3.1D) * Math.sin(u * 15.0D + (double)k * 1.7D);
            double under = this.point[1] < fx.ctx.groundY - 0.02D?0.0D:1.0D;
            this.kit.rad[i] = thick * (1.0D - 0.3D * u) * (1.0D + 0.16D * knots) * Math.max(0.15D, tip) * under;
            int cc = ColorUtil.shade(bark, (float)(0.86D + 0.18D * Math.sin(u * 31.0D + (double)k * 2.3D)));
            this.kit.col[i] = -16777216 | cc & 16777215;
            this.kit.keep[i] = 1.0F;
         }

         this.kit.frames(at, 26, false, f.rx(fx.ctx.feetX), f.rz(fx.ctx.feetZ));
         this.kit.tube(at, 26, false, cam, alpha);
      }

   }

   private void thorns(KillFx fx, OvergrowthStyle.State s, int n, float alpha) {
      float density = this.thornDensity.getFloat() / 100.0F;
      if(density > 0.0F) {
         int tip = this.thornTips.getColor(0.5F);
         float w = withered(fx);

         for(int k = 0; k < n; ++k) {
            double g = grown(fx, s, k);
            int count = Math.round(density * 22.0F);

            for(int q = 0; q < count; ++q) {
               double u = ((double)q + 0.5D + 0.6D * (hash(k * 31 + q, 3) - 0.5D)) / (double)count;
               if(u >= 0.05D && u <= g - 0.04D) {
                  double at = u / Math.max(1.0E-6D, g) * 25.0D;
                  double len = 0.07D * (double)fx.size * (0.8D + 0.4D * hash(k * 17 + q, 23)) * (1.0D - 0.3D * (double)w);
                  this.kit.thorn(k * 26, 26, at, 6.283185307179586D * hash(k * 13 + q, 7), len, tip, (double)w, alpha);
               }
            }
         }
      }

   }

   private void roses(KillFx fx, FxFrame f, OvergrowthStyle.State s, double sink, float alpha) {
      float bloom = Easing.outCubic(fx.phase(0.65F, 1.0F));
      float w = withered(fx);
      if(bloom > 0.0F) {
         int rose = this.roses.getColor(0.5F);
         int wilted = ColorUtil.lerp(rose, -15856630, 0.75F * w) | -16777216;
         double open = (double)bloom * (1.0D - 0.55D * (double)w);

         for(int i = 0; i < s.roses; ++i) {
            int k = s.roseVine[i];
            double g = grown(fx, s, k);
            if(s.roseAt[i] <= g) {
               this.vinePoint(fx, s, k, s.roseAt[i], sink);
               double x = this.point[0];
               double y = this.point[1];
               double z = this.point[2];
               if(y >= fx.ctx.groundY) {
                  double ox = x - fx.ctx.feetX;
                  double oz = z - fx.ctx.feetZ;
                  double ol = Math.max(1.0E-6D, Math.sqrt(ox * ox + oz * oz));
                  this.kit.rose(fx.seed + i & 7, f.rx(x), f.ry(y), f.rz(z), ox / ol, 0.25D, oz / ol, s.roseSize[i] * (0.4D + 0.6D * (double)bloom) * (1.0D - 0.25D * (double)w), open, wilted, alpha);
               }
            }
         }

         float big;
         if(fx.crit && (big = Easing.outBack(fx.phase(0.85F, 1.1F), 1.4F)) > 0.0F) {
            BodyProxy.Part b = fx.rest[1];
            double tx = -s.awayX;
            double tz = -s.awayZ;
            double x = b.x + tx * 0.2D * (double)fx.size;
            double y = b.y + 0.15D * (double)fx.size - sink;
            double z = b.z + tz * 0.2D * (double)fx.size;
            this.kit.rose(fx.seed & 7, f.rx(x), f.ry(y), f.rz(z), tx, 0.1D, tz, 0.2D * (double)fx.size * (double)big * (1.0D - 0.2D * (double)w), Math.min(1.0D, (double)big) * (1.0D - 0.5D * (double)w), wilted, alpha);
         }
      }

   }

   private void body(KillFx fx, FxFrame f) {
      BodyProxy.Look look = fx.look;
      look.kind = BodyLook.SKIN;
      look.sweepTo = null;
      BodyProxy.Part[] parts = fx.parts;

      for(int i = 0; i < 6; ++i) {
         parts[i].copy(fx.rest[i]);
      }

      double sway = Math.sin((double)fx.t * 7.0D) * 0.012D * (1.0D - (double)fx.phase(0.7F, 1.0F)) * (double)fx.size;
      double sink = (double)this.sunk(fx) * 2.05D * (double)fx.size;

      for(BodyProxy.Part p : parts) {
         double h = p.y - fx.ctx.feetY;
         p.x += sway * h;
         p.y -= sink;
         if(sink > 0.0D) {
            p.clip = true;
            p.cnx = 0.0D;
            p.cny = 1.0D;
            p.cnz = 0.0D;
            p.cd = fx.ctx.groundY + 0.002D;
         }
      }

      float d = this.ending.get() == OvergrowthStyle.Ending.SINK?0.0F:fx.phase(1.1F, 1.85F);
      if(d < 1.0F) {
         if(d > 0.0F) {
            look.dissolve = d;
            look.dissolveNoise = 0.6F;
            look.sweepX = 0.0D;
            look.sweepY = -1.0D;
            look.sweepZ = 0.0D;
            look.sweepFrom = -(fx.ctx.feetY + 2.0D);
            look.sweepLength = 2.0D;
            look.edgeColor = ColorUtil.shade(this.color.getColor(0.5F), 0.7F) | -16777216;
            look.edgeWidth = 0.1F;
         } else {
            look.dissolve = -1.0F;
         }

         fx.drawBody(f);
         RenderState.translucent(true);
      }

   }

   private void petals(KillFx fx, FxFrame f, OvergrowthStyle.State s, float alpha) {
      float c = collapsed(fx);
      if(c > 0.0F) {
         if(!s.blown) {
            this.blow(fx, s);
         }

         double dt = Math.max(0.0D, Math.min(0.1D, (double)(fx.t - s.lastT)));
         s.lastT = fx.t;
         double wx = fx.ctx.dirX * 1.4D;
         double wz = fx.ctx.dirZ * 1.4D;
         AmbienceModule ambience = !fx.preview && FuriahsVisuals.instance != null?FuriahsVisuals.instance.getAmbience():null;
         if(ambience != null && ambience.windAt(fx.ctx.feetX, fx.ctx.feetY + 1.0D, fx.ctx.feetZ, this.wind)) {
            wx = (double)this.wind[0];
            wz = (double)this.wind[2];
         }

         int petal = ColorUtil.lerp(this.roses.getColor(0.5F), -15856630, 0.45F) | -16777216;
         float a = alpha * (1.0F - Easing.smoothstep(0.6F, 1.0F, c));
         double groundKeep = Math.pow(0.9D, dt * 60.0D);

         for(int i = 0; i < s.petals; ++i) {
            double flutter = Math.sin((double)fx.t * 9.0D + (double)i * 1.7D) * 0.9D;
            s.vx[i] += ((wx - s.vx[i]) * 2.4D + flutter * Math.cos(s.spin[i])) * dt;
            s.vz[i] += ((wz - s.vz[i]) * 2.4D + flutter * Math.sin(s.spin[i])) * dt;
            s.vy[i] += (-1.6D - s.vy[i] * 2.4D * 0.6D) * dt;
            s.px[i] += s.vx[i] * dt;
            s.py[i] += s.vy[i] * dt;
            s.pz[i] += s.vz[i] * dt;
            if(s.py[i] < fx.ctx.groundY + 0.02D) {
               s.py[i] = fx.ctx.groundY + 0.02D;
               s.vy[i] = 0.0D;
               s.vx[i] *= groundKeep;
               s.vz[i] *= groundKeep;
            }

            s.spin[i] += 3.0D * dt;
            s.tumble[i] += (5.0D + 3.0D * Math.sin((double)i * 2.1D)) * dt;
            this.kit.tumblingPetal(f.rx(s.px[i]), f.ry(s.py[i]), f.rz(s.pz[i]), s.spin[i], s.tumble[i], s.size[i], petal, a);
         }
      }

   }

   private void blow(KillFx fx, OvergrowthStyle.State s) {
      s.blown = true;
      s.lastT = fx.t;
      int n = this.vines(fx);
      s.petals = Math.min(48, Math.round((float)(fx.lod?14:28) * fx.intensity) + s.roses * 2);

      for(int i = 0; i < s.petals; ++i) {
         int k = i % n;
         double u = i < s.roses * 2?s.roseAt[i / 2]:0.1D + 0.85D * hash(fx.seed, 200 + i);
         if(i < s.roses * 2) {
            k = s.roseVine[i / 2];
         }

         this.vinePoint(fx, s, k, u, 0.0D);
         s.px[i] = this.point[0];
         s.py[i] = this.point[1];
         s.pz[i] = this.point[2];
         double ox = this.point[0] - fx.ctx.feetX;
         double oz = this.point[2] - fx.ctx.feetZ;
         double ol = Math.max(1.0E-6D, Math.sqrt(ox * ox + oz * oz));
         double out = 0.6D + 0.8D * hash(fx.seed, 300 + i);
         s.vx[i] = ox / ol * out + fx.ctx.dirX * 0.4D;
         s.vy[i] = 0.4D + 0.8D * hash(fx.seed, 400 + i);
         s.vz[i] = oz / ol * out + fx.ctx.dirZ * 0.4D;
         s.spin[i] = 6.283185307179586D * hash(fx.seed, 500 + i);
         s.tumble[i] = 6.283185307179586D * hash(fx.seed, 600 + i);
         s.size[i] = (0.045D + 0.03D * hash(fx.seed, 700 + i)) * (double)fx.size;
      }

   }

   private void night(KillFx fx, FxFrame f) {
      float a = fx.alpha() * fx.night * 0.14F * (1.0F - collapsed(fx));
      if(a > 0.003F) {
         BodyProxy.Part b = fx.rest[1];
         Vfx.sprite(Textures.GLOW, f.rx(b.x + fx.ctx.dirX * 0.3D), f.ry(b.y + 0.2D), f.rz(b.z + fx.ctx.dirZ * 0.3D), 2.4D * (double)fx.size, 0.0F, alpha(-6309633, a));
      }

   }

   public String stateForTest(KillFx fx) {
      OvergrowthStyle.State s = (OvergrowthStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.7F?"growing and wrapping":(t < 1.0F?"blooming":(t < 1.7F?"withering":(this.ending.get() == OvergrowthStyle.Ending.SINK?"sinking":"collapsing")));
         return String.format(Locale.ROOT, "%s; %d vines, %d roses, thorns %d%%, %s, %d petals out; light %.2f, night %.2f%s", new Object[]{phase, Integer.valueOf(this.vines(fx)), Integer.valueOf(s.roses), Integer.valueOf(this.thornDensity.getInt()), ((OvergrowthStyle.Ending)this.ending.get()).displayName(), Integer.valueOf(s.petals), Float.valueOf(fx.light), Float.valueOf(fx.night), fx.crit?", crit rose":""});
      }
   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8 & 16777215) / 1.6777216E7D;
   }

   public static enum Ending implements Displayable {
      WITHER("Wither", "It dries out and collapses into petals"),
      SINK("Sink into the ground", "The thicket draws the body down into the ground");

      private final String displayName;
      private final String description;

      private Ending(String displayName, String description) {
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
      double awayX;
      double awayZ;
      final double[] start;
      final double[] delay;
      final int[] roseVine;
      final double[] roseAt;
      final double[] roseSize;
      int roses;
      int petals;
      boolean blown;
      final double[] px;
      final double[] py;
      final double[] pz;
      final double[] vx;
      final double[] vy;
      final double[] vz;
      final double[] spin;
      final double[] tumble;
      final double[] size;
      float lastT;

      private State() {
         this.start = new double[8];
         this.delay = new double[8];
         this.roseVine = new int[10];
         this.roseAt = new double[10];
         this.roseSize = new double[10];
         this.px = new double[48];
         this.py = new double[48];
         this.pz = new double[48];
         this.vx = new double[48];
         this.vy = new double[48];
         this.vz = new double[48];
         this.spin = new double[48];
         this.tumble = new double[48];
         this.size = new double[48];
      }
   }
}
