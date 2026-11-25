package mix.client.mixui.integration.haunt.fx.trail;

import mix.client.mixui.integration.haunt.fx.trail.TrailPose;
import mix.client.mixui.integration.haunt.setting.Displayable;

public enum TrailAnchor implements Displayable {
   FEET("Feet", "At the feet"),
   BOTH_FEET("Both feet", "One trail from each foot"),
   WAIST("Waist", "At the hips"),
   BACK("Back", "Just behind the back"),
   HEAD("Head", "At the head"),
   HANDS("Hands", "One trail from each hand");

   private static final double PX = 0.05859375D;
   private static final double SNEAK_LEAN = 0.5D;
   private final String name;
   private final String description;

   private TrailAnchor(String name, String description) {
      this.name = name;
      this.description = description;
   }

   public String displayName() {
      return this.name;
   }

   public String description() {
      return this.description;
   }

   public int sides() {
      return this != BOTH_FEET && this != HANDS?1:2;
   }

   public void place(TrailPose p, int side, double[] out) {
      double yaw = Math.toRadians((double)p.bodyYaw);
      double fx = -Math.sin(yaw);
      double fz = Math.cos(yaw);
      double rx = -fz;
      double right = 0.0D;
      double mz = 0.0D;
      boolean r = side == 1;
      double my;
      switch(this) {
      case FEET:
         out[0] = p.x;
         out[1] = p.y + 0.06D;
         out[2] = p.z;
         return;
      case BOTH_FEET:
         double b = (double)p.legAngle(r);
         double hy = p.sneaking?9.0D:12.0D;
         double hz = p.sneaking?4.0D:0.0D;
         right = r?1.9D:-1.9D;
         my = hy + 11.0D * Math.cos(b);
         mz = hz + 11.0D * Math.sin(b);
         break;
      case HANDS:
         double a = (double)p.armAngle(r);
         right = r?6.0D:-6.0D;
         my = 2.0D + 10.0D * Math.cos(a);
         mz = 10.0D * Math.sin(a);
         break;
      case WAIST:
         my = 9.5D;
         break;
      case BACK:
         my = 2.7D;
         mz = 4.8D;
         break;
      default:
         my = p.sneaking?-3.0D:-4.0D;
      }

      if(p.sneaking && (this == WAIST || this == BACK)) {
         double c = Math.cos(0.5D);
         double s = Math.sin(0.5D);
         double ny = my * c - mz * s;
         double nz = my * s + mz * c;
         my = ny;
         mz = nz;
      }

      double up = (24.0D - my) * 0.05859375D;
      if(p.sneaking) {
         up -= 0.1875D + (p.other?0.125D:0.0D);
      }

      double fwd = -mz * 0.05859375D;
      double side2 = right * 0.05859375D;
      out[0] = p.x + fx * fwd + rx * side2;
      out[1] = p.y + up;
      out[2] = p.z + fz * fwd + fx * side2;
   }
}
