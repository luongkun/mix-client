package mix.client.mixui.integration.haunt.module.kill.styles;

import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.Breeze;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Ink;
import mix.client.mixui.integration.haunt.fx.kit.Smoke;
import mix.client.mixui.integration.haunt.fx.look.KeepVisible;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.DissolveShader;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.look.BodyLook;
import mix.client.mixui.integration.haunt.render.stroke.Strokes;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class InkWashStyle extends KillStyleBase {
   private static final float SWEEP = 0.4F;
   private static final float STILL = 0.7F;
   private static final float STAMPED = 0.85F;
   private static final float DIFFUSE = 1.1F;
   private static final float AFTER = 0.05F;
   private static final int CURLS = 28;
   private static final int NIGHT = -3549953;
   private static final float STROKE_AT = 0.62F;
   private static final float STROKE_PAINT = 0.08F;
   private static final int STROKE_POINTS = 40;
   public final ColorSetting seal;
   public final SliderSetting levels;
   public final SliderSetting outline;
   public final BooleanSetting sealOn;
   public final ModeSetting<Ink.SealDesign> sealDesign;
   public final SliderSetting diffuseSpeed;
   public final BooleanSetting grain;
   private final Strokes.Style style = new Strokes.Style();
   private static final float NOISE = 0.55F;
   private static final float EDGE = 0.14F;
   private static final float CURL_WIND = 0.4F;
   private static final double[] FLOW = new double[3];

   public InkWashStyle(KillEffectsModule module) {
      super(module, KillStyle.INK_WASH, new ColorSetting("inkwash.color", "Ink", "The painting\'s ink, its outlines and the curls.", ColorSetting.Mode.STATIC, -15461356, -14013906, 1.0D));
      this.signature(this.color, -15461356);
      this.seal = (ColorSetting)this.setting((new ColorSetting("inkwash.seal", "Seal", "The red seal\'s color.", ColorSetting.Mode.STATIC, -3660241, -2078886, 1.0D)).secondary());
      this.signature(this.seal, -3660241);
      this.levels = (SliderSetting)this.setting(new SliderSetting("inkwash.levels", "Wash Levels", "How many shades of ink the painting has.", 2.0D, 5.0D, 3.0D, 1.0D));
      this.outline = this.look((SliderSetting)this.setting((new SliderSetting("inkwash.outline", "Outline Width", "How bold the brush outlines are.", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%")));
      this.sealOn = (BooleanSetting)this.setting(new BooleanSetting("inkwash.sealOn", "Seal", "A red seal stamps onto the painting\'s side.", true));
      this.sealDesign = (ModeSetting)this.setting((new ModeSetting("inkwash.sealDesign", "Seal Design", "The crest cut into the seal.", Ink.SealDesign.class, Ink.SealDesign.SUN)).style(ModeSetting.Style.CHIPS));
      this.diffuseSpeed = this.look((SliderSetting)this.setting((new SliderSetting("inkwash.diffuseSpeed", "Diffuse Speed", "How fast the painting diffuses away.", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%")));
      this.grain = (BooleanSetting)this.setting(new BooleanSetting("inkwash.grain", "Paper Grain", "The grain of rice paper in the painting.", true));
   }

   private float diffuse() {
      return 110.0F / this.diffuseSpeed.getFloat();
   }

   public float seconds(KillFx fx) {
      return 0.85F + this.diffuse() + 0.05F;
   }

   public void start(KillFx fx) {
      InkWashStyle.State s = new InkWashStyle.State();
      s.n = Math.min(28, fx.count(22.4F, false));
      BodyProxy.Part[] p = fx.rest;

      for(int i = 0; i < s.n; ++i) {
         int part = pick(hash(fx.seed, i * 3 + 1));
         BodyProxy.Part q = p[part];
         double ox = (hash(fx.seed, i * 3 + 2) - 0.5D) * 0.4D;
         double oy = (hash(fx.seed, i * 3 + 3) - 0.5D) * 0.6D;
         double oz = (hash(fx.seed, i * 3 + 4) - 0.5D) * 0.2D;
         s.x[i] = q.x + q.r[0] * ox + q.r[1] * oy + q.r[2] * oz;
         s.y[i] = q.y + q.r[3] * ox + q.r[4] * oy + q.r[5] * oz;
         s.z[i] = q.z + q.r[6] * ox + q.r[7] * oy + q.r[8] * oz;
         float sweep = Easing.clamp01((fx.ctx.feetY + 2.0D - s.y[i]) / 2.0D);
         float noise = DissolveShader.noiseAt(s.x[i] - fx.ctx.feetX, s.y[i] - fx.ctx.feetY, s.z[i] - fx.ctx.feetZ);
         s.at[i] = DissolveShader.thresholdAt(sweep, noise, 0.55F, 0.14F);
         s.size[i] = (float)((0.35D + 0.3D * hash(fx.seed, i * 3 + 5)) * (double)fx.size);
         s.spin[i] = (float)((hash(fx.seed, i * 3 + 6) - 0.5D) * 2.0D);
      }

      Breeze.at(fx.ctx.feetX, fx.ctx.feetY + 1.0D, fx.ctx.feetZ, fx.preview, s.wind);

      int n;
      for(int k = 0; k < 3; s.wind[n] *= 0.4F) {
         n = k++;
      }

      fx.data = s;
   }

   private static int pick(double h) {
      return h < 0.35D?1:(h < 0.55D?0:2 + (int)((h - 0.55D) / 0.45D * 4.0D));
   }

   public int passes(KillFx fx) {
      return FxPass.SOLID.bit() | FxPass.TRANSLUCENT.bit() | (fx.night > 0.01F?FxPass.GLOW.bit():0);
   }

   private float diffused(KillFx fx) {
      return fx.phase(0.85F, 0.85F + this.diffuse());
   }

   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      InkWashStyle.State s = (InkWashStyle.State)fx.data;
      if(s != null) {
         float alpha = fx.alpha();
         if(pass == FxPass.SOLID) {
            this.body(fx, f);
         } else if(pass == FxPass.TRANSLUCENT) {
            Camera cam = Vfx.camera();
            if(this.sealOn.get()) {
               this.seal(fx, f, alpha);
            }

            if(fx.crit) {
               this.stroke(fx, f, cam, alpha);
            }

            this.curls(fx, f, s, cam, alpha);
         } else if(pass == FxPass.GLOW) {
            this.night(fx, f, alpha);
         }
      }

   }

   private void body(KillFx fx, FxFrame f) {
      BodyProxy.Look look = fx.look;
      float t = fx.t;
      int ink = this.color.getColor(0.5F);
      look.params[1] = this.levels.getInt();
      look.params[2] = this.grain.get()?1:0;
      look.outlineColor = ink | -16777216;
      look.outlineWidth = 2.0F * this.outline.getFloat() / 100.0F;
      look.worldLight = fx.light;
      if(t < 0.4F) {
         look.kind = BodyLook.SKIN;
         look.sweepTo = BodyLook.INK;
         look.sweep = Easing.inOutCubic(t / 0.4F);
         look.sweepMode = 1;
         look.sweepOX = fx.ctx.feetX;
         look.sweepOY = fx.ctx.feetY;
         look.sweepOZ = fx.ctx.feetZ;
         look.sweepSpan = 1.95F;
         look.sweepEdgeColor = -790814;
      } else {
         look.kind = BodyLook.INK;
         look.sweepTo = null;
      }

      float d = this.diffused(fx);
      if(d > 0.0F) {
         look.dissolve = d;
         look.dissolveNoise = 0.55F;
         look.sweepX = 0.0D;
         look.sweepY = -1.0D;
         look.sweepZ = 0.0D;
         look.sweepFrom = -(fx.ctx.feetY + 2.0D);
         look.sweepLength = 2.0D;
         look.edgeColor = ink | -16777216;
         look.edgeWidth = 0.14F;
      } else {
         look.dissolve = -1.0F;
      }

      fx.drawBody(f);
      RenderState.translucent(true);
   }

   private void seal(KillFx fx, FxFrame f, float alpha) {
      float stamp = fx.phase(0.7F, 0.85F);
      if(stamp > 0.0F) {
         BodyProxy.Part b = fx.parts[1];
         double tx = -fx.ctx.dirX;
         double tz = -fx.ctx.dirZ;
         double lx = b.r[0];
         double lz = b.r[6];
         double bx = b.r[2];
         double bz = b.r[8];
         double dl = lx * tx + lz * tz;
         double db = bx * tx + bz * tz;
         double az;
         double ay;
         double ax;
         double half;
         double nz;
         double ny;
         double nx;
         if(Math.abs(db) >= Math.abs(dl)) {
            double sg = Math.signum(db);
            nx = b.r[2] * sg;
            ny = b.r[5] * sg;
            nz = b.r[8] * sg;
            half = 0.1171875D;
            ax = b.r[0];
            ay = b.r[3];
            az = b.r[6];
         } else {
            double sg = Math.signum(dl);
            nx = b.r[0] * sg;
            ny = b.r[3] * sg;
            nz = b.r[6] * sg;
            half = 0.234375D;
            ax = b.r[2];
            ay = b.r[5];
            az = b.r[8];
         }

         double ux = -b.r[1];
         double uy = -b.r[4];
         double uz = -b.r[7];
         double lift = half + 0.004D;
         double cx = b.x + nx * lift + ux * -0.12D + ax * 0.06D;
         double cy = b.y + ny * lift + uy * -0.12D + ay * 0.06D;
         double cz = b.z + nz * lift + uz * -0.12D + az * 0.06D;
         float k = Easing.outBack(stamp, 1.6F);
         double w = 0.22D * (double)fx.size * (1.35D - 0.35D * (double)k);
         double h = w * (0.8D + 0.2D * (double)Easing.outCubic(stamp));
         float sweep = Easing.clamp01((fx.ctx.feetY + 2.0D - cy) / 2.0D);
         float gone = Easing.smoothstep(sweep * 0.7F, sweep * 0.7F + 0.15F, this.diffused(fx));
         float a = alpha * Easing.clamp01(stamp * 3.0F) * (1.0F - gone);
         if(a > 0.003F) {
            int col = ColorUtil.shade(this.seal.getColor(0.5F), fx.light);
            RenderState.decal(true);
            Ink.quad(Ink.seals(), ((Ink.SealDesign)this.sealDesign.get()).ordinal(), 4, f.rx(cx), f.ry(cy), f.rz(cz), ax, ay, az, ux, uy, uz, w, h, 0.0F, alpha(col, a));
            RenderState.decal(false);
         }
      }

   }

   private void stroke(KillFx fx, FxFrame f, Camera cam, float alpha) {
      float painted = Easing.outCubic(fx.phase(0.62F, 0.7F));
      if(painted > 0.0F) {
         double sx = fx.ctx.swipeX;
         double sy = fx.ctx.swipeY;
         double sz = fx.ctx.swipeZ;
         double len = 1.5D * (double)fx.size;
         double cx = fx.ctx.feetX - fx.ctx.dirX * 0.4D;
         double cy = fx.ctx.feetY + 1.05D;
         double cz = fx.ctx.feetZ - fx.ctx.dirZ * 0.4D;

         for(int i = 0; i < 40; ++i) {
            double u = ((double)i / 39.0D - 0.5D) * len;
            double bow = 0.08D * len * Math.sin(3.141592653589793D * (double)i / 39.0D);
            double x = cx + sx * u;
            double y = cy + sy * u + bow;
            double z = cz + sz * u;
            Strokes.X[i] = f.rx(x);
            Strokes.Y[i] = f.ry(y);
            Strokes.Z[i] = f.rz(z);
            Strokes.A[i] = NearCamera.dark(cam, Strokes.X[i], Strokes.Y[i], Strokes.Z[i], 1.0F);
         }

         Strokes.wobble(40, 0.012D * len, fx.seed, fx.ctx.dirX, fx.ctx.dirY, fx.ctx.dirZ);
         this.style.reset();
         this.style.tool = Strokes.Tool.BRUSH;
         this.style.color = this.color.getColor(0.5F);
         this.style.width = 0.22D * (double)fx.size;
         this.style.alpha = alpha;
         this.style.progress = painted;
         this.style.heavyStart = 0.6F;
         this.style.dryness = 0.55F;
         this.style.diffuse = this.diffused(fx);
         this.style.light = fx.light;
         this.style.flat = true;
         this.style.normalX = fx.ctx.dirX;
         this.style.normalY = fx.ctx.dirY;
         this.style.normalZ = fx.ctx.dirZ;
         this.style.pointAlpha = true;
         Strokes.draw(40, this.style);
      }

   }

   private void curls(KillFx fx, FxFrame f, InkWashStyle.State s, Camera cam, float alpha) {
      float d = this.diffused(fx);
      if(d > 0.0F) {
         Smoke.Puffs puffs = s.puffs;
         puffs.count = 0;
         double[] flow = FLOW;
         float span = this.diffuse();

         for(int i = 0; i < s.n; ++i) {
            float born = 0.85F + s.at[i] * span;
            float age = fx.t - born;
            float k;
            if(age > 0.0F && (k = age / Math.min(1.4F, span)) < 1.0F) {
               Smoke.curl(s.x[i], s.y[i] + (double)age * 0.3D, s.z[i], (double)age, 1.6D, flow);
               double rise = 0.55D * (double)age + 0.25D * (double)age * (double)age;
               double x = s.x[i] + fx.ctx.dirX * 0.35D * (double)age + flow[0] * 0.25D * (double)age + (double)(s.wind[0] * age);
               double y = s.y[i] + rise + flow[1] * 0.15D * (double)age + (double)(s.wind[1] * age);
               double z = s.z[i] + fx.ctx.dirZ * 0.35D * (double)age + flow[2] * 0.25D * (double)age + (double)(s.wind[2] * age);
               int j = puffs.count++;
               puffs.x[j] = f.rx(x);
               puffs.y[j] = f.ry(y);
               puffs.z[j] = f.rz(z);
               puffs.size[j] = s.size[i] * (0.6F + 1.2F * k);
               float a = 0.55F * Easing.smoothstep(0.0F, 0.12F, k) * (1.0F - Easing.smoothstep(0.35F, 1.0F, k));
               puffs.alpha[j] = NearCamera.dark(cam, puffs.x[j], puffs.y[j], puffs.z[j], a);
               puffs.turn[j] = s.spin[i] * age;
               puffs.cell[j] = i % 4;
            }
         }

         if(puffs.count != 0) {
            puffs.sort(cam);
            puffs.draw(this.color.getColor(0.5F), alpha, (KeepVisible)null, cam, 0.0D, 0.0D, 1.0D);
         }
      }

   }

   private void night(KillFx fx, FxFrame f, float alpha) {
      float a = alpha * fx.night * 0.12F * (1.0F - this.diffused(fx));
      if(a > 0.003F) {
         BodyProxy.Part b = fx.parts[1];
         Vfx.sprite(Textures.GLOW, f.rx(b.x + fx.ctx.dirX * 0.3D), f.ry(b.y + 0.2D), f.rz(b.z + fx.ctx.dirZ * 0.3D), 2.2D * (double)fx.size, 0.0F, alpha(-3549953, a));
      }

   }

   public String stateForTest(KillFx fx) {
      InkWashStyle.State s = (InkWashStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.4F?"sweeping into ink":(t < 0.7F?"a still painting":(t < 0.85F?"the seal stamps":"diffusing"));
         int out = 0;
         float span = this.diffuse();

         for(int i = 0; i < s.n; ++i) {
            if(t > 0.85F + s.at[i] * span) {
               ++out;
            }
         }

         return String.format(Locale.ROOT, "%s; %d levels, outline %.1f px, dissolved %.2f (%s), %d of %d curls out%s; light %.2f, night %.2f", new Object[]{phase, Integer.valueOf(this.levels.getInt()), Float.valueOf(2.0F * this.outline.getFloat() / 100.0F), Float.valueOf(this.diffused(fx)), DissolveShader.available()?"shader":"fade", Integer.valueOf(out), Integer.valueOf(s.n), fx.crit?", crit stroke":"", Float.valueOf(fx.light), Float.valueOf(fx.night)});
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
      final Smoke.Puffs puffs;
      final double[] x;
      final double[] y;
      final double[] z;
      final float[] at;
      final float[] size;
      final float[] spin;
      int n;
      final float[] wind;

      private State() {
         this.puffs = new Smoke.Puffs(28);
         this.x = new double[28];
         this.y = new double[28];
         this.z = new double[28];
         this.at = new float[28];
         this.size = new float[28];
         this.spin = new float[28];
         this.wind = new float[3];
      }
   }
}
