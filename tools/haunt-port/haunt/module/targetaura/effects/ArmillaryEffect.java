package mix.client.mixui.integration.haunt.module.targetaura.effects;

import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.kit.Clockwork;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
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

public final class ArmillaryEffect extends AuraEffect {
   private static final int SEGMENTS = 48;
   private static final double[] COS = Clockwork.COS;
   private static final double[] SIN = Clockwork.SIN;
   private static final int MAX_RINGS = 5;
   private static final double[] PERIOD = new double[]{1.6D, 1.0D, 1.35D, 0.8D, 1.15D};
   private static final double[] STEP = new double[]{Math.toRadians(9.0D), Math.toRadians(-14.0D), Math.toRadians(11.0D), Math.toRadians(-7.0D), Math.toRadians(16.0D)};
   private static final double[] LEAN = new double[]{0.0D, Math.toRadians(25.0D), Math.toRadians(-38.0D), Math.toRadians(52.0D), Math.toRadians(-20.0D)};
   private static final float RIM = 0.16F;
   public final SliderSetting rings;
   public final ModeSetting<ArmillaryEffect.Position> position;
   public final ModeSetting<ArmillaryEffect.Motion> motion;
   public final ModeSetting<ArmillaryEffect.Glyphs> glyphs;
   public final BooleanSetting star;
   public final BooleanSetting glyphGlow;
   public final ColorSetting starColor;
   public final ColorSetting glyphColor;
   public final ModeSetting<Material> material = this.materialSetting("armillary", Material.BRASS, "What the sphere is made of. Custom takes the Metal Tint, with a finish.");
   public final ModeSetting<Finish> finish;
   private final double[] ux = new double[5];
   private final double[] uy = new double[5];
   private final double[] uz = new double[5];
   private final double[] vx = new double[5];
   private final double[] vy = new double[5];
   private final double[] vz = new double[5];
   private final double[] nx = new double[5];
   private final double[] ny = new double[5];
   private final double[] nz = new double[5];
   private final double[] radius = new double[5];
   private final double[] spin = new double[5];
   private final int[] face = new int[5];
   private double cx;
   private double cy;
   private double cz;
   private double axisTop;
   private double axisBottom;
   private double ringWidth;
   private double ringThick;
   private double unfold;
   private int ringCount;
   private final int[] sectionColors = new int[48];

