package mix.client.mixui.integration.haunt.fx.kit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D.Double;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Halftone;
import mix.client.mixui.integration.haunt.fx.kit.KitTextures;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import net.minecraft.client.Minecraft;

public final class Comic {
   public static final int SPIKY = 0;
   public static final int CLOUD = 1;
   public static final int ROUND = 2;
   public static final int VARIANTS = 4;
   public static final int PUFFS = 4;
   private static final int CELL = 256;
   private static final float STROKE = 0.035F;
   private static final KitTextures.Painted BALLOONS = new KitTextures.Painted("comic balloons", Comic::paintBalloons);
   private static final KitTextures.Painted PUFF_SHEET = new KitTextures.Painted("comic puffs", Comic::paintPuffs);
   private static final float[] UV = new float[4];
   public static final String[] BUILT_IN = new String[]{"POW!", "BAM!", "WHAM!", "SMACK!", "BONK!", "KAPOW!", "KRAK!", "K.O.!", "THUD!", "WHUMP!", "BOOM!", "HOP!"};
   private static final Map<String, Comic.Word> WORDS = new HashMap();
   private static ExecutorService painter;
   private static long uploadFrame = -1L;

   public static int balloonFill() {
      return BALLOONS.texture(0);
   }

   public static int balloonShade() {
      return BALLOONS.texture(1);
   }

   public static int balloonOutline() {
      return BALLOONS.texture(2);
   }

   static void warmUpTextures() {
      BALLOONS.warm();
      PUFF_SHEET.warm();
   }

   public static float[] balloonCell(int shape, int variant, float[] out) {
      int v = Math.floorMod(variant, 4);
      out[0] = (float)v / 4.0F;
      out[2] = (float)(v + 1) / 4.0F;
      out[1] = (float)shape / 3.0F;
      out[3] = (float)(shape + 1) / 3.0F;
      return out;
   }

   public static void balloon(int shape, int variant, double x, double y, double z, double rx, double ry, double rz, double ux, double uy, double uz, double w, double h, double turn, int fill, int dots, boolean halftone, int outline, float outlineWidth) {
      balloonCell(shape, variant, UV);
      fill(halftone, balloonShade(), balloonFill(), UV[0], UV[1], UV[2], UV[3], x, y, z, rx, ry, rz, ux, uy, uz, w, h, turn, fill, dots);
      outline(balloonOutline(), UV[0], UV[1], UV[2], UV[3], x, y, z, rx, ry, rz, ux, uy, uz, w, h, turn, outline, outlineWidth);
   }

   private static void fill(boolean halftone, int shadeTex, int printedTex, float u0, float v0, float u1, float v1, double x, double y, double z, double rx, double ry, double rz, double ux, double uy, double uz, double w, double h, double turn, int fill, int dots) {
      if(Halftone.usable()) {
         Vfx.flush();
         float period = Math.max(3.0F, (float)Minecraft.getMinecraft().displayHeight / 180.0F);
         Halftone.begin(period, halftone?dots:0, 1.0F);

         try {
            quad(shadeTex, u0, v0, u1, v1, x, y, z, rx, ry, rz, ux, uy, uz, w, h, turn, fill);
            Vfx.flush();
         } finally {
            Halftone.end();
         }
      } else {
         quad(halftone?printedTex:shadeTex, u0, v0, u1, v1, x, y, z, rx, ry, rz, ux, uy, uz, w, h, turn, fill);
      }

   }

   private static void outline(int tex, float u0, float v0, float u1, float v1, double x, double y, double z, double rx, double ry, double rz, double ux, double uy, double uz, double w, double h, double turn, int color, float width) {
      if((width = Math.max(0.5F, Math.min(2.0F, width))) < 1.0F) {
         int a = Math.round((float)(color >>> 24) * (0.55F + 0.45F * (width - 0.5F) / 0.5F));
         quad(tex, u0, v0, u1, v1, x, y, z, rx, ry, rz, ux, uy, uz, w, h, turn, a << 24 | color & 16777215);
      } else {
         quad(tex, u0, v0, u1, v1, x, y, z, rx, ry, rz, ux, uy, uz, w, h, turn, color);
         if(width > 1.05F) {
            double extra = (double)((width - 1.0F) * 0.035F) * 0.5D * Math.max(w, h);

            for(int k = 0; k < 8; ++k) {
               double a = 6.283185307179586D * (double)k / 8.0D;
               double ox = (rx * Math.cos(a) + ux * Math.sin(a)) * extra;
               double oy = (ry * Math.cos(a) + uy * Math.sin(a)) * extra;
               double oz = (rz * Math.cos(a) + uz * Math.sin(a)) * extra;
               quad(tex, u0, v0, u1, v1, x + ox, y + oy, z + oz, rx, ry, rz, ux, uy, uz, w, h, turn, color);
            }
         }
      }

   }

