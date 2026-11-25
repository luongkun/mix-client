package mix.client.mixui.integration.haunt.fx.kit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D.Double;
import java.awt.image.BufferedImage;
import mix.client.mixui.integration.haunt.fx.kit.Chains;
import mix.client.mixui.integration.haunt.fx.kit.KitTextures;
import mix.client.mixui.integration.haunt.fx.look.KeepVisible;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;

public final class Clockwork {
   private static final double TWO_PI = 6.283185307179586D;
   public static final int SEGMENTS = 48;
   public static final double[] COS = new double[48];
   public static final double[] SIN = new double[48];
   private static final int KITE_POINTS = 16;
   private static final double[] KITE_COS = new double[16];
   private static final double[] KITE_SIN = new double[16];
   private static final double RING_OUT = 1.0D / Math.sqrt(1.36D);
   private static final double RING_ACROSS = 0.6D / Math.sqrt(1.36D);
   private static final double GEAR_OUT = 1.0D / Math.sqrt(1.25D);
   private static final double GEAR_ALONG = 0.5D / Math.sqrt(1.25D);
   public static final double BETWEEN_TICKS = 0.06544984694978735D;
   private static final double[] HAND_ALONG;
   private static final double[] HAND_ACROSS;
   private static final double[] E;
   private static final double[] TOOTH;
   private static final double TOOTH_MID;
   private static final KitTextures.Painted NUMERALS;

   public static double ratchet(double t, double period, double step, boolean smooth) {
      if(smooth) {
         return t / period * step;
      } else {
         double n = Math.floor(t / period);
         double f = (t - n * period) / period;
         double e = f < 0.2D?(double)Easing.outBack((float)(f / 0.2D), 2.2F):1.0D;
         return (n + e) * step;
      }
   }

   public static double meshed(double driverTurn, int driverTeeth, int teeth, double towards) {
      double pitch = 6.283185307179586D / (double)teeth;
      return -driverTurn * (double)driverTeeth / (double)teeth + towards * (1.0D + (double)driverTeeth / (double)teeth) + 3.141592653589793D - (2.0D * TOOTH_MID + 0.5D) * pitch;
   }

   public static double angleOf(double ax, double ay, double az, double dx, double dy, double dz) {
      across(ax, ay, az, E);
      return Math.atan2(dx * E[3] + dy * E[4] + dz * E[5], dx * E[0] + dy * E[1] + dz * E[2]);
   }

   public static double module(double r, int teeth) {
      return r / ((double)teeth * 0.5D + 1.0D);
   }

   public static double tipRadius(double m, int teeth) {
      return m * ((double)teeth * 0.5D + 1.0D);
   }

   public static double meshDistance(double m, int teeth1, int teeth2) {
      return m * ((double)(teeth1 + teeth2) * 0.5D + 0.3D);
   }

   public static void band(double cx, double cy, double cz, double ux, double uy, double uz, double vx, double vy, double vz, double nx, double ny, double nz, double r, double width, double thick, int[] colors, int face) {
      double hw = width * 0.5D;
      double ht = thick * 0.5D;
      double qx = nx * ht;
      double qy = ny * ht;
      double qz = nz * ht;
      double mx = nx * RING_ACROSS;
      double my = ny * RING_ACROSS;
      double mz = nz * RING_ACROSS;
      int base = MeshBatch.stage(192);

      for(int i = 0; i < 48; ++i) {
         double rx = ux * COS[i] + vx * SIN[i];
         double ry = uy * COS[i] + vy * SIN[i];
         double rz = uz * COS[i] + vz * SIN[i];
         double px = cx + rx * r;
         double py = cy + ry * r;
         double pz = cz + rz * r;
         int col = colors[i];
         double ox = rx * hw;
         double oy = ry * hw;
         double oz = rz * hw;
         double sx = rx * RING_OUT;
         double sy = ry * RING_OUT;
         double sz = rz * RING_OUT;
         int o = base + i * 4;
         MeshBatch.vertex(o, px + ox + qx, py + oy + qy, pz + oz + qz, sx + mx, sy + my, sz + mz, col, 0.0F);
         MeshBatch.vertex(o + 1, px + ox - qx, py + oy - qy, pz + oz - qz, sx - mx, sy - my, sz - mz, col, 0.0F);
         MeshBatch.vertex(o + 2, px - ox - qx, py - oy - qy, pz - oz - qz, -sx - mx, -sy - my, -sz - mz, col, 0.0F);
         MeshBatch.vertex(o + 3, px - ox + qx, py - oy + qy, pz - oz + qz, -sx + mx, -sy + my, -sz + mz, col, 0.0F);
      }

      boolean front = face > 0;

      for(int i = 0; i < 48; ++i) {
         int a = base + i * 4;
         int b = base + (i + 1) % 48 * 4;
         MeshBatch.quad(a, b, b + 1, a + 1);
         MeshBatch.quad(a + 2, b + 2, b + 3, a + 3);
         if(front) {
            MeshBatch.quad(a + 3, b + 3, b, a);
         } else {
            MeshBatch.quad(a + 1, b + 1, b + 2, a + 2);
         }
      }

   }

