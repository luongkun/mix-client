package mix.client.mixui.integration.haunt.haunt.fx;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Ellipse2D.Float;
import java.awt.image.BufferedImage;
import java.util.Random;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.render.Textures;

public final class HauntTextures {
   public static int SKULL_HEAD = -1;
   public static int SKULL_JAW = -1;
   public static int SKULL_FLAT = -1;
   public static int BRUSH = -1;
   public static int ZOMBIE = -1;
   public static int SOIL = -1;
   public static int SPLASH = -1;
   public static int CRACKS = -1;
   public static int EYE = -1;
   private static volatile int[][] pixels;
   private static volatile boolean uploaded;
   private static volatile boolean started;
   private static final int[][] SIZES = new int[][]{{256, 256}, {256, 128}, {256, 256}, {256, 64}, {64, 64}, {64, 64}, {256, 256}, {256, 256}, {128, 64}};

   public static synchronized void warmUp() {
      if(!started) {
         started = true;
         Thread t = new Thread(new Runnable() {
            public void run() {
               try {
                  HauntTextures.pixels = HauntTextures.draw();
               } catch (Throwable var2) {
                  FuriahsVisuals.LOG.warn("HAUN\'T effect textures could not be drawn", var2);
               }

            }
         }, "HAUN\'T VISUALS textures");
         t.setDaemon(true);
         t.start();
      }

   }

   public static boolean ensure() {
      if(uploaded) {
         return true;
      } else {
         warmUp();
         int[][] px = pixels;
         if(px == null) {
            return false;
         } else {
            try {
               SKULL_HEAD = Textures.uploadImage(px[0], SIZES[0][0], SIZES[0][1], true, false);
               SKULL_JAW = Textures.uploadImage(px[1], SIZES[1][0], SIZES[1][1], true, false);
               SKULL_FLAT = Textures.uploadImage(px[2], SIZES[2][0], SIZES[2][1], true, false);
               BRUSH = Textures.uploadImage(px[3], SIZES[3][0], SIZES[3][1], true, false);
               ZOMBIE = Textures.uploadImage(px[4], SIZES[4][0], SIZES[4][1], true, true);
               SOIL = Textures.uploadImage(px[5], SIZES[5][0], SIZES[5][1], true, true);
               SPLASH = Textures.uploadImage(px[6], SIZES[6][0], SIZES[6][1], true, false);
               CRACKS = Textures.uploadImage(px[7], SIZES[7][0], SIZES[7][1], true, false);
               EYE = Textures.uploadImage(px[8], SIZES[8][0], SIZES[8][1], true, false);
            } catch (Throwable var2) {
               FuriahsVisuals.LOG.warn("HAUN\'T effect textures could not be uploaded", var2);
            }

            pixels = null;
            uploaded = true;
            return true;
         }
      }
   }

   static int[][] draw() {
      return new int[][]{skullHead(), skullJaw(), skullFlat(), brush(), zombie(), soil(), splash(), cracks(), eye()};
   }

   private static Graphics2D g(BufferedImage img) {
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      return g;
   }

   private static int[] argb(BufferedImage img) {
      return img.getRGB(0, 0, img.getWidth(), img.getHeight(), (int[])null, 0, img.getWidth());
   }

