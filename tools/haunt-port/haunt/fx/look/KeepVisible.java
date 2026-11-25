package mix.client.mixui.integration.haunt.fx.look;

import mix.client.mixui.integration.haunt.render.Camera;

public final class KeepVisible {
   public static final double CLEARANCE = 0.3D;
   private static final double FACE_CLEARANCE = -0.1D;
   private static final double EASE = 0.2D;
   private boolean on;
   private float inWay = 0.3F;
   private float darkCap = 0.35F;
   private double ex;
   private double ey;
   private double ez;
   private double rx;
   private double ry;
   private double rz;
   private double ux;
   private double uy;
   private double uz;
   private double fx;
   private double fy;
   private double fz;
   private double centerDepth;
   private double halfDepth;
   private double sxC;
   private double syC;
   private double halfW;
   private double halfH;
   private double soft;

   public void off() {
      this.on = false;
   }

   public void set(Camera cam, double feetX, double feetY, double feetZ, float width, float height, boolean enabled, float inWayOpacity, float darkOpacity) {
      this.on = false;
      if(enabled && cam != null) {
         this.inWay = inWayOpacity;
         this.darkCap = darkOpacity;
         this.ex = cam.posX;
         this.ey = cam.posY;
         this.ez = cam.posZ;
         this.rx = cam.rightX;
         this.ry = cam.rightY;
         this.rz = cam.rightZ;
         this.ux = cam.upX;
         this.uy = cam.upY;
         this.uz = cam.upZ;
         this.fx = cam.forwardX;
         this.fy = cam.forwardY;
         this.fz = cam.forwardZ;
         double cx = feetX - this.ex;
         double cy = feetY + (double)height * 0.5D - this.ey;
         double cz = feetZ - this.ez;
         this.centerDepth = cx * this.fx + cy * this.fy + cz * this.fz;
         if(this.centerDepth >= 0.4D) {
            this.halfDepth = (double)width * 0.5D;
            this.sxC = (cx * this.rx + cy * this.ry + cz * this.rz) / this.centerDepth;
            this.syC = (cx * this.ux + cy * this.uy + cz * this.uz) / this.centerDepth;
            this.halfW = (double)width * 0.71D / this.centerDepth;
            this.halfH = (double)height * 0.5D / this.centerDepth;
            this.soft = 0.12D / this.centerDepth;
            this.on = true;
         }
      }

   }

   public boolean isOn() {
      return this.on;
   }

   public float solid(double x, double y, double z) {
      if(!this.on) {
         return 1.0F;
      } else {
         double f = this.over(x, y, z, 0.3D);
         return (float)(1.0D - f * (1.0D - (double)this.inWay));
      }
   }

   public float face(double x, double y, double z) {
      if(!this.on) {
         return 1.0F;
      } else {
         double f = this.over(x, y, z, -0.1D);
         return (float)(1.0D - f * (1.0D - (double)this.inWay));
      }
   }

   public float cover(double x, double y, double z) {
      return this.face(x, y, z);
   }

   public float darkCap() {
      return this.on?this.darkCap:1.0F;
   }

   public float darkFront(double x, double y, double z) {
      return this.on?(float)this.over(x, y, z, -this.halfDepth):0.0F;
   }

   public float dark(double x, double y, double z, float alpha) {
      if(this.on && alpha > this.darkCap) {
         double f = this.over(x, y, z, -this.halfDepth);
         return (float)((double)alpha + (double)(this.darkCap - alpha) * f);
      } else {
         return alpha;
      }
   }

   private double over(double x, double y, double z, double clearance) {
      double dx = x - this.ex;
      double dy = y - this.ey;
      double dz = z - this.ez;
      double d = dx * this.fx + dy * this.fy + dz * this.fz;
      if(d <= 0.05D) {
         return 0.0D;
      } else {
         double ahead = this.centerDepth - this.halfDepth - d;
         double front = smooth(clearance, clearance + 0.2D, ahead);
         if(front <= 0.0D) {
            return 0.0D;
         } else {
            double sx = Math.abs((dx * this.rx + dy * this.ry + dz * this.rz) / d - this.sxC);
            double sy = Math.abs((dx * this.ux + dy * this.uy + dz * this.uz) / d - this.syC);
            double in = (1.0D - smooth(this.halfW, this.halfW + this.soft, sx)) * (1.0D - smooth(this.halfH, this.halfH + this.soft, sy));
            return front * in;
         }
      }
   }

   private static double smooth(double e0, double e1, double v) {
      double t = (v - e0) / (e1 - e0);
      t = t < 0.0D?0.0D:(t > 1.0D?1.0D:t);
      return t * t * (3.0D - 2.0D * t);
   }
}