   public static void ticks(double cx, double cy, double cz, double ux, double uy, double uz, double vx, double vy, double vz, double ox, double oy, double oz, double r, double width, double spin, int color, KeepVisible keep) {
      double cs = Math.cos(spin);
      double ss = Math.sin(spin);
      double out = width * 0.48D;

      for(int i = 0; i < 48; ++i) {
         double cos = cs * COS[i] - ss * SIN[i];
         double sin = ss * COS[i] + cs * SIN[i];
         double rx = ux * cos + vx * sin;
         double ry = uy * cos + vy * sin;
         double rz = uz * cos + vz * sin;
         double tx = -ux * sin + vx * cos;
         double ty = -uy * sin + vy * cos;
         double tz = -uz * sin + vz * cos;
         boolean major = (i & 3) == 0;
         double in = width * (major?0.42D:0.15D);
         double hw = width * (major?0.035D:0.0225D);
         double mx = cx + rx * r + ox;
         double my = cy + ry * r + oy;
         double mz = cz + rz * r + oz;
         int col = keep == null?color:faded(color, keep.solid(mx, my, mz));
         double x0 = mx - rx * in;
         double y0 = my - ry * in;
         double z0 = mz - rz * in;
         double x1 = mx + rx * out;
         double y1 = my + ry * out;
         double z1 = mz + rz * out;
         double var79;
         double var80;
         double var81;
         Vfx.quadUV(Textures.WHITE, x0 - (var79 = tx * hw), y0 - (var80 = ty * hw), z0 - (var81 = tz * hw), 0.5D, 0.5D, col, x1 - var79, y1 - var80, z1 - var81, 0.5D, 0.5D, col, x1 + var79, y1 + var80, z1 + var81, 0.5D, 0.5D, col, x0 + var79, y0 + var80, z0 + var81, 0.5D, 0.5D, col);
      }

   }

   public static void glyphs(double cx, double cy, double cz, double ux, double uy, double uz, double vx, double vy, double vz, double ox, double oy, double oz, double r, double width, double spin, double offset, int face, boolean roman, int color, KeepVisible keep) {
      int tex = roman?numerals():Textures.STAR;
      double cs = Math.cos(spin + offset);
      double ss = Math.sin(spin + offset);
      double s = width * 0.42D;
      double w = roman?s * 1.6D:s;

      for(int i = 0; i < 48; i += 4) {
         double cos = cs * COS[i] - ss * SIN[i];
         double sin = ss * COS[i] + cs * SIN[i];
         double rx = ux * cos + vx * sin;
         double ry = uy * cos + vy * sin;
         double rz = uz * cos + vz * sin;
         double tx = -ux * sin + vx * cos;
         double ty = -uy * sin + vy * cos;
         double tz = -uz * sin + vz * cos;
         double px = cx + rx * r + ox;
         double py = cy + ry * r + oy;
         double pz = cz + rz * r + oz;
         int col = keep == null?color:faded(color, keep.solid(px, py, pz));
         int numeral = i / 4;
         float u0 = roman?(float)numeral / 12.0F:0.0F;
         float u1 = roman?(float)(numeral + 1) / 12.0F:1.0F;
         double ax = tx * w * (double)face;
         double ay = ty * w * (double)face;
         double az = tz * w * (double)face;
         double bx = rx * s;
         double by = ry * s;
         double bz = rz * s;
         Vfx.quadUV(tex, px - ax - bx, py - ay - by, pz - az - bz, (double)u0, 1.0D, col, px + ax - bx, py + ay - by, pz + az - bz, (double)u1, 1.0D, col, px + ax + bx, py + ay + by, pz + az + bz, (double)u1, 0.0D, col, px - ax + bx, py - ay + by, pz - az + bz, (double)u0, 0.0D, col);
      }

   }

