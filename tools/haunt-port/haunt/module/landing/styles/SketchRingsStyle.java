package mix.client.mixui.integration.haunt.module.landing.styles;

import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.kit.Sketch;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.stroke.Strokes;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class SketchRingsStyle extends LandingStyleBase {
   private static final float DRAW = 0.15F;
   private static final float NEXT = 0.06F;
   private static final float ERASE_FROM = 0.7F;
   private static final float END = 1.0F;
   private static final int RING_POINTS = 80;
   private static final int ARC_POINTS = 14;
   private static final int MAX_DOODLES = 6;
   private static final int CRACKS = 4;
   private static final int CRACK_POINTS = 6;
   private static final int NIGHT = -3549953;
   private static final double RING = 0.6D;
   private static final double GAP = 0.32D;
   private static final double GROUND_WEIGHT = 1.8D;
   public final ColorSetting highlights;
   public final ModeSetting<Sketch.Tool> tool;
   public final SliderSetting rings;
   public final ModeSetting<SketchRingsStyle.Doodles> doodles;
   public final BooleanSetting crack;
   public final SliderSetting boil;
   public final SliderSetting lineWidth;
   private final Strokes.Style style = new Strokes.Style();
   private final Sketch.Plane plane = new Sketch.Plane();
   private Sketch.Tool lastTool;
   private double ex;
   private double ez;
   private double ax;
   private double az;

   public SketchRingsStyle(LandingImpactModule module) {
      super(module, LandingStyle.SKETCH_RINGS, 1.0F, 1.9F, new ColorSetting("sketchrings.color", "Lines", "The rings\' lines.", ColorSetting.Mode.STATIC, -12961222, -13668371, 1.0D));
      this.signature(this.color, -12961222);
      this.highlights = (ColorSetting)this.setting((new ColorSetting("sketchrings.highlights", "Highlights", "The doodles that hop out.", ColorSetting.Mode.STATIC, -14020, -37986, 1.0D)).secondary());
      this.signature(this.highlights, -14020);
      this.tool = (ModeSetting)this.setting((new ModeSetting("sketchrings.tool", "Tool", "Pencil, marker or chalk. With Lines Static and still the tool\'s color, switching the tool switches the color too.", Sketch.Tool.class, Sketch.Tool.PENCIL)).style(ModeSetting.Style.CHIPS));
      this.rings = this.look((SliderSetting)this.setting(new SliderSetting("sketchrings.rings", "Rings", "How many rings are drawn.", 1.0D, 3.0D, 2.0D, 1.0D)));
      this.doodles = (ModeSetting)this.setting((new ModeSetting("sketchrings.doodles", "Doodles", "What hops out of the rings.", SketchRingsStyle.Doodles.class, SketchRingsStyle.Doodles.STARS)).style(ModeSetting.Style.CHIPS));
      this.crack = (BooleanSetting)this.setting(new BooleanSetting("sketchrings.crack", "Crack On Big Drops", "From 4 blocks of fall a scribbled crack under the feet.", true));
      this.boil = (SliderSetting)this.setting((new SliderSetting("sketchrings.boil", "Boil", "How many times a second the lines are redrawn with a new wobble.", 0.0D, 12.0D, 10.0D, 1.0D)).unit("/s"));
      this.lineWidth = this.look((SliderSetting)this.setting((new SliderSetting("sketchrings.lineWidth", "Line Width", "How thick the lines are.", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%")));
      this.lastTool = (Sketch.Tool)this.tool.get();
      this.tool.addListener(new Runnable() {
         public void run() {
            Sketch.Tool now = (Sketch.Tool)SketchRingsStyle.this.tool.get();
            Sketch.Tool before = SketchRingsStyle.this.lastTool;
            SketchRingsStyle.this.lastTool = now;
            Sketch.followTool(SketchRingsStyle.this.color, before, now);
         }
      });
   }

   public int passes(LandingFx fx) {
      boolean night = fx.night > 0.01F && this.tool.get() == Sketch.Tool.PENCIL;
      return night?FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit():FxPass.TRANSLUCENT.bit();
   }

   public void start(LandingFx fx) {
      SketchRingsStyle.State s = new SketchRingsStyle.State();
      s.jump = fx.land.jump;
      s.scale = (double)fx.size;
      s.rings = s.jump?1:this.rings.getInt();

      for(int k = 0; k < 3; ++k) {
         s.start[k] = (double)fx.rand(10 + k) * 6.283185307179586D;
         s.ph1[k] = (double)fx.rand(20 + k) * 6.283185307179586D;
         s.ph2[k] = (double)fx.rand(30 + k) * 6.283185307179586D;
      }

      float k = fx.land.fallScale;
      s.doodles = !s.jump && this.doodles.get() != SketchRingsStyle.Doodles.NONE?Math.min(6, 3 + (k > 1.2F?1:0) + (k > 1.45F?2:0)):0;

      for(int i = 0; i < s.doodles; ++i) {
         s.dAngle[i] = 6.283185307179586D * ((double)i + 0.3D + 0.4D * (double)fx.rand(40 + i)) / (double)s.doodles;
         s.dHop[i] = 0.28D + 0.18D * (double)fx.rand(50 + i);
         s.dSpin[i] = ((double)fx.rand(60 + i) - 0.5D) * 2.0D;
         s.dAt[i] = 0.05F + 0.1F * fx.rand(70 + i);
      }

      s.cracked = !s.jump && this.crack.get() && fx.land.fallHeight >= 4.0F;

      for(int var5 = 0; var5 < 4; ++var5) {
         s.cAngle[var5] = 6.283185307179586D * ((double)var5 + 0.5D * (double)fx.rand(80 + var5)) / 4.0D;
         s.cLen[var5] = (0.35D + 0.3D * (double)fx.rand(90 + var5)) * Math.sqrt(s.scale);
      }

      fx.data = s;
   }

   private static double radius(SketchRingsStyle.State s, int k) {
      return (s.jump?0.42D:0.6D + 0.32D * (double)k) * s.scale;
   }

   private static float erased(LandingFx fx) {
      return Easing.clamp01((fx.styleTime() - 0.7F) / 0.3F);
   }

   public void renderPass(LandingFx fx, FxFrame f, FxPass pass) {
      SketchRingsStyle.State s = (SketchRingsStyle.State)fx.data;
      if(s != null) {
         float t = fx.styleTime();
         float alpha = fx.alpha();
         float erase = erased(fx);
         if(alpha > 0.003F && erase < 1.0F) {
            Camera cam = Vfx.camera();
            double rl = Math.sqrt(cam.rightX * cam.rightX + cam.rightZ * cam.rightZ);
            this.ex = rl < 1.0E-4D?1.0D:cam.rightX / rl;
            this.ez = rl < 1.0E-4D?0.0D:cam.rightZ / rl;
            this.ax = -this.ez;
            this.az = this.ex;
            if(this.ax * cam.forwardX + this.az * cam.forwardZ < 0.0D) {
               this.ax = -this.ax;
               this.az = -this.az;
            }

            if(pass == FxPass.GLOW) {
               this.nightLight(fx, f, s, t, alpha, erase);
            } else {
               Sketch.Tool tl = (Sketch.Tool)this.tool.get();
               int lines = this.color.getColor(0.5F);
               int high = this.highlights.getColor(0.5F);
               double width = tl.width() * 1.8D * (double)this.lineWidth.getFloat() / 100.0D * Math.sqrt(s.scale);
               int seed = this.boil.getInt() <= 0?0:(int)Math.floor((double)(fx.age * this.boil.getFloat()));
               double amount = 0.035D * Math.sqrt(s.scale);
               boolean mine = fx.land.self && f.firstPerson && tl == Sketch.Tool.PENCIL;

               for(int k = 0; k < s.rings; ++k) {
                  float drawn = Easing.outCubic(Easing.clamp01((t - 0.06F * (float)k) / 0.15F));
                  int n = this.ring(fx, f, s, k);
                  this.line(fx, cam, n, tl, lines, width, alpha, drawn, erase, seed * 31 + k * 7 + 1, amount, mine);
               }

               if(s.cracked) {
                  for(int c = 0; c < 4; ++c) {
                     int n = this.crack(fx, f, s, c, seed);
                     this.line(fx, cam, n, tl, lines, width * 0.8D, alpha, Easing.outCubic(Easing.clamp01(t / 0.12F)), erase, seed * 17 + c, amount * 0.4D, mine);
                  }
               }

               if(!s.jump && !fx.lod) {
                  for(int d = -1; d <= 1; d += 2) {
                     this.whoosh(fx, f, cam, s, d, t, tl, lines, width, alpha, erase, seed, amount, mine);
                  }

                  for(int i = 0; i < s.doodles; ++i) {
                     this.doodle(fx, f, cam, s, i, t, tl, high, width, alpha, erase, seed);
                  }
               }

               this.smear(fx, f, s, lines, width, alpha, erase);
            }
         }
      }

   }

   private int ring(LandingFx fx, FxFrame f, SketchRingsStyle.State s, int k) {
      double r = radius(s, k);

      for(int i = 0; i < 80; ++i) {
         double u = (double)i / 79.0D;
         double a = s.start[k] + 6.785840131753954D * u;
         double rr = r * (1.0D + 0.05D * Math.sin(2.0D * a + s.ph1[k]) + 0.03D * Math.sin(5.0D * a + s.ph2[k])) * (1.0D + 0.05D * u);
         point(fx, f, i, fx.x() + Math.cos(a) * rr, fx.z() + Math.sin(a) * rr);
      }

      return 80;
   }

   private static void point(LandingFx fx, FxFrame f, int i, double x, double z) {
      double y = Ground.y(fx.ground, x, z);
      boolean on = Ground.lastValid();
      Strokes.X[i] = f.rx(x);
      Strokes.Y[i] = f.ry((on?y:fx.y()) + 0.006D);
      Strokes.Z[i] = f.rz(z);
      Strokes.A[i] = on?1.0F:0.0F;
   }

   private int crack(LandingFx fx, FxFrame f, SketchRingsStyle.State s, int c, int seed) {
      double a = s.cAngle[c];
      double ca = Math.cos(a);
      double sa = Math.sin(a);

      for(int i = 0; i < 6; ++i) {
         double u = (double)i / 5.0D;
         double along = s.cLen[c] * u;
         double side = (i == 0?0.0D:(double)((i & 1) == 0?1:-1) * (0.05D + 0.04D * hash(c * 11 + i, seed & 3))) * Math.sqrt(s.scale);
         point(fx, f, i, fx.x() + ca * along - sa * side, fx.z() + sa * along + ca * side);
      }

      return 6;
   }

   private void whoosh(LandingFx fx, FxFrame f, Camera cam, SketchRingsStyle.State s, int d, float t, Sketch.Tool tl, int lines, double width, float alpha, float erase, int seed, double amount, boolean mine) {
      float drawn = Easing.outCubic(Easing.clamp01((t - 0.06F) / 0.12F));
      if(drawn > 0.0F) {
         double side = Math.atan2(this.ez * (double)d, this.ex * (double)d);
         double out = radius(s, s.rings - 1) + (0.2D + 0.3D * (double)Easing.outCubic(Easing.clamp01((t - 0.06F) / 0.3F))) * Math.sqrt(s.scale);

         for(int arc = 0; arc < 2; ++arc) {
            double r = out + (double)arc * 0.13D * Math.sqrt(s.scale);
            double span = 0.32D - 0.08D * (double)arc;

            for(int i = 0; i < 14; ++i) {
               double a = side - span + 2.0D * span * (double)i / 13.0D;
               point(fx, f, i, fx.x() + Math.cos(a) * r, fx.z() + Math.sin(a) * r);
            }

            this.line(fx, cam, 14, tl, lines, width * 0.85D, alpha, drawn, erase, seed * 13 + d * 3 + arc, amount * 0.5D, mine);
         }
      }

   }

   private void doodle(LandingFx fx, FxFrame f, Camera cam, SketchRingsStyle.State s, int i, float t, Sketch.Tool tl, int high, double width, float alpha, float erase, int seed) {
      float since = t - s.dAt[i];
      if(since > 0.0F) {
         double hop = (double)Easing.clamp01(since / 0.45F);
         double r0 = radius(s, s.rings - 1);
         double out = r0 + 0.45D * Math.sqrt(s.scale) * (double)Easing.outCubic((float)hop);
         double x = fx.x() + Math.cos(s.dAngle[i]) * out;
         double z = fx.z() + Math.sin(s.dAngle[i]) * out;
         double gy = Ground.y(fx.ground, x, z);
         if(Ground.lastValid()) {
            double size = 0.08D * Math.sqrt(s.scale);
            double y = gy + size * 1.1D + 4.0D * s.dHop[i] * Math.sqrt(s.scale) * hop * (1.0D - hop);
            double spin = s.dSpin[i] * hop;
            double cs = Math.cos(spin);
            double sn = Math.sin(spin);
            this.plane.ox = f.rx(x);
            this.plane.oy = f.ry(y);
            this.plane.oz = f.rz(z);
            this.plane.rx = cam.rightX * cs + cam.upX * sn;
            this.plane.ry = cam.rightY * cs + cam.upY * sn;
            this.plane.rz = cam.rightZ * cs + cam.upZ * sn;
            this.plane.ux = cam.upX * cs - cam.rightX * sn;
            this.plane.uy = cam.upY * cs - cam.rightY * sn;
            this.plane.uz = cam.upZ * cs - cam.rightZ * sn;
            int n;
            switch((SketchRingsStyle.Doodles)this.doodles.get()) {
            case SPIRALS:
               n = Sketch.swirl(this.plane, 0.0D, 0.0D, size, 1.6D, (i & 1) == 0?1:-1, s.dSpin[i] * 3.0D);
               break;
            case HEARTS:
               n = Sketch.heart(this.plane, 0.0D, 0.0D, size);
               break;
            default:
               n = Sketch.star(this.plane, 0.0D, 0.0D, size);
            }

            for(int k = 0; k < n; ++k) {
               Strokes.A[k] = NearCamera.fade(cam, Strokes.X[k], Strokes.Y[k], Strokes.Z[k]);
            }

            Strokes.wobble(n, 0.01D * Math.sqrt(s.scale), seed * 7 + i, -cam.forwardX, -cam.forwardY, -cam.forwardZ);
            this.style.reset();
            this.style.tool = tl.stroke();
            this.style.color = high;
            this.style.width = width / 1.8D * 0.9D;
            this.style.alpha = alpha * (1.0F - Easing.smoothstep(0.0F, 0.6F, erase));
            this.style.progress = Easing.outCubic(Easing.clamp01(since / 0.12F));
            this.style.tile = 0.5D;
            this.style.light = fx.light;
            this.style.pointAlpha = true;
            Strokes.draw(n, this.style);
         }
      }

   }

   private void line(LandingFx fx, Camera cam, int n, Sketch.Tool tl, int color, double width, float alpha, float progress, float erase, int seed, double amount, boolean mine) {
      if(progress > 0.0F && alpha > 0.003F) {
         Strokes.wobble(n, amount, seed, 0.0D, 1.0D, 0.0D);
         if(mine) {
            for(int i = 0; i < n; ++i) {
               Strokes.A[i] *= NearCamera.dark(cam, Strokes.X[i], Strokes.Y[i], Strokes.Z[i], 1.0F);
            }
         }

         this.style.reset();
         this.style.tool = tl.stroke();
         this.style.color = color;
         this.style.width = width;
         this.style.alpha = alpha;
         this.style.progress = progress;
         this.style.erase = erase;
         this.style.eraseX = this.ex;
         this.style.eraseY = 0.0D;
         this.style.eraseZ = this.ez;
         this.style.flat = true;
         this.style.normalX = 0.0D;
         this.style.normalY = 1.0D;
         this.style.normalZ = 0.0D;
         this.style.tile = 0.5D;
         this.style.light = fx.light;
         this.style.pointAlpha = true;
         Strokes.draw(n, this.style);
      }

   }

   private void smear(LandingFx fx, FxFrame f, SketchRingsStyle.State s, int lines, double width, float alpha, float erase) {
      if(erase > 0.0F && erase < 1.0F) {
         double reach = radius(s, s.rings - 1) + 0.3D * Math.sqrt(s.scale);
         double gy = Ground.y(fx.ground, fx.x(), fx.z());
         this.plane.ox = f.rx(fx.x());
         this.plane.oy = f.ry(gy + 0.01D);
         this.plane.oz = f.rz(fx.z());
         this.plane.rx = this.ex;
         this.plane.ry = 0.0D;
         this.plane.rz = this.ez;
         this.plane.ux = this.ax;
         this.plane.uy = 0.0D;
         this.plane.uz = this.az;
         int m = Sketch.smear(this.plane, (-1.2D + 2.4D * (double)erase) * reach, reach * 0.9D, 0.12D * Math.sqrt(s.scale));
         this.style.reset();
         this.style.tool = Strokes.Tool.CHALK;
         this.style.color = Sketch.smearColor(lines);
         this.style.width = width * 3.4D;
         this.style.alpha = alpha * 0.3F * (1.0F - erase);
         this.style.flat = true;
         this.style.normalX = 0.0D;
         this.style.normalY = 1.0D;
         this.style.normalZ = 0.0D;
         this.style.light = fx.light;
         Strokes.draw(m, this.style);
      }

   }

   private void nightLight(LandingFx fx, FxFrame f, SketchRingsStyle.State s, float t, float alpha, float erase) {
      float a = alpha * fx.night * 0.14F * (1.0F - erase) * Easing.clamp01(t / 0.15F);
      if(a > 0.003F) {
         RenderState.additive();

         for(int k = 0; k < s.rings; ++k) {
            double r = radius(s, k);

            for(int i = 0; i < 12; ++i) {
               double ang = 6.283185307179586D * (double)i / 12.0D;
               double x = fx.x() + Math.cos(ang) * r;
               double z = fx.z() + Math.sin(ang) * r;
               double gy = Ground.y(fx.ground, x, z);
               if(Ground.lastValid()) {
                  Vfx.sprite(Textures.GLOW, f.rx(x), f.ry(gy + 0.05D), f.rz(z), 0.32D * Math.sqrt(s.scale), 0.0F, alpha(-3549953, a));
               }
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
      SketchRingsStyle.State s = (SketchRingsStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.styleTime();
         float erase = erased(fx);
         String phase = t < 0.15F + 0.06F * (float)(s.rings - 1)?"drawing the rings":(erase <= 0.0F?"boiling":(erase < 1.0F?"erasing":"gone"));
         return String.format(Locale.ROOT, "%s; %s, %d ring(s) (outer %.2f), %d %s, crack %s, boil %d/s, lines #%06X, erased %.2f; fall %.1f (x%.2f)%s; %s, light %.2f, night %.2f", new Object[]{phase, ((Sketch.Tool)this.tool.get()).displayName(), Integer.valueOf(s.rings), Double.valueOf(radius(s, s.rings - 1)), Integer.valueOf(s.doodles), ((SketchRingsStyle.Doodles)this.doodles.get()).displayName(), s.cracked?"on":"off", Integer.valueOf(this.boil.getInt()), Integer.valueOf(this.color.getColorA() & 16777215), Float.valueOf(erase), Float.valueOf(fx.land.fallHeight), Float.valueOf(fx.land.fallScale), s.jump?", a jump":"", fx.lod?"far":"near", Float.valueOf(fx.light), Float.valueOf(fx.night)});
      }
   }

   public static enum Doodles implements Displayable {
      STARS("Stars", "Doodled stars"),
      SPIRALS("Spirals", "Little swirls"),
      HEARTS("Hearts", "Little hearts"),
      NONE("None", "Just the rings");

      private final String displayName;
      private final String description;

      private Doodles(String displayName, String description) {
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
      boolean jump;
      boolean cracked;
      int rings;
      int doodles;
      double scale;
      final double[] start;
      final double[] ph1;
      final double[] ph2;
      final double[] dAngle;
      final double[] dHop;
      final double[] dSpin;
      final float[] dAt;
      final double[] cAngle;
      final double[] cLen;

      private State() {
         this.start = new double[3];
         this.ph1 = new double[3];
         this.ph2 = new double[3];
         this.dAngle = new double[6];
         this.dHop = new double[6];
         this.dSpin = new double[6];
         this.dAt = new float[6];
         this.cAngle = new double[4];
         this.cLen = new double[4];
      }
   }
}