   private static KitTextures.Image[] paintBalloons() {
      int w = 1024;
      int h = 768;
      BufferedImage fill = new BufferedImage(w, h, 2);
      BufferedImage line = new BufferedImage(w, h, 2);
      Graphics2D gf = fill.createGraphics();
      Graphics2D gl = line.createGraphics();
      smooth(gf);
      smooth(gl);
      gf.setColor(Color.WHITE);
      gl.setColor(Color.WHITE);
      gl.setStroke(new BasicStroke(8.96F, 1, 1));

      for(int shape = 0; shape < 3; ++shape) {
         for(int v = 0; v < 4; ++v) {
            Shape s = balloon(shape, v, (double)(v * 256) + 128.0D, (double)(shape * 256) + 128.0D, 102.4D);
            gf.fill(s);
            gl.draw(s);
         }
      }

      gf.dispose();
      gl.dispose();
      int[] f = fill.getRGB(0, 0, w, h, (int[])null, 0, w);
      byte[] printed = new byte[w * h * 2];
      byte[] shade = new byte[w * h * 2];

      for(int y = 0; y < h; ++y) {
         for(int x = 0; x < w; ++x) {
            int i = y * w + x;
            int a = f[i] >>> 24;
            double lx = (double)(x % 256) / 256.0D - 0.5D;
            double ly = (double)(y % 256) / 256.0D - 0.5D;
            double s = Math.max(0.0D, Math.min(1.0D, (lx + ly) * 1.4D + 0.15D));
            double dot = halftone(x, y, 9.0D, s * 0.75D);
            printed[i * 2] = (byte)((int)Math.round(255.0D - dot * 95.0D));
            printed[i * 2 + 1] = (byte)a;
            shade[i * 2] = (byte)((int)Math.round(255.0D - s * 0.75D * 255.0D));
            shade[i * 2 + 1] = (byte)a;
         }
      }

      return new KitTextures.Image[]{KitTextures.Image.la(printed, w, h, false), KitTextures.Image.la(shade, w, h, false), KitTextures.Image.la(mask(line, w, h), w, h, false)};
   }

   private static double halftone(int x, int y, double period, double amount) {
      double u = (double)(x + y) / Math.sqrt(2.0D) / period;
      double v = (double)(x - y) / Math.sqrt(2.0D) / period;
      double du = u - Math.floor(u) - 0.5D;
      double dv = v - Math.floor(v) - 0.5D;
      double d = Math.sqrt(du * du + dv * dv);
      double r = Math.sqrt(Math.max(0.0D, amount)) * 0.62D;
      return Math.max(0.0D, Math.min(1.0D, (r - d) * period + 0.5D));
   }

