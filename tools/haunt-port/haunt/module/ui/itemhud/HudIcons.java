package mix.client.mixui.integration.haunt.module.ui.itemhud;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D.Double;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

final class HudIcons {
   static final float GLOW = 1.6F;
   private static final int COLUMNS = HudIcons.Icon.values().length;
   private static final int ROWS = HudIcons.Layer.values().length;
   private static DynamicTexture texture;
   private static int builtPx = -1;
   private static int builtPad;
   private static int wantedPx = -1;
   private static volatile int[] pending;
   private static volatile int pendingPx = -1;
   private static volatile int pendingW;
   private static volatile int pendingH;
   private static int atlasW;
   private static int atlasH;
   private static ExecutorService worker;

   static int pad(int px) {
      return 2 + (int)Math.ceil((double)((float)px * 1.6F / 9.0F));
   }

   static boolean ready(int sizePx) {
      final int px = Math.max(9, Math.min(160, sizePx));
      int[] made = pending;
      if(made != null && pendingPx == px) {
         upload(made, pendingW, pendingH, px);
         pending = null;
      }

      if(texture != null && builtPx == px) {
         return true;
      } else {
         if(wantedPx != px) {
            wantedPx = px;
            worker().execute(new Runnable() {
               public void run() {
                  HudIcons.build(px);
               }
            });
         }

         return texture != null;
      }
   }

