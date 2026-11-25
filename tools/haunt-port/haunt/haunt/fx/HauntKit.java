package mix.client.mixui.integration.haunt.haunt.fx;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.PlayerModel;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public final class HauntKit {
   private static final double LX = 0.32D;
   private static final double LY = 0.86D;
   private static final double LZ = 0.4D;
   private static int lastSwingSeen = Integer.MIN_VALUE;
   private static boolean lastSwinging;
   private static long swingStamp;
   private static double clipY = Double.NaN;
   private static final double[] POLY_IN = new double[20];
   private static final double[] POLY_OUT = new double[30];
   private static final double[] FINGER_X = new double[]{-0.042D, -0.014D, 0.014D, 0.042D};
   private static final double[] FINGER_LEN = new double[]{0.85D, 1.0D, 0.95D, 0.75D};

   private static int shade(int color, double nx, double ny, double nz, float bright) {
      double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
      if(l < 1.0E-9D) {
         l = 1.0D;
      }

      double lit = (nx * 0.32D + ny * 0.86D + nz * 0.4D) / l;
      float k = (float)(0.5D + 0.5D * Math.max(0.0D, lit)) * bright;
      int r = Math.min(255, Math.round((float)(color >> 16 & 255) * k));
      int g = Math.min(255, Math.round((float)(color >> 8 & 255) * k));
      int b = Math.min(255, Math.round((float)(color & 255) * k));
      return color & -16777216 | r << 16 | g << 8 | b;
   }

   public static void face(FxFrame f, HauntKit.Xf t, int tex, int color, float bright, double uvScale, double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double lnx, double lny, double lnz) {
      int c = shade(color, t.nx(lnx, lny, lnz), t.ny(lnx, lny, lnz), t.nz(lnx, lny, lnz), bright);
      double ax = Math.abs(lnx);
      double ay = Math.abs(lny);
      if(clipY == clipY) {
         clipped(f, t, tex, c, uvScale, ax, ay, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
      } else {
         Vfx.quadUV(tex, f.rx(t.px(x0, y0, z0)), f.ry(t.py(x0, y0, z0)), f.rz(t.pz(x0, y0, z0)), uv(ax, ay, x0, y0, z0, true) * uvScale, uv(ax, ay, x0, y0, z0, false) * uvScale, c, f.rx(t.px(x1, y1, z1)), f.ry(t.py(x1, y1, z1)), f.rz(t.pz(x1, y1, z1)), uv(ax, ay, x1, y1, z1, true) * uvScale, uv(ax, ay, x1, y1, z1, false) * uvScale, c, f.rx(t.px(x2, y2, z2)), f.ry(t.py(x2, y2, z2)), f.rz(t.pz(x2, y2, z2)), uv(ax, ay, x2, y2, z2, true) * uvScale, uv(ax, ay, x2, y2, z2, false) * uvScale, c, f.rx(t.px(x3, y3, z3)), f.ry(t.py(x3, y3, z3)), f.rz(t.pz(x3, y3, z3)), uv(ax, ay, x3, y3, z3, true) * uvScale, uv(ax, ay, x3, y3, z3, false) * uvScale, c);
      }
   }

   public static void clipBelow(double worldY) {
      clipY = worldY;
   }

   public static void noClip() {
      clipY = Double.NaN;
   }

   private static void put(double[] p, int k, HauntKit.Xf t, double ax, double ay, double uvScale, double x, double y, double z) {
      int o = k * 5;
      p[o] = t.px(x, y, z);
      p[o + 1] = t.py(x, y, z);
      p[o + 2] = t.pz(x, y, z);
      p[o + 3] = uv(ax, ay, x, y, z, true) * uvScale;
      p[o + 4] = uv(ax, ay, x, y, z, false) * uvScale;
   }

   private static void clipped(FxFrame f, HauntKit.Xf t, int tex, int c, double uvScale, double ax, double ay, double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3) {
      double[] in = POLY_IN;
      double[] out = POLY_OUT;
      put(in, 0, t, ax, ay, uvScale, x0, y0, z0);
      put(in, 1, t, ax, ay, uvScale, x1, y1, z1);
      put(in, 2, t, ax, ay, uvScale, x2, y2, z2);
      put(in, 3, t, ax, ay, uvScale, x3, y3, z3);
      int above = 0;

      for(int k = 0; k < 4; ++k) {
         if(in[k * 5 + 1] >= clipY) {
            ++above;
         }
      }

      if(above != 0) {
         int n = 4;
         double[] src = in;
         if(above != 4) {
            n = 0;

            for(int k = 0; k < 4; ++k) {
               int a = k * 5;
               int b = (k + 1) % 4 * 5;
               double da = in[a + 1] - clipY;
               double db = in[b + 1] - clipY;
               if(da >= 0.0D) {
                  System.arraycopy(in, a, out, n * 5, 5);
                  ++n;
               }

               if(da >= 0.0D != db >= 0.0D) {
                  double s = da / (da - db);

                  for(int j = 0; j < 5; ++j) {
                     out[n * 5 + j] = in[a + j] + (in[b + j] - in[a + j]) * s;
                  }

                  ++n;
               }
            }

            src = out;
         }

         if(n >= 3) {
            emit(f, tex, c, src, 0, 1, 2, n > 3?3:2);
            if(n == 5) {
               emit(f, tex, c, src, 0, 3, 4, 4);
            } else if(n == 6) {
               emit(f, tex, c, src, 0, 3, 4, 5);
            }
         }
      }

   }

   private static void emit(FxFrame f, int tex, int c, double[] p, int i0, int i1, int i2, int i3) {
      int a = i0 * 5;
      int b = i1 * 5;
      int d = i2 * 5;
      int e = i3 * 5;
      Vfx.quadUV(tex, f.rx(p[a]), f.ry(p[a + 1]), f.rz(p[a + 2]), p[a + 3], p[a + 4], c, f.rx(p[b]), f.ry(p[b + 1]), f.rz(p[b + 2]), p[b + 3], p[b + 4], c, f.rx(p[d]), f.ry(p[d + 1]), f.rz(p[d + 2]), p[d + 3], p[d + 4], c, f.rx(p[e]), f.ry(p[e + 1]), f.rz(p[e + 2]), p[e + 3], p[e + 4], c);
   }

   private static double uv(double ax, double ay, double x, double y, double z, boolean u) {
      return ay > 0.7D?(u?x:z):(ax > 0.7D?(u?z:-y):(u?x:-y));
   }

   public static void box(FxFrame f, HauntKit.Xf t, int tex, int color, float bright, double uvScale, double x0, double y0, double z0, double x1, double y1, double z1) {
      face(f, t, tex, color, bright, uvScale, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0.0D, 0.0D, 1.0D);
      face(f, t, tex, color, bright, uvScale, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0.0D, 0.0D, -1.0D);
      face(f, t, tex, color, bright, uvScale, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0.0D, 1.0D, 0.0D);
      face(f, t, tex, color, bright, uvScale, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0.0D, -1.0D, 0.0D);
      face(f, t, tex, color, bright, uvScale, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1.0D, 0.0D, 0.0D);
      face(f, t, tex, color, bright, uvScale, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1.0D, 0.0D, 0.0D);
   }

   public static void roundTop(FxFrame f, HauntKit.Xf t, int tex, int color, float bright, double uvScale, double y, double r, double d, int seg) {
      for(int k = 0; k < seg; ++k) {
         double a0 = 3.141592653589793D * (double)k / (double)seg;
         double a1 = 3.141592653589793D * (double)(k + 1) / (double)seg;
         double x0 = Math.cos(a0) * r;
         double h0 = Math.sin(a0) * r;
         double x1 = Math.cos(a1) * r;
         double h1 = Math.sin(a1) * r;
         face(f, t, tex, color, bright, uvScale, 0.0D, y, d, x0, y + h0, d, x1, y + h1, d, x1, y + h1, d, 0.0D, 0.0D, 1.0D);
         face(f, t, tex, color, bright, uvScale, 0.0D, y, -d, x1, y + h1, -d, x0, y + h0, -d, x0, y + h0, -d, 0.0D, 0.0D, -1.0D);
         double nx = Math.cos((a0 + a1) / 2.0D);
         double ny = Math.sin((a0 + a1) / 2.0D);
         face(f, t, tex, color, bright, uvScale, x0, y + h0, -d, x0, y + h0, d, x1, y + h1, d, x1, y + h1, -d, nx, ny, 0.0D);
      }

   }

   public static void hand(FxFrame f, HauntKit.Xf base, HauntKit.HandPose p, int skinTex, int skin, int nail, float bright, boolean forearm, double forearmLength) {
      HauntKit.Xf t = (new HauntKit.Xf()).set(base);
      if(forearm) {
         box(f, t, skinTex, darker(skin, 0.86F), bright, 3.0D, -0.055D, -forearmLength, -0.048D, 0.055D, 0.0D, 0.048D);
         box(f, t, skinTex, darker(skin, 0.7F), bright, 3.0D, -0.06D, -forearmLength * 0.45D, -0.052D, 0.06D, -forearmLength * 0.3D, 0.052D);
      }

      t.rotX((double)p.wristBend).rotY((double)p.wristTwist);
      box(f, t, skinTex, skin, bright, 3.0D, -0.06D, 0.0D, -0.022D, 0.06D, 0.12D, 0.022D);
      box(f, t, skinTex, darker(skin, 0.8F), bright, 3.0D, -0.05D, 0.02D, 0.018D, 0.05D, 0.09D, 0.03D);
      HauntKit.Xf ft = new HauntKit.Xf();

      for(int k = 0; k < 4; ++k) {
         double len = FINGER_LEN[k];
         ft.set(t).translate(FINGER_X[k], 0.12D, 0.0D).rotZ(-(FINGER_X[k] * 3.0D) * (double)p.spread);
         double[] seg = new double[]{0.052D * len, 0.036D * len, 0.03D * len};
         float c = p.curl[k];

         for(int j = 0; j < 3; ++j) {
            ft.rotX((double)(c * (j == 0?1.2F:1.45F)));
            double w = 0.012D - (double)j * 0.0015D;
            box(f, ft, skinTex, j == 2?darker(skin, 0.92F):skin, bright, 3.0D, -w, 0.0D, -w, w, seg[j], w);
            if(j == 2) {
               box(f, ft, Textures.WHITE, nail, bright, 1.0D, -w * 0.8D, seg[j] * 0.55D, -w * 1.15D, w * 0.8D, seg[j] * 1.05D, -w * 0.4D);
            }

            ft.translate(0.0D, seg[j], 0.0D);
         }
      }

      ft.set(t).translate(-0.06D, 0.035D, 0.008D).rotZ(0.9D - (double)(p.thumb * 0.5F)).rotX((double)(p.thumb * 0.6F));

      for(int j = 0; j < 2; ++j) {
         double w = 0.014D - (double)j * 0.002D;
         double len = j == 0?0.045D:0.035D;
         box(f, ft, skinTex, skin, bright, 3.0D, -w, 0.0D, -w, w, len, w);
         if(j == 1) {
            box(f, ft, Textures.WHITE, nail, bright, 1.0D, -w * 0.8D, len * 0.5D, -w * 1.15D, w * 0.8D, len * 1.05D, -w * 0.4D);
         }

         ft.translate(0.0D, len, 0.0D).rotX((double)(p.thumb * 0.9F));
      }

   }

   public static int darker(int c, float k) {
      int r = Math.round((float)(c >> 16 & 255) * k);
      int g = Math.round((float)(c >> 8 & 255) * k);
      int b = Math.round((float)(c & 255) * k);
      return c & -16777216 | r << 16 | g << 8 | b;
   }

   public static void ring(int tex, double cx, double cy, double cz, double r, double w, int color) {
      int n = 48;
      double rx = Vfx.camera().rightX;
      double ry = Vfx.camera().rightY;
      double rz = Vfx.camera().rightZ;
      double ux = Vfx.camera().upX;
      double uy = Vfx.camera().upY;
      double uz = Vfx.camera().upZ;

      for(int k = 0; k < n; ++k) {
         double a = 6.283185307179586D * (double)k / (double)n;
         double cs = Math.cos(a);
         double sn = Math.sin(a);
         Vfx.X[k] = cx + (rx * cs + ux * sn) * r;
         Vfx.Y[k] = cy + (ry * cs + uy * sn) * r;
         Vfx.Z[k] = cz + (rz * cs + uz * sn) * r;
         Vfx.W[k] = (float)w;
         Vfx.C[k] = color;
      }

      Vfx.ribbon(tex, n, true);
   }

   public static void rotateAbout(BodyProxy.Part p, double px, double py, double pz, double[] q) {
      double dx = p.x - px;
      double dy = p.y - py;
      double dz = p.z - pz;
      p.x = px + q[0] * dx + q[1] * dy + q[2] * dz;
      p.y = py + q[3] * dx + q[4] * dy + q[5] * dz;
      p.z = pz + q[6] * dx + q[7] * dy + q[8] * dz;
      double[] r = new double[9];

      for(int row = 0; row < 3; ++row) {
         for(int col = 0; col < 3; ++col) {
            r[row * 3 + col] = q[row * 3] * p.r[col] + q[row * 3 + 1] * p.r[3 + col] + q[row * 3 + 2] * p.r[6 + col];
         }
      }

      System.arraycopy(r, 0, p.r, 0, 9);
   }

   public static double[] pivot(BodyProxy.Part p, int part, boolean slim) {
      PlayerModel.Box b = PlayerModel.base(part, slim);
      double s = 0.05859375D * (double)p.scale;
      double lx = (double)(-b.cx()) * s;
      double ly = (double)(-b.cy()) * s;
      double lz = (double)(-b.cz()) * s;
      return new double[]{p.x + p.r[0] * lx + p.r[1] * ly + p.r[2] * lz, p.y + p.r[3] * lx + p.r[4] * ly + p.r[5] * lz, p.z + p.r[6] * lx + p.r[7] * ly + p.r[8] * lz};
   }

   public static double[] axisAngle(double ax, double ay, double az, double angle) {
      double l = Math.sqrt(ax * ax + ay * ay + az * az);
      if(l < 1.0E-9D) {
         return new double[]{1.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D, 0.0D, 1.0D};
      } else {
         ax = ax / l;
         ay = ay / l;
         az = az / l;
         double c = Math.cos(angle);
         double s = Math.sin(angle);
         double t = 1.0D - c;
         return new double[]{t * ax * ax + c, t * ax * ay - s * az, t * ax * az + s * ay, t * ax * ay + s * az, t * ay * ay + c, t * ay * az - s * ax, t * ax * az - s * ay, t * ay * az + s * ax, t * az * az + c};
      }
   }

   public static boolean newSwing() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      if(p == null) {
         return false;
      } else {
         boolean swinging = p.isSwingInProgress;
         int progress = p.swingProgressInt;
         boolean started = swinging && (!lastSwinging || progress < lastSwingSeen);
         lastSwinging = swinging;
         lastSwingSeen = progress;
         if(started) {
            long now = System.nanoTime();
            if(now - swingStamp > 60000000L) {
               swingStamp = now;
               return true;
            }
         }

         return false;
      }
   }

   public static boolean lookHits(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double reach) {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      if(p == null) {
         return false;
      } else {
         double ex = p.posX;
         double ey = p.posY + (p.isSneaking()?1.54D:1.62D);
         double ez = p.posZ;
         double yaw = Math.toRadians((double)p.rotationYaw);
         double pitch = Math.toRadians((double)p.rotationPitch);
         double dx = -Math.sin(yaw) * Math.cos(pitch);
         double dy = -Math.sin(pitch);
         double dz = Math.cos(yaw) * Math.cos(pitch);
         double tmin = 0.0D;
         double tmax = reach;
         double[] o = new double[]{ex, ey, ez};
         double[] d = new double[]{dx, dy, dz};
         double[] lo = new double[]{minX, minY, minZ};
         double[] hi = new double[]{maxX, maxY, maxZ};

         for(int k = 0; k < 3; ++k) {
            if(Math.abs(d[k]) < 1.0E-9D) {
               if(o[k] < lo[k] || o[k] > hi[k]) {
                  return false;
               }
            } else {
               double t1 = (lo[k] - o[k]) / d[k];
               double t2 = (hi[k] - o[k]) / d[k];
               if(t1 > t2) {
                  double tt = t1;
                  t1 = t2;
                  t2 = tt;
               }

               tmin = Math.max(tmin, t1);
               tmax = Math.min(tmax, t2);
               if(tmin > tmax) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   public static double[] lookDir() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      if(p == null) {
         return new double[]{0.0D, 0.0D, 1.0D};
      } else {
         double yaw = Math.toRadians((double)p.rotationYaw);
         double pitch = Math.toRadians((double)p.rotationPitch);
         return new double[]{-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)};
      }
   }

   public static final class HandPose {
      public final float[] curl = new float[4];
      public float thumb;
      public float spread = 0.15F;
      public float wristBend;
      public float wristTwist;
   }

   public static final class Xf {
      public double a = 1.0D;
      public double b;
      public double c;
      public double d;
      public double e = 1.0D;
      public double f;
      public double g;
      public double h;
      public double i = 1.0D;
      public double tx;
      public double ty;
      public double tz;

      public HauntKit.Xf set(HauntKit.Xf o) {
         this.a = o.a;
         this.b = o.b;
         this.c = o.c;
         this.d = o.d;
         this.e = o.e;
         this.f = o.f;
         this.g = o.g;
         this.h = o.h;
         this.i = o.i;
         this.tx = o.tx;
         this.ty = o.ty;
         this.tz = o.tz;
         return this;
      }

      public HauntKit.Xf identity() {
         this.a = this.e = this.i = 1.0D;
         this.b = this.c = this.d = this.f = this.g = this.h = 0.0D;
         this.tx = this.ty = this.tz = 0.0D;
         return this;
      }

      private HauntKit.Xf mul(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22, double x, double y, double z) {
         double ntx = this.a * x + this.b * y + this.c * z + this.tx;
         double nty = this.d * x + this.e * y + this.f * z + this.ty;
         double ntz = this.g * x + this.h * y + this.i * z + this.tz;
         double na = this.a * m00 + this.b * m10 + this.c * m20;
         double nb = this.a * m01 + this.b * m11 + this.c * m21;
         double nc = this.a * m02 + this.b * m12 + this.c * m22;
         double nd = this.d * m00 + this.e * m10 + this.f * m20;
         double ne = this.d * m01 + this.e * m11 + this.f * m21;
         double nf = this.d * m02 + this.e * m12 + this.f * m22;
         double ng = this.g * m00 + this.h * m10 + this.i * m20;
         double nh = this.g * m01 + this.h * m11 + this.i * m21;
         double ni = this.g * m02 + this.h * m12 + this.i * m22;
         this.a = na;
         this.b = nb;
         this.c = nc;
         this.d = nd;
         this.e = ne;
         this.f = nf;
         this.g = ng;
         this.h = nh;
         this.i = ni;
         this.tx = ntx;
         this.ty = nty;
         this.tz = ntz;
         return this;
      }

      public HauntKit.Xf translate(double x, double y, double z) {
         return this.mul(1.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D, 0.0D, 1.0D, x, y, z);
      }

      public HauntKit.Xf rotX(double r) {
         double cs = Math.cos(r);
         double sn = Math.sin(r);
         return this.mul(1.0D, 0.0D, 0.0D, 0.0D, cs, -sn, 0.0D, sn, cs, 0.0D, 0.0D, 0.0D);
      }

      public HauntKit.Xf rotY(double r) {
         double cs = Math.cos(r);
         double sn = Math.sin(r);
         return this.mul(cs, 0.0D, sn, 0.0D, 1.0D, 0.0D, -sn, 0.0D, cs, 0.0D, 0.0D, 0.0D);
      }

      public HauntKit.Xf rotZ(double r) {
         double cs = Math.cos(r);
         double sn = Math.sin(r);
         return this.mul(cs, -sn, 0.0D, sn, cs, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D, 0.0D);
      }

      public HauntKit.Xf scale(double s) {
         return this.mul(s, 0.0D, 0.0D, 0.0D, s, 0.0D, 0.0D, 0.0D, s, 0.0D, 0.0D, 0.0D);
      }

      public HauntKit.Xf scale(double x, double y, double z) {
         return this.mul(x, 0.0D, 0.0D, 0.0D, y, 0.0D, 0.0D, 0.0D, z, 0.0D, 0.0D, 0.0D);
      }

      public double px(double x, double y, double z) {
         return this.a * x + this.b * y + this.c * z + this.tx;
      }

      public double py(double x, double y, double z) {
         return this.d * x + this.e * y + this.f * z + this.ty;
      }

      public double pz(double x, double y, double z) {
         return this.g * x + this.h * y + this.i * z + this.tz;
      }

      public double nx(double x, double y, double z) {
         return this.a * x + this.b * y + this.c * z;
      }

      public double ny(double x, double y, double z) {
         return this.d * x + this.e * y + this.f * z;
      }

      public double nz(double x, double y, double z) {
         return this.g * x + this.h * y + this.i * z;
      }
   }
}
