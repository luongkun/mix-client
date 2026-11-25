package mix.client.mixui.integration.haunt.module.landing.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Clockwork;
import mix.client.mixui.integration.haunt.fx.look.KeepVisible;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class ClockfaceStyle extends LandingStyleBase {
   private static final float SLAM = 0.12F;
   private static final float SWEPT = 0.72F;
   private static final float FOLDED = 1.12F;
   private static final float END = 1.42F;
   private static final float JUMP_SLAM = 0.08F;
   private static final float JUMP_TICK = 0.22F;
   private static final float JUMP_FOLDED = 0.5F;
   private static final int MAX_GEARS = 8;
   private static final double RADIUS = 0.95D;
   private static final double MODULE = 0.034D;
   private static final int[] TEETH = new int[]{12, 8, 14, 9, 16, 10, 12, 8};
   private static final float RIM = 0.16F;
   private static final int ENGRAVING = -15068148;
   private static final float DUST_WIND = 0.3F;
   public final ModeSetting<Material> material;
   public final ModeSetting<Finish> finish;
   public final ColorSetting tint;
   public final SliderSetting gears;
   public final ModeSetting<ClockfaceStyle.Numerals> numerals;
   public final BooleanSetting handSweep;
   public final SliderSetting size;
   public final ModeSetting<ClockfaceStyle.Motion> motion;
   private static final double[] E = new double[6];

   public ClockfaceStyle(LandingImpactModule module) {
      super(module, LandingStyle.CLOCKFACE, 1.42F, 1.8F, new ColorSetting("clockface.color", "Glow", "The numerals\' light and the slam\'s flash.", ColorSetting.Mode.STATIC, -6237, -15782, 1.0D));
      this.signature(this.color, -6237);
      this.material = this.materialSetting("clockface", Material.BRASS, "What the dial and the gears are made of. Custom takes the Tint, with a finish.");
      this.finish = this.finishSetting("clockface", Finish.METALLIC, this.material);
      this.tint = (ColorSetting)this.setting((new ColorSetting("clockface.tint", "Tint", "The metal\'s color with the Custom material.", ColorSetting.Mode.STATIC, -3562674, -1521798, 1.0D)).secondary());
      this.tint.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return ClockfaceStyle.this.material.get() == Material.CUSTOM;
         }
      });
      this.signature(this.tint, -3562674);
      this.gears = this.look((SliderSetting)this.setting(new SliderSetting("clockface.gears", "Gears", "How many gears turn round the rim.", 3.0D, 8.0D, 5.0D, 1.0D)));
      this.numerals = (ModeSetting)this.setting((new ModeSetting("clockface.numerals", "Numerals", "What is engraved round the dial.", ClockfaceStyle.Numerals.class, ClockfaceStyle.Numerals.ROMAN)).style(ModeSetting.Style.CHIPS));
      this.handSweep = (BooleanSetting)this.setting(new BooleanSetting("clockface.handSweep", "Hand Sweep", "A hand sweeps once round the dial.", true));
      this.size = this.look((SliderSetting)this.setting((new SliderSetting("clockface.size", "Size", "How big the dial is.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%")));
      this.motion = (ModeSetting)this.setting((new ModeSetting("clockface.motion", "Motion", "Ratchet ticks, or gliding round.", ClockfaceStyle.Motion.class, ClockfaceStyle.Motion.CLOCKWORK)).style(ModeSetting.Style.CHIPS));
   }

   public int passes(LandingFx fx) {
      int solid = ((Material)this.material.get()).translucent((Finish)this.finish.get())?FxPass.TRANSLUCENT.bit():FxPass.SOLID.bit();
      return solid | FxPass.GLOW.bit();
   }

   public void start(LandingFx fx) {
      ClockfaceStyle.State s = new ClockfaceStyle.State();
      s.jump = fx.land.jump;
      s.radius = 0.95D * (double)this.size.getFloat() / 100.0D * (double)fx.size * (s.jump?0.45D:1.0D);
      s.module = s.radius * 0.034D;
      s.y = fx.y() + 0.02D;
      s.turn = (double)fx.rand(1) * 6.283185307179586D;
      int n = s.jump?0:this.gears.getInt() + Math.round(Math.max(0.0F, fx.land.fallScale - 1.0F) * 3.0F);
      s.gears = Math.max(0, Math.min(8, n));
      s.sweep = 0.6F / Math.max(1.0F, 0.85F + 0.5F * (fx.land.fallScale - 0.85F));
      double angle = s.turn;
      double prevR = 0.0D;
      double prevX = 0.0D;
      double prevZ = 0.0D;

      for(int k = 0; k < s.gears; ++k) {
         s.teeth[k] = TEETH[k % TEETH.length];
         s.tip[k] = Clockwork.tipRadius(s.module, s.teeth[k]);
         double r = s.radius + s.tip[k] * 0.55D;
         if(k > 0) {
            double d = Clockwork.meshDistance(s.module, s.teeth[k - 1], s.teeth[k]);
            double c = (prevR * prevR + r * r - d * d) / (2.0D * prevR * r);
            angle += Math.acos(Math.max(-1.0D, Math.min(1.0D, c)));
         }

         s.gx[k] = Math.cos(angle) * r;
         s.gz[k] = Math.sin(angle) * r;
         if(k > 0) {
            s.towards[k] = Clockwork.angleOf(0.0D, 1.0D, 0.0D, prevX - s.gx[k], 0.0D, prevZ - s.gz[k]);
         }

         prevR = r;
         prevX = s.gx[k];
         prevZ = s.gz[k];
      }

      fx.data = s;
   }

   private static float slam(LandingFx fx, ClockfaceStyle.State s) {
      return Easing.clamp01(fx.styleTime() / (s.jump?0.08F:0.12F));
   }

   private double hand(LandingFx fx, ClockfaceStyle.State s) {
      float t = fx.styleTime();
      boolean smooth = this.motion.get() == ClockfaceStyle.Motion.SMOOTH;
      if(s.jump) {
         double k = (double)Easing.clamp01((t - 0.08F) / 0.14F);
         return Clockwork.ratchet(k, 1.0D, 0.5235987755982988D, smooth);
      } else {
         double k = (double)Easing.clamp01((t - 0.12F) / s.sweep);
         return Math.min(6.283185307179586D, Clockwork.ratchet(k * 12.0D, 1.0D, 0.5235987755982988D, smooth));
      }
   }

   private double driver(LandingFx fx, ClockfaceStyle.State s) {
      float t = Math.max(0.0F, fx.styleTime() - 0.06F);
      return this.motion.get() == ClockfaceStyle.Motion.SMOOTH?5.0D * (double)t - 1.2D * (double)t * (double)t:Clockwork.ratchet((double)t, 0.1D, 6.283185307179586D / (double)s.teeth[0] * 1.5D, false);
   }

   private static float folded(LandingFx fx, ClockfaceStyle.State s, int k) {
      float from = (s.jump?0.22F:0.72F) + (float)k * 0.1F;
      float len = s.jump?0.15F:0.22F;
      return Easing.inCubic(Easing.clamp01((fx.styleTime() - from) / len));
   }

   private static float rolled(LandingFx fx) {
      return Easing.clamp01((fx.styleTime() - 1.12F) / 0.29999995F);
   }

   public void renderPass(LandingFx fx, FxFrame f, FxPass pass) {
      ClockfaceStyle.State s = (ClockfaceStyle.State)fx.data;
      if(s != null) {
         Camera cam = Vfx.camera();
         double cx = f.rx(fx.x());
         double cy = f.ry(s.y);
         double cz = f.rz(fx.z());
         float alpha = fx.alpha() * NearCamera.fade(cam, cx, cy + 0.3D, cz);
         if(pass == FxPass.GLOW) {
            this.glow(fx, f, s, cx, cy, cz, alpha);
         } else {
            boolean custom = this.material.get() == Material.CUSTOM;
            int col = custom?this.tint.getColor(0.5F):-1;
            int metal = ColorUtil.withAlpha(col, alpha * (custom?(float)(col >>> 24) / 255.0F:1.0F));
            float sl = slam(fx, s);
            double grow = 1.0D + 0.3D * (1.0D - (double)Easing.outCubic(sl));
            double drop = (1.0D - (double)Easing.outCubic(sl)) * 0.5D;
            double r = s.radius * grow;
            MeshBatch.begin((Material)this.material.get(), (Finish)this.finish.get(), fx.light, 0.16F, -1, true, pass == FxPass.SOLID);
            this.dial(fx, s, cx, cy + drop, cz, r, metal);
            this.gearsMesh(fx, f, s, cx, cy + drop, cz, metal, grow);
            MeshBatch.end();
            this.engraving(fx, s, cx, cy + drop, cz, r, alpha);
            this.dust(fx, s);
         }
      }

   }

   private void dial(LandingFx fx, ClockfaceStyle.State s, double cx, double cy, double cz, double r, int metal) {
      Clockwork.across(0.0D, 1.0D, 0.0D, E);
      double ex = E[0];
      double ey = E[1];
      double ez = E[2];
      double fx2 = E[3];
      double fy2 = E[4];
      double fz2 = E[5];
      double t = r * 0.07D;
      float f0 = folded(fx, s, 0);
      float f1 = folded(fx, s, 1);
      float f2 = folded(fx, s, 2);
      if(f0 < 1.0F) {
         ringPart(cx, cy + t * 0.5D, cz, r * 0.86D, r, t, f0, s.turn, metal, 0.1F, ex, ey, ez, fx2, fy2, fz2);
      }

      if(f1 < 1.0F) {
         ringPart(cx, cy + t * 0.2D, cz, r * 0.05D, r * 0.88D, t * 0.4D, f1, s.turn + 2.1D, metal, 0.05F, ex, ey, ez, fx2, fy2, fz2);
      }

      if(f2 < 1.0F) {
         ringPart(cx, cy + t * 0.55D, cz, r * 0.38D, r * 0.46D, t * 0.6D, f2, s.turn + 4.2D, metal, 0.12F, ex, ey, ez, fx2, fy2, fz2);
      }

      if(this.handSweep.get() && f1 < 0.5F) {
         double a = s.turn + this.hand(fx, s);
         double hx = Math.cos(a);
         double hz = Math.sin(a);
         double sink = (double)f1 * t * 4.0D;
         Clockwork.hand(cx, cy + t * 0.6D - sink, cz, 0.0D, 1.0D, 0.0D, hx, 0.0D, hz, r * 0.72D, r * 0.12D, r * 0.03D, metal, 0.3F);
      }

   }

   private static void ringPart(double cx, double cy, double cz, double inner, double outer, double t, float fold, double axis, int col, float glint, double ex, double ey, double ez, double fx, double fy, double fz) {
      if(fold <= 0.0F) {
         Clockwork.ring(cx, cy, cz, 0.0D, 1.0D, 0.0D, ex, ey, ez, fx, fy, fz, inner, outer, t, 48, col, glint);
      } else {
         double tip = (double)fold * 0.5D;
         double kx = Math.cos(axis);
         double kz = Math.sin(axis);
         double c = Math.cos(tip);
         double sn = Math.sin(tip);
         double ax = -kz * sn;
         double az = kx * sn;
         Clockwork.across(ax, c, az, E);
         double sink = (double)fold * (outer * 0.6D + t);
         Clockwork.ring(cx, cy - sink, cz, ax, c, az, E[0], E[1], E[2], E[3], E[4], E[5], inner, outer, t, 48, col, glint);
      }

   }

   private void gearsMesh(LandingFx fx, FxFrame f, ClockfaceStyle.State s, double cx, double cy, double cz, int metal, double grow) {
      if(s.gears != 0) {
         float roll = rolled(fx);
         int col = ColorUtil.mulAlpha(metal, 1.0F - Easing.smoothstep(0.35F, 1.0F, roll));
         if(col >>> 24 != 0) {
            double turn;
            double prev = turn = this.driver(fx, s);

            for(int k = 0; k < s.gears; ++k) {
               double gt;
               prev = gt = k == 0?turn:Clockwork.meshed(prev, s.teeth[k - 1], s.teeth[k], s.towards[k]);
               double out = (double)Easing.inCubic(roll) * 1.2D * s.radius;
               double ox = s.gx[k];
               double oz = s.gz[k];
               double ol = Math.sqrt(ox * ox + oz * oz) + 1.0E-9D;
               double x = cx + ox * grow + ox / ol * out;
               double z = cz + oz * grow + oz / ol * out;
               double thick = s.tip[k] * 0.22D;
               Clockwork.gear(x, cy + thick * 0.5D + 0.004D, z, 0.0D, 1.0D, 0.0D, s.tip[k] * grow, thick, gt - out / s.tip[k], s.teeth[k], s.teeth[k] >= 12?4:0, col, 0.2F);
            }
         }
      }

   }

   private void engraving(LandingFx fx, ClockfaceStyle.State s, double cx, double cy, double cz, double r, float alpha) {
      float f1 = folded(fx, s, 1);
      if(f1 <= 0.02F && this.numerals.get() != ClockfaceStyle.Numerals.NONE) {
         float a = 0.8F * alpha * Easing.clamp01(slam(fx, s) * 2.0F);
         if(a > 0.003F) {
            int dark = ColorUtil.withAlpha(-15068148, a);
            double lift = r * 0.07D * 0.4D + 0.003D;
            RenderState.decal(true);
            RenderState.translucent(false);

            try {
               Clockwork.ticks(cx, cy, cz, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, lift, 0.0D, r * 0.72D, r * 0.18D, s.turn, dark, (KeepVisible)null);
               if(this.numerals.get() == ClockfaceStyle.Numerals.ROMAN) {
                  Clockwork.glyphs(cx, cy, cz, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, lift, 0.0D, r * 0.56D, r * 0.2D, s.turn, 0.5235987755982988D, 1, true, dark, (KeepVisible)null);
               }
            } finally {
               RenderState.decal(false);
               RenderState.translucent(true);
            }
         }
      }

   }

   private void dust(LandingFx fx, ClockfaceStyle.State s) {
      if(!s.dusted && !fx.lod && slam(fx, s) >= 1.0F) {
         s.dusted = true;
         int n = fx.count(s.jump?5.0F:12.0F);

         for(int i = 0; i < n; ++i) {
            double ang = 6.283185307179586D * (double)i / (double)n + (double)fx.rand(70 + i) * 0.5D;
            double r = s.radius * 1.02D;
            float v = 0.6F + fx.rand(90 + i) * 0.6F;
            fx.particles.spawn(8, fx.x() + Math.cos(ang) * r, fx.y() + 0.05D, fx.z() + Math.sin(ang) * r, (float)Math.cos(ang) * v, 0.2F + fx.rand(110 + i) * 0.25F, (float)Math.sin(ang) * v, fx.seconds(0.7F), 0.28F * fx.size).end(0.6F).drag(2.4F).color(ColorUtil.lerp(fx.land.blockColor, -4675448, 0.5F)).alpha(0.5F * fx.alpha()).wind(0.3F);
         }
      }

   }

   private void glow(LandingFx fx, FxFrame f, ClockfaceStyle.State s, double cx, double cy, double cz, float alpha) {
      int warm = this.color.getColor(0.5F);
      float t = fx.styleTime();
      float flash = (float)Math.exp((double)(-Math.abs(t - (s.jump?0.08F:0.12F))) * 16.0D) * Easing.clamp01(slam(fx, s) * 3.0F);
      if(flash > 0.01F) {
         Vfx.sprite(Textures.GLOW, cx, cy + 0.1D, cz, s.radius * 3.0D, 0.0F, alpha(warm, 0.35F * flash * alpha));
      }

      if(folded(fx, s, 1) <= 0.02F) {
         double r = s.radius;
         double a = s.turn + this.hand(fx, s);
         if(this.numerals.get() != ClockfaceStyle.Numerals.NONE) {
            float done = (float)(this.hand(fx, s) / 6.283185307179586D);

            for(int h = 0; h < 12; ++h) {
               double ha = s.turn + 6.283185307179586D * (double)h / 12.0D;
               float passed = done * 12.0F - (float)h;
               float k = passed < 0.0F?0.0F:(float)Math.exp((double)(-passed) * 0.5D);
               if(k > 0.02F) {
                  double x = cx + Math.cos(ha) * r * 0.56D;
                  double z = cz + Math.sin(ha) * r * 0.56D;
                  Vfx.sprite(Textures.GLOW, x, cy + 0.03D, z, r * 0.3D, 0.0F, alpha(warm, 0.45F * k * alpha));
               }
            }
         }

         if(this.handSweep.get()) {
            double x = cx + Math.cos(a) * r * 0.7D;
            double z = cz + Math.sin(a) * r * 0.7D;
            Vfx.sprite(Textures.GLOW, x, cy + 0.05D, z, r * 0.22D, 0.0F, alpha(warm, 0.5F * alpha));
         }
      }

   }

   public String stateForTest(LandingFx fx) {
      ClockfaceStyle.State s = (ClockfaceStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.styleTime();
         String phase = s.jump?(t < 0.08F?"slamming down":(t < 0.22F?"one tick":(t < 0.5F?"folding away":"gone"))):(t < 0.12F?"slamming down":(t < 0.12F + s.sweep?"the hand sweeping":(t < 0.72F?"ticking":(t < 1.12F?"the rings folding away":(t < 1.42F?"the gears rolling away":"gone")))));
         return String.format(Locale.ROOT, "%s; dial %.2f, %d gears, hand at %.0f deg (%s, a turn in %.2f s), %s, %s; fall %.1f (x%.2f)%s; %s, light %.2f", new Object[]{phase, Double.valueOf(s.radius * 2.0D), Integer.valueOf(s.gears), Double.valueOf(Math.toDegrees(this.hand(fx, s))), ((ClockfaceStyle.Motion)this.motion.get()).displayName(), Float.valueOf(s.sweep), ((ClockfaceStyle.Numerals)this.numerals.get()).displayName(), ((Material)this.material.get()).displayName(), Float.valueOf(fx.land.fallHeight), Float.valueOf(fx.land.fallScale), s.jump?", a jump":"", fx.lod?"far":"near", Float.valueOf(fx.light)});
      }
   }

   public static enum Motion implements Displayable {
      CLOCKWORK("Clockwork", "In ratchet ticks"),
      SMOOTH("Smooth", "Gliding round");

      private final String displayName;
      private final String description;

      private Motion(String displayName, String description) {
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

   public static enum Numerals implements Displayable {
      ROMAN("Roman", "I to XII round the dial"),
      TICKS("Ticks", "Tick marks only"),
      NONE("None", "A plain dial");

      private final String displayName;
      private final String description;

      private Numerals(String displayName, String description) {
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

   private static final class State {
      boolean jump;
      boolean dusted;
      double radius;
      double module;
      double y;
      double turn;
      int gears;
      final double[] gx;
      final double[] gz;
      final double[] tip;
      final double[] towards;
      final int[] teeth;
      float sweep;

      private State() {
         this.gx = new double[8];
         this.gz = new double[8];
         this.tip = new double[8];
         this.towards = new double[8];
         this.teeth = new int[8];
      }
   }
}
