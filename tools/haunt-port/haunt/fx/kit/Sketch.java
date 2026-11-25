package mix.client.mixui.integration.haunt.fx.kit;

import mix.client.mixui.integration.haunt.core.ConfigManager;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.core.PresetManager;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.stroke.Strokes;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;

public final class Sketch {
   private static final double TWO_PI = 6.283185307179586D;

   public static void followTool(ColorSetting lines, Sketch.Tool before, Sketch.Tool now) {
      FuriahsVisuals mod = FuriahsVisuals.instance;
      if(mod != null) {
         ConfigManager config = mod.getConfig();
         PresetManager presets = mod.getPresets();
         if(config != null && config.isLoading() || presets != null && presets.isApplying()) {
            return;
         }
      }

      if(before != now && lines.getMode() == ColorSetting.Mode.STATIC && (lines.getColorA() & 16777215) == (before.color & 16777215)) {
         lines.setColorA(now.color);
      }

   }

   public static int star(Sketch.Plane p, double u, double v, double r) {
      int per = 6;
      int n = 5 * per + 1;

      for(int k = 0; k < 5; ++k) {
         double a0 = Math.toRadians((double)(90 + 144 * k));
         double a1 = Math.toRadians((double)(90 + 144 * (k + 1)));

         for(int i = 0; i < per; ++i) {
            double t = (double)i / (double)per;
            p.at(k * per + i, u + (Math.cos(a0) + (Math.cos(a1) - Math.cos(a0)) * t) * r, v + (Math.sin(a0) + (Math.sin(a1) - Math.sin(a0)) * t) * r);
         }
      }

      p.at(n - 1, u + Math.cos(Math.toRadians(90.0D)) * r, v + Math.sin(Math.toRadians(90.0D)) * r);
      return n;
   }

   public static int heart(Sketch.Plane p, double u, double v, double r) {
      int n = 40;

      for(int i = 0; i < n; ++i) {
         double a = 6.283185307179586D * (double)i / ((double)n - 1.0D);
         double x = 16.0D * Math.pow(Math.sin(a), 3.0D);
         double y = 13.0D * Math.cos(a) - 5.0D * Math.cos(2.0D * a) - 2.0D * Math.cos(3.0D * a) - Math.cos(4.0D * a);
         p.at(i, u + x / 16.0D * r, v + y / 16.0D * r);
      }

      return n;
   }

   public static int swirl(Sketch.Plane p, double u, double v, double r, double turns, int dir, double start) {
      int n = Math.max(12, (int)Math.round(turns * 22.0D));

      for(int i = 0; i < n; ++i) {
         double t = (double)i / ((double)n - 1.0D);
         double a = start + (double)dir * 6.283185307179586D * turns * t;
         double rr = r * (0.12D + 0.88D * t);
         p.at(i, u + Math.cos(a) * rr, v + Math.sin(a) * rr);
      }

      return n;
   }

   public static int loops(Sketch.Plane p, double u, double v, double w, double r, int count) {
      int n = Math.max(16, count * 18);
      double a = w / (6.283185307179586D * (double)count);
      double b = a * 1.9D;

      for(int i = 0; i < n; ++i) {
         double th = 6.283185307179586D * (double)count * (double)i / ((double)n - 1.0D);
         double x = a * th - b * Math.sin(th) - w * 0.5D;
         double y = -b * Math.cos(th);
         p.at(i, u + x, v + y * r / (2.0D * b));
      }

      return n;
   }

   public static int bangBar(Sketch.Plane p, double u, double v, double h) {
      int per = 8;
      int n = per * 2;
      double w = h * 0.07D;

      for(int i = 0; i < per; ++i) {
         double t = (double)i / ((double)per - 1.0D);
         p.at(i, u - w * (1.0D - t) * 0.5D, v + h * (1.0D - t) + h * 0.22D);
      }

      for(int var15 = 0; var15 < per; ++var15) {
         double t = (double)var15 / ((double)per - 1.0D);
         p.at(per + var15, u + w * t * 0.5D + w * 0.3D * (1.0D - t), v + h * 0.22D + h * t);
      }

      return n;
   }

   public static int bangDot(Sketch.Plane p, double u, double v, double h) {
      int n = 12;
      double r = h * 0.06D;

      for(int i = 0; i < n; ++i) {
         double a = 7.225663103256523D * (double)i / ((double)n - 1.0D);
         p.at(i, u + Math.cos(a) * r, v + Math.sin(a) * r);
      }

      return n;
   }

   public static int smear(Sketch.Plane p, double front, double half, double swing) {
      int m = 11;

      for(int i = 0; i < m; ++i) {
         double v = (1.0D - 2.0D * (double)i / ((double)m - 1.0D)) * half;
         p.at(i, front + ((i & 1) == 0?-swing:swing), v);
      }

      return m;
   }

   public static int smearColor(int lines) {
      return ColorUtil.lerp(lines | -16777216, -855310, 0.65F);
   }

   public static final class Plane {
      public double ox;
      public double oy;
      public double oz;
      public double rx;
      public double ry;
      public double rz;
      public double ux;
      public double uy;
      public double uz;

      public void at(int i, double u, double v) {
         Strokes.X[i] = this.ox + this.rx * u + this.ux * v;
         Strokes.Y[i] = this.oy + this.ry * u + this.uy * v;
         Strokes.Z[i] = this.oz + this.rz * u + this.uz * v;
      }

      public void normal(double[] out) {
         out[0] = this.ry * this.uz - this.rz * this.uy;
         out[1] = this.rz * this.ux - this.rx * this.uz;
         out[2] = this.rx * this.uy - this.ry * this.ux;
      }
   }

   public static enum Tool implements Displayable {
      PENCIL("Pencil", "Graphite on paper", Strokes.Tool.PENCIL, -12961222, 0.032D),
      MARKER("Marker", "A see-through marker", Strokes.Tool.MARKER, -13668371, 0.055D),
      CHALK("Chalk", "Dusty chalk", Strokes.Tool.CHALK, -723728, 0.065D);

      private final String displayName;
      private final String description;
      private final Strokes.Tool stroke;
      private final int color;
      private final double width;

      private Tool(String displayName, String description, Strokes.Tool stroke, int color, double width) {
         this.displayName = displayName;
         this.description = description;
         this.stroke = stroke;
         this.color = color;
         this.width = width;
      }

      public Strokes.Tool stroke() {
         return this.stroke;
      }

      public int color() {
         return this.color;
      }

      public double width() {
         return this.width;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }
}
