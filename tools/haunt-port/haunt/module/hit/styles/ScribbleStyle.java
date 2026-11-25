package mix.client.mixui.integration.haunt.module.hit.styles;

import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Sketch;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.stroke.Strokes;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class ScribbleStyle extends HitStyleBase {
   private static final float DRAWN = 0.18F;
   private static final float ERASE_FROM = 0.58F;
   private static final float GONE = 0.97F;
   private static final int POINTS = 170;
   private static final int NIGHT = -3549953;
   private static final int STAR = 0;
   private static final int SWIRL = 1;
   private static final int HEART = 2;
   public final ColorSetting highlights;
   public final ModeSetting<Sketch.Tool> tool;
   public final ModeSetting<ScribbleStyle.Doodles> doodles;
   public final SliderSetting lineWidth;
   public final SliderSetting wobble;
   public final SliderSetting boil;
   private final Strokes.Style style = new Strokes.Style();
   private final Sketch.Plane plane = new Sketch.Plane();
   private final Sketch.Plane doodle = new Sketch.Plane();
   private Sketch.Tool lastTool;
   private double cx;
   private double cy;
   private double cz;
   private double sc;
   private final double[] normal = new double[3];

   public ScribbleStyle(HitEffectModule module) {
      super(module, HitStyle.SCRIBBLE, 0.5F, new ColorSetting("scribble.color", "Lines", "The scribble\'s lines.", ColorSetting.Mode.STATIC, -12961222, -13668371, 1.0D));
      this.signature(this.color, -12961222);
      this.highlights = (ColorSetting)this.setting((new ColorSetting("scribble.highlights", "Highlights", "The little doodles that fly off.", ColorSetting.Mode.STATIC, -14020, -37986, 1.0D)).secondary());
      this.signature(this.highlights, -14020);
      this.tool = (ModeSetting)this.setting((new ModeSetting("scribble.tool", "Tool", "Pencil, marker or chalk. With Lines Static and still the tool\'s color, switching the tool switches the color too.", Sketch.Tool.class, Sketch.Tool.PENCIL)).style(ModeSetting.Style.CHIPS));
      this.doodles = (ModeSetting)this.setting((new ModeSetting("scribble.doodles", "Doodles", "What flies off the scribble.", ScribbleStyle.Doodles.class, ScribbleStyle.Doodles.MIXED)).style(ModeSetting.Style.CHIPS));
      this.lineWidth = this.look((SliderSetting)this.setting((new SliderSetting("scribble.lineWidth", "Line Width", "How thick the lines are.", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%")));
      this.wobble = (SliderSetting)this.setting((new SliderSetting("scribble.wobble", "Wobble", "How shaky the hand is.", 0.0D, 100.0D, 50.0D, 5.0D)).unit("%"));
      this.boil = (SliderSetting)this.setting((new SliderSetting("scribble.boil", "Boiling", "How many times a second the lines are redrawn with a new wobble.", 0.0D, 12.0D, 10.0D, 1.0D)).unit("/s"));
      this.lastTool = (Sketch.Tool)this.tool.get();
      this.tool.addListener(new Runnable() {
         public void run() {
            Sketch.Tool now = (Sketch.Tool)ScribbleStyle.this.tool.get();
            Sketch.Tool before = ScribbleStyle.this.lastTool;
            ScribbleStyle.this.lastTool = now;
            Sketch.followTool(ScribbleStyle.this.color, before, now);
         }
      });
   }

   public int passes(HitFx fx) {
      boolean night = fx.night > 0.01F && this.tool.get() == Sketch.Tool.PENCIL;
      return night?FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit():FxPass.TRANSLUCENT.bit();
   }

   private void setUp(HitFx fx, FxFrame f) {
      Basis b = fx.basis;
      this.sc = scale(fx) * (fx.crit?1.35D:1.0D);
      this.cx = f.rx(fx.ix()) - b.fx * 0.1D;
      this.cy = f.ry(fx.iy()) - b.fy * 0.1D;
      this.cz = f.rz(fx.iz()) - b.fz * 0.1D;
      double sx = fx.hit.swipeX;
      double sy = fx.hit.swipeY;
      double sz = fx.hit.swipeZ;
      double d = sx * b.fx + sy * b.fy + sz * b.fz;
      double l;
      if((l = Math.sqrt((sx = sx - b.fx * d) * sx + (sy = sy - b.fy * d) * sy + (sz = sz - b.fz * d) * sz)) < 1.0E-6D) {
         sx = b.rx;
         sy = b.ry;
         sz = b.rz;
      } else {
         sx = sx / l;
         sy = sy / l;
         sz = sz / l;
      }

      double qx = b.fy * sz - b.fz * sy;
      double qy = b.fz * sx - b.fx * sz;
      double qz = b.fx * sy - b.fy * sx;
      this.plane.ox = this.cx;
      this.plane.oy = this.cy;
      this.plane.oz = this.cz;
      this.plane.rx = sx;
      this.plane.ry = sy;
      this.plane.rz = sz;
      this.plane.ux = qx;
      this.plane.uy = qy;
      this.plane.uz = qz;
      this.normal[0] = b.fx;
      this.normal[1] = b.fy;
      this.normal[2] = b.fz;
   }

   public void renderPass(HitFx fx, FxFrame f, FxPass pass) {
      float p = fx.progress();
      float alpha = fx.alpha();
      if(alpha > 0.003F && p < 0.97F) {
         this.setUp(fx, f);
         float erase = Easing.clamp01((p - 0.58F) / 0.39000005F);
         if(pass == FxPass.GLOW) {
            this.nightLight(fx, p, alpha, erase);
         } else {
            Sketch.Tool tl = (Sketch.Tool)this.tool.get();
            Camera cam = Vfx.camera();
            int lines = this.color.getColor(0.5F);
            int high = this.highlights.getColor(0.5F);
            double width = tl.width() * (double)this.lineWidth.getFloat() / 100.0D * this.sc * 1.15D;
            int seed = this.boil.getInt() <= 0?0:(int)Math.floor((double)(fx.age * this.boil.getFloat()));
            double amount = (double)this.wobble.getFloat() / 100.0D * 0.03D * this.sc;
            boolean dark = tl == Sketch.Tool.PENCIL;
            int n = this.scribble(fx);
            float drawn = Easing.outCubic(Easing.clamp01(p / 0.18F));
            this.line(fx, cam, n, tl, lines, width, alpha, drawn, erase, seed, amount, 0, dark);
            if(this.doodles.get() != ScribbleStyle.Doodles.NONE) {
               int count = fx.crit?4:3;

               for(int k = 0; k < count; ++k) {
                  this.flyingDoodle(fx, cam, k, count, p, tl, high, width, alpha, erase, seed, amount, dark);
               }
            }

            if(fx.crit) {
               this.bang(fx, f, cam, p, tl, lines, width, alpha, erase, seed, amount, dark);
            }

            if(erase > 0.0F && erase < 1.0F) {
               double len = 0.45D * this.sc;
               int m = Sketch.smear(this.plane, (-1.35D + 2.7D * (double)erase) * len, 0.3D * this.sc, 0.06D * this.sc);
               this.style.reset();
               this.style.tool = Strokes.Tool.CHALK;
               this.style.color = Sketch.smearColor(lines);
               this.style.width = width * 3.2D;
               this.style.alpha = alpha * 0.32F * (1.0F - erase);
               this.style.light = fx.light;
               Strokes.draw(m, this.style);
            }
         }
      }

   }

   private int scribble(HitFx fx) {
      double len = 0.42D * this.sc;
      double wid = 0.2D * this.sc;
      double ph1 = hash(fx.seed, 1) * 6.283185307179586D;
      double ph2 = hash(fx.seed, 2) * 6.283185307179586D;
      double ph3 = hash(fx.seed, 3) * 6.283185307179586D;

      for(int i = 0; i < 170; ++i) {
         double t = (double)i / 169.0D;
         double open = 0.25D + 0.75D * (double)Easing.smoothstep(0.0F, 0.3F, (float)t);
         double u = (0.62D * Math.sin(9.42477796076938D * t + ph1) + 0.38D * Math.sin(43.982297150257104D * t + ph2)) * len * open;
         double v = (0.85D * Math.sin(52.15043804959057D * t + ph3) + 0.15D * Math.sin(13.194689145077131D * t)) * wid * open;
         this.plane.at(i, u, v);
      }

      return 170;
   }

   private void line(HitFx fx, Camera cam, int n, Sketch.Tool tl, int color, double width, float alpha, float progress, float erase, int seed, double amount, int index, boolean dark) {
      if(progress > 0.0F && alpha > 0.003F) {
         Strokes.wobble(n, amount, seed * 31 + index * 7 + 1, this.normal[0], this.normal[1], this.normal[2]);

         for(int i = 0; i < n; ++i) {
            double x = Strokes.X[i];
            double y = Strokes.Y[i];
            double z = Strokes.Z[i];
            float k = fx.keep.cover(x, y, z);
            Strokes.A[i] = dark?k * NearCamera.dark(cam, x, y, z, 1.0F):k;
         }

         this.style.reset();
         this.style.tool = tl.stroke();
         this.style.color = color;
         this.style.width = width;
         this.style.alpha = alpha;
         this.style.progress = progress;
         this.style.erase = erase;
         this.style.eraseX = this.plane.rx;
         this.style.eraseY = this.plane.ry;
         this.style.eraseZ = this.plane.rz;
         this.style.tile = 0.5D;
         this.style.light = fx.light;
         this.style.pointAlpha = true;
         Strokes.draw(n, this.style);
      }

   }

   private void flyingDoodle(HitFx fx, Camera cam, int k, int count, float p, Sketch.Tool tl, int color, double width, float alpha, float erase, int seed, double amount, boolean dark) {
      int id = fx.seed + k * 61;
      float start = 0.18F * (0.35F + 0.25F * (float)k);
      if(p >= start) {
         Basis b = fx.basis;
         double t = (double)(p - start) * 0.5D;
         double a = Math.toRadians(-40.0D + 260.0D * ((double)k + 0.5D + 0.3D * (hash(id, 1) - 0.5D)) / (double)count);
         double dr = Math.cos(a);
         double du = Math.sin(a);
         double travel = (1.0D - Math.exp(-3.0D * t)) / 3.0D;
         double out = 0.2D * this.sc + 2.6D * this.sc * travel;
         double along = 0.8D * this.sc * travel;
         double drop = 0.6D * t * t * this.sc;
         double x = this.cx + (b.rx * dr + b.ux * du) * out + b.fx * along;
         double y = this.cy + (b.ry * dr + b.uy * du) * out + b.fy * along - drop;
         double z = this.cz + (b.rz * dr + b.uz * du) * out + b.fz * along;
         double spin = (hash(id, 2) - 0.5D) * 3.0D * t;
         double cs = Math.cos(spin);
         double sn = Math.sin(spin);
         this.doodle.ox = x;
         this.doodle.oy = y;
         this.doodle.oz = z;
         this.doodle.rx = b.rx * cs + b.ux * sn;
         this.doodle.ry = b.ry * cs + b.uy * sn;
         this.doodle.rz = b.rz * cs + b.uz * sn;
         this.doodle.ux = b.ux * cs - b.rx * sn;
         this.doodle.uy = b.uy * cs - b.ry * sn;
         this.doodle.uz = b.uz * cs - b.rz * sn;
         double r = 0.075D * this.sc * (0.9D + 0.3D * hash(id, 3));
         int n;
         switch(this.kind(k)) {
         case 1:
            n = Sketch.swirl(this.doodle, 0.0D, 0.0D, r, 1.6D, hash(id, 4) < 0.5D?1:-1, hash(id, 5) * 6.283185307179586D);
            break;
         case 2:
            n = Sketch.heart(this.doodle, 0.0D, 0.0D, r);
            break;
         default:
            n = Sketch.star(this.doodle, 0.0D, 0.0D, r);
         }

         float drawn = Easing.outCubic(Easing.clamp01((p - start) / 0.12F));
         this.line(fx, cam, n, tl, color, width * 0.85D, alpha * (1.0F - Easing.smoothstep(0.0F, 0.7F, erase)), drawn, 0.0F, seed, amount * 0.35D, 2 + k, dark);
      }

   }

   private int kind(int k) {
      switch((ScribbleStyle.Doodles)this.doodles.get()) {
      case STARS:
         return 0;
      case SWIRLS:
         return 1;
      case HEARTS:
         return 2;
      default:
         return k % 3 == 0?0:(k % 3 == 1?1:2);
      }
   }

   private void bang(HitFx fx, FxFrame f, Camera cam, float p, Sketch.Tool tl, int color, double width, float alpha, float erase, int seed, double amount, boolean dark) {
      float start = 0.10800001F;
      if(p >= start) {
         Basis b = fx.basis;
         double s0 = scale(fx);
         double h = 0.32D * s0 * (double)Easing.outBack(Easing.clamp01((p - start) / 0.1F), 2.0F);
         if(h > 0.001D) {
            double top = fx.anchor.feetY() + (double)fx.hit.height - 0.12D * s0;
            double side = 0.45D * s0;
            this.doodle.ox = f.rx(fx.cx()) + b.rx * side - b.fx * 0.1D;
            this.doodle.oy = f.ry(top) + b.ry * side - b.fy * 0.1D;
            this.doodle.oz = f.rz(fx.cz()) + b.rz * side - b.fz * 0.1D;
            double lean = -0.15D;
            double cs = Math.cos(lean);
            double sn = Math.sin(lean);
            this.doodle.rx = b.rx * cs + b.ux * sn;
            this.doodle.ry = b.ry * cs + b.uy * sn;
            this.doodle.rz = b.rz * cs + b.uz * sn;
            this.doodle.ux = b.ux * cs - b.rx * sn;
            this.doodle.uy = b.uy * cs - b.ry * sn;
            this.doodle.uz = b.uz * cs - b.rz * sn;
            float a = alpha * (1.0F - Easing.smoothstep(0.0F, 0.7F, erase));
            float drawn = Easing.outCubic(Easing.clamp01((p - start) / 0.08F));
            int n = Sketch.bangBar(this.doodle, 0.0D, 0.0D, h);
            this.line(fx, cam, n, tl, color, width * 1.4D, a, drawn, 0.0F, seed, amount * 0.3D, 8, dark);
            n = Sketch.bangDot(this.doodle, 0.0D, 0.0D, h);
            this.line(fx, cam, n, tl, color, width * 1.6D, a, Easing.outCubic(Easing.clamp01((p - start - 0.05F) / 0.05F)), 0.0F, seed, amount * 0.2D, 9, dark);
         }
      }

   }

   private void nightLight(HitFx fx, float p, float alpha, float erase) {
      float a = alpha * fx.night * 0.12F * (1.0F - erase) * Easing.clamp01(p / 0.18F);
      if(a > 0.003F) {
         int n = this.scribble(fx);

         for(int i = 0; i < n; i += 14) {
            Vfx.sprite(Textures.GLOW, Strokes.X[i], Strokes.Y[i], Strokes.Z[i], 0.22D * this.sc, 0.0F, alpha(-3549953, a));
         }
      }

   }

   public String stateForTest(HitFx fx) {
      float p = fx.progress();
      float erase = Easing.clamp01((p - 0.58F) / 0.39000005F);
      return String.format(Locale.ROOT, "%s, lines #%06X, drawn %.2f, erased %.2f, doodles %s%s; light %.2f, night %.2f", new Object[]{((Sketch.Tool)this.tool.get()).displayName(), Integer.valueOf(this.color.getColorA() & 16777215), Float.valueOf(Easing.clamp01(p / 0.18F)), Float.valueOf(erase), ((ScribbleStyle.Doodles)this.doodles.get()).displayName(), fx.crit?", crit !":"", Float.valueOf(fx.light), Float.valueOf(fx.night)});
   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8 & 16777215) / 1.6777216E7D;
   }

   public static enum Doodles implements Displayable {
      MIXED("Mixed", "Stars, swirls and hearts"),
      STARS("Stars", "Little stars"),
      SWIRLS("Swirls", "Little swirls"),
      HEARTS("Hearts", "Little hearts"),
      NONE("None", "Just the scribble");

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
}
