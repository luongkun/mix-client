package mix.client.mixui.integration.haunt.module.targetaura.effects;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.kit.Ink;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.stroke.Strokes;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class InkEnsoEffect extends AuraEffect {
   private static final int POINTS = 120;
   private static final int NIGHT = -3549953;
   private static final double START = Math.toRadians(238.0D);
   private static final double SEAL_AT = Math.toRadians(-38.0D);
   public final ColorSetting sealColor = (ColorSetting)this.setting((new ColorSetting("inkenso.seal", "Seal", "The seal stamp\'s red.", ColorSetting.Mode.STATIC, -3660241, -2075078, 1.0D)).secondary());
   public final ModeSetting<InkEnsoEffect.Placement> placement;
   public final SliderSetting brushWidth;
   public final SliderSetting paintSpeed;
   public final SliderSetting dryness;
   public final SliderSetting gap;
   public final BooleanSetting seal;
   public final ModeSetting<Ink.SealDesign> sealDesign;
   public final SliderSetting splatter;
   private final Strokes.Style style = new Strokes.Style();
   private double ox;
   private double oy;
   private double oz;
   private double ax;
   private double ay;
   private double az;
   private double bx;
   private double by;
   private double bz;
   private double radius;

   public InkEnsoEffect(TargetAuraModule module) {
      super(module, EffectType.INK_ENSO, "inkenso", new ColorSetting("inkenso.color", "Ink", "The brush\'s ink.", ColorSetting.Mode.STATIC, -15461356, -14015424, 1.0D), true);
      this.signature(this.sealColor, -3660241);
      this.placement = (ModeSetting)this.setting(new ModeSetting("inkenso.placement", "Placement", "Round the body facing you, or flat on the ground.", InkEnsoEffect.Placement.class, InkEnsoEffect.Placement.BODY));
      this.brushWidth = this.look((SliderSetting)this.setting((new SliderSetting("inkenso.width", "Brush Width", "How wide the brush is at its heaviest.", 0.05D, 0.4D, 0.18D, 0.01D)).unit(" blocks")));
      this.paintSpeed = this.look((SliderSetting)this.setting((new SliderSetting("inkenso.paintSpeed", "Paint Speed", "How fast the circle is painted.", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%")));
      this.dryness = (SliderSetting)this.setting((new SliderSetting("inkenso.dryness", "Dryness", "How soon the brush runs dry into bristle streaks.", 0.0D, 100.0D, 50.0D, 5.0D)).unit("%"));
      this.gap = (SliderSetting)this.setting((new SliderSetting("inkenso.gap", "Gap", "Where the circle doesn\'t close.", 0.0D, 60.0D, 25.0D, 1.0D)).unit("°"));
      this.seal = (BooleanSetting)this.setting(new BooleanSetting("inkenso.sealOn", "Seal", "A red seal stamp at the circle\'s lower right.", true));
      this.sealDesign = (ModeSetting)this.setting((new ModeSetting("inkenso.sealDesign", "Seal Design", "The seal\'s design.", Ink.SealDesign.class, Ink.SealDesign.SUN)).style(ModeSetting.Style.CHIPS).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return InkEnsoEffect.this.seal.get();
         }
      }));
      this.splatter = this.look((SliderSetting)this.setting((new SliderSetting("inkenso.splatter", "Splatter", "Ink splatters round the seal and where the brush landed.", 0.0D, 100.0D, 40.0D, 5.0D)).unit("%")));
   }

   public void render(EffectContext c, FxPass pass) {
      if(pass == FxPass.TRANSLUCENT) {
         float alpha = this.intensity(c);
         if(alpha <= 0.003F) {
            return;
         }

         this.plane(c);
         float painted = this.painted(c);
         this.stroke(c, alpha, painted);
         this.stamp(c, alpha, painted);
      } else if(pass == FxPass.GLOW && c.night > 0.01F) {
         this.plane(c);
         this.nightLight(c, this.painted(c));
      }

   }

   private float painted(EffectContext c) {
      float f = Easing.clamp01((float)(c.time / (0.45D / Math.max(0.25D, (double)this.paintSpeed.getFloat() / 100.0D))));
      return f < 0.3F?0.31F * Easing.inQuad(f / 0.3F):0.31F + 0.69F * Easing.outCubic((f - 0.3F) / 0.7F);
   }

   private void plane(EffectContext c) {
      double sz = (double)this.size(c);
      Camera cam = c.cam != null?c.cam:Vfx.camera();
      if(this.placement.get() == InkEnsoEffect.Placement.GROUND) {
         this.ox = c.x;
         this.oy = c.groundY + 0.02D;
         this.oz = c.z;
         double rxv = cam.rightX;
         double rzv = cam.rightZ;
         double rl = Math.sqrt(rxv * rxv + rzv * rzv);
         if(rl < 1.0E-6D) {
            rxv = 1.0D;
            rzv = 0.0D;
            rl = 1.0D;
         }

         this.ax = rxv / rl;
         this.ay = 0.0D;
         this.az = rzv / rl;
         this.bx = -this.az;
         this.by = 0.0D;
         this.bz = this.ax;
         double fx = cam.forwardX;
         double fz = cam.forwardZ;
         if(this.bx * fx + this.bz * fz < 0.0D) {
            this.bx = -this.bx;
            this.bz = -this.bz;
         }

         this.radius = 0.85D * (0.8D + 0.2D * sz);
      } else {
         double fx = cam.forwardX;
         double fz = cam.forwardZ;
         double fl = Math.sqrt(fx * fx + fz * fz);
         double back = fl < 1.0E-6D?0.0D:0.45D / fl;
         this.ox = c.x + fx * back;
         this.oy = c.y + (double)c.height * 0.56D;
         this.oz = c.z + fz * back;
         this.ax = cam.rightX;
         this.ay = cam.rightY;
         this.az = cam.rightZ;
         this.bx = cam.upX;
         this.by = cam.upY;
         this.bz = cam.upZ;
         this.radius = 0.97D * (0.8D + 0.2D * sz);
      }

   }

   private void at(int i, double a, double r) {
      double ca = Math.cos(a);
      double sa = Math.sin(a);
      Strokes.X[i] = this.ox + (this.ax * ca + this.bx * sa) * r;
      Strokes.Y[i] = this.oy + (this.ay * ca + this.by * sa) * r;
      Strokes.Z[i] = this.oz + (this.az * ca + this.bz * sa) * r;
   }

   private void stroke(EffectContext c, float alpha, float painted) {
      double sweep = 6.283185307179586D - Math.toRadians((double)this.gap.getFloat());

      for(int i = 0; i < 120; ++i) {
         double u = (double)i / 119.0D;
         double a = START - sweep * u;
         double r = this.radius * (0.97D + 0.06D * u + 0.018D * Math.sin(a * 3.0D + 0.7D) + 0.01D * Math.sin(a * 7.0D + 1.9D));
         this.at(i, a, r);
      }

      this.style.reset();
      this.style.tool = Strokes.Tool.BRUSH;
      this.style.color = this.color(c, 0.5F);
      this.style.width = (double)(this.brushWidth.getFloat() * this.size(c)) * (1.0D + 0.02D * Math.sin(c.time * 1.3D));
      this.style.alpha = alpha;
      this.style.progress = painted;
      this.style.heavyStart = 1.0F;
      this.style.dryness = this.dryness.getFloat() / 100.0F;
      this.style.thinEnd = 0.45F + 0.3F * this.style.dryness;
      this.style.diffuse = Easing.clamp01(c.outro);
      this.style.light = c.light;
      if(this.placement.get() == InkEnsoEffect.Placement.GROUND) {
         this.style.flat = true;
         this.style.normalX = 0.0D;
         this.style.normalY = 1.0D;
         this.style.normalZ = 0.0D;
      }

      Strokes.draw(120, this.style);
   }

   private void stamp(EffectContext c, float alpha, float painted) {
      double t = c.time;
      double sz = (double)this.size(c);
      double paintTime = 0.45D / Math.max(0.25D, (double)this.paintSpeed.getFloat() / 100.0D);
      float fade = 1.0F - Easing.smoothstep(0.0F, 0.8F, c.outro);
      int ink = ColorUtil.shade(this.color(c, 0.5F), c.light);
      int splats = Math.round(this.splatter.getFloat() / 100.0F * 7.0F);
      if(splats > 0) {
         int tex = Ink.splats();

         for(int k = 0; k < splats; ++k) {
            boolean nearSeal = (k & 1) == 1;
            double when = nearSeal?paintTime:0.02D;
            float show = Easing.smoothstep(0.0F, 0.06F, (float)(t - when));
            if(show > 0.0F) {
               double a = nearSeal?SEAL_AT + ((double)hash(k, 3) - 0.5D) * 0.9D:START + ((double)hash(k, 3) - 0.3D) * 0.45D;
               double r = this.radius * (nearSeal?1.12D + 0.2D * (double)hash(k, 5):0.9D + 0.22D * (double)hash(k, 5));
               double s = sz * (0.035D + 0.06D * (double)hash(k, 7)) * (0.6D + 0.4D * (double)show);
               int col = alpha(ink, alpha * fade * 0.9F * show);
               this.quad(a, r, s, s, (float)((double)hash(k, 9) * 3.141592653589793D * 2.0D), tex, k % 4, 4, col);
            }
         }
      }

      if(this.seal.get()) {
         float stampT = (float)((t - paintTime) / 0.16D);
         if(stampT > 0.0F) {
            float e = Easing.outBack(Easing.clamp01(stampT), 2.0F);
            double sx = 1.0D + 0.25D * (1.0D - (double)e);
            double sy = 1.0D - 0.2D * (1.0D - (double)e);
            double s = (this.placement.get() == InkEnsoEffect.Placement.GROUND?0.3D:0.24D) * sz;
            int col = alpha(ColorUtil.shade(this.sealColor.getColor(0.5F), c.light), alpha * fade * Easing.clamp01(stampT * 3.0F));
            this.quad(SEAL_AT, this.radius + 0.19D * sz, s * sx, s * sy, 0.06F, Ink.seals(), ((Ink.SealDesign)this.sealDesign.get()).ordinal(), 4, col);
         }
      }

   }

   private void quad(double a, double r, double w, double h, float turn, int tex, int cell, int cells, int col) {
      double ca = Math.cos(a);
      double sa = Math.sin(a);
      double px = this.ox + (this.ax * ca + this.bx * sa) * r;
      double py = this.oy + (this.ay * ca + this.by * sa) * r;
      double pz = this.oz + (this.az * ca + this.bz * sa) * r;
      double ct = Math.cos((double)turn);
      double st = Math.sin((double)turn);
      double ux = (this.ax * ct + this.bx * st) * w * 0.5D;
      double uy = (this.ay * ct + this.by * st) * w * 0.5D;
      double uz = (this.az * ct + this.bz * st) * w * 0.5D;
      double vx = (this.bx * ct - this.ax * st) * h * 0.5D;
      double vy = (this.by * ct - this.ay * st) * h * 0.5D;
      double vz = (this.bz * ct - this.az * st) * h * 0.5D;
      float u0 = (float)cell / (float)cells;
      float u1 = (float)(cell + 1) / (float)cells;
      Vfx.quadUV(tex, px - ux - vx, py - uy - vy, pz - uz - vz, (double)u0, 1.0D, col, px + ux - vx, py + uy - vy, pz + uz - vz, (double)u1, 1.0D, col, px + ux + vx, py + uy + vy, pz + uz + vz, (double)u1, 0.0D, col, px - ux + vx, py - uy + vy, pz - uz + vz, (double)u0, 0.0D, col);
   }

   private void nightLight(EffectContext c, float painted) {
      float a = this.intensity(c) * c.night * 0.12F * (1.0F - Easing.clamp01(c.outro));
      if(a > 0.003F) {
         double sweep = (6.283185307179586D - Math.toRadians((double)this.gap.getFloat())) * (double)painted;
         int n = 28;
         float s = (float)((double)(this.brushWidth.getFloat() * this.size(c)) * 3.2D);

         for(int i = 0; i < n; ++i) {
            double ang = START - sweep * (double)i / ((double)n - 1.0D);
            double ca = Math.cos(ang);
            double sa = Math.sin(ang);
            Vfx.sprite(Textures.GLOW, this.ox + (this.ax * ca + this.bx * sa) * this.radius, this.oy + (this.ay * ca + this.by * sa) * this.radius, this.oz + (this.az * ca + this.bz * sa) * this.radius, (double)s, 0.0F, alpha(-3549953, a));
         }
      }

   }

   public String stateForTest(EffectContext c) {
      return String.format(Locale.ROOT, "painted %.2f, outro %.2f, %s", new Object[]{Float.valueOf(this.painted(c)), Float.valueOf(c.outro), ((InkEnsoEffect.Placement)this.placement.get()).displayName()});
   }

   public static enum Placement implements Displayable {
      BODY("Around body", "A circle round the target, facing you"),
      GROUND("On the ground", "A circle flat on the ground round their feet");

      private final String displayName;
      private final String description;

      private Placement(String displayName, String description) {
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