   private static Shape balloon(int shape, int seed, double cx, double cy, double r) {
      Random rnd = new Random((long)(2827 + seed * 131 + shape * 7));
      if(shape == 2) {
         Double p = new Double();
         int n = 48;
         double sq = 0.85D + 0.1D * rnd.nextDouble();

         for(int i = 0; i <= n; ++i) {
            double a = 6.283185307179586D * (double)i / (double)n;
            double rr = r * (1.0D + 0.03D * Math.sin(a * 3.0D + (double)seed));
            double x = cx + Math.cos(a) * rr;
            double y = cy + Math.sin(a) * rr * sq;
            if(i == 0) {
               p.moveTo(x, y);
            } else {
               p.lineTo(x, y);
            }
         }

         p.closePath();
         return p;
      } else if(shape != 1) {
         Double p = new Double();
         int spikes = 12 + rnd.nextInt(5);

         for(int ix = 0; ix < spikes * 2; ++ix) {
            double ang = 3.141592653589793D * (double)ix / (double)spikes + (rnd.nextDouble() - 0.5D) * 0.12D;
            double rr = (ix & 1) == 0?r * (0.86D + 0.18D * rnd.nextDouble()):r * (0.55D + 0.1D * rnd.nextDouble());
            double x = cx + Math.cos(ang) * rr;
            double y = cy + Math.sin(ang) * rr;
            if(ix == 0) {
               p.moveTo(x, y);
            } else {
               p.lineTo(x, y);
            }
         }

         p.closePath();
         return p;
      } else {
         Area a = new Area();
         int bumps = 8 + rnd.nextInt(3);

         for(int ixx = 0; ixx < bumps; ++ixx) {
            double ang = 6.283185307179586D * (double)ixx / (double)bumps + rnd.nextDouble() * 0.2D;
            double br = r * (0.36D + 0.1D * rnd.nextDouble());
            double d = r - br * 0.8D;
            a.add(new Area(new java.awt.geom.Ellipse2D.Double(cx + Math.cos(ang) * d - br, cy + Math.sin(ang) * d * 0.85D - br, br * 2.0D, br * 2.0D)));
         }

         a.add(new Area(new java.awt.geom.Ellipse2D.Double(cx - r * 0.72D, cy - r * 0.62D, r * 1.44D, r * 1.24D)));
         return a;
      }
   }

   public static int puffFill() {
      return PUFF_SHEET.texture(0);
   }

   public static int puffOutline() {
      return PUFF_SHEET.texture(1);
   }

   public static void puff(int k, double x, double y, double z, double rx, double ry, double rz, double ux, double uy, double uz, double w, double h, double turn, int fill, int outline, float outlineWidth) {
      int c = Math.floorMod(k, 4);
      float u0 = (float)c / 4.0F;
      float u1 = (float)(c + 1) / 4.0F;
      quad(puffFill(), u0, 0.0F, u1, 1.0F, x, y, z, rx, ry, rz, ux, uy, uz, w, h, turn, fill);
      outline(puffOutline(), u0, 0.0F, u1, 1.0F, x, y, z, rx, ry, rz, ux, uy, uz, w, h, turn, outline, outlineWidth);
   }

   private static KitTextures.Image[] paintPuffs() {
      int w = 1024;
      int h = 256;
      BufferedImage fill = new BufferedImage(w, h, 2);
      BufferedImage line = new BufferedImage(w, h, 2);
      Graphics2D gf = fill.createGraphics();
      Graphics2D gl = line.createGraphics();
      smooth(gf);
      smooth(gl);
      gf.setColor(Color.WHITE);
      gl.setColor(Color.WHITE);
      gl.setStroke(new BasicStroke(10.304F, 1, 1));
      Rectangle2D[] bounds = new Rectangle2D[4];

      for(int k = 0; k < 4; ++k) {
         Random rnd = new Random((long)(159 + k * 17));
         Area a = new Area();
         double cx = (double)(k * 256) + 128.0D;
         double cy = 140.8D;
         int n = 3 + rnd.nextInt(3);

         for(int i = 0; i < n; ++i) {
            double ang = 3.141592653589793D * (0.9D + 1.2D * (double)i / (double)Math.max(1, n - 1));
            double br = 256.0D * (0.17D + 0.07D * rnd.nextDouble());
            double d = 33.28D;
            a.add(new Area(new java.awt.geom.Ellipse2D.Double(cx + Math.cos(ang) * d - br, cy + Math.sin(ang) * d - br, br * 2.0D, br * 2.0D)));
         }

         a.add(new Area(new java.awt.geom.Ellipse2D.Double(cx - 76.8D, cy - 30.72D, 153.6D, 76.8D)));
         bounds[k] = a.getBounds2D();
         gf.fill(a);
         gl.draw(a);
      }

      gf.dispose();
      gl.dispose();
      int[] f = fill.getRGB(0, 0, w, h, (int[])null, 0, w);
      byte[] la = new byte[w * h * 2];

      for(int y = 0; y < h; ++y) {
         for(int x = 0; x < w; ++x) {
            int i = y * w + x;
            int k = x / 256;
            double t = ((double)y - bounds[k].getMinY()) / Math.max(1.0D, bounds[k].getHeight());
            la[i * 2] = (byte)(t > 0.68D + 0.04D * Math.sin((double)(x % 256) / 256.0D * 9.0D + (double)k)?200:255);
            la[i * 2 + 1] = (byte)(f[i] >>> 24);
         }
      }

      return new KitTextures.Image[]{KitTextures.Image.la(la, w, h, false), KitTextures.Image.la(mask(line, w, h), w, h, false)};
   }

