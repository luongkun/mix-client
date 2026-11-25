package mix.client.mixui.integration.haunt.module.targetaura.effects;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.kit.Liquid;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class LiquidChromeEffect extends AuraEffect {
   private static final int MAX_DROPS = 6;
   private static final int INTRO_DROPLETS = 4;
   private static final int OUTRO_DROPLETS = 5;
   private static final int MAX_BALLS = 36;
   private static final double SPAN = 1.72D;
   private static final double HALF = 1.05D;
   private static final double BOTTOM = -0.65D;
   private static final double TOP = 2.25D;
   private static final float RIM = 0.24F;
   private static final double RING_EVERY = 6.0D;
   public final ModeSetting<Material> material = this.materialSetting("liquidchrome", Material.CHROME, "What the drops are made of. Custom takes the Tint, with a finish.");
   public final ModeSetting<Finish> finish;
   public final SliderSetting drops;
   public final SliderSetting merge;
   public final SliderSetting wobble;
   public final BooleanSetting ringMoments;
   public final ModeSetting<LiquidChromeEffect.Quality> quality;
   private final Liquid.Field liquid = new Liquid.Field(36);
   private double ringY;
   private double ringRadius;
   private double ringRho;
   private double ringWeight;
   private double ripple;
   private double rippleCos;
   private double rippleSin;
   private double reachRatio;
   private final double[] orbitAt = new double[3];
   private final double[] orbitDir = new double[3];

   public LiquidChromeEffect(TargetAuraModule module) {
      super(module, EffectType.LIQUID_CHROME, "liquidchrome", new ColorSetting("liquidchrome.color", "Tint", "The metal\'s color with the Custom material.", ColorSetting.Mode.STATIC, -2300950, -6310712, 1.0D), true);
      this.finish = this.finishSetting("liquidchrome", Finish.METALLIC, this.material);
      this.color.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return LiquidChromeEffect.this.material.get() == Material.CUSTOM;
         }
      });
      this.drops = this.look((SliderSetting)this.setting(new SliderSetting("liquidchrome.drops", "Drops", "How many drops float round the target.", 2.0D, 6.0D, 4.0D, 1.0D)));
      this.merge = this.look((SliderSetting)this.setting((new SliderSetting("liquidchrome.merge", "Merge Strength", "How far apart two drops start to flow into one.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%")));
      this.wobble = this.look((SliderSetting)this.setting((new SliderSetting("liquidchrome.wobble", "Wobble", "How much the drops\' surfaces wobble.", 0.0D, 100.0D, 40.0D, 5.0D)).unit("%")));
      this.ringMoments = (BooleanSetting)this.setting(new BooleanSetting("liquidchrome.rings", "Ring Moments", "Every few seconds the drops gather into a rippling chrome ring round the waist, then break apart.", true));
      this.quality = (ModeSetting)this.setting(new ModeSetting("liquidchrome.quality", "Quality", "How fine the drops\' surface is: Low, Medium or High (a little more work each frame).", LiquidChromeEffect.Quality.class, LiquidChromeEffect.Quality.MEDIUM));
   }

   public void render(EffectContext c, FxPass pass) {
      boolean glass = ((Material)this.material.get()).translucent((Finish)this.finish.get());
      if(pass == FxPass.SOLID && !glass || pass == FxPass.TRANSLUCENT && glass) {
         float alpha = this.intensity(c);
         if(alpha <= 0.003F) {
            return;
         }

         this.balls(c);
         if(this.liquid.balls() == 0 && this.ringWeight <= 0.0D) {
            return;
         }

         Material m = (Material)this.material.get();
         MeshBatch.begin(m, (Finish)this.finish.get(), c.light, 0.24F, -1, true, !glass);
         double s = Math.max(0.75D, (double)this.size(c));
         double h = 1.72D * s / (double)((LiquidChromeEffect.Quality)this.quality.get()).cells;
         int half = (int)Math.ceil(1.05D * s / h);
         int below = (int)Math.ceil(0.65D / h);
         int above = (int)Math.ceil(2.25D * s / h);
         this.liquid.bounds(c.x - (double)half * h, c.y - (double)below * h, c.z - (double)half * h, c.x + (double)half * h, c.y + (double)above * h, c.z + (double)half * h);
         Camera cam = c.cam != null?c.cam:Vfx.camera();
         this.liquid.mesh(h, c.x, c.y, c.z, cam.distanceSq(c.x, c.y + 1.0D, c.z), alpha, m == Material.CUSTOM?this.color(c, 0.5F):-1, c.keep);
         MeshBatch.end();
      }

   }

   private void balls(EffectContext c) {
      double s = (double)this.size(c);
      double t = c.time;
      double spd = (double)this.speed();
      int n = this.drops.getInt();
      this.reachRatio = 1.5D + 0.9D * (double)this.merge.getFloat() / 100.0D;
      this.liquid.begin(this.reachRatio);
      double cy = c.y + 1.0D;
      double orbit = 0.66D * (0.7D + 0.3D * s);
      double wob = (double)this.wobble.getFloat() / 100.0D;
      float out = c.outro;
      double phase = t % 6.0D;
      double gather = 0.0D;
      double ringIn = 0.0D;
      if(this.ringMoments.get() && t > 3.0D) {
         gather = (double)(Easing.smoothstep(4.0F, 4.6F, (float)phase) * (1.0F - Easing.smoothstep(5.3F, 6.0F, (float)phase)));
         ringIn = (double)(Easing.smoothstep(4.35F, 4.7F, (float)phase) * (1.0F - Easing.smoothstep(5.15F, 5.6F, (float)phase)));
      }

      this.ringY = c.y + 0.95D;
      this.ringRadius = 0.58D * (0.8D + 0.2D * s);
      this.ringRho = 0.085D * s;
      this.ringWeight = ringIn * (1.0D - (double)Easing.smoothstep(0.0F, 0.35F, out));
      double rippleAt = t * 6.0D * spd;
      this.rippleCos = Math.cos(rippleAt);
      this.rippleSin = Math.sin(rippleAt);
      this.ripple = 0.03D * s * (0.5D + wob);
      float grown = 0.3F + 0.7F * Easing.smoothstep(0.1F, 0.4F, (float)t);
      float rush = Easing.inQuad(Easing.clamp01(t / 0.4D));
      float splash = Easing.smoothstep(0.0F, 0.35F, out);
      if(this.ringWeight > 0.0D) {
         this.liquid.ring(c.x, this.ringY, c.z, this.ringRadius, this.ringRho, this.ringWeight, this.ripple, this.rippleCos, this.rippleSin);
      }

      for(int i = 0; i < n; ++i) {
         this.orbit(i, n, t, orbit, spd);
         double x = c.x + this.orbitAt[0];
         double y = cy + this.orbitAt[1];
         double z = c.z + this.orbitAt[2];
         if(gather > 0.0D) {
            double ra = 6.283185307179586D * (double)i / (double)n + t * 0.8D * spd;
            x += (c.x + Math.cos(ra) * this.ringRadius - x) * gather;
            y += (this.ringY - y) * gather;
            z += (c.z + Math.sin(ra) * this.ringRadius - z) * gather;
         }

         double r = 0.15D * s * (0.92D + 0.16D * (double)hash(i, 7)) * (double)grown * (1.0D - 0.45D * ringIn) * (1.0D - (double)splash);
         double squash = 1.0D - wob * (0.14D + 0.1D * Math.sin(t * 5.3D * spd + (double)i * 1.7D));
         if(r > 0.001D) {
            this.add(x, y, z, r, 1.0D, this.orbitDir[0], this.orbitDir[1], this.orbitDir[2], squash);
         }

         if(t < 0.45D) {
            for(int q = 0; q < 4; ++q) {
               double a = 6.283185307179586D * (double)hash(i * 7 + q, 11);
               double e = ((double)hash(i * 7 + q, 13) - 0.5D) * 1.6D;
               double far = 0.9D * s * (1.0D - (double)rush);
               this.add(x + Math.cos(a) * Math.cos(e) * far, y + Math.sin(e) * far, z + Math.sin(a) * Math.cos(e) * far, 0.07D * s, 1.0D, 0.0D, 1.0D, 0.0D, 1.0D);
            }
         }

         if(out > 0.0F) {
            for(int q = 0; q < 5; ++q) {
               double a = 6.283185307179586D * ((double)q + (double)hash(i * 5 + q, 17) * 0.6D) / 5.0D;
               double up = 0.25D + 0.5D * (double)hash(i * 5 + q, 19);
               double fly = 0.35D * s * (double)out;
               double dr = 0.08D * s * (0.8D + 0.4D * (double)hash(i * 5 + q, 23)) * (1.0D - (double)Easing.smoothstep(0.45F, 1.0F, out)) * (double)splash;
               if(dr > 0.001D) {
                  this.add(x + Math.cos(a) * fly, y + up * fly - 1.1D * (double)out * (double)out, z + Math.sin(a) * fly, dr, 1.0D, 0.0D, 1.0D, 0.0D, 1.0D);
               }
            }
         }
      }

   }

   private void orbit(int i, int n, double t, double rr, double spd) {
      int pairs = (n + 1) / 2;
      int p = i >> 1;
      int m = i & 1;
      double tilt = Math.toRadians((double)(28.0F + 18.0F * hash(p, 3)));
      double dirA = 6.283185307179586D * (double)p / (double)pairs + 0.4D;
      double ux = Math.cos(dirA);
      double uz = Math.sin(dirA);
      double vx = -uz * Math.cos(tilt);
      double vy = Math.sin(tilt);
      double vz = ux * Math.cos(tilt);
      double w = (1.0D + 0.15D * (double)p) * spd * (m == 0?1.0D:-0.92D);
      double th = w * t + (double)m * 3.141592653589793D + (double)p * 1.1D;
      double ct = Math.cos(th);
      double st = Math.sin(th);
      double r = rr * (m == 0?1.035D:0.965D);
      this.orbitAt[0] = (ux * ct + vx * st) * r;
      this.orbitAt[1] = vy * st * r + 0.05D * Math.sin(t * 1.3D * spd + (double)i);
      this.orbitAt[2] = (uz * ct + vz * st) * r;
      this.orbitDir[0] = -ux * st + vx * ct;
      this.orbitDir[1] = vy * ct;
      this.orbitDir[2] = -uz * st + vz * ct;
   }

   public double meetForTest(int n, double apart) {
      double rr = 0.66D;
      double near = 0.15D * apart;
      double[] p = new double[3 * n];
      double before = Double.MAX_VALUE;

      for(double t = 1.5D; t < 15.0D; t += 0.01D) {
         for(int i = 0; i < n; ++i) {
            this.orbit(i, n, t, rr, 1.0D);
            p[3 * i] = this.orbitAt[0];
            p[3 * i + 1] = this.orbitAt[1];
            p[3 * i + 2] = this.orbitAt[2];
         }

         double d = Double.MAX_VALUE;

         for(int i = 0; i < n; ++i) {
            for(int j = i + 1; j < n; ++j) {
               double dx = p[3 * i] - p[3 * j];
               double dy = p[3 * i + 1] - p[3 * j + 1];
               double dz = p[3 * i + 2] - p[3 * j + 2];
               d = Math.min(d, Math.sqrt(dx * dx + dy * dy + dz * dz));
            }
         }

         if(d <= near && before > near && t > 1.5D) {
            return t;
         }

         before = d;
      }

      return -1.0D;
   }

   private void add(double x, double y, double z, double r, double weight, double sx, double sy, double sz, double squash) {
      this.liquid.ball(x, y, z, r, weight, sx, sy, sz, squash);
   }

   public String stateForTest() {
      String how = this.liquid.lastMode == 0?"meshed":(this.liquid.lastMode == 1?"its last mesh":(this.liquid.lastMode == 2?"spheres":"nothing"));
      return String.format(Locale.ROOT, "%s; %d balls, ring %.2f, grid %d points, drawn: %s (%d vertices, %d triangles)", new Object[]{Liquid.stateForTest(), Integer.valueOf(this.liquid.balls()), Double.valueOf(this.ringWeight), Integer.valueOf(this.liquid.lastPoints), how, Integer.valueOf(this.liquid.lastVertices), Integer.valueOf(this.liquid.lastTriangles)});
   }

   public static enum Quality implements Displayable {
      LOW("Low", "16 cells across", 16),
      MEDIUM("Medium", "24 cells across", 24),
      HIGH("High", "32 cells across", 32);

      private final String displayName;
      private final String description;
      final int cells;

      private Quality(String displayName, String description, int cells) {
         this.displayName = displayName;
         this.description = description;
         this.cells = cells;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }
}
