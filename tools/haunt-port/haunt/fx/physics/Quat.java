package mix.client.mixui.integration.haunt.fx.physics;

final class Quat {
   private static final double[] R = new double[9];

   static void toMatrix(double[] q, double[] m) {
      double x = q[0];
      double y = q[1];
      double z = q[2];
      double w = q[3];
      double xx = x * x;
      double yy = y * y;
      double zz = z * z;
      double xy = x * y;
      double xz = x * z;
      double yz = y * z;
      double wx = w * x;
      double wy = w * y;
      double wz = w * z;
      m[0] = 1.0D - 2.0D * (yy + zz);
      m[1] = 2.0D * (xy - wz);
      m[2] = 2.0D * (xz + wy);
      m[3] = 2.0D * (xy + wz);
      m[4] = 1.0D - 2.0D * (xx + zz);
      m[5] = 2.0D * (yz - wx);
      m[6] = 2.0D * (xz - wy);
      m[7] = 2.0D * (yz + wx);
      m[8] = 1.0D - 2.0D * (xx + yy);
   }

   static void fromMatrix(double[] m, double[] q) {
      double trace = m[0] + m[4] + m[8];
      if(trace > 0.0D) {
         double s = Math.sqrt(trace + 1.0D) * 2.0D;
         q[3] = 0.25D * s;
         q[0] = (m[7] - m[5]) / s;
         q[1] = (m[2] - m[6]) / s;
         q[2] = (m[3] - m[1]) / s;
      } else if(m[0] > m[4] && m[0] > m[8]) {
         double s = Math.sqrt(1.0D + m[0] - m[4] - m[8]) * 2.0D;
         q[3] = (m[7] - m[5]) / s;
         q[0] = 0.25D * s;
         q[1] = (m[1] + m[3]) / s;
         q[2] = (m[2] + m[6]) / s;
      } else if(m[4] > m[8]) {
         double s = Math.sqrt(1.0D + m[4] - m[0] - m[8]) * 2.0D;
         q[3] = (m[2] - m[6]) / s;
         q[0] = (m[1] + m[3]) / s;
         q[1] = 0.25D * s;
         q[2] = (m[5] + m[7]) / s;
      } else {
         double s = Math.sqrt(1.0D + m[8] - m[0] - m[4]) * 2.0D;
         q[3] = (m[3] - m[1]) / s;
         q[0] = (m[2] + m[6]) / s;
         q[1] = (m[5] + m[7]) / s;
         q[2] = 0.25D * s;
      }

      normalize(q);
   }

   static void normalize(double[] q) {
      double l = Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
      if(l < 1.0E-12D) {
         q[2] = 0.0D;
         q[1] = 0.0D;
         q[0] = 0.0D;
         q[3] = 1.0D;
      } else {
         q[0] /= l;
         q[1] /= l;
         q[2] /= l;
         q[3] /= l;
      }

   }

   static void extractRotation(double[] a, double[] q, int iterations) {
      for(int it = 0; it < iterations; ++it) {
         toMatrix(q, R);
         double ox = 0.0D;
         double oy = 0.0D;
         double oz = 0.0D;
         double dot = 0.0D;

         for(int c = 0; c < 3; ++c) {
            double rx = R[c];
            double ry = R[3 + c];
            double rz = R[6 + c];
            double ax = a[c];
            double ay = a[3 + c];
            double az = a[6 + c];
            ox += ry * az - rz * ay;
            oy += rz * ax - rx * az;
            oz += rx * ay - ry * ax;
            dot += rx * ax + ry * ay + rz * az;
         }

         double k = 1.0D / (Math.abs(dot) + 1.0E-9D);
         double w;
         if((w = Math.sqrt((ox = ox * k) * ox + (oy = oy * k) * oy + (oz = oz * k) * oz)) < 1.0E-9D) {
            break;
         }

         double s = Math.sin(w * 0.5D) / w;
         double c = Math.cos(w * 0.5D);
         double px = ox * s;
         double py = oy * s;
         double pz = oz * s;
         double nx = c * q[0] + px * q[3] + py * q[2] - pz * q[1];
         double ny = c * q[1] - px * q[2] + py * q[3] + pz * q[0];
         double nz = c * q[2] + px * q[1] - py * q[0] + pz * q[3];
         double nw = c * q[3] - px * q[0] - py * q[1] - pz * q[2];
         q[0] = nx;
         q[1] = ny;
         q[2] = nz;
         q[3] = nw;
         normalize(q);
      }

   }

   static void nlerp(double[] a, double[] b, double t, double[] out) {
      double sign = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3] < 0.0D?-1.0D:1.0D;

      for(int i = 0; i < 4; ++i) {
         out[i] = a[i] + (b[i] * sign - a[i]) * t;
      }

      normalize(out);
   }
}
