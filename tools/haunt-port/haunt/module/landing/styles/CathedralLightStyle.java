package mix.client.mixui.integration.haunt.module.landing.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.Terrain;
import mix.client.mixui.integration.haunt.fx.kit.Glass;
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
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class CathedralLightStyle extends LandingStyleBase {
   private static final float LIT = 0.25F;
   private static final float FADE_FROM = 1.0F;
   private static final float END = 1.6F;
   private static final double RADIUS = 1.35D;
   private static final double COLUMN = 3.2D;
   private static final double TURN = 0.3D;
   private static final double INSET = 0.9D;
   private static final int MAX_POINTS = 64;
   private static final int SUB = 3;
   private static final float MOTE_WIND = 0.25F;
   public final ColorSetting lead;
   public final ModeSetting<Glass.GlassColors> glassColors;
   public final ColorSetting glass1;
   public final ColorSetting glass2;
   public final ColorSetting glass3;
   public final ModeSetting<CathedralLightStyle.Pattern> pattern;
   public final SliderSetting size;
   public final BooleanSetting column;
   public final BooleanSetting motes;
   public final SliderSetting brightness;
   private final Glass.Pane pane = new Glass.Pane();
   private final double[] tu = new double[64];
   private final double[] tv = new double[64];
   private final int[] palette = new int[4];
   private final double[] center = new double[2];

   public CathedralLightStyle(LandingImpactModule module) {
      super(module, LandingStyle.CATHEDRAL_LIGHT, 1.6F, 2.3F, new ColorSetting("cathedrallight.color", "Light", "The column of light.", ColorSetting.Mode.STATIC, -3639, -9846, 1.0D));
      this.signature(this.color, -3639);
      this.lead = (ColorSetting)this.setting((new ColorSetting("cathedrallight.lead", "Lead", "The lead lines\' shadows across the light.", ColorSetting.Mode.STATIC, -15066594, -12961212, 1.0D)).secondary());
      this.signature(this.lead, -15066594);
      this.glassColors = (ModeSetting)this.setting((new ModeSetting("cathedrallight.glassColors", "Glass Colors", "Cathedral: ruby, sapphire, emerald and gold. Tinted: the three glass colors.", Glass.GlassColors.class, Glass.GlassColors.CATHEDRAL)).style(ModeSetting.Style.CHIPS));
      BooleanSupplier tinted = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CathedralLightStyle.this.glassColors.get() == Glass.GlassColors.TINTED;
         }
      };
      this.glass1 = (ColorSetting)this.setting((new ColorSetting("cathedrallight.glass1", "Glass 1", "The first glass color (Tinted).", ColorSetting.Mode.STATIC, -13935399, -9794305, 1.0D)).secondary().visibleWhen(tinted));
      this.glass2 = (ColorSetting)this.setting((new ColorSetting("cathedrallight.glass2", "Glass 2", "The second glass color (Tinted).", ColorSetting.Mode.STATIC, -4184513, -38273, 1.0D)).secondary().visibleWhen(tinted));
      this.glass3 = (ColorSetting)this.setting((new ColorSetting("cathedrallight.glass3", "Glass 3", "The third glass color (Tinted).", ColorSetting.Mode.STATIC, -868018, -6496, 1.0D)).secondary().visibleWhen(tinted));
      this.signature(this.glass1, -13935399);
      this.signature(this.glass2, -4184513);
      this.signature(this.glass3, -868018);
      this.pattern = (ModeSetting)this.setting((new ModeSetting("cathedrallight.pattern", "Pattern", "The window the light falls through.", CathedralLightStyle.Pattern.class, CathedralLightStyle.Pattern.ROSE)).style(ModeSetting.Style.CHIPS));
      this.size = this.look((SliderSetting)this.setting((new SliderSetting("cathedrallight.size", "Size", "How big the pool of light is.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%")));
      this.column = (BooleanSetting)this.setting(new BooleanSetting("cathedrallight.column", "Light Column", "A soft column of light rises from the pattern.", true));
      this.motes = (BooleanSetting)this.setting(new BooleanSetting("cathedrallight.motes", "Motes", "Dust motes drift slowly up in the light.", true));
      this.brightness = this.look((SliderSetting)this.setting((new SliderSetting("cathedrallight.brightness", "Brightness", "How bright the light is.", 0.0D, 100.0D, 70.0D, 5.0D)).unit("%")));
   }

   public int passes(LandingFx fx) {
      return FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit();
   }

   public void start(LandingFx fx) {
      CathedralLightStyle.State s = new CathedralLightStyle.State();
      s.jump = fx.land.jump;
      s.radius = 1.35D * (double)this.size.getFloat() / 100.0D * (double)fx.size * (s.jump?0.6D:1.0D);
      s.height = 3.2D * Math.sqrt((double)Math.max(0.6F, fx.land.fallScale)) * (double)this.size.getFloat() / 100.0D;
      s.bright = this.brightness.getFloat() / 100.0F * (0.85F + 0.15F * fx.land.fallScale);
      s.yaw = Math.toRadians((double)fx.land.yaw);
      fx.data = s;
   }

   private static Glass.Cells cells(CathedralLightStyle.Pattern p) {
      switch(p) {
      case MANDALA:
         return Glass.mandala();
      case LANCET:
         return Glass.lancet();
      default:
         return Glass.rose();
      }
   }

   private static float lit(LandingFx fx, Glass.Cells c, int i) {
      double appear = 0.25D * Math.min(1.0D, c.rad[i] / 0.875D);
      return Easing.clamp01((float)(((double)fx.styleTime() - appear) / 0.07D));
   }

   private static float fade(LandingFx fx) {
      return 1.0F - Easing.smoothstep(1.0F, 1.6F, fx.styleTime());
   }

   private Glass.Pane pane(LandingFx fx, FxFrame f, CathedralLightStyle.State s, double lift) {
      double cy = Math.cos(s.yaw);
      double sy = Math.sin(s.yaw);
      this.pane.ox = f.rx(fx.x());
      this.pane.oy = f.ry(fx.y());
      this.pane.oz = f.rz(fx.z());
      this.pane.rx = cy;
      this.pane.ry = 0.0D;
      this.pane.rz = sy;
      this.pane.ux = -sy;
      this.pane.uy = 0.0D;
      this.pane.uz = cy;
      this.pane.nx = 0.0D;
      this.pane.ny = 1.0D;
      this.pane.nz = 0.0D;
      this.pane.scale = s.radius;
      this.pane.offX = 0.0D;
      this.pane.offY = 0.0D;
      this.pane.offZ = 0.0D;
      this.pane.onGround(fx.ground, f.viewX, f.viewY, f.viewZ, lift);
      return this.pane;
   }

   public void renderPass(LandingFx fx, FxFrame f, FxPass pass) {
      CathedralLightStyle.State s = (CathedralLightStyle.State)fx.data;
      if(s != null) {
         float a = fx.alpha() * fade(fx);
         if(a > 0.003F) {
            if(this.glassColors.get() == Glass.GlassColors.TINTED) {
               Glass.tinted(this.glass1.getColor(0.5F), this.glass2.getColor(0.5F), this.glass3.getColor(0.5F), this.palette);
            } else {
               System.arraycopy(Glass.CATHEDRAL_SET, 0, this.palette, 0, 4);
            }

            Glass.Cells c = cells((CathedralLightStyle.Pattern)this.pattern.get());
            double turn = (double)fx.styleTime() * 0.3D;
            if(pass == FxPass.TRANSLUCENT) {
               this.shadows(fx, f, s, c, turn, a);
            } else {
               this.light(fx, f, s, c, turn, a);
               if(this.column.get() && !s.jump) {
                  this.column(fx, f, s, a);
               }

               if(this.motes.get() && !s.jump && !fx.lod) {
                  this.motes(fx, f, s);
               }
            }
         }
      }

   }

   private int cell(Glass.Cells c, int i, double turn, double scale, double[] center) {
      double ct = c.turns[i]?Math.cos(turn):1.0D;
      double st = c.turns[i]?Math.sin(turn):0.0D;
      double cu = c.cu[i] * ct - c.cv[i] * st;
      double cv = c.cu[i] * st + c.cv[i] * ct;
      center[0] = cu;
      center[1] = cv;
      return Glass.shard(c, i, c.turns[i]?turn:0.0D, scale, cu, cv, this.tu, this.tv);
   }

   private boolean grounded(LandingFx fx, CathedralLightStyle.State s, double u, double v) {
      double cy = Math.cos(s.yaw);
      double sy = Math.sin(s.yaw);
      double x = fx.x() + (cy * u - sy * v) * s.radius;
      double z = fx.z() + (sy * u + cy * v) * s.radius;
      Ground.y(fx.ground, x, z);
      return Ground.lastValid();
   }

   private void shadows(LandingFx fx, FxFrame f, CathedralLightStyle.State s, Glass.Cells c, double turn, float a) {
      int shade = ColorUtil.shade(this.lead.getColor(0.5F), Math.max(0.5F, fx.light)) | -16777216;
      float k = a * 0.55F * (0.6F + 0.4F * s.bright);
      Glass.Pane p = this.pane(fx, f, s, 0.008D);
      RenderState.decal(true);
      RenderState.translucent(false);

      try {
         float ring = Easing.clamp01((fx.styleTime() - 0.25F) / 0.1F);
         int sub = fx.ground.isFlat()?1:3;
         if(c.stone) {
            this.ringLead(fx, s, p, 0.0D, 0.655D, alpha(shade, k * 0.7F * ring));
         }

         for(int i = 0; i < c.n; ++i) {
            float l = lit(fx, c, i);
            if(l > 0.01F) {
               int n2 = this.cell(c, i, turn, 1.0D, this.center);
               if(this.grounded(fx, s, this.center[0], this.center[1])) {
                  lead(p, this.tu, this.tv, n2, 0.044800000000000006D, alpha(shade, k * l), sub);
               }
            }
         }

         this.ringLead(fx, s, p, 0.655D, 0.705D, alpha(shade, k * ring));
         this.ringLead(fx, s, p, 0.875D, 1.0D, alpha(shade, k * 0.8F * ring));
      } finally {
         RenderState.decal(false);
         p.onGround((Terrain.HeightField)null, 0.0D, 0.0D, 0.0D, 0.0D);
      }

   }

   private void light(LandingFx fx, FxFrame f, CathedralLightStyle.State s, Glass.Cells c, double turn, float a) {
      float k = a * s.bright * 0.85F;
      if(k > 0.003F) {
         Glass.Pane p = this.pane(fx, f, s, 0.012D);
         int sub = fx.ground.isFlat()?1:3;
         RenderState.decal(true);
         RenderState.additive();

         try {
            for(int i = 0; i < c.n; ++i) {
               float l = lit(fx, c, i);
               if(l > 0.01F) {
                  int n = this.cell(c, i, turn, 0.9D, this.center);
                  if(this.grounded(fx, s, this.center[0], this.center[1])) {
                     int base = ColorUtil.shade(this.palette[c.color[i]], 0.9F + 0.2F * hash(i, 13));
                     float flash = (1.0F - l) * 0.6F + 0.15F * (1.0F - Easing.clamp01((fx.styleTime() - 0.25F * (float)(c.rad[i] / 0.875D)) / 0.2F));
                     int mid = alpha(ColorUtil.lerp(base, -1, 0.3F + flash), k * l);
                     int edge = alpha(base, k * l * 0.75F);
                     fan(p, this.tu, this.tv, n, this.center[0], this.center[1], mid, edge, sub);
                  }
               }
            }
         } finally {
            RenderState.decal(false);
            p.onGround((Terrain.HeightField)null, 0.0D, 0.0D, 0.0D, 0.0D);
         }
      }

   }

   private static void fan(Glass.Pane p, double[] u, double[] v, int n, double cu, double cv, int mid, int edge, int sub) {
      if(sub <= 1) {
         Glass.glass(p, u, v, n, cu, cv, mid, edge);
      } else {
         int tex = Glass.texture();

         for(int k = 0; k < n; ++k) {
            int k2 = (k + 1) % n;
            double au = u[k] - cu;
            double av = v[k] - cv;
            double bu = u[k2] - u[k];
            double bv = v[k2] - v[k];

            for(int i = 0; i < sub; ++i) {
               for(int j = 0; j <= i; ++j) {
                  tri(p, tex, cu, cv, au, av, bu, bv, sub, mid, edge, i, j, i + 1, j, i + 1, j + 1);
                  if(j < i) {
                     tri(p, tex, cu, cv, au, av, bu, bv, sub, mid, edge, i, j, i + 1, j + 1, i, j + 1);
                  }
               }
            }
         }
      }

   }

   private static void tri(Glass.Pane p, int tex, double cu, double cv, double au, double av, double bu, double bv, int sub, int mid, int edge, int i0, int j0, int i1, int j1, int i2, int j2) {
      double u0 = cu + (au * (double)i0 + bu * (double)j0) / (double)sub;
      double v0 = cv + (av * (double)i0 + bv * (double)j0) / (double)sub;
      double u1 = cu + (au * (double)i1 + bu * (double)j1) / (double)sub;
      double v1 = cv + (av * (double)i1 + bv * (double)j1) / (double)sub;
      double u2 = cu + (au * (double)i2 + bu * (double)j2) / (double)sub;
      double v2 = cv + (av * (double)i2 + bv * (double)j2) / (double)sub;
      int c0 = ColorUtil.lerp(mid, edge, (float)i0 / (float)sub);
      int c1 = ColorUtil.lerp(mid, edge, (float)i1 / (float)sub);
      int c2 = ColorUtil.lerp(mid, edge, (float)i2 / (float)sub);
      Vfx.quadUV(tex, p.x(u0, v0), p.y(u0, v0), p.z(u0, v0), u0 * 1.6D, v0 * 1.6D, c0, p.x(u1, v1), p.y(u1, v1), p.z(u1, v1), u1 * 1.6D, v1 * 1.6D, c1, p.x(u2, v2), p.y(u2, v2), p.z(u2, v2), u2 * 1.6D, v2 * 1.6D, c2, p.x(u2, v2), p.y(u2, v2), p.z(u2, v2), u2 * 1.6D, v2 * 1.6D, c2);
   }

   private static void lead(Glass.Pane p, double[] u, double[] v, int n, double width, int col, int sub) {
      if(sub <= 1) {
         Glass.lead(p, u, v, n, width, col);
      } else {
         double hw = width * 0.5D;

         for(int k = 0; k < n; ++k) {
            int k2 = (k + 1) % n;
            double eu = u[k2] - u[k];
            double ev = v[k2] - v[k];
            double el = Math.sqrt(eu * eu + ev * ev);
            if(el >= 1.0E-6D) {
               double var43;
               double pu = -(var43 = ev / el) * hw;
               double var42;
               double pv = (var42 = eu / el) * hw;
               double su = u[k] - var42 * hw;
               double sv = v[k] - var43 * hw;
               double len = el + 2.0D * hw;
               int pieces = Math.max(1, (int)Math.ceil(len * p.scale / 0.12D));

               for(int q = 0; q < pieces; ++q) {
                  double a0 = len * (double)q / (double)pieces;
                  double a1 = len * (double)(q + 1) / (double)pieces;
                  double u0 = su + var42 * a0;
                  double v0 = sv + var43 * a0;
                  double u1 = su + var42 * a1;
                  double v1 = sv + var43 * a1;
                  Vfx.quadUV(Textures.WHITE, p.x(u0 - pu, v0 - pv), p.y(u0 - pu, v0 - pv), p.z(u0 - pu, v0 - pv), 0.5D, 0.5D, col, p.x(u1 - pu, v1 - pv), p.y(u1 - pu, v1 - pv), p.z(u1 - pu, v1 - pv), 0.5D, 0.5D, col, p.x(u1 + pu, v1 + pv), p.y(u1 + pu, v1 + pv), p.z(u1 + pu, v1 + pv), 0.5D, 0.5D, col, p.x(u0 + pu, v0 + pv), p.y(u0 + pu, v0 + pv), p.z(u0 + pu, v0 + pv), 0.5D, 0.5D, col);
               }
            }
         }
      }

   }

   private void ringLead(LandingFx fx, CathedralLightStyle.State s, Glass.Pane p, double r0, double r1, int col) {
      if(col >>> 24 != 0) {
         double rm = (r0 + r1) * 0.5D;

         for(int i = 0; i < 48; ++i) {
            double c0 = Glass.RING_COS[i];
            double c1 = Glass.RING_COS[i + 1];
            double cm = (c0 + c1) * 0.5D;
            double s0 = Glass.RING_SIN[i];
            double s1 = Glass.RING_SIN[i + 1];
            double sm = (s0 + s1) * 0.5D;
            if(this.grounded(fx, s, cm * rm, sm * rm)) {
               Vfx.quadUV(Textures.WHITE, p.x(c0 * r0, s0 * r0), p.y(c0 * r0, s0 * r0), p.z(c0 * r0, s0 * r0), 0.5D, 0.5D, col, p.x(c1 * r0, s1 * r0), p.y(c1 * r0, s1 * r0), p.z(c1 * r0, s1 * r0), 0.5D, 0.5D, col, p.x(c1 * r1, s1 * r1), p.y(c1 * r1, s1 * r1), p.z(c1 * r1, s1 * r1), 0.5D, 0.5D, col, p.x(c0 * r1, s0 * r1), p.y(c0 * r1, s0 * r1), p.z(c0 * r1, s0 * r1), 0.5D, 0.5D, col);
            }
         }
      }

   }

   private void column(LandingFx fx, FxFrame f, CathedralLightStyle.State s, float a) {
      float on = Easing.smoothstep(0.12F, 0.35F, fx.styleTime());
      Camera cam = Vfx.camera();
      double x = f.rx(fx.x());
      double z = f.rz(fx.z());
      double y0 = f.ry(fx.y() + 0.02D);
      double y1 = f.ry(fx.y() + s.height);
      double dx = cam.posX - x;
      double dz = cam.posZ - z;
      float near = Easing.smoothstep(0.7F, 1.8F, (float)Math.sqrt(dx * dx + dz * dz));
      float k = a * on * near * s.bright;
      if(k > 0.003F) {
         int light = this.color.getColor(0.5F) | -16777216;
         RenderState.additive();
         Glass.shaft(cam, x, y0, z, x, y1, z, s.radius * 1.7D, s.radius * 2.1D, alpha(light, 0.3F * k));
         Glass.shaft(cam, x, y0, z, x, y1, z, s.radius * 0.9D, s.radius * 1.1D, alpha(light, 0.42F * k));
      }

   }

   private void motes(LandingFx fx, FxFrame f, CathedralLightStyle.State s) {
      float t = fx.styleTime();
      if(t <= 1.0F && f.dt > 0.0F) {
         s.moteDebt += f.dt * 22.0F * Math.min(2.0F, fx.density) * (float)(s.radius / 1.35D);
         int light = this.color.getColor(0.5F) | -16777216;

         while(s.moteDebt >= 1.0F) {
            --s.moteDebt;
            int i = s.motes++;
            double ang = (double)fx.rand(1000 + i % 400) * 6.283185307179586D;
            double d = Math.sqrt((double)fx.rand(1400 + i % 400)) * s.radius * 0.8D;
            double x = fx.x() + Math.cos(ang) * d;
            double z = fx.z() + Math.sin(ang) * d;
            double y = fx.y() + 0.1D + (double)fx.rand(1800 + i % 400) * s.height * (this.column.get()?0.75D:0.25D);
            int col = i % 3 == 0?light:this.palette[i % 4];
            fx.particles.spawn(3, x, y, z, (fx.rand(2200 + i % 400) - 0.5F) * 0.08F, 0.12F + fx.rand(2600 + i % 400) * 0.18F, (fx.rand(3000 + i % 400) - 0.5F) * 0.08F, fx.seconds(0.9F + fx.rand(3400 + i % 400) * 0.6F), 0.035F + fx.rand(3800 + i % 400) * 0.03F).end(0.01F).drag(0.6F).color(col).alpha(fx.alpha() * s.bright).flags(2).sway(0.06F, fx.rand(4200 + i % 400)).wind(0.25F);
         }
      }

   }

   private static float hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (float)(h >>> 8) / 1.6777216E7F;
   }

   public String stateForTest(LandingFx fx) {
      CathedralLightStyle.State s = (CathedralLightStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         Glass.Cells c = cells((CathedralLightStyle.Pattern)this.pattern.get());
         int lit = 0;

         for(int i = 0; i < c.n; ++i) {
            if(lit(fx, c, i) > 0.5F) {
               ++lit;
            }
         }

         float t = fx.styleTime();
         String phase = t < 0.32F?"lighting up ring by ring":(t < 1.0F?"holding, turning":(t < 1.6F?"fading":"gone"));
         return String.format(Locale.ROOT, "%s; %s, %d of %d cells lit, radius %.2f, column %s (%.2f high), %d motes, brightness %.2f, %s; fall %.1f (x%.2f)%s; %s, light %.2f", new Object[]{phase, ((CathedralLightStyle.Pattern)this.pattern.get()).displayName(), Integer.valueOf(lit), Integer.valueOf(c.n), Double.valueOf(s.radius), this.column.get() && !s.jump?"on":"off", Double.valueOf(s.height), Integer.valueOf(s.motes), Float.valueOf(s.bright), ((Glass.GlassColors)this.glassColors.get()).displayName(), Float.valueOf(fx.land.fallHeight), Float.valueOf(fx.land.fallScale), s.jump?", a jump":"", fx.lod?"far":"near", Float.valueOf(fx.light)});
      }
   }

   public static enum Pattern implements Displayable {
      ROSE("Rose", "A rose window\'s petals and rings"),
      MANDALA("Mandala", "Rings of pointed cells"),
      LANCET("Lancet trio", "Three tall lancets in a circle of stone");

      private final String displayName;
      private final String description;

      private Pattern(String displayName, String description) {
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
      double radius;
      double height;
      double yaw;
      float bright;
      float moteDebt;
      int motes;

      private State() {
      }
   }
}