   public static void warmUp() {
      Ui.ensureFonts();

      for(String w : BUILT_IN) {
         word(w);
      }

   }

   public static Comic.Word word(String text) {
      String key = text == null?"":text.trim();
      Comic.Word w = (Comic.Word)WORDS.get(key);
      if(w == null) {
         Ui.ensureFonts();
         final Comic.Word made = new Comic.Word(key);
         final Font font = Ui.bold.awt(220.0F);
         WORDS.put(key, made);
         painter().execute(new Runnable() {
            public void run() {
               try {
                  Comic.paint(made, font);
               } catch (LinkageError | RuntimeException var2) {
                  made.failed = true;
                  FuriahsVisuals.LOG.warn("A comic word could not be drawn ({})", new Object[]{made.text, var2});
               }

            }
         });
         return made;
      } else {
         long frame;
         if(w.fillTexture == 0 && w.fillChain != null && (frame = FxFrame.currentFrame()) != uploadFrame) {
            uploadFrame = frame;
            w.fillTexture = Textures.uploadLAChain(w.fillChain, w.width, w.height, false);
            w.lineTexture = Textures.uploadLAChain(w.lineChain, w.width, w.height, false);
            w.fillChain = (byte[][])((byte[][])null);
            w.lineChain = (byte[][])((byte[][])null);
         }

         return w;
      }
   }

   public static void lettering(Comic.Word w, double x, double y, double z, double rx, double ry, double rz, double ux, double uy, double uz, double height, double turn, int fill, int outline, float outlineWidth) {
      if(w != null && w.ready()) {
         double width = height * (double)w.aspect;
         outline(w.lineTexture, 0.0F, 0.0F, 1.0F, 1.0F, x, y, z, rx, ry, rz, ux, uy, uz, width, height, turn, outline, outlineWidth);
         quad(w.fillTexture, 0.0F, 0.0F, 1.0F, 1.0F, x, y, z, rx, ry, rz, ux, uy, uz, width, height, turn, fill);
      }

   }

   private static void paint(Comic.Word w, Font font) {
      FontRenderContext frc = new FontRenderContext((AffineTransform)null, true, true);
      GlyphVector gv = font.createGlyphVector(frc, w.text.isEmpty()?" ":w.text);
      Shape glyphs = gv.getOutline();
      Rectangle2D b = glyphs.getBounds2D();
      double stroke = 34.0D;
      double shadow = 22.0D;
      double pad = stroke + shadow + 12.0D;
      int width = Math.max(1024, (int)Math.ceil(b.getWidth() + pad * 2.0D));
      int height = (int)Math.ceil(b.getHeight() + pad * 2.0D);
      double scale = 1.0D;
      if(width > 2048) {
         scale = 2048.0D / (double)width;
         width = 2048;
         height = (int)Math.ceil((double)height * scale);
      }

      AffineTransform at = new AffineTransform();
      at.scale(scale, scale);
      at.translate(((double)width / scale - b.getWidth()) / 2.0D - b.getX(), ((double)height / scale - b.getHeight()) / 2.0D - b.getY());
      Shape placed = at.createTransformedShape(glyphs);
      BufferedImage fill = new BufferedImage(width, height, 2);
      BufferedImage line = new BufferedImage(width, height, 2);
      Graphics2D gf = fill.createGraphics();
      Graphics2D gl = line.createGraphics();
      smooth(gf);
      smooth(gl);
      gl.setColor(Color.WHITE);
      BasicStroke heavy = new BasicStroke((float)(stroke * scale), 1, 1);
      Shape shadowShape = AffineTransform.getTranslateInstance(shadow * scale, shadow * scale).createTransformedShape(placed);
      gl.fill(heavy.createStrokedShape(shadowShape));
      gl.fill(shadowShape);
      gl.fill(heavy.createStrokedShape(placed));
      gl.fill(placed);
      gf.setColor(Color.WHITE);
      gf.fill(placed);
      gf.dispose();
      gl.dispose();
      w.width = width;
      w.height = height;
      w.aspect = (float)width / (float)height;
      w.lineChain = Textures.laMipChain(mask(line, width, height), width, height);
      w.fillChain = Textures.laMipChain(mask(fill, width, height), width, height);
   }