   private static Area cranium(float s) {
      Area a = new Area(new Float(0.16F * s, 0.06F * s, 0.68F * s, 0.62F * s));
      a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(0.27F * s, 0.5F * s, 0.46F * s, 0.26F * s, 0.16F * s, 0.16F * s)));
      Float cheekL = new Float(0.2F * s, 0.44F * s, 0.2F * s, 0.2F * s);
      Float cheekR = new Float(0.6F * s, 0.44F * s, 0.2F * s, 0.2F * s);
      a.add(new Area(cheekL));
      a.add(new Area(cheekR));
      return a;
   }

   private static int[] skullHead() {
      int s = 256;
      BufferedImage img = new BufferedImage(s, s, 2);
      Graphics2D g = g(img);
      Area head = cranium((float)s);
      g.setPaint(new RadialGradientPaint(0.42F * (float)s, 0.3F * (float)s, 0.62F * (float)s, new float[]{0.0F, 0.55F, 1.0F}, new Color[]{new Color(255, 120, 104), new Color(221, 38, 52), new Color(118, 8, 22)}));
      g.fill(head);
      Area shine = new Area(new Float(0.24F * (float)s, 0.1F * (float)s, 0.38F * (float)s, 0.24F * (float)s));
      shine.intersect(head);
      g.setColor(new Color(255, 196, 180, 210));
      g.fill(shine);
      g.setColor(new Color(255, 245, 240, 235));
      g.fill(new Float(0.32F * (float)s, 0.14F * (float)s, 0.12F * (float)s, 0.06F * (float)s));
      g.setColor(new Color(150, 10, 26, 120));
      g.fill(new Float(0.22F * (float)s, 0.27F * (float)s, 0.56F * (float)s, 0.14F * (float)s));
      g.setColor(new Color(30, 2, 6));
      g.fill(socket(0.25F * (float)s, 0.31F * (float)s, 0.215F * (float)s, 0.2F * (float)s, false));
      g.fill(socket(0.535F * (float)s, 0.31F * (float)s, 0.215F * (float)s, 0.2F * (float)s, true));
      g.setPaint(new RadialGradientPaint(0.37F * (float)s, 0.425F * (float)s, 0.05F * (float)s, new float[]{0.0F, 0.4F, 1.0F}, new Color[]{new Color(255, 236, 200), new Color(255, 70, 50), new Color(255, 40, 40, 0)}));
      g.fill(new Float(0.32F * (float)s, 0.375F * (float)s, 0.1F * (float)s, 0.1F * (float)s));
      g.setPaint(new RadialGradientPaint(0.63F * (float)s, 0.425F * (float)s, 0.05F * (float)s, new float[]{0.0F, 0.4F, 1.0F}, new Color[]{new Color(255, 236, 200), new Color(255, 70, 50), new Color(255, 40, 40, 0)}));
      g.fill(new Float(0.58F * (float)s, 0.375F * (float)s, 0.1F * (float)s, 0.1F * (float)s));
      g.setColor(new Color(30, 2, 6));
      GeneralPath nose = new GeneralPath();
      nose.moveTo(0.5F * (float)s, 0.5F * (float)s);
      nose.lineTo(0.455F * (float)s, 0.585F * (float)s);
      nose.quadTo(0.5F * (float)s, 0.605F * (float)s, 0.545F * (float)s, 0.585F * (float)s);
      nose.closePath();
      g.fill(nose);
      g.setColor(new Color(255, 236, 228));

      for(int i = 0; i < 6; ++i) {
         float x = (0.31F + (float)i * 0.064F) * (float)s;
         g.fill(new java.awt.geom.RoundRectangle2D.Float(x, 0.655F * (float)s, 0.056F * (float)s, 0.1F * (float)s, 0.025F * (float)s, 0.025F * (float)s));
      }

      g.setColor(new Color(90, 6, 16));
      g.setStroke(new BasicStroke(0.012F * (float)s));
      g.draw(new java.awt.geom.Line2D.Float(0.31F * (float)s, 0.655F * (float)s, 0.69F * (float)s, 0.655F * (float)s));
      GeneralPath crack = new GeneralPath();
      crack.moveTo(0.62F * (float)s, 0.09F * (float)s);
      crack.lineTo(0.6F * (float)s, 0.16F * (float)s);
      crack.lineTo(0.645F * (float)s, 0.2F * (float)s);
      crack.lineTo(0.615F * (float)s, 0.27F * (float)s);
      g.setStroke(new BasicStroke(0.01F * (float)s, 1, 1));
      g.setColor(new Color(110, 10, 20, 200));
      g.draw(crack);
      g.setStroke(new BasicStroke(0.018F * (float)s));
      g.setColor(new Color(70, 2, 10, 230));
      g.draw(head);
      g.dispose();
      return argb(img);
   }

   private static Shape socket(float x, float y, float w, float h, boolean mirror) {
      GeneralPath p = new GeneralPath();
      float[][] pts = new float[][]{{0.0F, 0.18F}, {0.55F, 0.0F}, {1.0F, 0.42F}, {0.92F, 0.86F}, {0.48F, 1.02F}, {0.06F, 0.72F}};

      for(int i = 0; i < pts.length; ++i) {
         float px = mirror?x + w * (1.0F - pts[i][0]):x + w * pts[i][0];
         float py = y + h * pts[i][1];
         if(i == 0) {
            p.moveTo(px, py);
         } else {
            float[] prev = pts[i - 1];
            float cx = mirror?x + w * (1.0F - (prev[0] + pts[i][0]) / 2.0F):x + w * (prev[0] + pts[i][0]) / 2.0F;
            float cy = y + h * ((prev[1] + pts[i][1]) / 2.0F) + (i % 2 == 0?h * 0.06F:-h * 0.04F);
            p.quadTo(cx, cy, px, py);
         }
      }

      p.closePath();
      return p;
   }

   private static int[] skullJaw() {
      int w = 256;
      int h = 128;
      BufferedImage img = new BufferedImage(w, h, 2);
      Graphics2D g = g(img);
      GeneralPath jaw = new GeneralPath();
      jaw.moveTo(0.27F * (float)w, 0.02F * (float)h);
      jaw.lineTo(0.73F * (float)w, 0.02F * (float)h);
      jaw.quadTo(0.76F * (float)w, 0.5F * (float)h, 0.64F * (float)w, 0.78F * (float)h);
      jaw.quadTo(0.5F * (float)w, 0.92F * (float)h, 0.36F * (float)w, 0.78F * (float)h);
      jaw.quadTo(0.24F * (float)w, 0.5F * (float)h, 0.27F * (float)w, 0.02F * (float)h);
      jaw.closePath();
      g.setPaint(new GradientPaint(0.0F, 0.0F, new Color(214, 34, 48), 0.0F, (float)h, new Color(110, 6, 20)));
      g.fill(jaw);
      g.setColor(new Color(255, 236, 228));

      for(int i = 0; i < 6; ++i) {
         float x = (0.31F + (float)i * 0.064F) * (float)w;
         g.fill(new java.awt.geom.RoundRectangle2D.Float(x, 0.03F * (float)h, 0.056F * (float)w, 0.2F * (float)h, 0.025F * (float)w, 0.025F * (float)w));
      }

      g.setColor(new Color(255, 170, 150, 150));
      g.fill(new Float(0.34F * (float)w, 0.38F * (float)h, 0.14F * (float)w, 0.12F * (float)h));
      g.setStroke(new BasicStroke(0.018F * (float)w));
      g.setColor(new Color(70, 2, 10, 230));
      g.draw(jaw);
      g.dispose();
      return argb(img);
   }

   private static int[] skullFlat() {
      int s = 256;
      BufferedImage img = new BufferedImage(s, s, 2);
      Graphics2D g = g(img);
      Area head = cranium((float)s);
      head.add(new Area(new java.awt.geom.RoundRectangle2D.Float(0.29F * (float)s, 0.66F * (float)s, 0.42F * (float)s, 0.16F * (float)s, 0.1F * (float)s, 0.1F * (float)s)));
      g.setColor(Color.WHITE);
      g.fill(head);
      g.setColor(new Color(0, 0, 0, 0));
      g.setComposite(AlphaComposite.Clear);
      g.fill(socket(0.25F * (float)s, 0.31F * (float)s, 0.215F * (float)s, 0.2F * (float)s, false));
      g.fill(socket(0.535F * (float)s, 0.31F * (float)s, 0.215F * (float)s, 0.2F * (float)s, true));
      g.dispose();
      return argb(img);
   }

   private static int[] brush() {
      int w = 256;
      int h = 64;
      int[] px = new int[w * h];
      Random r = new Random(77L);
      float[] streak = new float[h];

      for(int y = 0; y < h; ++y) {
         streak[y] = 0.55F + 0.45F * r.nextFloat();
      }

      for(int x = 0; x < w; ++x) {
         float u = (float)x / (float)(w - 1);
         float ends = smooth(0.0F, 0.12F, u) * (1.0F - smooth(0.7F, 1.0F, u));
         float edgeJitter = 0.08F * (float)Math.sin((double)(u * 41.0F)) + 0.05F * (float)Math.sin((double)(u * 97.0F + 1.3F));

         for(int y = 0; y < h; ++y) {
            float v = (float)y / (float)(h - 1) * 2.0F - 1.0F;
            float d = Math.abs(v) + edgeJitter * (0.6F + 0.4F * streak[y]);
            float core = 1.0F - smooth(0.45F, 0.95F, d);
            float bristle = 0.7F + 0.3F * streak[y] * (0.6F + 0.4F * (float)Math.sin((double)(u * 60.0F + (float)y)));
            float a = core * bristle * ends;
            int al = Math.max(0, Math.min(255, Math.round(a * 255.0F)));
            px[y * w + x] = al << 24 | 16777215;
         }
      }

      return px;
   }

   private static int[] zombie() {
      int s = 64;
      int[] px = new int[s * s];
      Random r = new Random(13L);
      float[] n = noise(s, r, 4);
      float[] n2 = noise(s, r, 12);

      for(int i = 0; i < s * s; ++i) {
         float k = 0.6F * n[i] + 0.4F * n2[i];
         int base = lerpRgb(5139263, 9281906, k);
         if(n2[i] > 0.82F) {
            base = lerpRgb(base, 3877410, 0.6F);
         }

         if(n[i] < 0.18F) {
            base = lerpRgb(base, 7228506, 0.35F);
         }

         px[i] = -16777216 | base;
      }

      return px;
   }

   private static int[] soil() {
      int s = 64;
      int[] px = new int[s * s];
      Random r = new Random(29L);
      float[] n = noise(s, r, 6);

      for(int i = 0; i < s * s; ++i) {
         int base = lerpRgb(2825492, 6045992, n[i]);
         if(r.nextFloat() < 0.04F) {
            base = lerpRgb(base, 9074016, 0.5F);
         }

         px[i] = -16777216 | base;
      }

      return px;
   }

   private static int[] splash() {
      int s = 256;
      BufferedImage img = new BufferedImage(s, s, 2);
      Graphics2D g = g(img);
      Random r = new Random(5L);
      g.setColor(Color.WHITE);

      for(int i = 0; i < 14; ++i) {
         double a = r.nextDouble() * 3.141592653589793D * 2.0D;
         float d = r.nextFloat() * 0.12F;
         float rad = 0.07F + r.nextFloat() * 0.1F;
         float cx = 0.5F + (float)Math.cos(a) * d;
         float cy = 0.5F + (float)Math.sin(a) * d;
         g.fill(new Float((cx - rad) * (float)s, (cy - rad) * (float)s, rad * 2.0F * (float)s, rad * 2.0F * (float)s));
      }

      for(int i = 0; i < 22; ++i) {
         double a = r.nextDouble() * 3.141592653589793D * 2.0D;
         float len = 0.2F + r.nextFloat() * 0.27F;
         float rad = 0.008F + r.nextFloat() * 0.03F;
         float cx = 0.5F + (float)Math.cos(a) * len;
         float cy = 0.5F + (float)Math.sin(a) * len;
         g.fill(new Float((cx - rad) * (float)s, (cy - rad) * (float)s, rad * 2.0F * (float)s, rad * 2.0F * (float)s));
         if(i % 3 == 0) {
            GeneralPath run = new GeneralPath();
            float bx = 0.5F + (float)Math.cos(a) * 0.15F;
            float by = 0.5F + (float)Math.sin(a) * 0.15F;
            float ox = (float)(-Math.sin(a)) * rad * 0.8F;
            float oy = (float)Math.cos(a) * rad * 0.8F;
            run.moveTo((bx + ox * 1.6F) * (float)s, (by + oy * 1.6F) * (float)s);
            run.lineTo((cx + ox) * (float)s, (cy + oy) * (float)s);
            run.lineTo((cx - ox) * (float)s, (cy - oy) * (float)s);
            run.lineTo((bx - ox * 1.6F) * (float)s, (by - oy * 1.6F) * (float)s);
            run.closePath();
            g.fill(run);
         }
      }

      g.dispose();
      return argb(img);
   }

   private static int[] cracks() {
      int s = 256;
      BufferedImage img = new BufferedImage(s, s, 2);
      Graphics2D g = g(img);
      Random r = new Random(41L);
      g.setColor(Color.WHITE);

      for(int k = 0; k < 9; ++k) {
         double a = (double)k / 9.0D * 3.141592653589793D * 2.0D + (double)r.nextFloat() * 0.4D;
         float x = 0.5F * (float)s;
         float y = 0.5F * (float)s;
         float width = 0.022F * (float)s;

         for(int step = 0; step < 6; ++step) {
            a += (double)(r.nextFloat() - 0.5F) * 0.9D;
            float len = (0.04F + r.nextFloat() * 0.05F) * (float)s;
            float nx = x + (float)Math.cos(a) * len;
            float ny = y + (float)Math.sin(a) * len;
            g.setStroke(new BasicStroke(width, 1, 1));
            g.draw(new java.awt.geom.Line2D.Float(x, y, nx, ny));
            if(r.nextFloat() < 0.35F) {
               double b = a + (r.nextBoolean()?0.8D:-0.8D);
               g.setStroke(new BasicStroke(width * 0.55F, 1, 1));
               g.draw(new java.awt.geom.Line2D.Float(nx, ny, nx + (float)Math.cos(b) * len * 0.6F, ny + (float)Math.sin(b) * len * 0.6F));
            }

            x = nx;
            y = ny;
            width *= 0.8F;
         }
      }

      g.dispose();
      return argb(img);
   }

   private static int[] eye() {
      int w = 128;
      int h = 64;
      BufferedImage img = new BufferedImage(w, h, 2);
      Graphics2D g = g(img);
      GeneralPath lid = new GeneralPath();
      lid.moveTo(0.04F * (float)w, 0.5F * (float)h);
      lid.quadTo(0.5F * (float)w, -0.12F * (float)h, 0.96F * (float)w, 0.5F * (float)h);
      lid.quadTo(0.5F * (float)w, 1.12F * (float)h, 0.04F * (float)w, 0.5F * (float)h);
      lid.closePath();
      g.setPaint(new RadialGradientPaint(0.5F * (float)w, 0.5F * (float)h, 0.5F * (float)w, new float[]{0.0F, 0.35F, 1.0F}, new Color[]{new Color(255, 255, 255), new Color(255, 255, 255, 230), new Color(255, 255, 255, 40)}));
      g.fill(lid);
      g.setComposite(AlphaComposite.Clear);
      g.fill(new Float(0.47F * (float)w, 0.2F * (float)h, 0.06F * (float)w, 0.6F * (float)h));
      g.dispose();
      return argb(img);
   }

   private static float smooth(float e0, float e1, float x) {
      float t = Math.max(0.0F, Math.min(1.0F, (x - e0) / (e1 - e0)));
      return t * t * (3.0F - 2.0F * t);
   }

   private static int lerpRgb(int a, int b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int r = Math.round((float)(a >> 16 & 255) + (float)((b >> 16 & 255) - (a >> 16 & 255)) * t);
      int gg = Math.round((float)(a >> 8 & 255) + (float)((b >> 8 & 255) - (a >> 8 & 255)) * t);
      int bl = Math.round((float)(a & 255) + (float)((b & 255) - (a & 255)) * t);
      return r << 16 | gg << 8 | bl;
   }

   private static float[] noise(int s, Random r, int cells) {
      float[] grid = new float[cells * cells];

      for(int i = 0; i < grid.length; ++i) {
         grid[i] = r.nextFloat();
      }

      float[] out = new float[s * s];

      for(int y = 0; y < s; ++y) {
         for(int x = 0; x < s; ++x) {
            float fx = (float)x / (float)s * (float)cells;
            float fy = (float)y / (float)s * (float)cells;
            int x0 = (int)fx;
            int y0 = (int)fy;
            float tx = fx - (float)x0;
            float ty = fy - (float)y0;
            tx = tx * tx * (3.0F - 2.0F * tx);
            ty = ty * ty * (3.0F - 2.0F * ty);
            float a = grid[y0 % cells * cells + x0 % cells];
            float b = grid[y0 % cells * cells + (x0 + 1) % cells];
            float c = grid[(y0 + 1) % cells * cells + x0 % cells];
            float d = grid[(y0 + 1) % cells * cells + (x0 + 1) % cells];
            out[y * s + x] = (a + (b - a) * tx) * (1.0F - ty) + (c + (d - c) * tx) * ty;
         }
      }

      return out;
   }
}