   private static ExecutorService worker() {
      if(worker == null) {
         worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
               Thread t = new Thread(r, "HAUN’T VISUALS HUD icons");
               t.setDaemon(true);
               return t;
            }
         });
      }

      return worker;
   }

   static int texture() {
      return texture.getGlTextureId();
   }

   static boolean hasDetail(HudIcons.Icon icon) {
      return icon == HudIcons.Icon.FOOD_DRUMSTICK || icon == HudIcons.Icon.ARMOR_CHEST;
   }

   static void quad(WorldRenderer wr, HudIcons.Icon icon, HudIcons.Layer layer, float x, float y, float size, float from, float to, int top, int bottom) {
      int cell = builtPx + builtPad * 2;
      float m = layer == HudIcons.Layer.GLOW?1.6F:0.0F;
      float l = from <= 0.0F?-m:9.0F * from;
      float r = to >= 1.0F?9.0F + m:9.0F * to;
      float pu = (float)builtPx / 9.0F;
      float sc = size / 9.0F;
      float ox = (float)(icon.ordinal() * cell + builtPad);
      float oy = (float)(layer.ordinal() * cell + builtPad);
      float u0 = (ox + l * pu) / (float)atlasW;
      float u1 = (ox + r * pu) / (float)atlasW;
      float v0 = (oy - m * pu) / (float)atlasH;
      float v1 = (oy + (9.0F + m) * pu) / (float)atlasH;
      float x0 = x + l * sc;
      float x1 = x + r * sc;
      float y0 = y - m * sc;
      float y1 = y + (9.0F + m) * sc;
      int tr = top >> 16 & 255;
      int tg = top >> 8 & 255;
      int tb = top & 255;
      int ta = top >>> 24;
      int br = bottom >> 16 & 255;
      int bg = bottom >> 8 & 255;
      int bb = bottom & 255;
      int ba = bottom >>> 24;
      wr.pos((double)x0, (double)y1, 0.0D).tex((double)u0, (double)v1).color(br, bg, bb, ba).endVertex();
      wr.pos((double)x1, (double)y1, 0.0D).tex((double)u1, (double)v1).color(br, bg, bb, ba).endVertex();
      wr.pos((double)x1, (double)y0, 0.0D).tex((double)u1, (double)v0).color(tr, tg, tb, ta).endVertex();
      wr.pos((double)x0, (double)y0, 0.0D).tex((double)u0, (double)v0).color(tr, tg, tb, ta).endVertex();
   }

   static void quad(WorldRenderer wr, HudIcons.Icon icon, HudIcons.Layer layer, float x, float y, float size, float from, float to, int color) {
      quad(wr, icon, layer, x, y, size, from, to, color, color);
   }

   private static void upload(int[] argb, int w, int h, int px) {
      if(texture != null) {
         texture.deleteGlTexture();
      }

      DynamicTexture t = new DynamicTexture(w, h);
      System.arraycopy(argb, 0, t.getTextureData(), 0, argb.length);
      t.updateDynamicTexture();
      GlStateManager.bindTexture(t.getGlTextureId());
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, '脯');
      GL11.glTexParameteri(3553, 10243, '脯');
      texture = t;
      builtPx = px;
      builtPad = pad(px);
      atlasW = w;
      atlasH = h;
   }

   private static BasicStroke round(float width) {
      return new BasicStroke(width, 1, 1);
   }

   private static Shape rounded(Shape sharp, float r) {
      Area a = new Area(sharp);
      a.add(new Area(round(r * 2.0F).createStrokedShape(sharp)));
      return a;
   }

   private static Path2D polygon(double... xy) {
      Double p = new Double();
      p.moveTo(xy[0], xy[1]);

      for(int i = 2; i < xy.length; i += 2) {
         p.lineTo(xy[i], xy[i + 1]);
      }

      p.closePath();
      return p;
   }

   private static Shape scaled(Shape s, double k, double cx, double cy) {
      AffineTransform t = new AffineTransform();
      t.translate(cx, cy);
      t.scale(k, k);
      t.translate(-cx, -cy);
      return t.createTransformedShape(s);
   }

   private static Shape meat() {
      return AffineTransform.getRotateInstance(0.7853981633974483D, 3.5D, 3.5D).createTransformedShape(new java.awt.geom.Ellipse2D.Double(0.45D, 1.15D, 6.1D, 4.7D));
   }

   private static Shape bone() {
      Area a = new Area((new BasicStroke(1.4F, 0, 1)).createStrokedShape(new java.awt.geom.Line2D.Double(5.0D, 5.0D, 6.85D, 6.85D)));
      a.add(new Area(new java.awt.geom.Ellipse2D.Double(6.5D, 5.45D, 1.7D, 1.7D)));
      a.add(new Area(new java.awt.geom.Ellipse2D.Double(5.45D, 6.5D, 1.7D, 1.7D)));
      return a;
   }

   static Shape shape(HudIcons.Icon icon) {
      switch(icon) {
      case HEART:
         Double p = new Double();
         p.moveTo(4.5D, 8.25D);
         p.curveTo(3.2D, 7.2D, 0.55D, 5.4D, 0.55D, 3.15D);
         p.curveTo(0.55D, 1.65D, 1.65D, 0.75D, 2.85D, 0.75D);
         p.curveTo(3.65D, 0.75D, 4.2D, 1.2D, 4.5D, 1.85D);
         p.curveTo(4.8D, 1.2D, 5.35D, 0.75D, 6.15D, 0.75D);
         p.curveTo(7.35D, 0.75D, 8.45D, 1.65D, 8.45D, 3.15D);
         p.curveTo(8.45D, 5.4D, 5.8D, 7.2D, 4.5D, 8.25D);
         p.closePath();
         return scaled(p, 0.9D, 4.5D, 4.5D);
      case HEART_SQUARE:
         return new java.awt.geom.RoundRectangle2D.Double(1.1D, 1.1D, 6.8D, 6.8D, 3.2D, 3.2D);
      case HEART_HEX:
         Double p = new Double();

         for(int i = 0; i < 6; ++i) {
            double ax = Math.toRadians((double)(90 + 60 * i));
            double x = 4.5D + Math.cos(ax) * 3.4D;
            double y = 4.5D - Math.sin(ax) * 3.4D;
            if(i == 0) {
               p.moveTo(x, y);
            } else {
               p.lineTo(x, y);
            }
         }

         p.closePath();
         return rounded(p, 0.45F);
      case HEART_CIRCLE:
         return new java.awt.geom.Ellipse2D.Double(1.0D, 1.0D, 7.0D, 7.0D);
      case FOOD_DRUMSTICK:
         Area a = new Area(meat());
         a.add(new Area(bone()));
         return a;
      case FOOD_BOLT:
         return rounded(polygon(new double[]{5.55D, 0.85D, 1.95D, 5.0D, 4.25D, 5.0D, 3.45D, 8.15D, 7.05D, 3.85D, 4.75D, 3.85D}), 0.3F);
      case FOOD_CIRCLE:
         return new java.awt.geom.Ellipse2D.Double(1.0D, 1.0D, 7.0D, 7.0D);
      case ARMOR_SHIELD:
         Double p = new Double();
         p.moveTo(4.5D, 0.8D);
         p.curveTo(5.55D, 1.4D, 6.75D, 1.7D, 7.9D, 1.75D);
         p.curveTo(7.9D, 5.2D, 6.5D, 7.3D, 4.5D, 8.3D);
         p.curveTo(2.5D, 7.3D, 1.1D, 5.2D, 1.1D, 1.75D);
         p.curveTo(2.25D, 1.7D, 3.45D, 1.4D, 4.5D, 0.8D);
         p.closePath();
         return p;
      case ARMOR_CHEST:
         Double p = new Double();
         p.moveTo(3.15D, 0.95D);
         p.curveTo(3.45D, 2.25D, 5.55D, 2.25D, 5.85D, 0.95D);
         p.lineTo(7.3D, 1.45D);
         p.curveTo(8.2D, 1.75D, 8.35D, 2.85D, 7.7D, 3.45D);
         p.curveTo(6.95D, 4.15D, 6.75D, 5.2D, 6.8D, 6.55D);
         p.lineTo(6.75D, 7.5D);
         p.curveTo(5.6D, 8.3D, 3.4D, 8.3D, 2.25D, 7.5D);
         p.lineTo(2.2D, 6.55D);
         p.curveTo(2.25D, 5.2D, 2.05D, 4.15D, 1.3D, 3.45D);
         p.curveTo(0.65D, 2.85D, 0.8D, 1.75D, 1.7D, 1.45D);
         p.closePath();
         return p;
      case BUBBLE:
         return new java.awt.geom.Ellipse2D.Double(1.3D, 1.3D, 6.4D, 6.4D);
      default:
         Area a = new Area();

         for(int i = 0; i < 4; ++i) {
            java.awt.geom.Arc2D.Double arc = new java.awt.geom.Arc2D.Double(1.1D, 1.1D, 6.8D, 6.8D, (double)(20 + 90 * i), 50.0D, 0);
            a.add(new Area(round(1.1F).createStrokedShape(arc)));
         }

         return a;
      }
   }

   private static Shape spot(HudIcons.Icon icon, Shape s) {
      switch(icon) {
      case HEART:
         return AffineTransform.getRotateInstance(Math.toRadians(-38.0D), 2.75D, 2.85D).createTransformedShape(new java.awt.geom.Ellipse2D.Double(1.8D, 2.3D, 1.9D, 1.1D));
      case FOOD_DRUMSTICK:
         return AffineTransform.getRotateInstance(Math.toRadians(-45.0D), 2.6D, 2.6D).createTransformedShape(new java.awt.geom.Ellipse2D.Double(1.5D, 2.05D, 2.2D, 1.1D));
      default:
         Rectangle2D b = s.getBounds2D();
         double w = b.getWidth() * 0.34D;
         double h = b.getHeight() * 0.17D;
         double cx = b.getX() + b.getWidth() * 0.34D;
         double cy = b.getY() + b.getHeight() * 0.24D;
         return new java.awt.geom.Ellipse2D.Double(cx - w / 2.0D, cy - h / 2.0D, w, h);
      }
   }

   private static void build(int px) {
      try {
         int pad = pad(px);
         int cell = px + pad * 2;
         int w = cell * COLUMNS;
         int h = cell * ROWS;
         int[] argb = new int[w * h];

         for(HudIcons.Icon icon : HudIcons.Icon.values()) {
            for(HudIcons.Layer layer : HudIcons.Layer.values()) {
               float[] m = mask(icon, layer, px);
               int ox = icon.ordinal() * cell;
               int oy = layer.ordinal() * cell;

               for(int y = 0; y < cell; ++y) {
                  for(int x = 0; x < cell; ++x) {
                     int a = Math.round(Math.max(0.0F, Math.min(1.0F, m[y * cell + x])) * 255.0F);
                     argb[(oy + y) * w + ox + x] = a << 24 | 16777215;
                  }
               }
            }
         }

         pendingW = w;
         pendingH = h;
         pendingPx = px;
         pending = argb;
      } catch (Throwable var20) {
         FuriahsVisuals.LOG.warn("UI: the HUD icons could not be drawn", var20);
      }

   }

   static float[] mask(HudIcons.Icon icon, HudIcons.Layer layer, int px) {
      int pad = pad(px);
      int n = px + pad * 2;
      BufferedImage img = new BufferedImage(n, n, 2);
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.translate(pad, pad);
      g.scale((double)px / 9.0D, (double)px / 9.0D);
      Shape s = shape(icon);
      draw(g, icon, s, layer);
      g.dispose();
      float[] a = new float[n * n];
      int[] row = img.getRGB(0, 0, n, n, (int[])null, 0, n);

      for(int i = 0; i < a.length; ++i) {
         a[i] = (float)(row[i] >>> 24) / 255.0F;
      }

      if(layer == HudIcons.Layer.GLOW && icon != HudIcons.Icon.BUBBLE_POP) {
         float[] blur = blur(a, n, 0.8F * (float)px / 9.0F);

         for(int i = 0; i < a.length; ++i) {
            a[i] = Math.min(1.0F, blur[i] * 1.6F) * (1.0F - a[i]);
         }
      }

      return a;
   }

   private static void draw(Graphics2D g, HudIcons.Icon icon, Shape s, HudIcons.Layer layer) {
      boolean bubble = icon == HudIcons.Icon.BUBBLE;
      boolean pop = icon == HudIcons.Icon.BUBBLE_POP;
      switch(layer) {
      case FILL:
         if(bubble) {
            g.setColor(new Color(255, 255, 255, 80));
            g.fill(s);
            g.setStroke(round(0.9F));
            g.setColor(Color.WHITE);
            g.draw(s);
         } else {
            g.setColor(Color.WHITE);
            g.fill(s);
         }
         break;
      case GLOSS:
         if(!pop) {
            if(bubble) {
               g.setColor(new Color(255, 255, 255, 240));
               g.fill(new java.awt.geom.Ellipse2D.Double(2.55D, 2.45D, 1.9D, 1.3D));
            } else {
               Rectangle2D b = s.getBounds2D();
               Area inset = new Area(s);
               inset.subtract(new Area(round(1.3F).createStrokedShape(s)));
               g.clip(inset);
               g.setPaint(new GradientPaint(0.0F, (float)b.getY(), new Color(255, 255, 255, 150), 0.0F, (float)(b.getY() + b.getHeight() * 0.55D), new Color(255, 255, 255, 0)));
               g.fill(b);
               g.setClip((Shape)null);
               Area spot = new Area(spot(icon, s));
               spot.intersect(new Area(s));
               g.setColor(Color.WHITE);
               g.fill(spot);
            }
         }
         break;
      case RIM:
         if(!bubble && !pop) {
            Rectangle2D b = s.getBounds2D();
            g.clip(s);
            g.setStroke(round(1.0F));
            g.setPaint(new GradientPaint(0.0F, (float)b.getY(), new Color(255, 255, 255, 255), 0.0F, (float)(b.getY() + b.getHeight()), new Color(255, 255, 255, 70)));
            g.draw(s);
         }
         break;
      case DETAIL:
         Shape detail = detailShape(icon);
         if(detail != null) {
            g.setColor(Color.WHITE);
            g.fill(detail);
         }
         break;
      default:
         if(!pop) {
            g.setColor(Color.WHITE);
            g.fill(s);
         }
      }

   }

   private static float[] blur(float[] a, int n, float sigma) {
      int r = (int)Math.ceil((double)(sigma * 3.0F));
      float[] k = new float[r * 2 + 1];
      float sum = 0.0F;

      for(int i2 = -r; i2 <= r; ++i2) {
         k[i2 + r] = (float)Math.exp((double)((float)(-i2 * i2) / (2.0F * sigma * sigma)));
         sum += k[i2 + r];
      }

      int n2;
      for(int var18 = 0; var18 < k.length; k[n2] /= sum) {
         n2 = var18++;
      }

      float[] t = new float[n * n];
      float[] o = new float[n * n];

      for(int y = 0; y < n; ++y) {
         for(int x = 0; x < n; ++x) {
            float v = 0.0F;

            for(int i = -r; i <= r; ++i) {
               int xx = x + i;
               if(xx >= 0 && xx < n) {
                  v += a[y * n + xx] * k[i + r];
               }
            }

            t[y * n + x] = v;
         }
      }

      for(int var17 = 0; var17 < n; ++var17) {
         for(int x = 0; x < n; ++x) {
            float v = 0.0F;

            for(int ix = -r; ix <= r; ++ix) {
               int yy = var17 + ix;
               if(yy >= 0 && yy < n) {
                  v += t[yy * n + x] * k[ix + r];
               }
            }

            o[var17 * n + x] = v;
         }
      }

      return o;
   }

   static Shape detailShape(HudIcons.Icon icon) {
      if(icon == HudIcons.Icon.FOOD_DRUMSTICK) {
         Area bone = new Area(bone());
         bone.subtract(new Area(meat()));
         return bone;
      } else if(icon == HudIcons.Icon.ARMOR_CHEST) {
         Area ridge = new Area(round(0.5F).createStrokedShape(new java.awt.geom.Line2D.Double(4.5D, 2.6D, 4.5D, 7.7D)));
         ridge.intersect(new Area(shape(icon)));
         return ridge;
      } else {
         return null;
      }
   }

   static Shape spotShape(HudIcons.Icon icon) {
      Shape s = shape(icon);
      Area spot = new Area(spot(icon, s));
      spot.intersect(new Area(s));
      return spot;
   }

   static enum Icon {
      HEART,
      HEART_SQUARE,
      HEART_HEX,
      HEART_CIRCLE,
      FOOD_DRUMSTICK,
      FOOD_BOLT,
      FOOD_CIRCLE,
      ARMOR_SHIELD,
      ARMOR_CHEST,
      BUBBLE,
      BUBBLE_POP;
   }

   static enum Layer {
      FILL,
      GLOSS,
      RIM,
      GLOW,
      DETAIL;
   }
}
