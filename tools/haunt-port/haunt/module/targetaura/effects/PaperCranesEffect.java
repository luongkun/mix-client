package mix.client.mixui.integration.haunt.module.targetaura.effects;

import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.kit.Paper;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class PaperCranesEffect extends AuraEffect {
   private static final int[] PASTELS = new int[]{-4855081, -3682582, -9535, -1904437, -801541, -6241025, -3672, -14619};
   public final ModeSetting<PaperCranesEffect.PaperColors> paperColors = (ModeSetting)this.setting(new ModeSetting("papercranes.paperColors", "Paper Colors", "Each crane a different pastel, all the Paper color, or the Paper color\'s hue turning across the flock.", PaperCranesEffect.PaperColors.class, PaperCranesEffect.PaperColors.PASTEL));
   public final SliderSetting cranes = this.look((SliderSetting)this.setting(new SliderSetting("papercranes.cranes", "Cranes", "How many cranes fly round the target.", 3.0D, 9.0D, 6.0D, 1.0D)));
   public final ModeSetting<PaperCranesEffect.Flight> flight = (ModeSetting)this.setting((new ModeSetting("papercranes.flight", "Flight", "How the cranes fly: round the target, each its own way, or rising and dipping round it.", PaperCranesEffect.Flight.class, PaperCranesEffect.Flight.ORBIT)).style(ModeSetting.Style.CHIPS));
   public final SliderSetting flapSpeed = this.look((SliderSetting)this.setting((new SliderSetting("papercranes.flapSpeed", "Flap Speed", "How fast the wings beat.", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%")));
   public final BooleanSetting perch = (BooleanSetting)this.setting(new BooleanSetting("papercranes.perch", "Perch", "Now and then a crane lands on the target\'s shoulder for a moment.", true));
   public final BooleanSetting paperTexture = (BooleanSetting)this.setting(new BooleanSetting("papercranes.paperTexture", "Paper Texture", "The fine fibers of the paper.", true));
   private static final int MAX_CRANES = 9;
   private final Paper.Pose pose = new Paper.Pose();
   private final double[] cx = new double[9];
   private final double[] cy = new double[9];
   private final double[] cz = new double[9];
   private final double[] fx = new double[9];
   private final double[] fy = new double[9];
   private final double[] fz = new double[9];
   private final double[] ux = new double[9];
   private final double[] uy = new double[9];
   private final double[] uz = new double[9];
   private final double[] rx = new double[9];
   private final double[] ry = new double[9];
   private final double[] rz = new double[9];
   private final double[] scale = new double[9];
   private final double[] fold = new double[9];
   private final double[] flap = new double[9];
   private final float[] alphaOf = new float[9];
   private final int[] colorOf = new int[9];
   private final double[] path = new double[3];
   private final double[] pathA = new double[3];
   private final double[] pathB = new double[3];
   private int count;
   private final float[] hsb = new float[3];

   public PaperCranesEffect(TargetAuraModule module) {
      super(module, EffectType.PAPER_CRANES, "papercranes", new ColorSetting("papercranes.color", "Paper", "The cranes\' paper (the first crane\'s in the pastel set).", ColorSetting.Mode.STATIC, -19294, -6241025, 1.0D), true);
   }

   public void render(EffectContext c, FxPass pass) {
      if(pass == FxPass.SOLID) {
         float alpha = this.intensity(c);
         if(alpha > 0.003F) {
            this.layout(c);
            MeshBatch.begin(Material.CUSTOM, Finish.MATTE, c.light, 0.08F, -1, false, true);

            for(int k = 0; k < this.count; ++k) {
               this.crane(c, k, alpha);
            }

            MeshBatch.end();
            if(this.paperTexture.get()) {
               RenderState.decal(true);

               try {
                  for(int var8 = 0; var8 < this.count; ++var8) {
                     this.fibersOn(c, var8, alpha);
                  }
               } finally {
                  RenderState.decal(false);
               }
            }
         }
      }

   }

   private void layout(EffectContext c) {
      double s = (double)this.size(c);
      double t = c.time;
      double spd = (double)this.speed();
      int n;
      this.count = n = this.cranes.getInt();
      float out = c.outro;
      double perchEvery = 6.5D;
      double perchLong = 3.0D;
      long perchIndex = (long)Math.floor((t - 2.0D) / perchEvery);
      double perchAt = t - 2.0D - (double)perchIndex * perchEvery;
      int percher = this.perch.get() && t > 2.0D && perchAt < perchLong?(int)(perchIndex % (long)n):-1;
      int base = this.color(c, 0.5F);

      for(int k = 0; k < n; ++k) {
         this.pathAt(c, k, n, t * spd, s, this.path);
         this.pathAt(c, k, n, t * spd - 0.05D, s, this.pathA);
         this.pathAt(c, k, n, t * spd + 0.05D, s, this.pathB);
         double px = this.path[0];
         double py = this.path[1];
         double pz = this.path[2];
         double hx = this.pathB[0] - this.pathA[0];
         double hy = this.pathB[1] - this.pathA[1];
         double hz = this.pathB[2] - this.pathA[2];
         double kx = this.pathB[0] - 2.0D * px + this.pathA[0];
         double kz = this.pathB[2] - 2.0D * pz + this.pathA[2];
         double sc = 0.13D * s;
         double hl = Math.sqrt(hx * hx + hz * hz) + 1.0E-9D;
         double side = (kx * -hz + kz * hx) / hl;
         double bank = clamp(-side / Math.max(1.0E-4D, hl * hl) * 1.6D, -0.6D, 0.6D);
         if(k == percher) {
            double yaw = Math.toRadians((double)c.bodyYaw);
            side = (perchIndex & 1L) == 0L?1.0D:-1.0D;
            double sx = c.x - Math.cos(yaw) * 0.3D * side;
            double sy = c.y + 1.42D + sc * 0.28D;
            double sz = c.z - Math.sin(yaw) * 0.3D * side;
            double w = (double)(Easing.smoothstep(0.0F, 0.7F, (float)perchAt) * (1.0F - Easing.smoothstep(2.2F, 3.0F, (float)perchAt)));
            px += (sx - px) * w;
            py += (sy - py) * w + Math.sin(w * 3.141592653589793D) * 0.25D;
            pz += (sz - pz) * w;
            double fxs = -Math.sin(yaw);
            double fzs = Math.cos(yaw);
            hx += (fxs - hx) * w;
            hy *= 1.0D - w;
            hz += (fzs - hz) * w;
            bank *= 1.0D - w;
         }

         if((hl = Math.sqrt(hx * hx + hy * hy + hz * hz)) < 1.0E-9D) {
            hx = 1.0D;
            hy = 0.0D;
            hz = 0.0D;
            hl = 1.0D;
         }

         double var97;
         double r0x = -(var97 = hz / hl);
         double var95;
         double r0z = var95 = hx / hl;
         double rl = Math.sqrt(r0x * r0x + r0z * r0z) + 1.0E-9D;
         double var96;
         double var103;
         double u0x = -(var103 = r0z / rl) * (var96 = hy / hl);
         double var101;
         double u0y = var103 * var95 - (var101 = r0x / rl) * var97;
         double u0z = var101 * var96;
         double cb = Math.cos(bank);
         double sb = Math.sin(bank);
         double rrx = var101 * cb + u0x * sb;
         double rry = u0y * sb;
         double rrz = var103 * cb + u0z * sb;
         double uux = u0x * cb - var101 * sb;
         double uuy = u0y * cb;
         double uuz = u0z * cb - var103 * sb;
         double introFold = (double)Easing.inOutCubic(Easing.clamp01((t - 0.06D * (double)k) / 0.3D));
         double unfold = (double)Easing.smoothstep(0.0F, 0.45F, out);
         double f = introFold * (1.0D - unfold);
         if(out > 0.0F) {
            double fall = (double)(out * out) * 1.1D;
            py -= fall;
            px += rrx * Math.sin((double)out * 9.0D + (double)k) * 0.12D * (double)out;
            pz += rrz * Math.sin((double)out * 9.0D + (double)k) * 0.12D * (double)out;
         }

         this.cx[k] = px;
         this.cy[k] = py;
         this.cz[k] = pz;
         double flatTurn = (1.0D - f) * (out > 0.0F?(double)out * 4.0D + (double)k:0.0D);
         double ct = Math.cos(flatTurn);
         double st = Math.sin(flatTurn);
         this.fx[k] = var95 * ct + rrx * st;
         this.fy[k] = var96 * ct + rry * st;
         this.fz[k] = var97 * ct + rrz * st;
         this.rx[k] = rrx * ct - var95 * st;
         this.ry[k] = rry * ct - var96 * st;
         this.rz[k] = rrz * ct - var97 * st;
         this.ux[k] = uux;
         this.uy[k] = uuy;
         this.uz[k] = uuz;
         this.scale[k] = sc;
         this.fold[k] = f;
         double period = 1.2D / Math.max(0.25D, (double)this.flapSpeed.getFloat() / 100.0D * spd);
         double cyc = (t + (double)k * 0.37D) % period / period;
         double beat = cyc < 0.4D?Math.sin(cyc / 0.4D * 3.141592653589793D * 2.0D * 2.0D) * 0.85D:0.18D + 0.05D * Math.sin(t * 2.0D + (double)k);
         if(k == percher) {
            double w = (double)(Easing.smoothstep(0.5F, 0.8F, (float)perchAt) * (1.0F - Easing.smoothstep(2.1F, 2.4F, (float)perchAt)));
            beat += (0.95D - beat) * w;
         }

         this.flap[k] = beat;
         this.alphaOf[k] = (float)((double)c.keep.solid(px, py, pz) * (1.0D - (double)Easing.smoothstep(0.7F, 1.0F, out)));
         this.colorOf[k] = this.paperColor(base, k, n);
      }

   }

   public String stateForTest(EffectContext c) {
      this.layout(c);
      StringBuilder sb = new StringBuilder(this.count + " cranes, folded");

      for(int k = 0; k < this.count; ++k) {
         sb.append(String.format(Locale.ROOT, " %.2f", new Object[]{Double.valueOf(this.fold[k])}));
      }

      double dx = this.cx[0] - c.x;
      double dz = this.cz[0] - c.z;
      sb.append(String.format(Locale.ROOT, "; crane 1 %.2f up, %.2f out", new Object[]{Double.valueOf(this.cy[0] - c.y), Double.valueOf(Math.sqrt(dx * dx + dz * dz))}));
      return sb.toString();
   }

   private void pathAt(EffectContext c, int k, int n, double t, double s, double[] out) {
      double radius = 0.9D * (0.8D + 0.2D * s);
      double phase = 6.283185307179586D * (double)k / (double)n;
      double hk = (double)hash(k, 41);
      double hk2 = (double)hash(k, 43);
      switch((PaperCranesEffect.Flight)this.flight.get()) {
      case SWARM:
         double a = t * (0.55D + 0.35D * hk) + phase * 1.7D;
         double b = t * (0.37D + 0.25D * hk2) + phase;
         double r = radius * (0.8D + 0.25D * Math.sin(b * 1.3D + (double)k));
         out[0] = c.x + Math.cos(a) * r;
         out[1] = c.y + 1.15D + 0.5D * Math.sin(b) * (0.8D + 0.4D * hk);
         out[2] = c.z + Math.sin(a) * r;
         break;
      case FIGURE_EIGHT:
         double a = t * 0.8D + phase;
         double r = radius * (1.0D + 0.06D * Math.sin(a * 3.0D + (double)k));
         out[0] = c.x + Math.cos(a) * r;
         out[1] = c.y + 1.2D + 0.42D * Math.sin(2.0D * a);
         out[2] = c.z + Math.sin(a) * r;
         break;
      default:
         double a = t * (0.7D + 0.18D * hk) + phase;
         double r = radius * (1.0D + 0.08D * Math.sin(a * 2.0D + (double)k * 1.3D));
         out[0] = c.x + Math.cos(a) * r;
         out[1] = c.y + 0.65D + 1.15D * (((double)k + 0.5D) / (double)n) + 0.08D * Math.sin(a * 3.0D + (double)k);
         out[2] = c.z + Math.sin(a) * r;
      }

   }

   private int paperColor(int base, int k, int n) {
      switch((PaperCranesEffect.PaperColors)this.paperColors.get()) {
      case ONE:
         return base;
      case GRADIENT:
         float t = n <= 1?0.0F:(float)k / (float)(n - 1);
         return Paper.hueTurn(base, (t - 0.5F) * 0.33F, this.hsb);
      default:
         return k == 0?base:PASTELS[(k - 1) % PASTELS.length];
      }
   }

   private Paper.Pose pose(int k) {
      this.pose.x = this.cx[k];
      this.pose.y = this.cy[k];
      this.pose.z = this.cz[k];
      this.pose.fx = this.fx[k];
      this.pose.fy = this.fy[k];
      this.pose.fz = this.fz[k];
      this.pose.ux = this.ux[k];
      this.pose.uy = this.uy[k];
      this.pose.uz = this.uz[k];
      this.pose.rx = this.rx[k];
      this.pose.ry = this.ry[k];
      this.pose.rz = this.rz[k];
      this.pose.scale = this.scale[k];
      this.pose.fold = this.fold[k];
      this.pose.flap = this.flap[k];
      return this.pose;
   }

   private void crane(EffectContext c, int k, float alpha) {
      float a = alpha * this.alphaOf[k];
      if(a > 0.003F && this.scale[k] > 1.0E-4D) {
         Paper.facets(Paper.CRANE, this.pose(k), Math.round(255.0F * Easing.clamp01(a)) << 24 | this.colorOf[k] & 16777215);
      }

   }

   private void fibersOn(EffectContext c, int k, float alpha) {
      float a = alpha * this.alphaOf[k] * 0.55F;
      if(a > 0.01F && this.scale[k] > 1.0E-4D) {
         Paper.fibers(Paper.CRANE, this.pose(k), Math.round(255.0F * a) << 24 | ColorUtil.shade(this.colorOf[k], 0.55F * c.light) & 16777215);
      }

   }

   private static double clamp(double v, double lo, double hi) {
      return v < lo?lo:(v > hi?hi:v);
   }

   public static enum Flight implements Displayable {
      ORBIT("Orbit", "Round the target at different heights"),
      SWARM("Swarm", "Each on a wandering path of its own"),
      FIGURE_EIGHT("Figure-eight", "Round the target, rising and dipping twice a lap");

      private final String displayName;
      private final String description;

      private Flight(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }

   public static enum PaperColors implements Displayable {
      PASTEL("Pastel set", "Each crane a different pastel"),
      ONE("One color", "Every crane the Paper color"),
      GRADIENT("Gradient across the flock", "The Paper color, its hue turning crane by crane");

      private final String displayName;
      private final String description;

      private PaperColors(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }
}
