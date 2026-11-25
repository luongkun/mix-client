package mix.client.mixui.integration.haunt.fx.trail;

import mix.client.mixui.integration.haunt.render.Vfx;

public final class TrailStrip {
   public static final int FACING = 0;
   public static final int FLAT = 1;
   public static final int UPRIGHT = 2;
   private static final double[] SX = new double[1024];
   private static final double[] SY = new double[1024];
   private static final double[] SZ = new double[1024];
   private static final double[] TX = new double[1024];
   private static final double[] TY = new double[1024];
   private static final double[] TZ = new double[1024];

   public static void sides(int from, int to, int mode, double cx, double cy, double cz, float twist, float[] along, int alongFrom) {
      if(to - from >= 2) {
         double lastX = 0.0D;
         double lastY = 1.0D;
         double lastZ = 0.0D;
         boolean have = false;

         for(int i = from; i < to; ++i) {
            int ip = Math.max(from, i - 1);
            int in = Math.min(to - 1, i + 1);
            double tx = Vfx.X[in] - Vfx.X[ip];
            double tz;
            double ty;
            double tl = Math.sqrt(tx * tx + (ty = Vfx.Y[in] - Vfx.Y[ip]) * ty + (tz = Vfx.Z[in] - Vfx.Z[ip]) * tz);
            if(tl > 1.0E-9D) {
               tx = tx / tl;
               ty = ty / tl;
               tz = tz / tl;
            } else {
               tx = 0.0D;
               ty = 0.0D;
               tz = 1.0D;
            }

            TX[i] = tx;
            TY[i] = ty;
            TZ[i] = tz;
            double sz;
            double sy;
            double sx;
            if(mode == 2) {
               sx = 0.0D;
               sy = 1.0D;
               sz = 0.0D;
            } else if(mode == 1) {
               sx = -tz;
               sy = 0.0D;
               sz = tx;
            } else {
               double vx = cx - Vfx.X[i];
               double vy = cy - Vfx.Y[i];
               double vz = cz - Vfx.Z[i];
               sx = ty * vz - tz * vy;
               sy = tz * vx - tx * vz;
               sz = tx * vy - ty * vx;
            }

            double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if(sl < 1.0E-6D) {
               if(have) {
                  sx = lastX;
                  sy = lastY;
                  sz = lastZ;
               } else {
                  sx = 1.0D;
                  sy = 0.0D;
                  sz = 0.0D;
               }
            } else {
               sx = sx / sl;
               sy = sy / sl;
               sz = sz / sl;
               if(have && sx * lastX + sy * lastY + sz * lastZ < 0.0D) {
                  sx = -sx;
                  sy = -sy;
                  sz = -sz;
               }
            }

            if(mode != 0 && twist != 0.0F && along != null) {
               double a = (double)(along[alongFrom + (i - from)] * twist) * 3.141592653589793D * 2.0D;
               double cos = Math.cos(a);
               double sin = Math.sin(a);
               double kx = ty * sz - tz * sy;
               double ky = tz * sx - tx * sz;
               double kz = tx * sy - ty * sx;
               sx = sx * cos + kx * sin;
               sy = sy * cos + ky * sin;
               sz = sz * cos + kz * sin;
            }

            SX[i] = sx;
            SY[i] = sy;
            SZ[i] = sz;
            lastX = sx;
            lastY = sy;
            lastZ = sz;
            have = true;
         }

         if(mode == 0) {
            double px = SX[from];
            double py = SY[from];
            double pz = SZ[from];

            for(int i = from + 1; i < to - 1; ++i) {
               double nx = (px + SX[i] * 2.0D + SX[i + 1]) * 0.25D;
               double ny = (py + SY[i] * 2.0D + SY[i + 1]) * 0.25D;
               double nz = (pz + SZ[i] * 2.0D + SZ[i + 1]) * 0.25D;
               px = SX[i];
               py = SY[i];
               pz = SZ[i];
               double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
               if(l > 1.0E-6D) {
                  SX[i] = nx / l;
                  SY[i] = ny / l;
                  SZ[i] = nz / l;
               }
            }
         }
      }

   }