   private static int faded(int color, float k) {
      return k >= 0.999F?color:Math.round((float)(color >>> 24) * k) << 24 | color & 16777215;
   }

   public static int numerals() {
      return NUMERALS.texture();
   }

   private static KitTextures.Image[] paintNumerals() {
      int cell = 128;
      int w = cell * 12;
      BufferedImage img = new BufferedImage(w, cell, 2);
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      g.setColor(Color.WHITE);
      String[] numerals = new String[]{"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};

      for(int i = 0; i < 12; ++i) {
         drawNumeral(g, numerals[i], i * cell, cell);
      }

      g.dispose();
      int[] px = img.getRGB(0, 0, w, cell, (int[])null, 0, w);

      for(int i = 0; i < px.length; ++i) {
         px[i] = px[i] & -16777216 | 16777215;
      }

      return new KitTextures.Image[]{KitTextures.Image.argb(px, w, cell, true, false)};
   }

   private static void drawNumeral(Graphics2D g, String s, int x0, int cell) {
      double letterW = 0.0D;
      double gap = (double)cell * 0.05D;
      double iW = (double)cell * 0.13D;
      double vW = (double)cell * 0.3D;
      double xW = (double)cell * 0.3D;

      for(char ch : s.toCharArray()) {
         letterW += (ch == 73?iW:(ch == 86?vW:xW)) + gap;
      }

      double var30;
      double scale = Math.min(1.0D, (double)cell * 0.9D / (var30 = letterW - gap));
      double x = (double)x0 + ((double)cell - var30 * scale) / 2.0D;
      double top = (double)cell * 0.2D;
      double bottom = (double)cell * 0.8D;
      g.setStroke(new BasicStroke((float)((double)cell * 0.075D * scale), 1, 1));

      for(char ch : s.toCharArray()) {
         double w = (ch == 73?iW:(ch == 86?vW:xW)) * scale;
         double mid = x + w / 2.0D;
         if(ch == 73) {
            g.draw(new Double(mid, top, mid, bottom));
            g.draw(new Double(mid - w * 0.5D, top, mid + w * 0.5D, top));
            g.draw(new Double(mid - w * 0.5D, bottom, mid + w * 0.5D, bottom));
         } else if(ch == 86) {
            g.draw(new Double(x, top, mid, bottom));
            g.draw(new Double(x + w, top, mid, bottom));
         } else {
            g.draw(new Double(x, top, x + w, bottom));
            g.draw(new Double(x + w, top, x, bottom));
         }

         x += w + gap * scale;
      }

   }

   public static void hand(double px, double py, double pz, double nx, double ny, double nz, double dx, double dy, double dz, double length, double width, double thick, int col, float glint) {
      across(nx, ny, nz, E);
      double ex = E[0];
      double ey = E[1];
      double ez = E[2];
      double fx = E[3];
      double fy = E[4];
      double fz = E[5];
      double wx = ny * dz - nz * dy;
      double wy = nz * dx - nx * dz;
      double wz = nx * dy - ny * dx;
      int n = HAND_ALONG.length;
      double ht = thick * 0.5D;
      double hw = width * 0.5D;
      int base = MeshBatch.stage(n * 2 + n * 4);

      for(int i = 0; i < n; ++i) {
         double a = HAND_ALONG[i] * length;
         double b = HAND_ACROSS[i] * hw;
         double x = px + dx * a + wx * b;
         double y = py + dy * a + wy * b;
         double z = pz + dz * a + wz * b;
         MeshBatch.vertex(base + i, x + nx * ht, y + ny * ht, z + nz * ht, nx, ny, nz, col, glint);
         MeshBatch.vertex(base + n + i, x - nx * ht, y - ny * ht, z - nz * ht, -nx, -ny, -nz, col, glint);
      }

      for(int var91 = 1; var91 < n - 1; ++var91) {
         MeshBatch.tri(base, base + var91, base + var91 + 1);
         MeshBatch.tri(base + n, base + n + var91, base + n + var91 + 1);
      }

      int side = base + n * 2;

      for(int i2 = 0; i2 < n; ++i2) {
         int j = (i2 + 1) % n;
         double b1 = HAND_ACROSS[j] * hw;
         double b0 = HAND_ACROSS[i2] * hw;
         double eb = b1 - b0;
         double oa = -eb;
         double a1 = HAND_ALONG[j] * length;
         double a0 = HAND_ALONG[i2] * length;
         double ea = a1 - a0;
         double ol = Math.sqrt(oa * oa + ea * ea);
         if(ol < 1.0E-9D) {
            ol = 1.0D;
         }

         double onx = (dx * oa + wx * ea) / ol;
         double ony = (dy * oa + wy * ea) / ol;
         double onz = (dz * oa + wz * ea) / ol;
         double x0 = px + dx * a0 + wx * b0;
         double y0 = py + dy * a0 + wy * b0;
         double z0 = pz + dz * a0 + wz * b0;
         double x1 = px + dx * a1 + wx * b1;
         double y1 = py + dy * a1 + wy * b1;
         double z1 = pz + dz * a1 + wz * b1;
         int o = side + i2 * 4;
         MeshBatch.vertex(o, x0 + nx * ht, y0 + ny * ht, z0 + nz * ht, onx, ony, onz, col, glint);
         MeshBatch.vertex(o + 1, x1 + nx * ht, y1 + ny * ht, z1 + nz * ht, onx, ony, onz, col, glint);
         MeshBatch.vertex(o + 2, x1 - nx * ht, y1 - ny * ht, z1 - nz * ht, onx, ony, onz, col, glint);
         MeshBatch.vertex(o + 3, x0 - nx * ht, y0 - ny * ht, z0 - nz * ht, onx, ony, onz, col, glint);
         MeshBatch.quad(o, o + 1, o + 2, o + 3);
      }

      ring(px + nx * ht, py + ny * ht, pz + nz * ht, nx, ny, nz, ex, ey, ez, fx, fy, fz, 0.0D, width * 0.62D, thick * 1.2D, 12, col, glint);
   }

   public static void across(double ax, double ay, double az, double[] out) {
      double ez;
      double ey;
      double ex;
      if(Math.abs(ay) < 0.9D) {
         ex = -az;
         ey = 0.0D;
         ez = ax;
      } else {
         ex = 0.0D;
         ey = az;
         ez = -ay;
      }

      double el = Math.sqrt(ex * ex + ey * ey + ez * ez);
      double var17;
      out[0] = var17 = ex / el;
      double var16;
      out[1] = var16 = ey / el;
      double var15;
      out[2] = var15 = ez / el;
      out[3] = ay * var15 - az * var16;
      out[4] = az * var17 - ax * var15;
      out[5] = ax * var16 - ay * var17;
   }

   static void warmUp() {
      NUMERALS.warm();
   }

   public static void kiteGear(double px, double py, double pz, double ax, double ay, double az, double r, double t, double turn, int col) {
      across(ax, ay, az, E);
      double ex = E[0];
      double ey = E[1];
      double ez = E[2];
      double fx = E[3];
      double fy = E[4];
      double fz = E[5];
      int n = 16;
      int base = MeshBatch.stage(n * 2 + 2);
      double ht = t * 0.5D;
      double hx = ax * ht;
      double hy = ay * ht;
      double hz = az * ht;
      double lx = ax * GEAR_ALONG;
      double ly = ay * GEAR_ALONG;
      double lz = az * GEAR_ALONG;
      double ct = Math.cos(turn);
      double st = Math.sin(turn);

      for(int i2 = 0; i2 < n; ++i2) {
         double cos = ct * KITE_COS[i2] - st * KITE_SIN[i2];
         double sin = st * KITE_COS[i2] + ct * KITE_SIN[i2];
         double rr = (i2 & 1) == 0?r:r * 0.78D;
         double dx = ex * cos + fx * sin;
         double dy = ey * cos + fy * sin;
         double dz = ez * cos + fz * sin;
         double qx = px + dx * rr;
         double qy = py + dy * rr;
         double qz = pz + dz * rr;
         double sx = dx * GEAR_OUT;
         double sy = dy * GEAR_OUT;
         double sz = dz * GEAR_OUT;
         MeshBatch.vertex(base + i2, qx + hx, qy + hy, qz + hz, sx + lx, sy + ly, sz + lz, col, 0.0F);
         MeshBatch.vertex(base + n + i2, qx - hx, qy - hy, qz - hz, sx - lx, sy - ly, sz - lz, col, 0.0F);
      }

      int top = base + n * 2;
      int bottom = top + 1;
      MeshBatch.vertex(top, px + hx, py + hy, pz + hz, ax, ay, az, col, 0.0F);
      MeshBatch.vertex(bottom, px - hx, py - hy, pz - hz, -ax, -ay, -az, col, 0.0F);

      for(int i = 0; i < n; i += 2) {
         int before = base + (i + n - 1) % n;
         int tip = base + i;
         int after = base + i + 1;
         MeshBatch.quad(top, before, tip, after);
         MeshBatch.quad(bottom, before + n, tip + n, after + n);
      }

      for(int var77 = 0; var77 < n; ++var77) {
         int j = (var77 + 1) % n;
         MeshBatch.quad(base + var77, base + j, base + n + j, base + n + var77);
      }

   }

   public static void gear(double px, double py, double pz, double ax, double ay, double az, double r, double t, double turn, int teeth, int spokes, int col, float glint) {
      teeth = Math.max(6, Math.min(32, teeth));
      across(ax, ay, az, E);
      double ex = E[0];
      double ey = E[1];
      double ez = E[2];
      double fx = E[3];
      double fy = E[4];
      double fz = E[5];
      double ht = t * 0.5D;
      double depth = 2.2D * module(r, teeth);
      double root = r - depth;
      double hub = r * 0.22D;
      double bore = r * 0.08D;
      double rim = Math.max(hub, root - Math.max(r * 0.1D, depth * 0.6D));
      int pts = teeth * 4;
      int base = MeshBatch.stage(pts * 2 * 2);
      double ct = Math.cos(turn);
      double st = Math.sin(turn);

      for(int i = 0; i < teeth; ++i) {
         double a0 = 6.283185307179586D * (double)i / (double)teeth;

         for(int k = 0; k < 4; ++k) {
            double a = a0 + 6.283185307179586D / (double)teeth * TOOTH[k];
            double c = Math.cos(a);
            double s = Math.sin(a);
            double cos = ct * c - st * s;
            double sin = st * c + ct * s;
            double dx = ex * cos + fx * sin;
            double dy = ey * cos + fy * sin;
            double dz = ez * cos + fz * sin;
            double rr = k != 1 && k != 2?root:r;
            int o = base + i * 4 + k;
            MeshBatch.vertex(o, px + dx * rr + ax * ht, py + dy * rr + ay * ht, pz + dz * rr + az * ht, dx * 0.6D + ax * 0.8D, dy * 0.6D + ay * 0.8D, dz * 0.6D + az * 0.8D, col, glint);
            MeshBatch.vertex(o + pts, px + dx * rr - ax * ht, py + dy * rr - ay * ht, pz + dz * rr - az * ht, dx * 0.6D - ax * 0.8D, dy * 0.6D - ay * 0.8D, dz * 0.6D - az * 0.8D, col, glint);
            MeshBatch.vertex(o + pts * 2, px + dx * rim + ax * ht, py + dy * rim + ay * ht, pz + dz * rim + az * ht, ax, ay, az, col, glint);
            MeshBatch.vertex(o + pts * 3, px + dx * rim - ax * ht, py + dy * rim - ay * ht, pz + dz * rim - az * ht, -ax, -ay, -az, col, glint);
         }
      }

      for(int var79 = 0; var79 < pts; ++var79) {
         int j = (var79 + 1) % pts;
         MeshBatch.quad(base + var79, base + j, base + pts + j, base + pts + var79);
         MeshBatch.quad(base + var79, base + j, base + pts * 2 + j, base + pts * 2 + var79);
         MeshBatch.quad(base + pts + var79, base + pts + j, base + pts * 3 + j, base + pts * 3 + var79);
         MeshBatch.quad(base + pts * 2 + var79, base + pts * 2 + j, base + pts * 3 + j, base + pts * 3 + var79);
      }

      ring(px, py, pz, ax, ay, az, ex, ey, ez, fx, fy, fz, bore, hub, t * 1.25D, 12, col, glint);
      if(rim > hub * 1.05D) {
         if(spokes <= 0) {
            ring(px, py, pz, ax, ay, az, ex, ey, ez, fx, fy, fz, hub * 0.95D, rim * 1.02D, t * 0.55D, 24, col, glint);
         } else {
            double sw = r * 0.09D;

            for(int k = 0; k < spokes; ++k) {
               double a = turn + 6.283185307179586D * (double)k / (double)spokes;
               double c = Math.cos(a);
               double s = Math.sin(a);
               double dx = ex * c + fx * s;
               double dy = ey * c + fy * s;
               double dz = ez * c + fz * s;
               double wx = ex * -s + fx * c;
               double wy = ey * -s + fy * c;
               double wz = ez * -s + fz * c;
               double mid = (hub + rim) * 0.5D;
               double half = (rim - hub) * 0.5D + sw * 0.4D;
               Chains.box(px + dx * mid, py + dy * mid, pz + dz * mid, dx, dy, dz, wx, wy, wz, ax, ay, az, half, sw * 0.5D, ht * 0.7D, col, glint);
            }
         }
      }

   }

   public static void ring(double px, double py, double pz, double ax, double ay, double az, double ex, double ey, double ez, double fx, double fy, double fz, double inner, double outer, double t, int segs, int col, float glint) {
      double ht = t * 0.5D;
      int base = MeshBatch.stage(segs * 4);

      for(int i = 0; i < segs; ++i) {
         double a = 6.283185307179586D * (double)i / (double)segs;
         double c = Math.cos(a);
         double s = Math.sin(a);
         double dx = ex * c + fx * s;
         double dy = ey * c + fy * s;
         double dz = ez * c + fz * s;
         int o = base + i * 4;
         MeshBatch.vertex(o, px + dx * outer + ax * ht, py + dy * outer + ay * ht, pz + dz * outer + az * ht, dx * 0.5D + ax * 0.85D, dy * 0.5D + ay * 0.85D, dz * 0.5D + az * 0.85D, col, glint);
         MeshBatch.vertex(o + 1, px + dx * outer - ax * ht, py + dy * outer - ay * ht, pz + dz * outer - az * ht, dx * 0.5D - ax * 0.85D, dy * 0.5D - ay * 0.85D, dz * 0.5D - az * 0.85D, col, glint);
         MeshBatch.vertex(o + 2, px + dx * inner - ax * ht, py + dy * inner - ay * ht, pz + dz * inner - az * ht, -dx * 0.5D - ax * 0.85D, -dy * 0.5D - ay * 0.85D, -dz * 0.5D - az * 0.85D, col, glint);
         MeshBatch.vertex(o + 3, px + dx * inner + ax * ht, py + dy * inner + ay * ht, pz + dz * inner + az * ht, -dx * 0.5D + ax * 0.85D, -dy * 0.5D + ay * 0.85D, -dz * 0.5D + az * 0.85D, col, glint);
      }

      for(int var50 = 0; var50 < segs; ++var50) {
         int a = base + var50 * 4;
         int b = base + (var50 + 1) % segs * 4;
         MeshBatch.quad(a, b, b + 1, a + 1);
         MeshBatch.quad(a + 1, b + 1, b + 2, a + 2);
         MeshBatch.quad(a + 2, b + 2, b + 3, a + 3);
         MeshBatch.quad(a + 3, b + 3, b, a);
      }

   }

   public static void spring(double x0, double y0, double z0, double x1, double y1, double z1, double radius, double wire, int coils, int col, float glint) {
      double ax = x1 - x0;
      double ay = y1 - y0;
      double az = z1 - z0;
      double len = Math.sqrt(ax * ax + ay * ay + az * az);
      if(len >= 1.0E-6D) {
         double var77;
         double var78;
         double var79;
         across(var77 = ax / len, var78 = ay / len, var79 = az / len, E);
         double ex = E[0];
         double ey = E[1];
         double ez = E[2];
         double fx = E[3];
         double fy = E[4];
         double fz = E[5];
         int steps = Math.max(8, coils * 10);
         int sides = 5;
         int base = MeshBatch.stage((steps + 1) * sides);

         for(int i = 0; i <= steps; ++i) {
            double u = (double)i / (double)steps;
            double a = u * (double)coils * 6.283185307179586D;
            double c = Math.cos(a);
            double s = Math.sin(a);
            double ox = ex * c + fx * s;
            double oy = ey * c + fy * s;
            double oz = ez * c + fz * s;
            double cx = x0 + var77 * len * u + ox * radius;
            double cy = y0 + var78 * len * u + oy * radius;
            double cz = z0 + var79 * len * u + oz * radius;

            for(int j = 0; j < sides; ++j) {
               double p = 6.283185307179586D * (double)j / (double)sides;
               double cp = Math.cos(p);
               double sp = Math.sin(p);
               double nx = ox * cp + var77 * sp;
               double ny = oy * cp + var78 * sp;
               double nz = oz * cp + var79 * sp;
               MeshBatch.vertex(base + i * sides + j, cx + nx * wire, cy + ny * wire, cz + nz * wire, nx, ny, nz, col, glint);
            }
         }

         for(int var76 = 0; var76 < steps; ++var76) {
            for(int j = 0; j < sides; ++j) {
               int j2 = (j + 1) % sides;
               MeshBatch.quad(base + var76 * sides + j, base + (var76 + 1) * sides + j, base + (var76 + 1) * sides + j2, base + var76 * sides + j2);
            }
         }
      }

   }

   static {
      for(int i = 0; i < 48; ++i) {
         COS[i] = Math.cos((double)i * 6.283185307179586D / 48.0D);
         SIN[i] = Math.sin((double)i * 6.283185307179586D / 48.0D);
      }

      for(int var1 = 0; var1 < 16; ++var1) {
         KITE_COS[var1] = Math.cos((double)var1 * 6.283185307179586D / 16.0D);
         KITE_SIN[var1] = Math.sin((double)var1 * 6.283185307179586D / 16.0D);
      }

      HAND_ALONG = new double[]{1.0D, 0.12D, -0.22D, -0.22D, 0.12D};
      HAND_ACROSS = new double[]{0.0D, -1.0D, -0.7D, 0.7D, 1.0D};
      E = new double[6];
      TOOTH = new double[]{0.0D, 0.18D, 0.47D, 0.65D};
      TOOTH_MID = (TOOTH[1] + TOOTH[2]) * 0.5D;
      NUMERALS = new KitTextures.Painted("clock numerals", Clockwork::paintNumerals);
   }
}
