package mix.client.mixui.integration.haunt.module.watermark;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Ellipse2D.Float;
import java.awt.geom.Line2D.Double;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.EnumMap;
import java.util.Map;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.module.watermark.SvgPath;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public final class IslandIcons {
   static final String DISCORD_SVG = "/assets/furiahsvisuals/discord/Discord-Symbol-Blurple.svg";
   static final int DISCORD_WHITE = -1;
   static final int DISCORD_BLURPLE = -10983950;
   static final int CELL = 128;
   static final int COLS = 8;
   static final int ROWS = 10;
   private static final int W = 1024;
   private static final int H = 1280;
   private static int texture;
   private static boolean failed;

   public static int texture() {
      if(texture == 0 && !failed) {
         try {
            texture = build();
         } catch (Throwable var1) {
            failed = true;
            FuriahsVisuals.LOG.warn("WaterMark icons could not be made", var1);
         }
      }

      return texture;
   }

   public static float u0(IslandIcons.Icon icon) {
      return (float)(icon.ordinal() % 8 * 128) / 1024.0F;
   }

   public static float v0(IslandIcons.Icon icon) {
      return (float)(icon.ordinal() / 8 * 128) / 1280.0F;
   }

   public static float du() {
      return 0.125F;
   }

   public static float dv() {
      return 0.1F;
   }

   public static void draw(IslandIcons.Icon icon, float x, float y, float size, int color) {
      int tex = texture();
      if(tex != 0) {
         Ui.image(tex, x, y, size, size, u0(icon), v0(icon), u0(icon) + du(), v0(icon) + dv(), color);
      }

   }

   public static void drawPlay(float cx, float cy, float size, int color) {
      int tex = texture();
      if(tex != 0) {
         Ui.image(tex, cx - size / 2.0F, cy - size / 2.0F, size, size, u0(IslandIcons.Icon.PLAY), v0(IslandIcons.Icon.PLAY), u0(IslandIcons.Icon.PLAY) + du(), v0(IslandIcons.Icon.PLAY) + dv(), color);
      }

   }

   public static Map<IslandIcons.Icon, BufferedImage> images() {
      BufferedImage all = paint();
      EnumMap<IslandIcons.Icon, BufferedImage> out = new EnumMap(IslandIcons.Icon.class);

      for(IslandIcons.Icon icon : IslandIcons.Icon.values()) {
         out.put(icon, all.getSubimage(icon.ordinal() % 8 * 128, icon.ordinal() / 8 * 128, 128, 128));
      }

      return out;
   }

   static BufferedImage paint() {
      BufferedImage img = new BufferedImage(1024, 1280, 2);
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setColor(Color.WHITE);

      for(IslandIcons.Icon icon : IslandIcons.Icon.values()) {
         AffineTransform saved = g.getTransform();
         g.translate(icon.ordinal() % 8 * 128, icon.ordinal() / 8 * 128);
         g.setClip(0, 0, 128, 128);
         draw(g, icon);
         g.setClip((Shape)null);
         g.setTransform(saved);
      }

      g.dispose();
      return img;
   }

   private static int build() {
      BufferedImage img = paint();
      int levels = 5;
      int[][] data = new int[levels][];
      int[] alpha = new int[1310720];
      int[] argb = img.getRGB(0, 0, 1024, 1280, (int[])null, 0, 1024);

      for(int i = 0; i < argb.length; ++i) {
         alpha[i] = argb[i] >>> 24;
      }

      int w = 1024;
      int h = 1280;

      for(int l = 0; l < levels; ++l) {
         int[] level = new int[w * h];

         for(int i = 0; i < level.length; ++i) {
            level[i] = alpha[i] << 24 | 16777215;
         }

         data[l] = level;
         if(l + 1 < levels) {
            int nw = w / 2;
            int nh = h / 2;
            int[] next = new int[nw * nh];

            for(int y = 0; y < nh; ++y) {
               for(int x = 0; x < nw; ++x) {
                  int i = y * 2 * w + x * 2;
                  next[y * nw + x] = (alpha[i] + alpha[i + 1] + alpha[i + w] + alpha[i + w + 1] + 2) / 4;
               }
            }

            alpha = next;
            w = nw;
            h = nh;
         }
      }

      int id = TextureUtil.glGenTextures();
      GlStateManager.bindTexture(id);
      GL11.glTexParameteri(3553, '脽', levels - 1);
      GL11.glTexParameteri(3553, 10241, 9987);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, '脯');
      GL11.glTexParameteri(3553, 10243, '脯');
      int var16 = 1024;
      int var17 = 1280;

      for(int l = 0; l < levels; ++l) {
         IntBuffer buffer = BufferUtils.createIntBuffer(var16 * var17);
         buffer.put(data[l]).flip();
         GL11.glTexImage2D(3553, l, '聘', var16, var17, 0, '胡', '荧', buffer);
         var16 /= 2;
         var17 /= 2;
      }

      return id;
   }

   private static BasicStroke stroke(float width) {
      return new BasicStroke(width, 1, 1);
   }

   private static void draw(Graphics2D g, IslandIcons.Icon icon) {
      switch(icon) {
      case GLOBE: {
         g.setStroke(stroke(11.0F));
         g.draw(new Float(18.0F, 18.0F, 92.0F, 92.0F));
         g.draw(new Float(44.0F, 18.0F, 40.0F, 92.0F));
         g.draw(new java.awt.geom.Line2D.Float(20.0F, 64.0F, 108.0F, 64.0F));
         break;
      }
      case MONITOR: {
         g.setStroke(stroke(11.0F));
         g.draw(new java.awt.geom.RoundRectangle2D.Float(16.0F, 22.0F, 96.0F, 66.0F, 22.0F, 22.0F));
         g.draw(new java.awt.geom.Line2D.Float(64.0F, 90.0F, 64.0F, 104.0F));
         g.draw(new java.awt.geom.Line2D.Float(42.0F, 106.0F, 86.0F, 106.0F));
         break;
      }
      case CLOCK: {
         g.setStroke(stroke(11.0F));
         g.draw(new Float(16.0F, 16.0F, 96.0F, 96.0F));
         g.draw(new java.awt.geom.Line2D.Float(64.0F, 64.0F, 64.0F, 34.0F));
         g.draw(new java.awt.geom.Line2D.Float(64.0F, 64.0F, 86.0F, 76.0F));
         break;
      }
      case SPEED: {
         g.setStroke(stroke(11.0F));
         g.draw(new java.awt.geom.Arc2D.Float(18.0F, 26.0F, 92.0F, 92.0F, -25.0F, 230.0F, 0));
         g.draw(new java.awt.geom.Line2D.Float(64.0F, 72.0F, 90.0F, 46.0F));
         g.fill(new Float(55.0F, 63.0F, 18.0F, 18.0F));
         break;
      }
      case CHIP: {
         g.setStroke(stroke(10.0F));
         g.draw(new java.awt.geom.RoundRectangle2D.Float(32.0F, 32.0F, 64.0F, 64.0F, 18.0F, 18.0F));
         g.fill(new java.awt.geom.RoundRectangle2D.Float(50.0F, 50.0F, 28.0F, 28.0F, 8.0F, 8.0F));

         for(int ixxx = 0; ixxx < 3; ++ixxx) {
            float px = (float)(46 + ixxx * 18);
            g.draw(new java.awt.geom.Line2D.Float(px, 16.0F, px, 28.0F));
            g.draw(new java.awt.geom.Line2D.Float(px, 100.0F, px, 112.0F));
            g.draw(new java.awt.geom.Line2D.Float(16.0F, px, 28.0F, px));
            g.draw(new java.awt.geom.Line2D.Float(100.0F, px, 112.0F, px));
         }

         return;
      }
      case SUN: {
         g.fill(new Float(40.0F, 40.0F, 48.0F, 48.0F));
         g.setStroke(stroke(9.0F));

         for(int ixxx = 0; ixxx < 8; ++ixxx) {
            double ax = (double)ixxx * 3.141592653589793D / 4.0D;
            g.draw(new Double(64.0D + Math.cos(ax) * 36.0D, 64.0D + Math.sin(ax) * 36.0D, 64.0D + Math.cos(ax) * 50.0D, 64.0D + Math.sin(ax) * 50.0D));
         }

         return;
      }
      case MOON: {
         Area moon = new Area(new Float(18.0F, 18.0F, 90.0F, 90.0F));
         moon.subtract(new Area(new Float(44.0F, 8.0F, 82.0F, 82.0F)));
         g.fill(moon);
         break;
      }
      case CLOUD: {
         g.fill(cloud(0.0F, 0.0F, 1.0F));
         break;
      }
      case RAIN: {
         g.fill(cloud(0.0F, -16.0F, 0.9F));
         g.setStroke(stroke(8.0F));

         for(int ixxx = 0; ixxx < 3; ++ixxx) {
            float x = (float)(40 + ixxx * 22);
            g.draw(new java.awt.geom.Line2D.Float(x + 6.0F, 90.0F, x - 2.0F, 110.0F));
         }

         return;
      }
      case SNOW: {
         g.fill(cloud(0.0F, -16.0F, 0.9F));

         for(int ixxx = 0; ixxx < 3; ++ixxx) {
            float x = (float)(38 + ixxx * 24);
            g.fill(new Float(x - 6.0F, (float)(92 + ixxx % 2 * 8), 12.0F, 12.0F));
         }

         return;
      }
      case STORM: {
         Area a = new Area(cloud(0.0F, -18.0F, 0.9F));
         java.awt.geom.Path2D.Float bolt = new java.awt.geom.Path2D.Float();
         bolt.moveTo(66.0D, 56.0D);
         bolt.lineTo(46.0D, 90.0D);
         bolt.lineTo(62.0D, 90.0D);
         bolt.lineTo(54.0D, 116.0D);
         bolt.lineTo(84.0D, 78.0D);
         bolt.lineTo(68.0D, 78.0D);
         bolt.lineTo(78.0D, 56.0D);
         bolt.closePath();
         Area gap = new Area((new BasicStroke(10.0F, 1, 1)).createStrokedShape(bolt));
         a.subtract(gap);
         g.fill(a);
         g.fill(bolt);
         break;
      }
      case NOTE: {
         AffineTransform at = AffineTransform.getRotateInstance(Math.toRadians(-20.0D), 46.0D, 92.0D);
         g.fill(at.createTransformedShape(new Float(26.0F, 78.0F, 40.0F, 28.0F)));
         g.setStroke(stroke(10.0F));
         g.draw(new java.awt.geom.Line2D.Float(63.0F, 88.0F, 63.0F, 20.0F));
         g.draw(new java.awt.geom.QuadCurve2D.Float(63.0F, 20.0F, 100.0F, 30.0F, 92.0F, 64.0F));
         break;
      }
      case PLAY: {
         java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
         p.moveTo(40.0D, 26.0D);
         p.lineTo(102.0D, 64.0D);
         p.lineTo(40.0D, 102.0D);
         p.closePath();
         g.setStroke(stroke(10.0F));
         g.draw(p);
         g.fill(p);
         break;
      }
      case PAUSE: {
         g.fill(new java.awt.geom.RoundRectangle2D.Float(32.0F, 24.0F, 22.0F, 80.0F, 12.0F, 12.0F));
         g.fill(new java.awt.geom.RoundRectangle2D.Float(74.0F, 24.0F, 22.0F, 80.0F, 12.0F, 12.0F));
         break;
      }
      case PREV: {
      case NEXT: {
         java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
         p.moveTo(100.0D, 30.0D);
         p.lineTo(46.0D, 64.0D);
         p.lineTo(100.0D, 98.0D);
         p.closePath();
         Area a = new Area(p);
         a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(26.0F, 28.0F, 16.0F, 72.0F, 10.0F, 10.0F)));
         Shape s = a;
         if(icon == IslandIcons.Icon.NEXT) {
            s = (new AffineTransform(-1.0F, 0.0F, 0.0F, 1.0F, 128.0F, 0.0F)).createTransformedShape(a);
         }

         g.setStroke(stroke(6.0F));
         g.draw(s);
         g.fill(s);
         break;
      }
      }
      case APP: {
         g.setStroke(stroke(11.0F));
         g.draw(new java.awt.geom.RoundRectangle2D.Float(18.0F, 24.0F, 92.0F, 80.0F, 22.0F, 22.0F));
         g.draw(new java.awt.geom.Line2D.Float(20.0F, 48.0F, 108.0F, 48.0F));
         g.fill(new Float(30.0F, 32.0F, 8.0F, 8.0F));
         g.fill(new Float(42.0F, 32.0F, 8.0F, 8.0F));
         break;
      }
      case AURA: {
         g.setStroke(stroke(8.0F));
         g.draw(new Float(22.0F, 22.0F, 84.0F, 84.0F));
         g.fill(new Float(50.0F, 50.0F, 28.0F, 28.0F));
         g.draw(new java.awt.geom.Line2D.Float(64.0F, 10.0F, 64.0F, 22.0F));
         g.draw(new java.awt.geom.Line2D.Float(64.0F, 106.0F, 64.0F, 118.0F));
         g.draw(new java.awt.geom.Line2D.Float(10.0F, 64.0F, 22.0F, 64.0F));
         g.draw(new java.awt.geom.Line2D.Float(106.0F, 64.0F, 118.0F, 64.0F));
         break;
      }
      case HIT: {
         GeneralPath p = new GeneralPath();

         for(int ixx = 0; ixx < 16; ++ixx) {
            double ax = (double)ixx * 3.141592653589793D / 8.0D - 1.5707963267948966D;
            double r = ixx % 2 == 0?52.0D:24.0D;
            double x = 64.0D + Math.cos(ax) * r;
            double y = 64.0D + Math.sin(ax) * r;
            if(ixx == 0) {
               p.moveTo(x, y);
            } else {
               p.lineTo(x, y);
            }
         }

         p.closePath();
         g.fill(p);
         break;
      }
      case LANDING: {
         g.setStroke(stroke(9.0F));
         g.draw(new java.awt.geom.Line2D.Float(64.0F, 16.0F, 64.0F, 70.0F));
         java.awt.geom.Path2D.Float head = new java.awt.geom.Path2D.Float();
         head.moveTo(44.0D, 54.0D);
         head.lineTo(64.0D, 76.0D);
         head.lineTo(84.0D, 54.0D);
         g.draw(head);
         g.draw(new java.awt.geom.Arc2D.Float(18.0F, 80.0F, 92.0F, 34.0F, 0.0F, -180.0F, 0));
         g.draw(new java.awt.geom.Line2D.Float(26.0F, 88.0F, 16.0F, 80.0F));
         g.draw(new java.awt.geom.Line2D.Float(102.0F, 88.0F, 112.0F, 80.0F));
         break;
      }
      case GLOW: {
         g.fill(sparkle(58.0F, 66.0F, 46.0F, 12.0F));
         g.fill(sparkle(96.0F, 30.0F, 18.0F, 5.0F));
         break;
      }
      case PARTICLES: {
         g.fill(new Float(24.0F, 60.0F, 26.0F, 26.0F));
         g.fill(new Float(60.0F, 28.0F, 20.0F, 20.0F));
         g.fill(new Float(78.0F, 70.0F, 30.0F, 30.0F));
         g.fill(new Float(40.0F, 96.0F, 14.0F, 14.0F));
         g.fill(new Float(92.0F, 34.0F, 12.0F, 12.0F));
         break;
      }
      case SKY: {
         Area moon = new Area(new Float(18.0F, 34.0F, 70.0F, 70.0F));
         moon.subtract(new Area(new Float(40.0F, 26.0F, 64.0F, 64.0F)));
         g.fill(moon);
         g.fill(sparkle(92.0F, 36.0F, 20.0F, 5.0F));
         g.fill(sparkle(104.0F, 80.0F, 13.0F, 3.5F));
         break;
      }
      case GEAR: {
         Area gear = new Area(new Float(28.0F, 28.0F, 72.0F, 72.0F));

         for(int ixx = 0; ixx < 8; ++ixx) {
            AffineTransform atx = AffineTransform.getRotateInstance((double)ixx * 3.141592653589793D / 4.0D, 64.0D, 64.0D);
            gear.add(new Area(atx.createTransformedShape(new java.awt.geom.RoundRectangle2D.Float(55.0F, 12.0F, 18.0F, 30.0F, 8.0F, 8.0F))));
         }

         gear.subtract(new Area(new Float(47.0F, 47.0F, 34.0F, 34.0F)));
         g.fill(gear);
         break;
      }
      case LAYERS: {
         g.fill(new java.awt.geom.RoundRectangle2D.Float(22.0F, 22.0F, 84.0F, 22.0F, 20.0F, 20.0F));
         g.setStroke(stroke(8.0F));
         g.draw(new java.awt.geom.RoundRectangle2D.Float(26.0F, 58.0F, 76.0F, 16.0F, 16.0F, 16.0F));
         g.draw(new java.awt.geom.RoundRectangle2D.Float(32.0F, 90.0F, 64.0F, 14.0F, 14.0F, 14.0F));
         break;
      }
      case COMPASS: {
         g.setStroke(stroke(10.0F));
         g.draw(new Float(18.0F, 18.0F, 92.0F, 92.0F));
         java.awt.geom.Path2D.Float n = new java.awt.geom.Path2D.Float();
         n.moveTo(64.0D, 30.0D);
         n.lineTo(78.0D, 64.0D);
         n.lineTo(50.0D, 64.0D);
         n.closePath();
         g.fill(n);
         java.awt.geom.Path2D.Float s = new java.awt.geom.Path2D.Float();
         s.moveTo(64.0D, 98.0D);
         s.lineTo(78.0D, 64.0D);
         s.lineTo(50.0D, 64.0D);
         s.closePath();
         g.draw(s);
         break;
      }
      case SIGNAL: {
         for(int ixx = 0; ixx < 4; ++ixx) {
            float hx = (float)(26 + ixx * 22);
            g.fill(new java.awt.geom.RoundRectangle2D.Float((float)(20 + ixx * 26), 110.0F - hx, 18.0F, hx, 8.0F, 8.0F));
         }

         return;
      }
      case WIFI: {
         g.fill(new Float(54.0F, 90.0F, 20.0F, 20.0F));
         g.setStroke(stroke(11.0F));

         for(int ixx = 0; ixx < 3; ++ixx) {
            float r = (float)(30 + ixx * 24);
            g.draw(new java.awt.geom.Arc2D.Float(64.0F - r, 100.0F - r, 2.0F * r, 2.0F * r, 45.0F, 90.0F, 0));
         }

         return;
      }
      case CHART: {
         g.fill(new java.awt.geom.RoundRectangle2D.Float(20.0F, 66.0F, 24.0F, 44.0F, 9.0F, 9.0F));
         g.fill(new java.awt.geom.RoundRectangle2D.Float(52.0F, 42.0F, 24.0F, 68.0F, 9.0F, 9.0F));
         g.fill(new java.awt.geom.RoundRectangle2D.Float(84.0F, 18.0F, 24.0F, 92.0F, 9.0F, 9.0F));
         break;
      }
      case BOLT: {
         java.awt.geom.Path2D.Float b = new java.awt.geom.Path2D.Float();
         b.moveTo(72.0D, 10.0D);
         b.lineTo(32.0D, 70.0D);
         b.lineTo(60.0D, 70.0D);
         b.lineTo(50.0D, 118.0D);
         b.lineTo(96.0D, 54.0D);
         b.lineTo(68.0D, 54.0D);
         b.lineTo(84.0D, 10.0D);
         b.closePath();
         g.setStroke(stroke(5.0F));
         g.draw(b);
         g.fill(b);
         break;
      }
      case SERVER: {
         g.setStroke(stroke(10.0F));
         g.draw(new java.awt.geom.RoundRectangle2D.Float(20.0F, 20.0F, 88.0F, 38.0F, 16.0F, 16.0F));
         g.draw(new java.awt.geom.RoundRectangle2D.Float(20.0F, 70.0F, 88.0F, 38.0F, 16.0F, 16.0F));
         g.fill(new Float(32.0F, 33.0F, 13.0F, 13.0F));
         g.fill(new Float(32.0F, 83.0F, 13.0F, 13.0F));
         g.draw(new java.awt.geom.Line2D.Float(60.0F, 39.0F, 92.0F, 39.0F));
         g.draw(new java.awt.geom.Line2D.Float(60.0F, 89.0F, 92.0F, 89.0F));
         break;
      }
      case HOURGLASS: {
         g.setStroke(stroke(10.0F));
         g.draw(new java.awt.geom.Line2D.Float(28.0F, 16.0F, 100.0F, 16.0F));
         g.draw(new java.awt.geom.Line2D.Float(28.0F, 112.0F, 100.0F, 112.0F));
         java.awt.geom.Path2D.Float glass = new java.awt.geom.Path2D.Float();
         glass.moveTo(38.0D, 18.0D);
         glass.lineTo(90.0D, 18.0D);
         glass.lineTo(69.0D, 64.0D);
         glass.lineTo(90.0D, 110.0D);
         glass.lineTo(38.0D, 110.0D);
         glass.lineTo(59.0D, 64.0D);
         glass.closePath();
         g.setStroke(stroke(8.0F));
         g.draw(glass);
         java.awt.geom.Path2D.Float sand = new java.awt.geom.Path2D.Float();
         sand.moveTo(47.0D, 104.0D);
         sand.lineTo(81.0D, 104.0D);
         sand.lineTo(64.0D, 80.0D);
         sand.closePath();
         g.fill(sand);
         break;
      }
      case THERMO: {
         Area t = new Area(new java.awt.geom.RoundRectangle2D.Float(50.0F, 12.0F, 28.0F, 72.0F, 28.0F, 28.0F));
         t.add(new Area(new Float(40.0F, 66.0F, 48.0F, 48.0F)));
         g.setStroke(stroke(9.0F));
         g.draw(t);
         g.fill(new Float(52.0F, 78.0F, 24.0F, 24.0F));
         g.fill(new java.awt.geom.RoundRectangle2D.Float(60.0F, 36.0F, 8.0F, 50.0F, 8.0F, 8.0F));
         break;
      }
      case PIN: {
         Area pin = new Area(new Float(28.0F, 12.0F, 72.0F, 72.0F));
         java.awt.geom.Path2D.Float tip = new java.awt.geom.Path2D.Float();
         tip.moveTo(32.0D, 60.0D);
         tip.lineTo(64.0D, 118.0D);
         tip.lineTo(96.0D, 60.0D);
         tip.closePath();
         pin.add(new Area(tip));
         pin.subtract(new Area(new Float(49.0F, 33.0F, 30.0F, 30.0F)));
         g.fill(pin);
         break;
      }
      case HEART: {
         java.awt.geom.Path2D.Float h = new java.awt.geom.Path2D.Float();
         h.moveTo(64.0D, 112.0D);
         h.curveTo(22.0D, 82.0D, 10.0D, 56.0D, 24.0D, 34.0D);
         h.curveTo(38.0D, 14.0D, 58.0D, 18.0D, 64.0D, 38.0D);
         h.curveTo(70.0D, 18.0D, 90.0D, 14.0D, 104.0D, 34.0D);
         h.curveTo(118.0D, 56.0D, 106.0D, 82.0D, 64.0D, 112.0D);
         h.closePath();
         g.fill(h);
         break;
      }
      case STAR: {
         GeneralPath p = new GeneralPath();

         for(int ix = 0; ix < 10; ++ix) {
            double ax = -1.5707963267948966D + (double)ix * 3.141592653589793D / 5.0D;
            double r = ix % 2 == 0?56.0D:23.0D;
            double x = 64.0D + Math.cos(ax) * r;
            double y = 68.0D + Math.sin(ax) * r;
            if(ix == 0) {
               p.moveTo(x, y);
            } else {
               p.lineTo(x, y);
            }
         }

         p.closePath();
         g.setStroke(stroke(6.0F));
         g.draw(p);
         g.fill(p);
         break;
      }
      case CROWN: {
         java.awt.geom.Path2D.Float c = new java.awt.geom.Path2D.Float();
         c.moveTo(20.0D, 94.0D);
         c.lineTo(22.0D, 40.0D);
         c.lineTo(45.0D, 66.0D);
         c.lineTo(64.0D, 30.0D);
         c.lineTo(83.0D, 66.0D);
         c.lineTo(106.0D, 40.0D);
         c.lineTo(108.0D, 94.0D);
         c.closePath();
         g.setStroke(stroke(6.0F));
         g.draw(c);
         g.fill(c);
         g.fill(new java.awt.geom.RoundRectangle2D.Float(20.0F, 102.0F, 88.0F, 12.0F, 6.0F, 6.0F));
         g.fill(new Float(15.0F, 31.0F, 14.0F, 14.0F));
         g.fill(new Float(57.0F, 20.0F, 14.0F, 14.0F));
         g.fill(new Float(99.0F, 31.0F, 14.0F, 14.0F));
         break;
      }
      case FLAME: {
         java.awt.geom.Path2D.Float f = new java.awt.geom.Path2D.Float();
         f.moveTo(64.0D, 10.0D);
         f.curveTo(100.0D, 44.0D, 108.0D, 68.0D, 101.0D, 88.0D);
         f.curveTo(95.0D, 108.0D, 80.0D, 118.0D, 64.0D, 118.0D);
         f.curveTo(46.0D, 118.0D, 32.0D, 106.0D, 27.0D, 88.0D);
         f.curveTo(22.0D, 70.0D, 34.0D, 56.0D, 45.0D, 40.0D);
         f.curveTo(47.0D, 56.0D, 53.0D, 62.0D, 59.0D, 64.0D);
         f.curveTo(57.0D, 44.0D, 58.0D, 24.0D, 64.0D, 10.0D);
         f.closePath();
         Area flame = new Area(f);
         java.awt.geom.Path2D.Float core = new java.awt.geom.Path2D.Float();
         core.moveTo(64.0D, 72.0D);
         core.curveTo(80.0D, 88.0D, 82.0D, 98.0D, 76.0D, 106.0D);
         core.curveTo(71.0D, 112.0D, 57.0D, 112.0D, 52.0D, 106.0D);
         core.curveTo(46.0D, 98.0D, 50.0D, 86.0D, 64.0D, 72.0D);
         core.closePath();
         flame.subtract(new Area(core));
         g.fill(flame);
         break;
      }
      case SWORD: {
         Area s = new Area();
         java.awt.geom.Path2D.Float blade = new java.awt.geom.Path2D.Float();
         blade.moveTo(64.0D, 8.0D);
         blade.lineTo(73.0D, 20.0D);
         blade.lineTo(73.0D, 80.0D);
         blade.lineTo(55.0D, 80.0D);
         blade.lineTo(55.0D, 20.0D);
         blade.closePath();
         s.add(new Area(blade));
         s.add(new Area(new java.awt.geom.RoundRectangle2D.Float(38.0F, 80.0F, 52.0F, 11.0F, 6.0F, 6.0F)));
         s.add(new Area(new java.awt.geom.RoundRectangle2D.Float(58.0F, 90.0F, 12.0F, 18.0F, 4.0F, 4.0F)));
         s.add(new Area(new Float(56.0F, 104.0F, 16.0F, 16.0F)));
         g.fill(AffineTransform.getRotateInstance(Math.toRadians(45.0D), 64.0D, 64.0D).createTransformedShape(s));
         break;
      }
      case GEM: {
         java.awt.geom.Path2D.Float d = new java.awt.geom.Path2D.Float();
         d.moveTo(36.0D, 22.0D);
         d.lineTo(92.0D, 22.0D);
         d.lineTo(114.0D, 48.0D);
         d.lineTo(64.0D, 112.0D);
         d.lineTo(14.0D, 48.0D);
         d.closePath();
         Area gem = new Area(d);
         java.awt.geom.Path2D.Float facets = new java.awt.geom.Path2D.Float();
         facets.moveTo(14.0D, 48.0D);
         facets.lineTo(114.0D, 48.0D);
         facets.moveTo(48.0D, 24.0D);
         facets.lineTo(40.0D, 48.0D);
         facets.lineTo(64.0D, 110.0D);
         facets.moveTo(80.0D, 24.0D);
         facets.lineTo(88.0D, 48.0D);
         facets.lineTo(64.0D, 110.0D);
         gem.subtract(new Area((new BasicStroke(5.0F, 0, 1)).createStrokedShape(facets)));
         g.fill(gem);
         break;
      }
      case ARROWS: {
         g.setStroke(stroke(14.0F));
         java.awt.geom.Path2D.Float a = new java.awt.geom.Path2D.Float();
         a.moveTo(24.0D, 30.0D);
         a.lineTo(56.0D, 64.0D);
         a.lineTo(24.0D, 98.0D);
         a.moveTo(66.0D, 30.0D);
         a.lineTo(98.0D, 64.0D);
         a.lineTo(66.0D, 98.0D);
         g.draw(a);
         break;
      }
      case PARTLY: {
         Shape cloud = cloud(12.0F, 18.0F, 0.8F);
         Area sun = new Area(new Float(16.0F, 16.0F, 52.0F, 52.0F));
         sun.subtract(new Area((new BasicStroke(12.0F, 1, 1)).createStrokedShape(cloud)));
         sun.subtract(new Area(cloud));
         g.fill(sun);
         g.fill(cloud);
         break;
      }
      case WIND: {
         g.setStroke(stroke(10.0F));
         java.awt.geom.Path2D.Float top = new java.awt.geom.Path2D.Float();
         top.moveTo(18.0D, 50.0D);
         top.lineTo(78.0D, 50.0D);
         top.curveTo(96.0D, 50.0D, 102.0D, 28.0D, 86.0D, 22.0D);
         top.curveTo(76.0D, 18.0D, 68.0D, 26.0D, 72.0D, 34.0D);
         g.draw(top);
         java.awt.geom.Path2D.Float middle = new java.awt.geom.Path2D.Float();
         middle.moveTo(18.0D, 72.0D);
         middle.lineTo(94.0D, 72.0D);
         middle.curveTo(110.0D, 72.0D, 114.0D, 94.0D, 100.0D, 100.0D);
         middle.curveTo(90.0D, 104.0D, 82.0D, 96.0D, 86.0D, 88.0D);
         g.draw(middle);
         g.draw(new java.awt.geom.Line2D.Float(30.0F, 94.0F, 64.0F, 94.0F));
         break;
      }
      case SKULL: {
         Area skull = new Area(new Float(20.0F, 14.0F, 88.0F, 80.0F));
         skull.add(new Area(new java.awt.geom.RoundRectangle2D.Float(38.0F, 70.0F, 52.0F, 40.0F, 18.0F, 18.0F)));
         skull.subtract(new Area(new Float(34.0F, 44.0F, 24.0F, 26.0F)));
         skull.subtract(new Area(new Float(70.0F, 44.0F, 24.0F, 26.0F)));
         java.awt.geom.Path2D.Float nose = new java.awt.geom.Path2D.Float();
         nose.moveTo(64.0D, 70.0D);
         nose.lineTo(56.0D, 84.0D);
         nose.lineTo(72.0D, 84.0D);
         nose.closePath();
         skull.subtract(new Area(nose));

         for(int ix = 0; ix < 2; ++ix) {
            float x = (float)(54 + ix * 14);
            skull.subtract(new Area(new java.awt.geom.RoundRectangle2D.Float(x, 94.0F, 6.0F, 18.0F, 4.0F, 4.0F)));
         }

         g.fill(skull);
         break;
      }
      case DISCORD: {
         g.fill(discordLogo());
         break;
      }
      case MIC: {
         g.fill(mic());
         break;
      }
      case MIC_OFF: {
         g.fill(slashed(mic()));
         break;
      }
      case HEADSET: {
         g.fill(headset());
         break;
      }
      case HEADSET_OFF: {
         g.fill(slashed(headset()));
         break;
      }
      case HANGUP: {
         Area h = new Area(stroke(20.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(18.0F, 38.0F, 92.0F, 64.0F, 28.0F, 124.0F, 0)));
         h.add(new Area(AffineTransform.getRotateInstance(Math.toRadians(-28.0D), 28.0D, 76.0D).createTransformedShape(new java.awt.geom.RoundRectangle2D.Float(12.0F, 64.0F, 32.0F, 26.0F, 12.0F, 12.0F))));
         h.add(new Area(AffineTransform.getRotateInstance(Math.toRadians(28.0D), 100.0D, 76.0D).createTransformedShape(new java.awt.geom.RoundRectangle2D.Float(84.0F, 64.0F, 32.0F, 26.0F, 12.0F, 12.0F))));
         g.fill(h);
         break;
      }
      case CHAT: {
         Area b = new Area(new java.awt.geom.RoundRectangle2D.Float(14.0F, 18.0F, 100.0F, 74.0F, 40.0F, 40.0F));
         java.awt.geom.Path2D.Float tail = new java.awt.geom.Path2D.Float();
         tail.moveTo(30.0D, 80.0D);
         tail.lineTo(22.0D, 114.0D);
         tail.lineTo(60.0D, 88.0D);
         tail.closePath();
         b.add(new Area(tail));

         for(int ix = 0; ix < 3; ++ix) {
            b.subtract(new Area(new Float((float)(35 + ix * 23), 46.0F, 14.0F, 14.0F)));
         }

         g.fill(b);
         break;
      }
      case AT: {
         Area a = new Area(stroke(11.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(16.0F, 16.0F, 96.0F, 96.0F, -38.0F, 318.0F, 0)));
         a.add(new Area(stroke(11.0F).createStrokedShape(new Float(44.0F, 42.0F, 40.0F, 44.0F))));
         a.add(new Area(stroke(11.0F).createStrokedShape(new java.awt.geom.Line2D.Float(84.0F, 46.0F, 84.0F, 76.0F))));
         a.add(new Area(stroke(11.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(84.0F, 58.0F, 24.0F, 36.0F, 180.0F, 150.0F, 0))));
         g.fill(a);
         break;
      }
      case SPEAKER: {
         g.fill(speaker(true));
         break;
      }
      case SPEAKER_OFF: {
         g.fill(slashed(new Area(AffineTransform.getTranslateInstance(20.0D, 0.0D).createTransformedShape(speaker(false)))));
         break;
      }
      case UI: {
         Area a = new Area(stroke(10.0F).createStrokedShape(new java.awt.geom.RoundRectangle2D.Float(16.0F, 22.0F, 96.0F, 84.0F, 24.0F, 24.0F)));
         a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(32.0F, 84.0F, 64.0F, 10.0F, 10.0F, 10.0F)));
         a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(72.0F, 36.0F, 26.0F, 30.0F, 10.0F, 10.0F)));
         g.fill(a);
         break;
      }
      case CROSSHAIR: {
         g.setStroke(stroke(12.0F));
         g.draw(new java.awt.geom.Line2D.Float(64.0F, 16.0F, 64.0F, 44.0F));
         g.draw(new java.awt.geom.Line2D.Float(64.0F, 84.0F, 64.0F, 112.0F));
         g.draw(new java.awt.geom.Line2D.Float(16.0F, 64.0F, 44.0F, 64.0F));
         g.draw(new java.awt.geom.Line2D.Float(84.0F, 64.0F, 112.0F, 64.0F));
         g.fill(new Float(56.0F, 56.0F, 16.0F, 16.0F));
         break;
      }
      case SCOREBOARD: {
         Area a = new Area(stroke(9.0F).createStrokedShape(new java.awt.geom.RoundRectangle2D.Float(22.0F, 16.0F, 84.0F, 96.0F, 20.0F, 20.0F)));
         a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(22.0F, 16.0F, 84.0F, 24.0F, 20.0F, 20.0F)));

         for(int ix = 0; ix < 3; ++ix) {
            float y = (float)(54 + ix * 18);
            a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(36.0F, y, 40.0F, 9.0F, 9.0F, 9.0F)));
            a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(82.0F, y, 12.0F, 9.0F, 9.0F, 9.0F)));
         }

         g.fill(a);
         break;
      }
      case TABLIST: {
         Area back = new Area(new Float(70.0F, 28.0F, 30.0F, 30.0F));
         back.add(new Area(new java.awt.geom.Arc2D.Float(58.0F, 64.0F, 54.0F, 56.0F, 0.0F, 180.0F, 2)));
         Area front = new Area(new Float(26.0F, 22.0F, 36.0F, 36.0F));
         front.add(new Area(new java.awt.geom.Arc2D.Float(14.0F, 64.0F, 60.0F, 64.0F, 0.0F, 180.0F, 2)));
         back.subtract(new Area(stroke(12.0F).createStrokedShape(front)));
         back.add(front);
         g.fill(back);
         break;
      }
      case NAMETAG: {
         java.awt.geom.Path2D.Float tag = new java.awt.geom.Path2D.Float();
         tag.moveTo(18.0D, 40.0D);
         tag.lineTo(78.0D, 40.0D);
         tag.lineTo(112.0D, 64.0D);
         tag.lineTo(78.0D, 88.0D);
         tag.lineTo(18.0D, 88.0D);
         tag.closePath();
         Area a = new Area(stroke(10.0F).createStrokedShape(tag));
         a.add(new Area(tag));
         a.subtract(new Area(new Float(76.0F, 56.0F, 16.0F, 16.0F)));
         a.subtract(new Area(new java.awt.geom.RoundRectangle2D.Float(30.0F, 58.0F, 34.0F, 12.0F, 12.0F, 12.0F)));
         g.fill(a);
         break;
      }
      case HOTBAR: {
         Area a = new Area();

         for(int i = 0; i < 4; ++i) {
            float x = (float)(14 + i * 26);
            if(i == 1) {
               a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(x - 3.0F, 61.0F, 30.0F, 44.0F, 12.0F, 12.0F)));
            } else {
               a.add(new Area(stroke(7.0F).createStrokedShape(new java.awt.geom.RoundRectangle2D.Float(x, 64.0F, 24.0F, 38.0F, 10.0F, 10.0F))));
            }
         }

         for(int var19 = 0; var19 < 3; ++var19) {
            a.add(new Area(heart((float)(30 + var19 * 34), 34.0F, 14.0F)));
         }

         g.fill(a);
         break;
      }
      case BACK: {
      case FORWARD: {
         java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
         p.moveTo(80.0D, 26.0D);
         p.lineTo(42.0D, 64.0D);
         p.lineTo(80.0D, 102.0D);
         Shape s = (Shape)(icon == IslandIcons.Icon.FORWARD?(new AffineTransform(-1.0F, 0.0F, 0.0F, 1.0F, 128.0F, 0.0F)).createTransformedShape(p):p);
         g.setStroke(stroke(14.0F));
         g.draw(s);
         break;
      }
      }
      case ARMOR: {
         java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
         p.moveTo(32.0D, 20.0D);
         p.lineTo(48.0D, 20.0D);
         p.curveTo(50.0D, 34.0D, 78.0D, 34.0D, 80.0D, 20.0D);
         p.lineTo(96.0D, 20.0D);
         p.lineTo(116.0D, 44.0D);
         p.lineTo(98.0D, 58.0D);
         p.lineTo(94.0D, 52.0D);
         p.lineTo(94.0D, 104.0D);
         p.quadTo(94.0D, 110.0D, 88.0D, 110.0D);
         p.lineTo(40.0D, 110.0D);
         p.quadTo(34.0D, 110.0D, 34.0D, 104.0D);
         p.lineTo(34.0D, 52.0D);
         p.lineTo(30.0D, 58.0D);
         p.lineTo(12.0D, 44.0D);
         p.closePath();
         Area a = new Area(p);
         a.subtract(new Area(new java.awt.geom.RoundRectangle2D.Float(60.0F, 46.0F, 8.0F, 52.0F, 8.0F, 8.0F)));
         g.fill(a);
         break;
      }
      case SHAKE: {
         Area a = new Area(stroke(9.0F).createStrokedShape(new java.awt.geom.RoundRectangle2D.Float(32.0F, 40.0F, 64.0F, 52.0F, 16.0F, 16.0F)));
         a.add(new Area(stroke(9.0F).createStrokedShape(new Float(53.0F, 55.0F, 22.0F, 22.0F))));
         a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(44.0F, 30.0F, 20.0F, 14.0F, 6.0F, 6.0F)));
         a.add(new Area(stroke(8.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(6.0F, 42.0F, 26.0F, 48.0F, 120.0F, 120.0F, 0))));
         a.add(new Area(stroke(8.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(96.0F, 42.0F, 26.0F, 48.0F, -60.0F, 120.0F, 0))));
         g.fill(a);
         break;
      }
      case SKIN: {
         Area a = new Area(new java.awt.geom.RoundRectangle2D.Float(42.0F, 12.0F, 44.0F, 44.0F, 10.0F, 10.0F));
         a.subtract(new Area(new java.awt.geom.RoundRectangle2D.Float(50.0F, 30.0F, 9.0F, 9.0F, 2.0F, 2.0F)));
         a.subtract(new Area(new java.awt.geom.RoundRectangle2D.Float(69.0F, 30.0F, 9.0F, 9.0F, 2.0F, 2.0F)));
         a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(22.0F, 62.0F, 84.0F, 50.0F, 24.0F, 24.0F)));
         g.fill(a);
         break;
      }
      case PALETTE: {
         java.awt.geom.Path2D.Float drop = new java.awt.geom.Path2D.Float();
         drop.moveTo(64.0D, 12.0D);
         drop.curveTo(64.0D, 12.0D, 100.0D, 52.0D, 100.0D, 78.0D);
         drop.curveTo(100.0D, 98.0D, 84.0D, 114.0D, 64.0D, 114.0D);
         drop.curveTo(44.0D, 114.0D, 28.0D, 98.0D, 28.0D, 78.0D);
         drop.curveTo(28.0D, 52.0D, 64.0D, 12.0D, 64.0D, 12.0D);
         drop.closePath();
         Area a = new Area(stroke(9.0F).createStrokedShape(drop));
         Area half = new Area(drop);
         half.intersect(new Area(new java.awt.geom.RoundRectangle2D.Float(0.0F, 0.0F, 64.0F, 128.0F, 0.0F, 0.0F)));
         a.add(half);
         g.fill(a);
         break;
      }
      case LIST: {
         g.setStroke(stroke(12.0F));

         for(int i = 0; i < 3; ++i) {
            float y = (float)(34 + i * 30);
            g.fill(new Float(16.0F, y - 9.0F, 18.0F, 18.0F));
            g.draw(new java.awt.geom.Line2D.Float(50.0F, y, 110.0F, y));
         }

         return;
      }
      case MOTION: {
         g.fill(new Float(66.0F, 38.0F, 50.0F, 50.0F));
         g.setStroke(stroke(10.0F));
         g.draw(new java.awt.geom.Line2D.Float(14.0F, 46.0F, 50.0F, 46.0F));
         g.draw(new java.awt.geom.Line2D.Float(26.0F, 63.0F, 56.0F, 63.0F));
         g.draw(new java.awt.geom.Line2D.Float(14.0F, 80.0F, 50.0F, 80.0F));
         break;
      }
      case TRAIL: {
         java.awt.geom.Path2D.Float band = new java.awt.geom.Path2D.Float();
         band.moveTo(12.0D, 112.0D);
         band.curveTo(40.0D, 108.0D, 66.0D, 86.0D, 84.0D, 50.0D);
         band.lineTo(96.0D, 58.0D);
         band.curveTo(76.0D, 96.0D, 46.0D, 114.0D, 12.0D, 112.0D);
         band.closePath();
         Area a = new Area(band);
         a.add(new Area(new Float(78.0F, 18.0F, 34.0F, 34.0F)));
         a.add(new Area(stroke(7.0F).createStrokedShape(new java.awt.geom.QuadCurve2D.Float(18.0F, 88.0F, 44.0F, 80.0F, 62.0F, 54.0F))));
         g.fill(a);
      }

   }

   private static Shape heart(float cx, float cy, float r) {
      java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
      p.moveTo(cx, cy + r);
      p.curveTo(cx - r * 1.6F, cy - r * 0.1F, cx - r * 0.9F, cy - r * 1.3F, cx, cy - r * 0.45F);
      p.curveTo(cx + r * 0.9F, cy - r * 1.3F, cx + r * 1.6F, cy - r * 0.1F, cx, cy + r);
      p.closePath();
      return p;
      }
   }

   private static Shape discordLogo() {
      try {
         SvgPath svg = SvgPath.resource("/assets/furiahsvisuals/discord/Discord-Symbol-Blurple.svg");
         double[] box = svg.viewBox;
         double scale = Math.min(100.0D / box[2], 100.0D / box[3]);
         AffineTransform at = new AffineTransform();
         at.translate(64.0D - box[2] * scale / 2.0D, 64.0D - box[3] * scale / 2.0D);
         at.scale(scale, scale);
         at.translate(-box[0], -box[1]);
         return at.createTransformedShape(svg.path);
      } catch (IOException var5) {
         FuriahsVisuals.LOG.warn("WaterMark: Discord\'s logo could not be read", var5);
         return new java.awt.geom.RoundRectangle2D.Float(24.0F, 34.0F, 80.0F, 60.0F, 24.0F, 24.0F);
      }
   }

   private static Area mic() {
      Area a = new Area(new java.awt.geom.RoundRectangle2D.Float(45.0F, 12.0F, 38.0F, 68.0F, 38.0F, 38.0F));
      a.add(new Area(stroke(10.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(29.0F, 30.0F, 70.0F, 68.0F, 180.0F, 180.0F, 0))));
      a.add(new Area(stroke(10.0F).createStrokedShape(new java.awt.geom.Line2D.Float(64.0F, 98.0F, 64.0F, 112.0F))));
      a.add(new Area(stroke(10.0F).createStrokedShape(new java.awt.geom.Line2D.Float(46.0F, 114.0F, 82.0F, 114.0F))));
      return a;
   }

   private static Area headset() {
      Area a = new Area(stroke(11.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(20.0F, 20.0F, 88.0F, 88.0F, 0.0F, 180.0F, 0)));
      a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(14.0F, 62.0F, 28.0F, 46.0F, 14.0F, 14.0F)));
      a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(86.0F, 62.0F, 28.0F, 46.0F, 14.0F, 14.0F)));
      return a;
   }

   private static Area speaker(boolean waves) {
      java.awt.geom.Path2D.Float cone = new java.awt.geom.Path2D.Float();
      cone.moveTo(18.0D, 48.0D);
      cone.lineTo(40.0D, 48.0D);
      cone.lineTo(66.0D, 22.0D);
      cone.lineTo(66.0D, 106.0D);
      cone.lineTo(40.0D, 80.0D);
      cone.lineTo(18.0D, 80.0D);
      cone.closePath();
      Area a = new Area(stroke(6.0F).createStrokedShape(cone));
      a.add(new Area(cone));
      if(waves) {
         a.add(new Area(stroke(9.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(58.0F, 42.0F, 32.0F, 44.0F, -55.0F, 110.0F, 0))));
         a.add(new Area(stroke(9.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(56.0F, 24.0F, 58.0F, 80.0F, -55.0F, 110.0F, 0))));
      }

      return a;
   }

   private static Area slashed(Area icon) {
      java.awt.geom.Line2D.Float line = new java.awt.geom.Line2D.Float(22.0F, 16.0F, 106.0F, 112.0F);
      Area a = new Area(icon);
      a.subtract(new Area(stroke(26.0F).createStrokedShape(line)));
      a.add(new Area(stroke(11.0F).createStrokedShape(line)));
      return a;
   }

   private static Shape cloud(float dx, float dy, float s) {
      Area a = new Area(new Float(20.0F, 58.0F, 40.0F, 40.0F));
      a.add(new Area(new Float(38.0F, 34.0F, 54.0F, 54.0F)));
      a.add(new Area(new Float(70.0F, 52.0F, 40.0F, 40.0F)));
      a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(24.0F, 66.0F, 82.0F, 32.0F, 30.0F, 30.0F)));
      AffineTransform at = new AffineTransform();
      at.translate((double)(64.0F + dx), (double)(66.0F + dy));
      at.scale((double)s, (double)s);
      at.translate(-64.0D, -66.0D);
      return at.createTransformedShape(a);
   }

   private static Shape sparkle(float cx, float cy, float outer, float inner) {
      GeneralPath p = new GeneralPath();

      for(int i = 0; i < 8; ++i) {
         double a = (double)i * 3.141592653589793D / 4.0D - 1.5707963267948966D;
         double r = i % 2 == 0?(double)outer:(double)inner;
         double x = (double)cx + Math.cos(a) * r;
         double y = (double)cy + Math.sin(a) * r;
         if(i == 0) {
            p.moveTo(x, y);
         } else {
            p.lineTo(x, y);
         }
      }

      p.closePath();
      return p;
   }

   public static enum Icon {
      GLOBE,
      MONITOR,
      CLOCK,
      SPEED,
      CHIP,
      SUN,
      MOON,
      CLOUD,
      RAIN,
      SNOW,
      STORM,
      NOTE,
      PLAY,
      PAUSE,
      PREV,
      NEXT,
      APP,
      AURA,
      HIT,
      LANDING,
      GLOW,
      PARTICLES,
      SKY,
      GEAR,
      LAYERS,
      COMPASS,
      SIGNAL,
      WIFI,
      CHART,
      BOLT,
      SERVER,
      HOURGLASS,
      THERMO,
      PIN,
      HEART,
      STAR,
      CROWN,
      FLAME,
      SWORD,
      GEM,
      ARROWS,
      PARTLY,
      WIND,
      SKULL,
      DISCORD,
      MIC,
      MIC_OFF,
      HEADSET,
      HEADSET_OFF,
      HANGUP,
      CHAT,
      AT,
      SPEAKER,
      SPEAKER_OFF,
      UI,
      CROSSHAIR,
      SCOREBOARD,
      TABLIST,
      NAMETAG,
      HOTBAR,
      BACK,
      FORWARD,
      ARMOR,
      SHAKE,
      SKIN,
      PALETTE,
      LIST,
      MOTION,
      TRAIL;
   }
}