   private static ExecutorService painter() {
      if(painter == null) {
         painter = Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
               Thread t = new Thread(r, "HAUN’T VISUALS comic words");
               t.setDaemon(true);
               t.setPriority(2);
               return t;
            }
         });
      }

      return painter;
   }

   public static String stateForTest() {
      int painted = 0;
      int uploaded = 0;
      int failed = 0;

      for(Comic.Word w : WORDS.values()) {
         if(w.ready()) {
            ++uploaded;
         } else if(w.fillChain != null) {
            ++painted;
         }

         if(w.failed) {
            ++failed;
         }
      }

      return WORDS.size() + " words: " + uploaded + " uploaded, " + painted + " painted and waiting, " + failed + " failed";
   }

   public static void quad(int tex, float u0, float v0, float u1, float v1, double x, double y, double z, double rx, double ry, double rz, double ux, double uy, double uz, double w, double h, double turn, int color) {
      if(color >>> 24 != 0) {
         double ct = Math.cos(turn);
         double st = Math.sin(turn);
         double ax = (rx * ct + ux * st) * w * 0.5D;
         double ay = (ry * ct + uy * st) * w * 0.5D;
         double az = (rz * ct + uz * st) * w * 0.5D;
         double bx = (ux * ct - rx * st) * h * 0.5D;
         double by = (uy * ct - ry * st) * h * 0.5D;
         double bz = (uz * ct - rz * st) * h * 0.5D;
         Vfx.quadUV(tex, x - ax - bx, y - ay - by, z - az - bz, (double)u0, (double)v1, color, x + ax - bx, y + ay - by, z + az - bz, (double)u1, (double)v1, color, x + ax + bx, y + ay + by, z + az + bz, (double)u1, (double)v0, color, x - ax + bx, y - ay + by, z - az + bz, (double)u0, (double)v0, color);
      }

   }

   public static void actionLines(double x, double y, double z, double rx, double ry, double rz, double ux, double uy, double uz, double inner, double outer, int count, int seed, double width, int color) {
      if(color >>> 24 != 0) {
         for(int i = 0; i < count; ++i) {
            double a = 6.283185307179586D * ((double)i + 0.5D * (double)hash(seed, i)) / (double)count;
            double ca = Math.cos(a);
            double sa = Math.sin(a);
            double r0 = inner * (0.9D + 0.25D * (double)hash(seed, i + 50));
            double r1 = outer * (0.75D + 0.3D * (double)hash(seed, i + 90));
            double dx = rx * ca + ux * sa;
            double dy = ry * ca + uy * sa;
            double dz = rz * ca + uz * sa;
            double w = width * (0.6D + 0.6D * (double)hash(seed, i + 130));

            for(int k = 0; k < 3; ++k) {
               double r = r0 + (r1 - r0) * (double)k / 2.0D;
               Vfx.X[k] = x + dx * r;
               Vfx.Y[k] = y + dy * r;
               Vfx.Z[k] = z + dz * r;
               Vfx.W[k] = (float)(w * (k == 0?0.05D:(k == 1?0.6D:1.0D)));
               Vfx.C[k] = color;
            }

            Vfx.ribbon(Textures.LINE_CORE, 3, false);
         }
      }

   }

   private static float hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (float)(h >>> 8) / 1.6777216E7F;
   }

   private static byte[] mask(BufferedImage img, int w, int h) {
      int[] px = img.getRGB(0, 0, w, h, (int[])null, 0, w);
      byte[] la = new byte[w * h * 2];

      for(int i = 0; i < px.length; ++i) {
         la[i * 2] = -1;
         la[i * 2 + 1] = (byte)(px[i] >>> 24);
      }

      return la;
   }

   private static void smooth(Graphics2D g) {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
   }

   public static final class Word {
      final String text;
      volatile byte[][] fillChain;
      volatile byte[][] lineChain;
      volatile boolean failed;
      int width;
      int height;
      int fillTexture;
      int lineTexture;
      public volatile float aspect = 3.0F;

      Word(String text) {
         this.text = text;
      }

      public boolean ready() {
         return this.fillTexture != 0;
      }

      public boolean failed() {
         return this.failed;
      }
   }
}
