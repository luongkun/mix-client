package mix.client.mixui.integration.haunt.module.landing.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.kit.Ink;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.stroke.Strokes;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class InkBlotStyle extends LandingStyleBase {
   private static final float BLOT = 0.12F;
   private static final float DROPS = 0.32F;
   private static final float SEAL_FROM = 0.32F;
   private static final float SEAL = 0.47F;
   private static final float SOAK = 0.5F;
   private static final float SOAK_TIME = 0.9F;
   private static final int MAX_DROPS = 40;
   private static final int MAX_FLUNG = 8;
   private static final int RING_POINTS = 64;
   private static final double BLOT_HALF = 0.74D;
   private static final int NIGHT_RIM = -2563329;
   public final ColorSetting seal;
   public final SliderSetting blotSize;
   public final SliderSetting splatter;
   public final BooleanSetting sealOn;
   public final ModeSetting<Ink.SealDesign> sealDesign;
   public final BooleanSetting enso;
   public final SliderSetting soakSpeed;
   private final Strokes.Style brush = new Strokes.Style();

   public InkBlotStyle(LandingImpactModule module) {
      super(module, LandingStyle.INK_BLOT, 1.4F, 2.6F, new ColorSetting("inkblot.color", "Ink", "The ink.", ColorSetting.Mode.STATIC, -15461356, -13948102, 1.0D));
      this.signature(this.color, -15461356);
      this.seal = (ColorSetting)this.setting((new ColorSetting("inkblot.seal", "Seal", "The seal stamped beside the blot.", ColorSetting.Mode.STATIC, -3660241, -2078145, 1.0D)).secondary());
      this.signature(this.seal, -3660241);
      this.blotSize = this.look((SliderSetting)this.setting((new SliderSetting("inkblot.blotSize", "Blot Size", "How big the blot is.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%")));
      this.splatter = this.look((SliderSetting)this.setting((new SliderSetting("inkblot.splatter", "Splatter", "How far the rays reach and how many droplets fly.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%")));
      this.sealOn = (BooleanSetting)this.setting(new BooleanSetting("inkblot.sealOn", "Seal", "A red seal stamps beside the blot.", true));
      this.sealDesign = (ModeSetting)this.setting((new ModeSetting("inkblot.sealDesign", "Seal Design", "The crest cut into the seal.", Ink.SealDesign.class, Ink.SealDesign.SUN)).style(ModeSetting.Style.CHIPS).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return InkBlotStyle.this.sealOn.get();
         }
      }));
      this.enso = (BooleanSetting)this.setting((new BooleanSetting("inkblot.enso", "Enso Ring", "A brush circle painted round the blot.", false)).keywords(new String[]{"enso", "ensō", "circle", "brush ring"}));
      this.soakSpeed = (SliderSetting)this.setting((new SliderSetting("inkblot.soakSpeed", "Soak Speed", "How fast the ink soaks into the ground.", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%"));
   }

   public float baseSeconds() {
      return 0.5F + 0.9F / Math.max(0.5F, this.soakSpeed.getFloat() / 100.0F);
   }

   public int passes(LandingFx fx) {
      return FxPass.TRANSLUCENT.bit() | (fx.night > 0.01F?FxPass.GLOW.bit():0);
   }

   public void start(LandingFx fx) {
      InkBlotStyle.State s = new InkBlotStyle.State();
      s.jump = fx.land.jump;
      float k = this.splatter.getFloat() / 100.0F;
      s.half = 0.74D * (double)this.blotSize.getFloat() / 100.0D * (double)fx.size * (s.jump?0.55D:1.0D);
      s.rot = (double)fx.rand(1) * 6.283185307179586D;
      s.blotCell = 0 + (fx.rand(2) < 0.5F?0:1);
      s.rayCell = 2 + (fx.rand(3) < 0.5F?0:1);
      s.soak = Math.max(0.5F, this.soakSpeed.getFloat() / 100.0F);
      double yaw = Math.toRadians((double)fx.land.yaw);
      double fwdX = -Math.sin(yaw);
      double fwdZ = Math.cos(yaw);
      if(s.jump) {
         int n = Math.min(40, 3 + Math.round(4.0F * k * fx.density));

         for(int i = 0; i < n; ++i) {
            double back = s.half * (1.3D + 0.9D * (double)i / (double)Math.max(1, n - 1)) + 0.1D * (double)fx.rand(40 + i);
            double side = (double)fx.srand(60 + i) * s.half * 0.35D;
            s.dx[i] = -fwdX * back - fwdZ * side;
            s.dz[i] = -fwdZ * back + fwdX * side;
            s.dsize[i] = s.half * (0.32D - 0.16D * (double)i / (double)Math.max(1, n - 1)) * (0.8D + 0.4D * (double)fx.rand(80 + i));
            s.dturn[i] = Math.atan2(-fwdZ, -fwdX);
            s.tail[i] = true;
            s.dat[i] = 0.12F + 0.12F * (float)i / (float)Math.max(1, n - 1);
         }

         s.drops = n;
      } else {
         int n = Math.min(40, Math.round((6.0F + 14.0F * k) * fx.density * (fx.lod?0.5F:1.0F)));

         for(int i = 0; i < n; ++i) {
            double ang = 6.283185307179586D * ((double)i + 0.6D * (double)fx.rand(100 + i)) / (double)Math.max(1, n);
            double r = s.half * (1.05D + (0.5D + 0.8D * (double)k) * (double)fx.rand(140 + i));
            s.dx[i] = Math.cos(ang) * r;
            s.dz[i] = Math.sin(ang) * r;
            s.dsize[i] = s.half * (0.07D + 0.11D * (double)fx.rand(180 + i)) * (1.25D - 0.4D * (r / s.half - 1.0D));
            s.dturn[i] = ang;
            s.tail[i] = fx.rand(220 + i) < 0.55F;
            s.dat[i] = 0.12F + 0.19999999F * (float)((r / s.half - 1.0D) / (0.5D + 0.8D * (double)k + 0.001D)) * 0.9F;
         }

         s.drops = n;
         if(fx.land.fallHeight >= 4.0F && !fx.lod) {
            int m = Math.min(8, 3 + Math.round(fx.land.fallScale * 3.0F));

            for(int i = 0; i < m && s.drops < 40; ++i) {
               double ang = (double)fx.rand(300 + i) * 6.283185307179586D;
               double reach = s.half * (1.8D + 1.4D * (double)fx.rand(320 + i));
               float up = 2.4F + fx.rand(340 + i) * 1.4F;
               float flight = up * 2.0F / 14.0F;
               s.fvx[i] = Math.cos(ang) * reach / (double)flight;
               s.fvz[i] = Math.sin(ang) * reach / (double)flight;
               s.fvy[i] = (double)up;
               s.fland[i] = 0.04F + flight;
               int d = s.drops++;
               s.dx[d] = Math.cos(ang) * reach;
               s.dz[d] = Math.sin(ang) * reach;
               s.dsize[d] = s.half * (0.1D + 0.06D * (double)fx.rand(360 + i));
               s.dturn[d] = ang;
               s.tail[d] = true;
               s.dat[d] = s.fland[i];
            }

            s.flung = m;
         }
      }

      double rightX = -fwdZ;
      double out = s.half * 1.15D;
      s.sx = rightX * out + fwdX * s.half * 0.25D;
      s.sz = fwdX * out + fwdZ * s.half * 0.25D;
      s.shalf = 0.21D * Math.sqrt((double)fx.size);
      s.sturn = yaw + 0.25D * (double)fx.srand(5);
      fx.data = s;
   }

   private static float soaked(LandingFx fx, InkBlotStyle.State s) {
      return Easing.clamp01((fx.styleTime() - 0.5F) / (0.9F / s.soak));
   }

   private static float left(float soak, float from, float to) {
      return 1.0F - Easing.smoothstep(from, to, soak);
   }

   public void renderPass(LandingFx fx, FxFrame f, FxPass pass) {
      InkBlotStyle.State s = (InkBlotStyle.State)fx.data;
      if(s != null) {
         if(pass == FxPass.GLOW) {
            this.night(fx, f, s);
         } else {
            Camera cam = Vfx.camera();
            float t = fx.styleTime();
            float soak = soaked(fx, s);
            int ink = ColorUtil.shade(this.color.getColor(0.5F), Math.max(0.4F, fx.light)) | -16777216;
            double cx = f.rx(fx.x());
            double cy = f.ry(fx.y());
            double cz = f.rz(fx.z());
            float alpha = NearCamera.dark(cam, cx, cy, cz, fx.alpha());
            RenderState.decal(true);
            RenderState.translucent(false);

            try {
               double spread = 1.0D + 0.08D * (double)soak;
               float grow = Easing.outCubic(Easing.clamp01(t / 0.12F));
               if(grow > 0.0F) {
                  double half = s.half * (0.35D + 0.65D * (double)grow) * spread;
                  if(!s.jump) {
                     decal(fx, f, Ink.shapes(), s.rayCell, 6, 0.0D, 0.0D, half * (1.15D + 0.45D * (double)this.splatter.getFloat() / 100.0D), s.rot + 1.3D, alpha(ink, alpha * left(soak, 0.1F, 0.65F)));
                  }

                  decal(fx, f, Ink.shapes(), s.blotCell, 6, 0.0D, 0.0D, half, s.rot, alpha(ink, alpha * left(soak, 0.25F, 0.85F)));
                  decal(fx, f, Ink.shapes(), s.blotCell, 6, 0.0D, 0.0D, half * 0.62D, s.rot, alpha(ink, alpha * 0.85F * left(soak, 0.45F, 1.0F) * (1.0F - left(soak, 0.25F, 0.85F))));
               }

               this.drops(fx, f, s, t, soak, ink, alpha);
               if(this.sealOn.get() && !s.jump) {
                  this.sealDecal(fx, f, s, t, soak, alpha);
               }
            } finally {
               RenderState.decal(false);
               RenderState.translucent(true);
            }

            if(this.enso.get() && !s.jump) {
               this.ring(fx, f, s, t, soak, ink, alpha);
            }

            this.flung(fx, f, s, t, ink, alpha, cam);
         }
      }

   }

   private static void decal(LandingFx fx, FxFrame f, int tex, int cell, int cells, double ox, double oz, double half, double turn, int col) {
      if(col >>> 24 != 0 && half > 1.0E-4D) {
         float u0 = (float)cell / (float)cells;
         float u1 = (float)(cell + 1) / (float)cells;
         Ground.decal(f, fx.ground, tex, u0, 0.0F, u1, 1.0F, fx.x() + ox, fx.z() + oz, half, turn, col, half > 0.4D?6:2);
      }

   }

   private void drops(LandingFx fx, FxFrame f, InkBlotStyle.State s, float t, float soak, int ink, float alpha) {
      float a = alpha * left(soak, 0.0F, 0.55F);
      if(a > 0.003F) {
         int tex = Ink.drops();

         for(int i = 0; i < s.drops; ++i) {
            float land = (t - s.dat[i]) / 0.05F;
            if(land > 0.0F) {
               double pop = 1.0D + 0.35D * (1.0D - (double)Easing.clamp01(land));
               double half = s.dsize[i] * pop * (1.0D + 0.06D * (double)soak);
               decal(fx, f, tex, s.tail[i]?1:0, 2, s.dx[i], s.dz[i], half, s.dturn[i], alpha(ink, a * Easing.clamp01(land)));
            }
         }
      }

   }

   private void sealDecal(LandingFx fx, FxFrame f, InkBlotStyle.State s, float t, float soak, float alpha) {
      float in = Easing.clamp01((t - 0.32F) / 0.15F);
      if(in > 0.0F) {
         double press = 1.0D + 0.4D * (1.0D - (double)Easing.outCubic(in));
         int red = ColorUtil.shade(this.seal.getColor(0.5F), Math.max(0.45F, fx.light)) | -16777216;
         decal(fx, f, Ink.seals(), ((Ink.SealDesign)this.sealDesign.get()).ordinal(), 4, s.sx, s.sz, s.shalf * press, s.sturn, alpha(red, alpha * Easing.clamp01(in * 2.5F) * left(soak, 0.3F, 0.9F)));
      }

   }

   private void ring(LandingFx fx, FxFrame f, InkBlotStyle.State s, float t, float soak, int ink, float alpha) {
      float drawn = Easing.inOutCubic(Easing.clamp01((t - 0.12F) / 0.3F));
      if(drawn > 0.0F) {
         double r = s.half * 1.45D + 0.1D * (double)fx.size;
         double start = s.rot + 2.1D;
         int n = 64;

         for(int i = 0; i < n; ++i) {
            double a = start + 5.843362335677016D * (double)i / (double)(n - 1);
            double wobble = 1.0D + 0.04D * Math.sin(a * 3.0D + s.rot) + 0.02D * Math.sin(a * 7.0D);
            double x = fx.x() + Math.cos(a) * r * wobble;
            double z = fx.z() + Math.sin(a) * r * wobble;
            Strokes.X[i] = f.rx(x);
            Strokes.Y[i] = f.ry(Ground.y(fx.ground, x, z) + 0.004D);
            Strokes.Z[i] = f.rz(z);
         }

         Strokes.Style b = this.brush.reset();
         b.tool = Strokes.Tool.BRUSH;
         b.color = ink;
         b.width = 0.16D * Math.sqrt((double)fx.size);
         b.alpha = alpha * 0.92F;
         b.progress = drawn;
         b.flat = true;
         b.normalX = 0.0D;
         b.normalY = 1.0D;
         b.normalZ = 0.0D;
         b.heavyStart = 1.0F;
         b.thinEnd = 0.75F;
         b.dryness = 0.6F;
         b.diffuse = Easing.smoothstep(0.35F, 1.0F, soak);
         b.light = Math.max(0.4F, fx.light);
         Strokes.draw(n, b);
      }

   }

   private void flung(LandingFx fx, FxFrame f, InkBlotStyle.State s, float t, int ink, float alpha, Camera cam) {
      if(s.flung != 0) {
         int tex = Ink.drops();
         RenderState.translucent(false);

         for(int i = 0; i < s.flung; ++i) {
            float age = t - 0.04F;
            if(age > 0.0F && t < s.fland[i]) {
               double x = fx.x() + s.fvx[i] * (double)age;
               double y = fx.y() + 0.15D + s.fvy[i] * (double)age - 7.0D * (double)age * (double)age;
               double z = fx.z() + s.fvz[i] * (double)age;
               double vx = s.fvx[i];
               double vy = s.fvy[i] - 14.0D * (double)age;
               double vz = s.fvz[i];
               double rx = f.rx(x);
               double ry = f.ry(y);
               double rz = f.rz(z);
               double sxv = vx * cam.rightX + vy * cam.rightY + vz * cam.rightZ;
               double syv = vx * cam.upX + vy * cam.upY + vz * cam.upZ;
               float turn = (float)Math.atan2(syv, sxv);
               double size = s.half * 0.16D;
               Vfx.sprite(tex, rx, ry, rz, size * 1.2D, size * 0.6D, turn, 0.5F, 0.0F, 1.0F, 1.0F, alpha(ink, NearCamera.dark(cam, rx, ry, rz, alpha)));
            }
         }

         RenderState.translucent(true);
      }

   }

   private void night(LandingFx fx, FxFrame f, InkBlotStyle.State s) {
      float k = fx.night * fx.alpha() * left(soaked(fx, s), 0.2F, 0.85F) * Easing.clamp01(fx.styleTime() / 0.12F);
      if(k > 0.01F) {
         RenderState.decal(true);
         decal(fx, f, Ink.shapes(), s.blotCell, 6, 0.0D, 0.0D, s.half * 1.08D, s.rot, alpha(-2563329, 0.16F * k));
         RenderState.decal(false);
      }

   }

   public String stateForTest(LandingFx fx) {
      InkBlotStyle.State s = (InkBlotStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.styleTime();
         float soak = soaked(fx, s);
         String phase = t < 0.12F?"splashing":(t < 0.32F?"droplets landing":(t < 0.47F?"stamping the seal":(soak < 1.0F?"soaking in":"gone")));
         int landed = 0;

         for(int i = 0; i < s.drops; ++i) {
            landed += t >= s.dat[i]?1:0;
         }

         return String.format(Locale.ROOT, "%s; blot %.2f, %d of %d droplets down (%d flung first)%s; seal %s; enso %s; soak %.2f (x%.2f); fall %.1f (x%.2f); %s, light %.2f, night %.2f", new Object[]{phase, Double.valueOf(s.half * 2.0D), Integer.valueOf(landed), Integer.valueOf(s.drops), Integer.valueOf(s.flung), s.jump?", a jump\'s trail":"", this.sealOn.get() && !s.jump?((Ink.SealDesign)this.sealDesign.get()).displayName():"off", this.enso.get()?"on":"off", Float.valueOf(soak), Float.valueOf(s.soak), Float.valueOf(fx.land.fallHeight), Float.valueOf(fx.land.fallScale), fx.lod?"far":"near", Float.valueOf(fx.light), Float.valueOf(fx.night)});
      }
   }

   private static final class State {
      double half;
      double rot;
      int blotCell;
      int rayCell;
      int drops;
      final double[] dx;
      final double[] dz;
      final double[] dsize;
      final double[] dturn;
      final boolean[] tail;
      final float[] dat;
      int flung;
      final double[] fvx;
      final double[] fvy;
      final double[] fvz;
      final float[] fland;
      double sx;
      double sz;
      double shalf;
      double sturn;
      float soak;
      boolean jump;

      private State() {
         this.dx = new double[40];
         this.dz = new double[40];
         this.dsize = new double[40];
         this.dturn = new double[40];
         this.tail = new boolean[40];
         this.dat = new float[40];
         this.fvx = new double[8];
         this.fvy = new double[8];
         this.fvz = new double[8];
         this.fland = new float[8];
      }
   }
}
