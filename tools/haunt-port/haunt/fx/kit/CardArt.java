package mix.client.mixui.integration.haunt.fx.kit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D.Double;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public final class CardArt {
   public static final int W = 1024;
   public static final int H = 1536;
   public static final int CELL = 512;
   static final int ATLAS_W = 1536;
   static final int ATLAS_H = 1024;
   public static final int SUN = 0;
   public static final int MOON = 1;
   public static final int STAR = 2;
   public static final int EYE = 3;
   public static final int TOWER = 4;
   public static final int WHEEL = 5;
   public static final int CELESTIAL = 0;
   public static final int FLORAL = 1;
   public static final int MINIMAL = 2;
   private static final int DESIGNS = 3;
   private static final int SYMBOLS_SLOT = 6;
   private static final int PORTRAIT_SLOT = 7;
   private static final int SLOTS = 10;
   private static final double PICTURE_TOP = 205.0D;
   private static final double PICTURE_BOTTOM = 1225.0D;
   private static final double TITLE_TOP = 1236.0D;
   private static final double TITLE_BOTTOM = 1372.0D;
   public static final double PICTURE_Y = 0.0517578125D;
   public static final double PICTURE_H = 0.99609375D;
   public static final double PICTURE_W = 0.6640625D;
   public static final double TITLE_Y = -0.5234375D;
   public static final double TITLE_W = 0.5859375D;
   public static final double TITLE_H = 0.1328125D;
   private static final int[] TEXTURES = new int[10];
   private static final boolean[] REQUESTED = new boolean[10];
   private static final int[][][] PAINTED = new int[10][][];
   private static final ExecutorService PAINTER = Executors.newSingleThreadExecutor(new ThreadFactory() {
      public Thread newThread(Runnable r) {
         Thread t = new Thread(r, "HAUN’T VISUALS card art");
         t.setDaemon(true);
         t.setPriority(1);
         return t;
      }
   });
   private static boolean uploadedThisFrame;
   private static long frameMark;

   public static int face(int d) {
      return get(d * 2);
   }

   public static int back(int d) {
      return get(d * 2 + 1);
   }

   public static int symbols() {
      return get(6);
   }

   public static int portraitFace(int d) {
      return get(7 + d);
   }

   public static void cell(int symbol, float[] out) {
      int cx = symbol % 3;
      int cy = symbol / 3;
      out[0] = (float)cx / 3.0F;
      out[1] = (float)cy / 2.0F;
      out[2] = (float)(cx + 1) / 3.0F;
      out[3] = (float)(cy + 1) / 2.0F;
   }

   private static int get(int slot) {
      if(TEXTURES[slot] != 0) {
         return TEXTURES[slot];
      } else if(!REQUESTED[slot]) {
         request(slot);
         return 0;
      } else {
         long now = System.nanoTime();
         if(now - frameMark > 4000000L) {
            frameMark = now;
            uploadedThisFrame = false;
         }

         if(uploadedThisFrame) {
            return 0;
         } else {
            int[][][] var4 = PAINTED;
            synchronized(PAINTED) {
               int[][] levels = PAINTED[slot];
               PAINTED[slot] = (int[][])((int[][])null);
               if(levels != null) {
                  TEXTURES[slot] = upload(levels, slot == 6?1536:1024, slot == 6?1024:1536);
                  uploadedThisFrame = true;
                  return TEXTURES[slot];
               } else {
                  return 0;
               }
            }
         }
      }
   }

   private static void request(final int slot) {
      REQUESTED[slot] = true;
      PAINTER.execute(new Runnable() {
         public void run() {
            try {
               long start = System.nanoTime();
               int w = 1024;
               int h = 1536;
               int[] px;
               if(slot == 6) {
                  px = CardArt.symbolAtlas();
                  w = 1536;
                  h = 1024;
               } else {
                  px = slot >= 7?CardArt.faceFrame(slot - 7, true):((slot & 1) == 0?CardArt.faceFrame(slot / 2, false):CardArt.cardBack(slot / 2));
               }

               int[][] levels = CardArt.mipmaps(px, w, h);
               int[][][] nArray = CardArt.PAINTED;
               synchronized(nArray) {
                  CardArt.PAINTED[slot] = levels;
               }

               FuriahsVisuals.LOG.debug("Card art {} painted in {} ms", new Object[]{Integer.valueOf(slot), Long.valueOf((System.nanoTime() - start) / 1000000L)});
            } catch (RuntimeException var11) {
               FuriahsVisuals.LOG.warn("Painting the card art failed; the cards show without it", var11);
            }

         }
      });
   }

   public static void paintAllForTest() {
      for(int s = 0; s < 10; ++s) {
         if(TEXTURES[s] == 0) {
            REQUESTED[s] = true;
            int[] px = s == 6?symbolAtlas():(s >= 7?faceFrame(s - 7, true):((s & 1) == 0?faceFrame(s / 2, false):cardBack(s / 2)));
            int w = s == 6?1536:1024;
            int h = s == 6?1024:1536;
            TEXTURES[s] = upload(mipmaps(px, w, h), w, h);
         }
      }

   }

   public static String stateForTest() {
      int n = 0;

      for(int t : TEXTURES) {
         if(t != 0) {
            ++n;
         }
      }

      return n + " of " + 10;
   }

   private static Graphics2D pen(BufferedImage img) {
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setColor(Color.WHITE);
      return g;
   }

   private static void width(Graphics2D g, double w) {
      g.setStroke(new BasicStroke((float)w, 1, 1));
   }

   private static int[] pixels(BufferedImage img) {
      int w = img.getWidth();
      int h = img.getHeight();
      int[] px = img.getRGB(0, 0, w, h, (int[])null, 0, w);

      for(int i = 0; i < px.length; ++i) {
         px[i] = px[i] & -16777216 | 16777215;
      }

      return px;
   }

   static int[] faceFrame(int design, boolean portrait) {
      BufferedImage img = new BufferedImage(1024, 1536, 2);
      Graphics2D g = pen(img);
      double sx = 512.0D;
      double sy = 600.0D;
      if(design == 0) {
         width(g, 7.0D);
         g.draw(new Double(64.0D, 64.0D, 896.0D, 1408.0D, 96.0D, 96.0D));
         width(g, 2.5D);
         g.draw(new Double(90.0D, 90.0D, 844.0D, 1356.0D, 70.0D, 70.0D));

         for(double[] c : new double[][]{{150.0D, 150.0D}, {874.0D, 150.0D}, {150.0D, 1386.0D}, {874.0D, 1386.0D}}) {
            width(g, 4.0D);
            star(g, c[0], c[1], 4, 40.0D, 10.0D, 0.0D);
            width(g, 2.5D);
            g.draw(new java.awt.geom.Ellipse2D.Double(c[0] - 16.0D, c[1] - 16.0D, 32.0D, 32.0D));
         }

         for(double x = 230.0D; x <= 794.0D; x += 56.0D) {
            dot(g, x, 120.0D, 4.0D);
            dot(g, x, 1416.0D, 4.0D);
         }

         if(!portrait) {
            width(g, 3.0D);
            g.draw(new java.awt.geom.Ellipse2D.Double(sx - 330.0D, sy - 330.0D, 660.0D, 660.0D));
            width(g, 1.8D);
            g.draw(new java.awt.geom.Ellipse2D.Double(sx - 350.0D, sy - 350.0D, 700.0D, 700.0D));

            for(int i = 0; i < 48; ++i) {
               double a = (double)i * 3.141592653589793D * 2.0D / 48.0D;
               double in = i % 4 == 0?300.0D:316.0D;
               g.draw(new java.awt.geom.Line2D.Double(sx + Math.cos(a) * in, sy + Math.sin(a) * in, sx + Math.cos(a) * 330.0D, sy + Math.sin(a) * 330.0D));
            }
         }

         width(g, 4.0D);
         crescent(g, sx, 205.0D, 44.0D, 0.55D);
         width(g, 3.0D);
         star(g, sx - 120.0D, 215.0D, 4, 16.0D, 5.0D, 0.0D);
         star(g, sx + 120.0D, 215.0D, 4, 16.0D, 5.0D, 0.0D);
         plate(g, 1, portrait);
      } else if(design == 1) {
         width(g, 6.0D);
         g.draw(new Double(64.0D, 64.0D, 896.0D, 1408.0D, 120.0D, 120.0D));
         width(g, 3.0D);
         vine(g, 120.0D, 260.0D, 120.0D, 1276.0D, 1);
         vine(g, 904.0D, 260.0D, 904.0D, 1276.0D, -1);

         for(double[] c : new double[][]{{150.0D, 150.0D}, {874.0D, 150.0D}, {150.0D, 1386.0D}, {874.0D, 1386.0D}}) {
            rosette(g, c[0], c[1], 44.0D, 6);
         }

         if(!portrait) {
            width(g, 3.0D);

            for(int i = 0; i < 22; ++i) {
               double a = (double)i * 3.141592653589793D * 2.0D / 22.0D;
               leaf(g, sx + Math.cos(a) * 330.0D, sy + Math.sin(a) * 330.0D, a + 1.5707963267948966D, 46.0D, 18.0D);
            }

            width(g, 2.0D);
            g.draw(new java.awt.geom.Ellipse2D.Double(sx - 300.0D, sy - 300.0D, 600.0D, 600.0D));
         }

         plate(g, 2, portrait);
      } else {
         width(g, 4.0D);
         g.draw(new Double(72.0D, 72.0D, 880.0D, 1392.0D, 64.0D, 64.0D));
         width(g, 3.0D);

         for(double[] c : new double[][]{{120.0D, 120.0D, 1.0D, 1.0D}, {904.0D, 120.0D, -1.0D, 1.0D}, {120.0D, 1416.0D, 1.0D, -1.0D}, {904.0D, 1416.0D, -1.0D, -1.0D}}) {
            g.draw(new java.awt.geom.Line2D.Double(c[0], c[1], c[0] + 60.0D * c[2], c[1]));
            g.draw(new java.awt.geom.Line2D.Double(c[0], c[1], c[0], c[1] + 60.0D * c[3]));
         }

         if(!portrait) {
            width(g, 2.5D);
            g.draw(new java.awt.geom.Ellipse2D.Double(sx - 320.0D, sy - 320.0D, 640.0D, 640.0D));
         }

         plate(g, 0, portrait);
      }

      g.dispose();
      return pixels(img);
   }

   private static void plate(Graphics2D g, int style, boolean portrait) {
      double y = 1236.0D;
      width(g, 3.0D);
      g.draw(new java.awt.geom.Line2D.Double(180.0D, y, 452.0D, y));
      g.draw(new java.awt.geom.Line2D.Double(572.0D, y, 844.0D, y));
      if(style == 0) {
         dot(g, 512.0D, y, 7.0D);
      } else if(style == 1) {
         width(g, 3.5D);
         diamond(g, 512.0D, y, 34.0D, 18.0D);
         dot(g, 512.0D, y, 5.0D);
      } else {
         rosette(g, 512.0D, y, 28.0D, 5);
      }

      width(g, 2.0D);
      if(!portrait) {
         g.draw(new java.awt.geom.Line2D.Double(240.0D, y + 46.0D, 784.0D, y + 46.0D));
      } else {
         g.draw(new java.awt.geom.Line2D.Double(250.0D, 1372.0D, 774.0D, 1372.0D));
         width(g, 2.5D);
         diamond(g, 236.0D, 1372.0D, 12.0D, 8.0D);
         diamond(g, 788.0D, 1372.0D, 12.0D, 8.0D);
      }

   }

   static int[] cardBack(int design) {
      BufferedImage img = new BufferedImage(1024, 1536, 2);
      Graphics2D g = pen(img);
      double cx = 512.0D;
      double cy = 768.0D;
      width(g, 7.0D);
      g.draw(new Double(64.0D, 64.0D, 896.0D, 1408.0D, 96.0D, 96.0D));
      width(g, 2.5D);
      g.draw(new Double(92.0D, 92.0D, 840.0D, 1352.0D, 70.0D, 70.0D));
      if(design == 0) {
         Random r = new Random(500231L);

         for(int i = 0; i < 140; ++i) {
            double x = 130.0D + r.nextDouble() * 764.0D;
            double dx = x - cx;
            double y;
            double dy;
            if(dx * dx + (dy = (y = 130.0D + r.nextDouble() * 1276.0D) - cy) * dy >= 90000.0D) {
               double s = r.nextDouble();
               if(s < 0.75D) {
                  dot(g, x, y, 2.0D + s * 4.0D);
               } else {
                  width(g, 2.5D);
                  star(g, x, y, 4, 10.0D + s * 14.0D, 3.0D, r.nextDouble());
               }
            }
         }

         width(g, 5.0D);
         star(g, cx, cy, 8, 200.0D, 70.0D, 0.0D);
         width(g, 3.0D);
         g.draw(new java.awt.geom.Ellipse2D.Double(cx - 240.0D, cy - 240.0D, 480.0D, 480.0D));
         g.draw(new java.awt.geom.Ellipse2D.Double(cx - 260.0D, cy - 260.0D, 520.0D, 520.0D));
         width(g, 4.0D);

         for(int var19 = 0; var19 < 4; ++var19) {
            double a = (double)var19 * 3.141592653589793D / 2.0D + 0.7853981633974483D;
            crescent(g, cx + Math.cos(a) * 330.0D, cy + Math.sin(a) * 330.0D, 30.0D, 0.55D);
         }
      } else if(design == 1) {
         width(g, 3.5D);
         petals(g, cx, cy, 12, 90.0D, 250.0D, 60.0D);
         petals(g, cx, cy, 8, 50.0D, 150.0D, 44.0D);
         petals(g, cx, cy, 6, 18.0D, 80.0D, 30.0D);
         g.draw(new java.awt.geom.Ellipse2D.Double(cx - 280.0D, cy - 280.0D, 560.0D, 560.0D));
         dot(g, cx, cy, 12.0D);

         for(double[] c : new double[][]{{170.0D, 170.0D, 0.7853981633974483D}, {854.0D, 170.0D, 2.356194490192345D}, {170.0D, 1366.0D, -0.7853981633974483D}, {854.0D, 1366.0D, -2.356194490192345D}}) {
            for(int k = -1; k <= 1; ++k) {
               leaf(g, c[0], c[1], c[2] + (double)k * 0.5D, 70.0D, 24.0D);
            }
         }

         width(g, 3.0D);
         vine(g, cx - 300.0D, 200.0D, cx + 300.0D, 200.0D, 1);
         vine(g, cx - 300.0D, 1336.0D, cx + 300.0D, 1336.0D, -1);
      } else {
         width(g, 2.0D);

         for(double y = 170.0D; y <= 1366.0D; y += 80.0D) {
            for(double x = (double)(170 + (int)((y - 170.0D) / 80.0D) % 2 * 40); x <= 854.0D; x += 80.0D) {
               double dx = x - cx;
               double dy = y - cy;
               if(dx * dx + dy * dy >= 48400.0D) {
                  diamond(g, x, y, 12.0D, 12.0D);
               }
            }
         }

         width(g, 4.0D);
         g.draw(new java.awt.geom.Ellipse2D.Double(cx - 170.0D, cy - 170.0D, 340.0D, 340.0D));
         width(g, 3.0D);
         diamond(g, cx, cy, 70.0D, 70.0D);
         dot(g, cx, cy, 10.0D);
      }

      g.dispose();
      return pixels(img);
   }

   static int[] symbolAtlas() {
      BufferedImage img = new BufferedImage(1536, 1024, 2);
      Graphics2D g = pen(img);

      label28:
      for(int s = 0; s < 6; ++s) {
         double cx = (double)(s % 3 * 512) + 256.0D;
         double cy = (double)(s / 3 * 512) + 256.0D;
         switch(s) {
         case 0:
            width(g, 12.0D);
            g.draw(new java.awt.geom.Ellipse2D.Double(cx - 110.0D, cy - 110.0D, 220.0D, 220.0D));
            width(g, 5.0D);
            g.draw(new java.awt.geom.Ellipse2D.Double(cx - 72.0D, cy - 72.0D, 144.0D, 144.0D));
            int i = 0;

            for(; i < 16; ++i) {
               double a = (double)i * 3.141592653589793D * 2.0D / 16.0D;
               double ca = Math.cos(a);
               double sa = Math.sin(a);
               if(i % 2 == 0) {
                  width(g, 10.0D);
                  g.draw(new java.awt.geom.Line2D.Double(cx + ca * 138.0D, cy + sa * 138.0D, cx + ca * 222.0D, cy + sa * 222.0D));
               } else {
                  width(g, 7.0D);
                  java.awt.geom.Path2D.Double eyePath = new java.awt.geom.Path2D.Double();
                  double px = -sa;
                  eyePath.moveTo(cx + ca * 138.0D, cy + sa * 138.0D);
                  eyePath.curveTo(cx + ca * 160.0D + px * 14.0D, cy + sa * 160.0D + ca * 14.0D, cx + ca * 175.0D - px * 14.0D, cy + sa * 175.0D - ca * 14.0D, cx + ca * 196.0D, cy + sa * 196.0D);
                  g.draw(eyePath);
               }
            }

            dot(g, cx, cy, 14.0D);
            break;
         case 1:
            width(g, 12.0D);
            crescent(g, cx - 20.0D, cy, 150.0D, 0.62D);
            width(g, 6.0D);
            star(g, cx + 120.0D, cy - 120.0D, 4, 34.0D, 9.0D, 0.0D);
            star(g, cx + 150.0D, cy + 40.0D, 4, 22.0D, 6.0D, 0.0D);
            star(g, cx + 70.0D, cy + 150.0D, 4, 16.0D, 5.0D, 0.0D);
            break;
         case 2:
            width(g, 11.0D);
            star(g, cx, cy, 8, 200.0D, 80.0D, 0.0D);
            width(g, 6.0D);
            g.draw(new java.awt.geom.Ellipse2D.Double(cx - 56.0D, cy - 56.0D, 112.0D, 112.0D));
            dot(g, cx, cy, 16.0D);
            width(g, 5.0D);
            star(g, cx - 175.0D, cy - 175.0D, 4, 26.0D, 6.0D, 0.0D);
            star(g, cx + 175.0D, cy + 175.0D, 4, 26.0D, 6.0D, 0.0D);
            break;
         case 3:
            width(g, 11.0D);
            java.awt.geom.Path2D.Double p = new java.awt.geom.Path2D.Double();
            p.moveTo(cx - 200.0D, cy + 20.0D);
            p.quadTo(cx, cy - 170.0D, cx + 200.0D, cy + 20.0D);
            p.quadTo(cx, cy + 190.0D, cx - 200.0D, cy + 20.0D);
            p.closePath();
            g.draw(p);
            width(g, 8.0D);
            g.draw(new java.awt.geom.Ellipse2D.Double(cx - 66.0D, cy - 46.0D, 132.0D, 132.0D));
            dot(g, cx, cy + 20.0D, 30.0D);
            width(g, 7.0D);
            int starIndex = 0;

            while(true) {
               if(starIndex >= 9) {
                  continue label28;
               }

               double a = 3.141592653589793D + 3.141592653589793D * (double)(starIndex + 1) / 10.0D;
               g.draw(new java.awt.geom.Line2D.Double(cx + Math.cos(a) * 140.0D, cy - 30.0D + Math.sin(a) * 110.0D, cx + Math.cos(a) * 205.0D, cy - 30.0D + Math.sin(a) * 170.0D));
               ++starIndex;
            }
         case 4:
            width(g, 10.0D);
            double l = cx - 72.0D;
            double r = cx + 72.0D;
            double top = cy - 120.0D;
            double bottom = cy + 190.0D;
            java.awt.geom.Path2D.Double crownPath = new java.awt.geom.Path2D.Double();
            crownPath.moveTo(l - 12.0D, bottom);
            crownPath.lineTo(l, top);
            double step = (r - l) / 5.0D;
            crownPath.lineTo(l, top - 40.0D);

            for(int toothIndex = 0; toothIndex < 5; ++toothIndex) {
               double x0 = l + (double)toothIndex * step;
               crownPath.lineTo(x0 + step, toothIndex % 2 == 0?top - 40.0D:top);
               if(toothIndex < 4) {
                  crownPath.lineTo(x0 + step, toothIndex % 2 == 0?top:top - 40.0D);
               }
            }

            crownPath.lineTo(r, top);
            crownPath.lineTo(r + 12.0D, bottom);
            g.draw(crownPath);
            g.draw(new java.awt.geom.Line2D.Double(l - 40.0D, bottom, r + 40.0D, bottom));
            width(g, 7.0D);
            g.draw(new java.awt.geom.Arc2D.Double(cx - 28.0D, bottom - 100.0D, 56.0D, 70.0D, 0.0D, 180.0D, 0));
            g.draw(new java.awt.geom.Line2D.Double(cx - 28.0D, bottom - 65.0D, cx - 28.0D, bottom));
            g.draw(new java.awt.geom.Line2D.Double(cx + 28.0D, bottom - 65.0D, cx + 28.0D, bottom));
            g.draw(new java.awt.geom.Line2D.Double(cx - 30.0D, cy - 50.0D, cx - 30.0D, cy - 10.0D));
            g.draw(new java.awt.geom.Line2D.Double(cx + 30.0D, cy - 50.0D, cx + 30.0D, cy - 10.0D));
            width(g, 9.0D);
            java.awt.geom.Path2D.Double bolt = new java.awt.geom.Path2D.Double();
            bolt.moveTo(cx - 210.0D, cy - 220.0D);
            bolt.lineTo(cx - 120.0D, cy - 150.0D);
            bolt.lineTo(cx - 150.0D, cy - 140.0D);
            bolt.lineTo(cx - 60.0D, cy - 170.0D);
            g.draw(bolt);
            break;
         default:
            width(g, 12.0D);
            g.draw(new java.awt.geom.Ellipse2D.Double(cx - 195.0D, cy - 195.0D, 390.0D, 390.0D));
            width(g, 5.0D);
            g.draw(new java.awt.geom.Ellipse2D.Double(cx - 152.0D, cy - 152.0D, 304.0D, 304.0D));
            width(g, 9.0D);
            g.draw(new java.awt.geom.Ellipse2D.Double(cx - 36.0D, cy - 36.0D, 72.0D, 72.0D));

            for(int rayIndex = 0; rayIndex < 8; ++rayIndex) {
               double a = (double)rayIndex * 3.141592653589793D / 4.0D;
               width(g, 8.0D);
               g.draw(new java.awt.geom.Line2D.Double(cx + Math.cos(a) * 36.0D, cy + Math.sin(a) * 36.0D, cx + Math.cos(a) * 152.0D, cy + Math.sin(a) * 152.0D));
               width(g, 5.0D);
               double b = a + 0.39269908169872414D;
               diamond(g, cx + Math.cos(b) * 174.0D, cy + Math.sin(b) * 174.0D, 12.0D, 12.0D);
            }
         }
      }

      g.dispose();
      return pixels(img);
   }

   private static void dot(Graphics2D g, double x, double y, double r) {
      g.fill(new java.awt.geom.Ellipse2D.Double(x - r, y - r, r * 2.0D, r * 2.0D));
   }

   private static void star(Graphics2D g, double cx, double cy, int points, double outer, double inner, double turn) {
      GeneralPath p = new GeneralPath();

      for(int i = 0; i < points * 2; ++i) {
         double a = -1.5707963267948966D + ((double)i + turn * 2.0D) * 3.141592653589793D / (double)points;
         double r = (i & 1) == 0?outer:inner;
         double x = cx + Math.cos(a) * r;
         double y = cy + Math.sin(a) * r;
         if(i == 0) {
            p.moveTo(x, y);
         } else {
            p.lineTo(x, y);
         }
      }

      p.closePath();
      g.draw(p);
   }

   private static void crescent(Graphics2D g, double cx, double cy, double r, double bite) {
      Area moon = new Area(new java.awt.geom.Ellipse2D.Double(cx - r, cy - r, r * 2.0D, r * 2.0D));
      double off = r * bite;
      moon.subtract(new Area(new java.awt.geom.Ellipse2D.Double(cx - r + off, cy - r - off * 0.4D, r * 2.0D, r * 2.0D)));
      g.draw(moon);
   }

   private static void diamond(Graphics2D g, double cx, double cy, double rx, double ry) {
      GeneralPath p = new GeneralPath();
      p.moveTo(cx, cy - ry);
      p.lineTo(cx + rx, cy);
      p.lineTo(cx, cy + ry);
      p.lineTo(cx - rx, cy);
      p.closePath();
      g.draw(p);
   }

   private static void leaf(Graphics2D g, double x, double y, double a, double len, double wid) {
      double ca = Math.cos(a);
      double sa = Math.sin(a);
      double nx = -sa;
      double tx = x + ca * len;
      double ty = y + sa * len;
      java.awt.geom.Path2D.Double p = new java.awt.geom.Path2D.Double();
      p.moveTo(x, y);
      p.quadTo(x + ca * len * 0.5D + nx * wid, y + sa * len * 0.5D + ca * wid, tx, ty);
      p.quadTo(x + ca * len * 0.5D - nx * wid, y + sa * len * 0.5D - ca * wid, x, y);
      g.draw(p);
      g.draw(new java.awt.geom.Line2D.Double(x, y, x + ca * len * 0.8D, y + sa * len * 0.8D));
   }

   private static void rosette(Graphics2D g, double cx, double cy, double r, int n) {
      width(g, 3.0D);

      for(int i = 0; i < n; ++i) {
         double a = -1.5707963267948966D + (double)i * 3.141592653589793D * 2.0D / (double)n;
         leaf(g, cx, cy, a, r, r * 0.42D);
      }

      dot(g, cx, cy, r * 0.16D);
   }

   private static void petals(Graphics2D g, double cx, double cy, int n, double from, double to, double wid) {
      for(int i = 0; i < n; ++i) {
         double a = (double)i * 3.141592653589793D * 2.0D / (double)n;
         leaf(g, cx + Math.cos(a) * from, cy + Math.sin(a) * from, a, to - from, wid);
      }

   }

   private static void vine(Graphics2D g, double x0, double y0, double x1, double y1, int side) {
      double dx = x1 - x0;
      double dy = y1 - y0;
      double len = Math.sqrt(dx * dx + dy * dy);
      double ux = dx / len;
      double uy = dy / len;
      double nx = -uy * (double)side;
      double ny = ux * (double)side;
      int waves = (int)Math.max(2L, Math.round(len / 160.0D));
      java.awt.geom.Path2D.Double p = new java.awt.geom.Path2D.Double();
      p.moveTo(x0, y0);
      int steps = waves * 16;

      for(int i = 1; i <= steps; ++i) {
         double t = (double)i / (double)steps;
         double w = Math.sin(t * 3.141592653589793D * 2.0D * (double)waves) * 14.0D;
         p.lineTo(x0 + dx * t + nx * w, y0 + dy * t + ny * w);
      }

      g.draw(p);

      for(int var40 = 0; var40 < waves * 2; ++var40) {
         double t = ((double)var40 + 0.5D) / ((double)waves * 2.0D);
         double w = Math.sin(t * 3.141592653589793D * 2.0D * (double)waves) * 14.0D;
         double bx = x0 + dx * t + nx * w;
         double by = y0 + dy * t + ny * w;
         double s = (var40 & 1) == 0?1.0D:-1.0D;
         leaf(g, bx, by, Math.atan2(uy + ny * s * 1.4D, ux + nx * s * 1.4D), 34.0D, 12.0D);
      }

   }

   static int[][] mipmaps(int[] px, int w, int h) {
      int levels = 1;

      for(int m = Math.max(w, h); m > 1; m >>= 1) {
         ++levels;
      }

      int[][] out = new int[levels][];
      out[0] = px;
      int lw = w;
      int lh = h;

      for(int l = 1; l < levels; ++l) {
         int nw = Math.max(1, lw / 2);
         int nh = Math.max(1, lh / 2);
         int[] src = out[l - 1];
         int[] dst = new int[nw * nh];

         for(int y = 0; y < nh; ++y) {
            for(int x = 0; x < nw; ++x) {
               long a = 0L;
               long r = 0L;
               long gg = 0L;
               long b = 0L;
               int n = 0;

               for(int k = 0; k < 4; ++k) {
                  int sx = Math.min(lw - 1, x * 2 + (k & 1));
                  int sy = Math.min(lh - 1, y * 2 + (k >> 1));
                  int c = src[sy * lw + sx];
                  int ca = c >>> 24;
                  a += (long)ca;
                  r += (long)((c >> 16 & 255) * ca);
                  gg += (long)((c >> 8 & 255) * ca);
                  b += (long)((c & 255) * ca);
                  ++n;
               }

               int aa = (int)((a + (long)(n / 2)) / (long)n);
               int rr = a == 0L?255:(int)(r / a);
               int g2 = a == 0L?255:(int)(gg / a);
               int bb = a == 0L?255:(int)(b / a);
               dst[y * nw + x] = aa << 24 | rr << 16 | g2 << 8 | bb;
            }
         }

         out[l] = dst;
         lw = nw;
         lh = nh;
      }

      return out;
   }

   private static int upload(int[][] levels, int w, int h) {
      int id = TextureUtil.glGenTextures();
      GlStateManager.bindTexture(id);
      GL11.glTexParameteri(3553, '脽', levels.length - 1);
      GL11.glTexParameteri(3553, 10241, 9987);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, '脯');
      GL11.glTexParameteri(3553, 10243, '脯');
      int lw = w;
      int lh = h;

      for(int l = 0; l < levels.length; ++l) {
         IntBuffer buffer = BufferUtils.createIntBuffer(lw * lh);
         buffer.put(levels[l], 0, lw * lh);
         buffer.flip();
         GL11.glTexImage2D(3553, l, '聘', lw, lh, 0, '胡', '荧', buffer);
         lw = Math.max(1, lw / 2);
         lh = Math.max(1, lh / 2);
      }

      return id;
   }
}
