package mix.client.mixui.integration.haunt.module.hit.styles;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Ink;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.stroke.Strokes;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class InkSlashStyle extends HitStyleBase {
   private static final int POINTS = 40;
   private static final int NIGHT = -3549953;
   private static final float PAINT = 0.17777778F;
   private static final float HOLD = 0.44444448F;
   private static final float SECOND = 0.11111112F;
   private static final float SEAL = 0.2888889F;
   private static final float SQUASH = 0.15555556F;
   private static final double CROSS = Math.toRadians(70.0D);
   private static final int MAX_DROPS = 24;
   private static final int MAX_SPLATS = 4;
   private static final int POOL = 8;
   private static final double GRAVITY = 12.0D;
   public final ColorSetting sealColor;
   public final ModeSetting<InkSlashStyle.Shape> stroke;
   public final SliderSetting brushWidth;
   public final SliderSetting dryness;
   public final SliderSetting splatter;
   public final BooleanSetting seal;
   public final ModeSetting<Ink.SealDesign> sealDesign;
   private final Strokes.Style style = new Strokes.Style();
   private final Basis plane = new Basis();
   private final double[] end = new double[6];
   private final double[] point = new double[3];
   private final ArrayDeque<InkSlashStyle.State> pool = new ArrayDeque();

   public InkSlashStyle(HitEffectModule module) {
      super(module, HitStyle.INK_SLASH, 0.45F, new ColorSetting("inkslash.color", "Ink", "The brush\'s ink.", ColorSetting.Mode.STATIC, -15461356, -14015424, 1.0D));
      this.signature(this.color, -15461356);
      this.sealColor = (ColorSetting)this.setting((new ColorSetting("inkslash.seal", "Seal", "The seal stamp\'s red (critical hits).", ColorSetting.Mode.STATIC, -3660241, -2075078, 1.0D)).secondary());
      this.signature(this.sealColor, -3660241);
      this.stroke = (ModeSetting)this.setting((new ModeSetting("inkslash.stroke", "Stroke", "The stroke\'s shape.", InkSlashStyle.Shape.class, InkSlashStyle.Shape.STRAIGHT)).style(ModeSetting.Style.CHIPS));
      this.brushWidth = this.look((SliderSetting)this.setting((new SliderSetting("inkslash.width", "Brush Width", "How wide the brush is at its heaviest.", 0.05D, 0.4D, 0.16D, 0.01D)).unit(" blocks")));
      this.dryness = (SliderSetting)this.setting((new SliderSetting("inkslash.dryness", "Dryness", "How soon the brush runs dry into bristle streaks.", 0.0D, 100.0D, 50.0D, 5.0D)).unit("%"));
      this.splatter = this.look((SliderSetting)this.setting((new SliderSetting("inkslash.splatter", "Splatter", "Ink droplets flung off the stroke\'s end, and splatters.", 0.0D, 100.0D, 50.0D, 5.0D)).unit("%")));
      this.seal = (BooleanSetting)this.setting(new BooleanSetting("inkslash.sealOn", "Seal on Crits", "A red seal stamps where a critical hit\'s strokes cross.", true));
      this.sealDesign = (ModeSetting)this.setting((new ModeSetting("inkslash.sealDesign", "Seal Design", "The seal\'s design.", Ink.SealDesign.class, Ink.SealDesign.SUN)).style(ModeSetting.Style.CHIPS).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return InkSlashStyle.this.seal.get();
         }
      }));
   }

   public void finish(HitFx fx) {
      if(fx.data instanceof InkSlashStyle.State && this.pool.size() < 8) {
         this.pool.push((InkSlashStyle.State)fx.data);
      }

      fx.data = null;
   }

   public void start(HitFx fx) {
      InkSlashStyle.State s = (InkSlashStyle.State)this.pool.poll();
      if(s == null) {
         s = new InkSlashStyle.State();
      }

      Arrays.fill(s.thrown, false);
      float amount = this.splatter.getFloat() / 100.0F;
      s.drops = fx.lod?0:Math.min(24, Math.round(amount * 12.0F * fx.density * (fx.crit?1.5F:1.0F)));
      s.splats = fx.lod?0:Math.min(4, Math.round(amount * 4.0F));

      for(int i = 0; i < s.drops; ++i) {
         boolean k = fx.crit && (i & 1) == 1;
         s.at[i] = 0.17777778F * (0.85F + 0.25F * hash(fx.seed + i, 3)) + (k?1.0F:0.0F) * 0.11111112F;
         s.size[i] = 0.018F + 0.032F * hash(fx.seed + i, 5) * hash(fx.seed + i, 7) * 1.6F;
      }

      fx.data = s;
   }

   public int passes(HitFx fx) {
      return fx.night > 0.01F?FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit():FxPass.TRANSLUCENT.bit();
   }

   private void frame(HitFx fx, int k) {
      Basis b = fx.basis;
      double ux = fx.hit.swipeX;
      double uy = fx.hit.swipeY;
      double uz = fx.hit.swipeZ;
      if(k == 1) {
         double c = Math.cos(CROSS);
         double s = Math.sin(CROSS);
         double vx = b.fy * uz - b.fz * uy;
         double vy = b.fz * ux - b.fx * uz;
         double vz = b.fx * uy - b.fy * ux;
         ux = ux * c + vx * s;
         uy = uy * c + vy * s;
         uz = uz * c + vz * s;
      }

      this.plane.rx = ux;
      this.plane.ry = uy;
      this.plane.rz = uz;
      double cx = uy * b.fz - uz * b.fy;
      double cy = uz * b.fx - ux * b.fz;
      double cz = ux * b.fy - uy * b.fx;
      double cl = Math.sqrt(cx * cx + cy * cy + cz * cz);
      if(cl < 1.0E-9D) {
         cx = b.ux;
         cy = b.uy;
         cz = b.uz;
         cl = 1.0D;
      }

      this.plane.ux = cx / cl;
      this.plane.uy = cy / cl;
      this.plane.uz = cz / cl;
      this.plane.fx = b.fx;
      this.plane.fy = b.fy;
      this.plane.fz = b.fz;
   }

   private static double length(HitFx fx, int k) {
      return 1.7D * scale(fx) * (k == 1?0.85D:1.0D);
   }

   private void path(HitFx fx, int k, double s) {
      double len = length(fx, k);
      double ox = fx.ix() - fx.basis.fx * 0.12D;
      double oy = fx.iy() - fx.basis.fy * 0.12D;
      double oz = fx.iz() - fx.basis.fz * 0.12D;
      double across;
      double along;
      switch((InkSlashStyle.Shape)this.stroke.get()) {
      case CURVED:
         along = (s - 0.5D) * len;
         across = (0.16D * Math.sin(3.141592653589793D * s) - 0.06D) * len;
         break;
      case HOOKED:
         double straight = 0.74D * len;
         double rho = (len - straight) / 2.0943951023931953D;
         double at = s * len;
         double start = -0.6D * straight;
         if(at <= straight) {
            along = start + at;
            across = 0.02D * len * Math.sin(3.141592653589793D * at / straight);
         } else {
            double th = (at - straight) / rho;
            along = start + straight + rho * Math.sin(th);
            across = -rho + rho * Math.cos(th);
         }
         break;
      default:
         along = (s - 0.5D) * len;
         across = 0.035D * len * Math.sin(3.141592653589793D * s);
      }

      this.point[0] = ox + this.plane.rx * along + this.plane.ux * across;
      this.point[1] = oy + this.plane.ry * along + this.plane.uy * across;
      this.point[2] = oz + this.plane.rz * along + this.plane.uz * across;
   }

   private static float painted(float p, int k) {
      return Easing.inOutCubic(Easing.clamp01((p - (float)k * 0.11111112F) / 0.17777778F));
   }

   private static float diffused(float p) {
      return Easing.clamp01((p - 0.44444448F) / 0.5555555F);
   }

   public void renderPass(HitFx fx, FxFrame f, FxPass pass) {
      InkSlashStyle.State s = (InkSlashStyle.State)fx.data;
      float p = fx.progress();
      float alpha = fx.alpha();
      if(s != null && alpha > 0.003F) {
         int strokes = fx.crit?2:1;
         if(pass == FxPass.GLOW) {
            this.nightLight(fx, f, p, alpha, strokes);
         } else {
            Camera cam = Vfx.camera();
            int ink = ColorUtil.shade(this.color.getColor(0.5F), fx.light);
            float d = diffused(p);

            for(int k = 0; k < strokes; ++k) {
               float painted = painted(p, k);
               if(painted > 0.0F) {
                  this.frame(fx, k);
                  this.brush(fx, f, cam, k, alpha, painted, d);
                  this.splats(fx, f, cam, s, k, alpha, painted, d, ink);
               }
            }

            if(fx.crit && this.seal.get()) {
               this.stamp(fx, f, p, alpha, d);
            }

            this.drops(fx, f, cam, s, p, alpha, d, ink);
         }
      }

   }

   private void brush(HitFx fx, FxFrame f, Camera cam, int k, float alpha, float painted, float d) {
      for(int i = 0; i < 40; ++i) {
         this.path(fx, k, (double)i / 39.0D);
         Strokes.X[i] = f.rx(this.point[0]);
         Strokes.Y[i] = f.ry(this.point[1]);
         Strokes.Z[i] = f.rz(this.point[2]);
      }

      Strokes.wobble(40, 0.012D * length(fx, k), fx.seed + k * 31, this.plane.fx, this.plane.fy, this.plane.fz);

      for(int var15 = 0; var15 < 40; ++var15) {
         double x = Strokes.X[var15];
         double y = Strokes.Y[var15];
         double z = Strokes.Z[var15];
         Strokes.A[var15] = fx.keep.cover(x, y, z) * NearCamera.dark(cam, x, y, z, 1.0F);
      }

      this.style.reset();
      this.style.tool = Strokes.Tool.BRUSH;
      this.style.color = this.color.getColor(0.5F);
      this.style.width = (double)this.brushWidth.getFloat() * scale(fx) * (fx.crit?1.1D:1.0D) * (k == 1?0.85D:1.0D);
      this.style.alpha = alpha;
      this.style.progress = painted;
      this.style.heavyStart = 0.5F;
      this.style.dryness = this.dryness.getFloat() / 100.0F;
      this.style.thinEnd = 0.45F + 0.3F * this.style.dryness;
      this.style.diffuse = d;
      this.style.light = fx.light;
      this.style.flat = true;
      this.style.normalX = this.plane.fx;
      this.style.normalY = this.plane.fy;
      this.style.normalZ = this.plane.fz;
      this.style.pointAlpha = true;
      Strokes.draw(40, this.style);
   }

   private void splats(HitFx fx, FxFrame f, Camera cam, InkSlashStyle.State s, int k, float alpha, float painted, float d, int ink) {
      if(s.splats != 0 && k != 1) {
         int tex = Ink.splats();
         double w = (double)this.brushWidth.getFloat() * scale(fx);
         float fade = 1.0F - Easing.smoothstep(0.0F, 0.8F, d);

         for(int i = 0; i < s.splats; ++i) {
            double at = i == 0?0.03D:0.12D + 0.5D * (double)hash(fx.seed + i, 9);
            float show = Easing.smoothstep(0.0F, 0.08F, (float)((double)painted - at));
            if(show > 0.0F) {
               this.path(fx, k, at);
               double side = (double)((double)hash(fx.seed + i, 11) < 0.5D?-1:1) * w * (0.7D + 0.8D * (double)hash(fx.seed + i, 13));
               double x = f.rx(this.point[0] + this.plane.ux * side);
               double y = f.ry(this.point[1] + this.plane.uy * side);
               double z = f.rz(this.point[2] + this.plane.uz * side);
               double size = scale(fx) * (0.06D + 0.07D * (double)hash(fx.seed + i, 15)) * (0.6D + 0.4D * (double)show);
               float a = alpha * fade * 0.9F * show * fx.keep.cover(x, y, z);
               a = NearCamera.dark(cam, x, y, z, a);
               Ink.quad(tex, i % 4, 4, x, y, z, this.plane.rx, this.plane.ry, this.plane.rz, this.plane.ux, this.plane.uy, this.plane.uz, size, size, (float)((double)hash(fx.seed + i, 17) * 3.141592653589793D * 2.0D), alpha(ink, a));
            }
         }
      }

   }

   private void stamp(HitFx fx, FxFrame f, float p, float alpha, float d) {
      float t = (p - 0.2888889F) / 0.15555556F;
      if(t > 0.0F) {
         this.frame(fx, 0);
         float e = Easing.outBack(Easing.clamp01(t), 2.0F);
         double sx = 1.0D + 0.25D * (1.0D - (double)e);
         double sy = 1.0D - 0.2D * (1.0D - (double)e);
         double size = 0.3D * scale(fx);
         double x = f.rx(fx.ix() - fx.basis.fx * 0.14D);
         double y = f.ry(fx.iy() - fx.basis.fy * 0.14D);
         double z = f.rz(fx.iz() - fx.basis.fz * 0.14D);
         float a = alpha * Easing.clamp01(t * 3.0F) * (1.0F - Easing.smoothstep(0.0F, 0.8F, d));
         int col = alpha(ColorUtil.shade(this.sealColor.getColor(0.5F), fx.light), a);
         Basis b = fx.basis;
         Ink.quad(Ink.seals(), ((Ink.SealDesign)this.sealDesign.get()).ordinal(), 4, x, y, z, b.rx, b.ry, b.rz, b.ux, b.uy, b.uz, size * sx, size * sy, 0.06F, col);
      }

   }

   private void drops(HitFx fx, FxFrame f, Camera cam, InkSlashStyle.State s, float p, float alpha, float d, int ink) {
      if(s.drops != 0) {
         int tex = Ink.drops();
         float fade = 1.0F - Easing.smoothstep(0.1F, 0.9F, d);

         for(int i = 0; i < s.drops; ++i) {
            if(p >= s.at[i]) {
               if(!s.thrown[i]) {
                  this.throwDrop(fx, s, i);
               }

               double t = (double)((p - s.at[i]) * fx.life);
               double wx = s.x[i] + s.vx[i] * t;
               double wy = s.y[i] + s.vy[i] * t - 6.0D * t * t;
               double wz = s.z[i] + s.vz[i] * t;
               double x = f.rx(wx);
               double y = f.ry(wy);
               double z = f.rz(wz);
               double vx = s.vx[i];
               double vy = s.vy[i] - 12.0D * t;
               double vz = s.vz[i];
               double sx = vx * cam.rightX + vy * cam.rightY + vz * cam.rightZ;
               double sy = vx * cam.upX + vy * cam.upY + vz * cam.upZ;
               double speed = Math.sqrt(sx * sx + sy * sy);
               double r = (double)s.size[i] * scale(fx);
               float a = alpha * fade * fx.keep.cover(x, y, z);
               a = NearCamera.dark(cam, x, y, z, a);
               Vfx.sprite(tex, x, y, z, r * (1.0D + Math.min(1.6D, speed * 0.25D)), r * 0.62D, (float)Math.atan2(sy, sx), 0.5F, 0.0F, 1.0F, 1.0F, alpha(ink, a));
            }
         }
      }

   }

   private void throwDrop(HitFx fx, InkSlashStyle.State s, int i) {
      s.thrown[i] = true;
      int k = fx.crit && (i & 1) == 1?1:0;
      this.frame(fx, k);
      this.path(fx, k, 0.97D);
      double ex = this.point[0];
      double ey = this.point[1];
      double ez = this.point[2];
      this.path(fx, k, 1.0D);
      double tx = this.point[0] - ex;
      double ty = this.point[1] - ey;
      double tz = this.point[2] - ez;
      double tl = Math.sqrt(tx * tx + ty * ty + tz * tz) + 1.0E-9D;
      tx = tx / tl;
      ty = ty / tl;
      tz = tz / tl;
      double speed = 2.0D + 2.2D * (double)hash(fx.seed + i, 19);
      double spread = ((double)hash(fx.seed + i, 23) - 0.5D) * 1.6D;
      double away = 1.0D + 1.2D * (double)hash(fx.seed + i, 29);
      Basis b = fx.basis;
      s.x[i] = this.point[0];
      s.y[i] = this.point[1];
      s.z[i] = this.point[2];
      s.vx[i] = tx * speed + this.plane.ux * spread + b.fx * away;
      s.vy[i] = ty * speed + this.plane.uy * spread + b.fy * away + 0.6D;
      s.vz[i] = tz * speed + this.plane.uz * spread + b.fz * away;
   }

   private void nightLight(HitFx fx, FxFrame f, float p, float alpha, int strokes) {
      float a = alpha * fx.night * 0.12F * (1.0F - diffused(p));
      if(a > 0.003F) {
         float size = (float)((double)this.brushWidth.getFloat() * scale(fx) * 3.2D);

         for(int k = 0; k < strokes; ++k) {
            float painted = painted(p, k);
            if(painted > 0.0F) {
               this.frame(fx, k);
               int n = 14;

               for(int i = 0; i < n; ++i) {
                  this.path(fx, k, (double)(painted * (float)i) / ((double)n - 1.0D));
                  Vfx.sprite(Textures.GLOW, f.rx(this.point[0]), f.ry(this.point[1]), f.rz(this.point[2]), (double)size, 0.0F, alpha(-3549953, a));
               }
            }
         }
      }

   }

   public String stateForTest(HitFx fx) {
      InkSlashStyle.State s = (InkSlashStyle.State)fx.data;
      float p = fx.progress();
      int flying = 0;
      if(s != null) {
         for(int i = 0; i < s.drops; ++i) {
            flying += p >= s.at[i]?1:0;
         }
      }

      return String.format(Locale.ROOT, "%s, painted %.2f%s, diffused %.2f, droplets %d of %d, splats %d, light %.2f, night %.2f", new Object[]{((InkSlashStyle.Shape)this.stroke.get()).displayName(), Float.valueOf(painted(p, 0)), fx.crit?String.format(Locale.ROOT, " and %.2f", new Object[]{Float.valueOf(painted(p, 1))}):"", Float.valueOf(diffused(p)), Integer.valueOf(flying), Integer.valueOf(s == null?0:s.drops), Integer.valueOf(s == null?0:s.splats), Float.valueOf(fx.light), Float.valueOf(fx.night)});
   }

   private static float hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (float)(h >>> 8 & 16777215) / 1.6777216E7F;
   }

   public static enum Shape implements Displayable {
      STRAIGHT("Straight", "One straight cut"),
      CURVED("Curved", "A sweeping arc"),
      HOOKED("Hooked", "A cut that hooks back at its end");

      private final String displayName;
      private final String description;

      private Shape(String displayName, String description) {
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
      int drops;
      final float[] at;
      final float[] size;
      final double[] x;
      final double[] y;
      final double[] z;
      final double[] vx;
      final double[] vy;
      final double[] vz;
      final boolean[] thrown;
      int splats;

      private State() {
         this.at = new float[24];
         this.size = new float[24];
         this.x = new double[24];
         this.y = new double[24];
         this.z = new double[24];
         this.vx = new double[24];
         this.vy = new double[24];
         this.vz = new double[24];
         this.thrown = new boolean[24];
      }
   }
}