   public ArmillaryEffect(TargetAuraModule module) {
      super(module, EffectType.ARMILLARY, "armillary", new ColorSetting("armillary.color", "Metal Tint", "The metal\'s color with the Custom material.", ColorSetting.Mode.STATIC, -3562674, -6237, 1.0D), true);
      this.finish = this.finishSetting("armillary", Finish.METALLIC, this.material);
      this.color.visibleWhen(this.customMaterial());
      this.starColor = (ColorSetting)this.setting(new ColorSetting("armillary.star", "Star", "The star glowing at the top of the axis.", ColorSetting.Mode.STATIC, -6237, -1, 1.0D));
      this.glyphGlow = (BooleanSetting)this.setting(new BooleanSetting("armillary.glyphGlow", "Glyph Glow", "The engraved numerals and stars glow softly instead of being cut into the metal.", false));
      this.glyphColor = (ColorSetting)this.setting((new ColorSetting("armillary.glyph", "Glyph Glow Color", "The glow of the engraved glyphs.", ColorSetting.Mode.STATIC, -2858, -1, 1.0D)).secondary().visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return ArmillaryEffect.this.glyphGlow.get();
         }
      }));
      this.signature(this.starColor, -6237);
      this.signature(this.glyphColor, -2858);
      this.rings = this.look((SliderSetting)this.setting(new SliderSetting("armillary.rings", "Rings", "How many rings turn inside each other.", 3.0D, 5.0D, 4.0D, 1.0D)));
      this.position = (ModeSetting)this.setting(new ModeSetting("armillary.position", "Position", "Round the torso, or smaller above the head.", ArmillaryEffect.Position.class, ArmillaryEffect.Position.BODY));
      this.motion = (ModeSetting)this.setting(new ModeSetting("armillary.motion", "Motion", "Clockwork: the rings step forward in small ticks with a tiny overshoot. Smooth: they turn evenly.", ArmillaryEffect.Motion.class, ArmillaryEffect.Motion.CLOCKWORK));
      this.glyphs = (ModeSetting)this.setting((new ModeSetting("armillary.glyphs", "Glyphs", "What is engraved on the rings.", ArmillaryEffect.Glyphs.class, ArmillaryEffect.Glyphs.TICKS)).style(ModeSetting.Style.CHIPS));
      this.star = (BooleanSetting)this.setting(new BooleanSetting("armillary.starOn", "Star", "A small star glowing at the top of the axis.", true));
   }

   private BooleanSupplier customMaterial() {
      return new BooleanSupplier() {
         public boolean getAsBoolean() {
            return ArmillaryEffect.this.material.get() == Material.CUSTOM;
         }
      };
   }

   public void render(EffectContext c, FxPass pass) {
      if(pass == FxPass.SOLID || pass == FxPass.TRANSLUCENT && ((Material)this.material.get()).translucent((Finish)this.finish.get())) {
         if(pass == FxPass.SOLID != ((Material)this.material.get()).translucent((Finish)this.finish.get())) {
            this.layout(c);
            this.solid(c, pass == FxPass.SOLID);
            this.engraving(c, false);
         }
      } else if(pass == FxPass.GLOW) {
         this.layout(c);
         if(this.glyphGlow.get()) {
            this.engraving(c, true);
         }

         this.starGlow(c);
      }

   }

   private void layout(EffectContext c) {
      double sz = (double)this.size(c);
      boolean head = this.position.get() == ArmillaryEffect.Position.HEAD;
      double big = (head?0.3D:0.64D) * sz;
      this.cx = c.x;
      this.cz = c.z;
      this.cy = head?c.y + (double)c.nameTop + 0.06D + big * 1.12D:c.y + (double)c.height * 0.56D;
      this.ringCount = this.rings.getInt();
      this.ringWidth = (head?0.026D:0.046D) * sz;
      this.ringThick = (head?0.01D:0.016D) * sz;
      float in = Easing.outCubic(Easing.clamp01(c.intro / 0.75F));
      float out = c.outro;
      this.unfold = Math.max(0.0D, Math.min(1.0D, (double)in * (1.0D - (double)Easing.inOutCubic(Easing.clamp01(out / 0.7F)))));
      Camera cam = c.cam != null?c.cam:Vfx.camera();
      double ex = cam.posX - this.cx;
      double ey = cam.posY - this.cy;
      double ez = cam.posZ - this.cz;
      double t = c.time * (double)this.speed();
      double fx = 1.0D;
      double fy = 0.0D;
      double fz = 0.0D;
      double gx = 0.0D;
      double gy = 1.0D;
      double gz = 0.0D;
      double hx = 0.0D;
      double hy = 0.0D;
      double hz = 1.0D;

      for(int k = 0; k < this.ringCount; ++k) {
         double angle = this.turn(t, k) + LEAN[k];
         double fold = (double)Easing.smoothstep((float)k * 0.12F, (float)k * 0.12F + 0.72F, (float)this.unfold);
         double a = angle * fold;
         double ca = Math.cos(a);
         double sa = Math.sin(a);
         if(k == 0) {
            fx = ca;
            fy = 0.0D;
            fz = sa;
            gx = 0.0D;
            gy = 1.0D;
            gz = 0.0D;
            hx = -sa;
            hy = 0.0D;
            hz = ca;
         } else {
            double ngx = gx * ca + hx * sa;
            double ngy = gy * ca + hy * sa;
            double ngz = gz * ca + hz * sa;
            double nhx = hx * ca - gx * sa;
            double nhy = hy * ca - gy * sa;
            double nhz = hz * ca - gz * sa;
            double ofx = fx;
            double ofy = fy;
            double ofz = fz;
            fx = ngx;
            fy = ngy;
            fz = ngz;
            gx = ofx;
            gy = ofy;
            gz = ofz;
            hx = nhx;
            hy = nhy;
            hz = nhz;
         }

         this.ux[k] = fx;
         this.uy[k] = fy;
         this.uz[k] = fz;
         this.vx[k] = gx;
         this.vy[k] = gy;
         this.vz[k] = gz;
         this.nx[k] = hx;
         this.ny[k] = hy;
         this.nz[k] = hz;
         this.face[k] = ex * hx + ey * hy + ez * hz >= 0.0D?1:-1;
         this.radius[k] = big * (1.0D - 0.13D * (double)k) * (0.45D + 0.55D * this.unfold);
         this.spin[k] = this.turn(t * 0.35D, (k + 2) % 5) * 0.5D;
      }

      double r0 = big * (0.45D + 0.55D * this.unfold);
      double top;
      if(head) {
         top = this.cy + r0 * 1.25D;
      } else {
         double room = (double)(c.nameBottom - c.headTop);
         double lift = room > 0.0D?Math.min(0.1D, room * 0.5D):0.1D;
         top = Math.max(c.y + (double)c.headTop + lift, this.cy + r0 * 1.06D);
      }

      double axisFade = out > 0.7F?1.0D - (double)Easing.clamp01((out - 0.7F) / 0.3F):1.0D;
      this.axisBottom = this.cy - r0 * 1.12D * axisFade;
      this.axisTop = this.cy + (top - this.cy) * axisFade;
   }

   public double turnForTest(double t, int k) {
      return Math.toDegrees(this.turn(t, k));
   }

   public double starHeightForTest(EffectContext c) {
      this.layout(c);
      return this.axisTop - c.y;
   }

   public double bottomForTest(EffectContext c) {
      this.layout(c);
      return this.axisBottom - c.y;
   }

   private double turn(double t, int k) {
      return Clockwork.ratchet(t, PERIOD[k], STEP[k], this.motion.get() == ArmillaryEffect.Motion.SMOOTH);
   }

   private void solid(EffectContext c, boolean depthWrite) {
      float alpha = this.intensity(c) * (float)Math.min(1.0D, 0.15D + this.unfold * 1.2D);
      if(alpha > 0.003F) {
         Material m = (Material)this.material.get();
         boolean custom = m == Material.CUSTOM;
         MeshBatch.begin(m, (Finish)this.finish.get(), c.light, 0.16F, -1, true, depthWrite);

         for(int k = 0; k < this.ringCount; ++k) {
            this.ring(c, k, alpha, custom);
         }

         double gr = this.ringWidth * 0.75D;
         double t = c.time * (double)this.speed();

         for(int k = 0; k < this.ringCount; ++k) {
            if(k == 0) {
               double spin0 = this.turn(t, 0) * 3.0D;

               for(int s = -1; s <= 1; s += 2) {
                  this.gear(c, this.cx, this.cy + (double)s * this.radius[0], this.cz, 0.0D, 1.0D, 0.0D, gr, this.ringThick * 1.6D, spin0, alpha, custom);
               }
            } else {
               double ax = this.ux[k];
               double ay = this.uy[k];
               double az = this.uz[k];
               double r = this.radius[k] + (this.radius[k - 1] - this.radius[k]) * 0.5D;
               double spinK = this.turn(t, k) * 2.5D;

               for(int s = -1; s <= 1; s += 2) {
                  this.gear(c, this.cx + ax * r * (double)s, this.cy + ay * r * (double)s, this.cz + az * r * (double)s, ax, ay, az, gr * 0.85D, this.ringThick * 1.4D, spinK * (double)s, alpha, custom);
               }
            }
         }

         this.axis(c, alpha, custom);
         MeshBatch.end();
      }

   }

   private void ring(EffectContext c, int k, float alpha, boolean custom) {
      double r = this.radius[k];
      double ax = this.ux[k];
      double ay = this.uy[k];
      double az = this.uz[k];
      double bx = this.vx[k];
      double by = this.vy[k];
      double bz = this.vz[k];

      for(int i = 0; i < 48; ++i) {
         double rx = ax * COS[i] + bx * SIN[i];
         double ry = ay * COS[i] + by * SIN[i];
         double rz = az * COS[i] + bz * SIN[i];
         double px = this.cx + rx * r;
         double py = this.cy + ry * r;
         double pz = this.cz + rz * r;
         this.sectionColors[i] = this.colorAt(c, custom, (float)i / 48.0F, alpha * c.keep.solid(px, py, pz), 1.0F);
      }

      Clockwork.band(this.cx, this.cy, this.cz, ax, ay, az, bx, by, bz, this.nx[k], this.ny[k], this.nz[k], r, this.ringWidth, this.ringThick, this.sectionColors, this.face[k]);
   }

   private void gear(EffectContext c, double px, double py, double pz, double ax, double ay, double az, double r, double t, double turn, float alpha, boolean custom) {
      int col = this.colorAt(c, custom, 0.5F, alpha * c.keep.solid(px, py, pz), 0.92F);
      Clockwork.kiteGear(px, py, pz, ax, ay, az, r, t, turn, col);
   }

   private void axis(EffectContext c, float alpha, boolean custom) {
      if(this.axisTop - this.axisBottom >= 0.001D) {
         int sides = 8;
         double r = this.ringThick * 0.55D;
         int col = this.colorAt(c, custom, 0.5F, alpha * c.keep.solid(this.cx, (this.axisTop + this.axisBottom) * 0.5D, this.cz), 0.95F);
         int base = MeshBatch.stage(sides * 2);

         for(int i = 0; i < sides; ++i) {
            double dx = COS[i * 6];
            double dz = SIN[i * 6];
            MeshBatch.vertex(base + i, this.cx + dx * r, this.axisBottom, this.cz + dz * r, dx, 0.0D, dz, col, 0.0F);
            MeshBatch.vertex(base + sides + i, this.cx + dx * r, this.axisTop, this.cz + dz * r, dx, 0.0D, dz, col, 0.0F);
         }

         for(int var14 = 0; var14 < sides; ++var14) {
            int j = (var14 + 1) % sides;
            MeshBatch.quad(base + var14, base + j, base + sides + j, base + sides + var14);
         }
      }

   }

   private int colorAt(EffectContext c, boolean custom, float along, float alpha, float shade) {
      int rgb = custom?this.color(c, along):-1;
      if(shade < 1.0F) {
         rgb = ColorUtil.lerp(-16777216, rgb | -16777216, shade);
      }

      return Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, alpha))) << 24 | rgb & 16777215;
   }

   private void engraving(EffectContext c, boolean glow) {
      ArmillaryEffect.Glyphs g = (ArmillaryEffect.Glyphs)this.glyphs.get();
      if(g != ArmillaryEffect.Glyphs.NONE && this.unfold >= 0.05D) {
         float alpha = this.intensity(c) * (float)Math.min(1.0D, this.unfold * 1.3D);
         RenderState.decal(true);
         if(!glow) {
            int dark = ColorUtil.withAlpha(-15068148, 0.78F * alpha);

            for(int k = 0; k < this.ringCount; ++k) {
               this.ticks(c, k, dark);
            }
         }

         if(g != ArmillaryEffect.Glyphs.TICKS) {
            int color = glow?alphaOf(this.glyphColor.getColor(0.5F), alpha * 0.9F):ColorUtil.withAlpha(-15068148, 0.78F * alpha);

            for(int k = 0; k < this.ringCount; ++k) {
               this.glyphs(c, k, g == ArmillaryEffect.Glyphs.ROMAN, color);
            }
         }

         RenderState.decal(false);
      }

   }

   private static int alphaOf(int color, float a) {
      return ColorUtil.withAlpha(color, (float)(color >>> 24) / 255.0F * Math.max(0.0F, Math.min(1.0F, a)));
   }

   private void ticks(EffectContext c, int k, int dark) {
      double ht = (this.ringThick * 0.5D + 8.0E-4D) * (double)this.face[k];
      Clockwork.ticks(this.cx, this.cy, this.cz, this.ux[k], this.uy[k], this.uz[k], this.vx[k], this.vy[k], this.vz[k], this.nx[k] * ht, this.ny[k] * ht, this.nz[k] * ht, this.radius[k], this.ringWidth, this.spin[k], dark, c.keep);
   }

   private void glyphs(EffectContext c, int k, boolean roman, int color) {
      double ht = (this.ringThick * 0.5D + 0.0012D) * (double)this.face[k];
      Clockwork.glyphs(this.cx, this.cy, this.cz, this.ux[k], this.uy[k], this.uz[k], this.vx[k], this.vy[k], this.vz[k], this.nx[k] * ht, this.ny[k] * ht, this.nz[k] * ht, this.radius[k], this.ringWidth, this.spin[k], 0.06544984694978735D, this.face[k], roman, color, c.keep);
   }

   private void starGlow(EffectContext c) {
      if(this.star.get() && this.unfold >= 0.02D) {
         float a = this.intensity(c) * (float)Math.min(1.0D, this.unfold * 1.5D) * (1.0F - Easing.clamp01(c.outro * 1.4F));
         if(a > 0.003F) {
            float s = Math.min(1.2F, this.size(c)) * (this.position.get() == ArmillaryEffect.Position.HEAD?0.75F:1.0F);
            int col = this.starColor.getColor(0.5F);
            float twinkle = 0.85F + 0.15F * (float)Math.sin(c.time * 3.1D);
            double y = this.axisTop + 0.012D * (double)s;
            Vfx.sprite(Textures.GLOW, this.cx, y, this.cz, (double)(0.36F * s), 0.0F, alpha(col, 0.55F * a * twinkle));
            Vfx.sprite(Textures.FLARE, this.cx, y, this.cz, (double)(0.26F * s * twinkle), (float)(c.time * 0.4D), core(col, 0.95F * a));
            Vfx.sprite(Textures.SPARK, this.cx, y, this.cz, (double)(0.08F * s), 0.0F, core(col, a));
         }
      }

   }

   public static enum Glyphs implements Displayable {
      TICKS("Ticks", "Tick marks"),
      ROMAN("Roman numerals", "Tick marks and numerals"),
      STARS("Stars", "Tick marks and small stars"),
      NONE("None", "Plain rings");

      private final String displayName;
      private final String description;

      private Glyphs(String displayName, String description) {
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

   public static enum Motion implements Displayable {
      CLOCKWORK("Clockwork", "Steps forward in small ticks"),
      SMOOTH("Smooth", "Turns evenly");

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

   public static enum Position implements Displayable {
      BODY("Around body", "Round the torso"),
      HEAD("Above head", "Smaller, above the head");

      private final String displayName;
      private final String description;

      private Position(String displayName, String description) {
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
