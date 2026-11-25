package mix.client.mixui.integration.haunt.fx.kit;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Locale;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.kit.KitTextures;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import net.minecraft.client.renderer.texture.TextureUtil;

public final class Glitch {
   private static int atlas;
   private static boolean atlasModFont;
   private static final float[] ADVANCE = new float[99];
   private static final KitTextures.Painted MOD_FONT_ATLAS = new KitTextures.Painted("glitch glyphs", () -> {
      return new KitTextures.Image[]{paintAtlas(true)};
   });

   static void warmUp() {
      if(Ui.bold != null) {
         MOD_FONT_ATLAS.warm();
      }

   }

   public static boolean reduceFlashing() {
      return FuriahsVisuals.instance != null && FuriahsVisuals.instance.isReduceFlashing();
   }

   public static float hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      h = h * -1028477387;
      h = h ^ h >>> 16;
      return (float)(h >>> 8) / 1.6777216E7F;
   }

   public static int alphaOf(int color, float a) {
      return Math.round(255.0F * Easing.clamp01(a) * ((float)(color >>> 24) / 255.0F)) << 24 | color & 16777215;
   }

   public static void scanlines(Glitch.Plane p, double t, double speed, double w, double h, float a, float burst, int cg, int ca, int cb) {
      double bandH = 0.32D;

      for(int band = 0; band < 2; ++band) {
         double q = (t * 0.12D * speed + (double)band * 0.5D) % 1.0D;
         double center = h - q * (2.0D * h + bandH) + bandH * 0.5D;

         for(int i = 0; i < 7; ++i) {
            double v = center - bandH * 0.5D + (double)i * bandH / 6.0D;
            if(v >= -h && v <= h) {
               float k = (float)Math.sin(3.141592653589793D * (double)i / 6.0D);
               int col = i % 3 == 0?ca:(i % 3 == 1?cb:cg);
               p.rect(-w, v - 0.004D, w, v + 0.004D, alphaOf(col, a * (0.16F + 0.3F * burst) * k));
            }
         }
      }

   }

   public static void blocks(Glitch.Plane p, int seed, float burst, double w, double h, double d, float a, int ca, int cb, int cg, float peak) {
      int n = 4 + Math.round(10.0F * burst);

      for(int i = 0; i < n; ++i) {
         float r0 = hash(seed, i * 5);
         float r1 = hash(seed, i * 5 + 1);
         float r2 = hash(seed, i * 5 + 2);
         float r3 = hash(seed, i * 5 + 3);
         int side = (int)(r0 * 4.0F);
         double along = (double)(r1 * 2.0F - 1.0F);
         double bw = 0.08D + 0.42D * (double)r2;
         double bh = 0.025D + 0.1D * (double)r3 * (double)r3;
         double v;
         double u;
         if(side < 2) {
            u = along * w;
            v = (double)(side == 0?1:-1) * h;
         } else {
            u = (double)(side == 2?1:-1) * w;
            v = along * h;
         }

         int core = (i & 1) == 0?cg:((i & 2) == 0?ca:cb);
         double var32;
         p.splitRect((var32 = u + ((double)hash(seed, i * 5 + 4) - 0.5D) * 0.5D * (double)burst) - bw / 2.0D, v - bh / 2.0D, var32 + bw / 2.0D, v + bh / 2.0D, d * 1.5D, alphaOf(ca, a * 0.75F * burst), alphaOf(cb, a * 0.75F * burst), alphaOf(core, a * peak * burst));
      }

   }

   public static void streaks(Glitch.Plane p, int seed, float k, int cols, double w, double top, double reach, float a, int ca, int cb, int cg) {
      if(k > 0.02F) {
         for(int i = 0; i < cols; ++i) {
            float r0 = hash(seed + 3, i);
            float r1 = hash(seed + 5, i);
            double u = (double)(r0 * 2.0F - 1.0F) * w * 0.95D;
            double cw = 0.04D + 0.06D * (double)r1;
            double len = (0.25D + 0.75D * (double)hash(seed + 7, i)) * (double)k * reach;
            int segs = 6;

            for(int s = 0; s < segs; ++s) {
               double v0 = top - len * (double)s / (double)segs;
               double v1 = top - len * (double)(s + 1) / (double)segs;
               float fade = 1.0F - (float)s / (float)segs;
               int col = s % 3 == 0?cg:(s % 3 == 1?ca:cb);
               double shift = ((double)hash(i * 13 + s, seed) - 0.5D) * cw * 0.8D;
               p.rect(u + shift - cw / 2.0D, v1, u + shift + cw / 2.0D, v0, alphaOf(col, a * 0.7F * fade * k));
            }
         }
      }

   }

   public static float crtHeight(float out) {
      return 1.0F - Easing.inCubic(Easing.clamp01(out / 0.55F));
   }

   public static float crtWidth(float out) {
      return 1.0F - Easing.inCubic(Easing.clamp01((out - 0.55F) / 0.3F));
   }

   public static float crtDot(float out) {
      return Easing.smoothstep(0.75F, 0.85F, out) * (1.0F - Easing.clamp01((out - 0.85F) / 0.15F));
   }

   public static double textWidth(CharSequence s) {
      double w = 0.0D;

      for(int i = 0; i < s.length(); ++i) {
         w += (double)ADVANCE[index(s.charAt(i))];
      }

      return w;
   }

   private static int index(char ch) {
      if(ch >= 32 && ch < 127) {
         return ch - 32;
      } else {
         switch(ch) {
         case '█':
            return 98;
         case '░':
            return 95;
         case '▒':
            return 96;
         case '▓':
            return 97;
         default:
            return 31;
         }
      }
   }

   public static int atlas() {
      boolean modFont = Ui.bold != null;
      if(atlas == 0 || modFont && !atlasModFont) {
         float[] advance;
         int tex;
         if(modFont) {
            tex = MOD_FONT_ATLAS.texture();
            advance = (float[])((float[])MOD_FONT_ATLAS.extra(0));
         } else {
            KitTextures.Image im = paintAtlas(false);
            tex = im.upload();
            advance = (float[])((float[])im.extra);
         }

         System.arraycopy(advance, 0, ADVANCE, 0, ADVANCE.length);
         if(atlas != 0) {
            TextureUtil.deleteTexture(atlas);
         }

         atlas = tex;
         atlasModFont = modFont;
      }

      return atlas;
   }

   private static KitTextures.Image paintAtlas(boolean modFont) {
      float[] advance = new float[ADVANCE.length];
      int cell = 64;
      int w = cell * 16;
      int h = 512;
      BufferedImage img = new BufferedImage(w, h, 2);
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      Font font = modFont?Ui.bold.awt(46.0F):new Font("SansSerif", 1, 46);
      g.setFont(font);
      g.setColor(Color.WHITE);
      FontMetrics fm = g.getFontMetrics();

      for(int i2 = 0; i2 < 95; ++i2) {
         char ch = (char)(32 + i2);
         int x = i2 % 16 * cell;
         int y = i2 / 16 * cell;
         g.drawString(String.valueOf(ch), x + 4, y + 50);
         advance[i2] = (float)(fm.charWidth(ch) + 3) / (float)cell;
      }

      for(int k = 0; k < 4; ++k) {
         int i = 95 + k;
         int x0 = i % 16 * cell;
         int y0 = i / 16 * cell;

         for(int by = 0; by < 7; ++by) {
            for(int bx = 0; bx < 5; ++bx) {
               boolean fill;
               switch(k) {
               case 0:
                  fill = (bx + by * 2) % 4 == 0;
                  break;
               case 1:
                  fill = (bx + by) % 2 == 0;
                  break;
               case 2:
                  fill = (bx + by * 2) % 4 != 0;
                  break;
               default:
                  fill = true;
               }

               if(fill) {
                  g.fillRect(x0 + 4 + bx * 8, y0 + 4 + by * 8, 8, 8);
               }
            }
         }

         advance[i] = 0.71875F;
      }

      g.dispose();
      int[] px = img.getRGB(0, 0, w, h, (int[])null, 0, w);

      for(int i = 0; i < px.length; ++i) {
         px[i] = px[i] & -16777216 | 16777215;
      }

      return KitTextures.Image.argb(px, w, h, true, false).with(advance);
   }

   public static final class Bursts {
      public int seed;
      public double since = 9.0D;

      public float at(double t, double perSecond, int seed0, boolean reduce) {
         if(reduce) {
            perSecond = Math.min(2.0D, perSecond);
         }

         double slot = 1.0D / Math.max(0.1D, perSecond);
         if(reduce) {
            slot = Math.max(slot, 0.52D);
         }

         long k = (long)Math.floor(t / slot);
         float best = 0.0F;
         this.since = 9.0D;

         for(long j = k - 1L; j <= k; ++j) {
            if(j >= 1L) {
               double start = (double)j * slot + (reduce?slot * 0.5D:slot * 0.7D * (double)Glitch.hash((int)j, seed0));
               double age = t - start;
               if(age >= 0.0D) {
                  if(age < this.since) {
                     this.since = age;
                     this.seed = (int)j * -1640531535 + seed0;
                  }

                  double len;
                  if(age <= (len = 0.06D + 0.14D * (double)Glitch.hash((int)j, seed0 + 1))) {
                     float e = reduce?Easing.smoothstep(0.0F, 0.04F, (float)age) * (1.0F - Easing.smoothstep((float)len - 0.06F, (float)len, (float)age)):1.0F;
                     best = Math.max(best, e);
                  }
               }
            }
         }

         return best;
      }

      public String countForTest(double perSecond, boolean reduce) {
         int starts = 0;
         int most = 0;
         boolean was = false;
         ArrayDeque<Double> recent = new ArrayDeque();

         for(double t = 0.3D; t < 20.3D; t += 0.002D) {
            boolean on = this.at(t, perSecond, 1234, reduce) > 0.0F;
            if(on && !was) {
               ++starts;
               recent.addLast(Double.valueOf(t));

               while(!recent.isEmpty() && ((Double)recent.peekFirst()).doubleValue() < t - 1.0D) {
                  recent.removeFirst();
               }

               most = Math.max(most, recent.size());
            }

            was = on;
         }

         return String.format(Locale.ROOT, "%d bursts in 20 s, at most %d in any second (Reduce Flashing %s)", new Object[]{Integer.valueOf(starts), Integer.valueOf(most), reduce?"on":"off"});
      }
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

      public void set(double ox, double oy, double oz, double rx, double ry, double rz, double ux, double uy, double uz) {
         this.ox = ox;
         this.oy = oy;
         this.oz = oz;
         this.rx = rx;
         this.ry = ry;
         this.rz = rz;
         this.ux = ux;
         this.uy = uy;
         this.uz = uz;
      }

      public void rect(double u0, double v0, double u1, double v1, int col) {
         if(col >>> 24 != 0) {
            Vfx.quadUV(Textures.WHITE, this.ox + this.rx * u0 + this.ux * v0, this.oy + this.ry * u0 + this.uy * v0, this.oz + this.rz * u0 + this.uz * v0, 0.5D, 0.5D, col, this.ox + this.rx * u1 + this.ux * v0, this.oy + this.ry * u1 + this.uy * v0, this.oz + this.rz * u1 + this.uz * v0, 0.5D, 0.5D, col, this.ox + this.rx * u1 + this.ux * v1, this.oy + this.ry * u1 + this.uy * v1, this.oz + this.rz * u1 + this.uz * v1, 0.5D, 0.5D, col, this.ox + this.rx * u0 + this.ux * v1, this.oy + this.ry * u0 + this.uy * v1, this.oz + this.rz * u0 + this.uz * v1, 0.5D, 0.5D, col);
         }

      }

      public void splitRect(double u0, double v0, double u1, double v1, double d, int a, int b, int core) {
         if(d > 5.0E-4D) {
            this.rect(u0 - d, v0, u1 - d, v1, a);
            this.rect(u0 + d, v0, u1 + d, v1, b);
         }

         this.rect(u0, v0, u1, v1, core);
      }

      public void text(int tex, CharSequence s, double u, double v, double gh, int col) {
         if(col >>> 24 != 0) {
            double x = u;

            for(int i = 0; i < s.length(); ++i) {
               int g = Glitch.index(s.charAt(i));
               float u0 = (float)(g % 16) / 16.0F;
               float v0 = (float)(g / 16) * 64.0F / 512.0F;
               float u1 = u0 + 0.0625F;
               float v1 = v0 + 0.125F;
               double x1 = x + gh;
               double y0 = v - gh * 0.22D;
               double y1 = v + gh * 0.78D;
               Vfx.quadUV(tex, this.ox + this.rx * x + this.ux * y0, this.oy + this.ry * x + this.uy * y0, this.oz + this.rz * x + this.uz * y0, (double)u0, (double)v1, col, this.ox + this.rx * x1 + this.ux * y0, this.oy + this.ry * x1 + this.uy * y0, this.oz + this.rz * x1 + this.uz * y0, (double)u1, (double)v1, col, this.ox + this.rx * x1 + this.ux * y1, this.oy + this.ry * x1 + this.uy * y1, this.oz + this.rz * x1 + this.uz * y1, (double)u1, (double)v0, col, this.ox + this.rx * x + this.ux * y1, this.oy + this.ry * x + this.uy * y1, this.oz + this.rz * x + this.uz * y1, (double)u0, (double)v0, col);
               x += (double)Glitch.ADVANCE[g] * gh;
            }
         }

      }

      public void splitText(int tex, CharSequence s, double u, double v, double gh, double d, int a, int b, int core) {
         if(d > 5.0E-4D) {
            this.text(tex, s, u - d, v, gh, a);
            this.text(tex, s, u + d, v, gh, b);
         }

         this.text(tex, s, u, v, gh, core);
      }
   }
}
