package mix.client.mixui.integration.haunt.fx;

public final class Basis {
   public double fx;
   public double fy;
   public double fz;
   public double rx;
   public double ry;
   public double rz;
   public double ux;
   public double uy;
   public double uz;

   public Basis set(double dx, double dy, double dz) {
      double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if(len < 1.0E-9D) {
         dx = 0.0D;
         dy = 0.0D;
         dz = 1.0D;
         len = 1.0D;
      }

      this.fx = dx / len;
      this.fy = dy / len;
      this.fz = dz / len;
      double ax = -this.fz;
      double ay = 0.0D;
      double az = this.fx;
      double al = Math.sqrt(ax * ax + az * az);
      if(al < 1.0E-4D) {
         ax = 1.0D;
         ay = 0.0D;
         az = 0.0D;
         al = 1.0D;
      }

      this.rx = ax / al;
      this.ry = ay / al;
      this.rz = az / al;
      this.ux = this.ry * this.fz - this.rz * this.fy;
      this.uy = this.rz * this.fx - this.rx * this.fz;
      this.uz = this.rx * this.fy - this.ry * this.fx;
      return this;
   }

   public Basis copy(Basis o) {
      this.fx = o.fx;
      this.fy = o.fy;
      this.fz = o.fz;
      this.rx = o.rx;
      this.ry = o.ry;
      this.rz = o.rz;
      this.ux = o.ux;
      this.uy = o.uy;
      this.uz = o.uz;
      return this;
   }

   public double x(double a, double b, double c) {
      return this.rx * a + this.ux * b + this.fx * c;
   }

   public double y(double a, double b, double c) {
      return this.ry * a + this.uy * b + this.fy * c;
   }

   public double z(double a, double b, double c) {
      return this.rz * a + this.uz * b + this.fz * c;
   }
}