   public static void draw(int texture, int from, int to, float widthScale, double offset, float alpha) {
      if(to - from >= 2 && alpha > 0.0F) {
         double pLx = 0.0D;
         double pLy = 0.0D;
         double pLz = 0.0D;
         double pRx = 0.0D;
         double pRy = 0.0D;
         double pRz = 0.0D;
         int pC = 0;

         for(int i = from; i < to; ++i) {
            double hw = (double)(Vfx.W[i] * widthScale) * 0.5D;
            double o = (double)Vfx.W[i] * offset;
            double cx = Vfx.X[i] + SX[i] * o;
            double cy = Vfx.Y[i] + SY[i] * o;
            double cz = Vfx.Z[i] + SZ[i] * o;
            double lx = cx - SX[i] * hw;
            double ly = cy - SY[i] * hw;
            double lz = cz - SZ[i] * hw;
            double rx = cx + SX[i] * hw;
            double ry = cy + SY[i] * hw;
            double rz = cz + SZ[i] * hw;
            int c = alpha >= 0.999F?Vfx.C[i]:scaleAlpha(Vfx.C[i], alpha);
            if(i > from && (pC | c) >>> 24 != 0) {
               double u0 = (double)(i - 1 - from) / (double)(to - from);
               double u1 = (double)(i - from) / (double)(to - from);
               Vfx.quadUV(texture, pLx, pLy, pLz, u0, 0.0D, pC, pRx, pRy, pRz, u0, 1.0D, pC, rx, ry, rz, u1, 1.0D, c, lx, ly, lz, u1, 0.0D, c);
            }

            pLx = lx;
            pLy = ly;
            pLz = lz;
            pRx = rx;
            pRy = ry;
            pRz = rz;
            pC = c;
         }
      }

   }

   public static void drawAlong(int texture, int from, int to, float widthScale, double offset, float alpha, float[] u) {
      if(to - from >= 2 && alpha > 0.0F) {
         double pLx = 0.0D;
         double pLy = 0.0D;
         double pLz = 0.0D;
         double pRx = 0.0D;
         double pRy = 0.0D;
         double pRz = 0.0D;
         int pC = 0;
         float pU = 0.0F;

         for(int i = from; i < to; ++i) {
            double hw = (double)(Vfx.W[i] * widthScale) * 0.5D;
            double o = (double)Vfx.W[i] * offset;
            double cx = Vfx.X[i] + SX[i] * o;
            double cy = Vfx.Y[i] + SY[i] * o;
            double cz = Vfx.Z[i] + SZ[i] * o;
            double lx = cx - SX[i] * hw;
            double ly = cy - SY[i] * hw;
            double lz = cz - SZ[i] * hw;
            double rx = cx + SX[i] * hw;
            double ry = cy + SY[i] * hw;
            double rz = cz + SZ[i] * hw;
            int c = alpha >= 0.999F?Vfx.C[i]:scaleAlpha(Vfx.C[i], alpha);
            float uu = u[i - from];
            if(i > from && (pC | c) >>> 24 != 0) {
               Vfx.quadUV(texture, pLx, pLy, pLz, (double)pU, 0.0D, pC, pRx, pRy, pRz, (double)pU, 1.0D, pC, rx, ry, rz, (double)uu, 1.0D, c, lx, ly, lz, (double)uu, 0.0D, c);
            }

            pLx = lx;
            pLy = ly;
            pLz = lz;
            pRx = rx;
            pRy = ry;
            pRz = rz;
            pC = c;
            pU = uu;
         }
      }

   }

   private static int scaleAlpha(int c, float a) {
      int al = Math.round((float)(c >>> 24) * Math.max(0.0F, Math.min(1.0F, a)));
      return al << 24 | c & 16777215;
   }
}
