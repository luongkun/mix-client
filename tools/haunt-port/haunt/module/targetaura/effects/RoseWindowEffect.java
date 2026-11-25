package mix.client.mixui.integration.haunt.module.targetaura.effects;

import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.kit.Glass;
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

public final class RoseWindowEffect extends AuraEffect {
   private static final int RUBY = 0;
   private static final int SAPPHIRE = 1;
   private static final int EMERALD = 2;
   private static final int GOLD = 3;
   private static final int[] CATHEDRAL = new int[]{-5238742, -15382856, -15496642, -1527238};
   private static final int STONE = -7436419;
   private static final double LEAD = 0.028D;
   private static final double FRAME_IN = 0.875D;
   private static final double BAND_IN = 0.655D;
   private static final double BAND_OUT = 0.705D;
   private static final int RING_SEGS = 48;
   private static final int FRAME_PIECES = 8;
   private static final double[] RING_COS = Glass.RING_COS;
   private static final double[] RING_SIN = Glass.RING_SIN;
   private static final double[] FRAME_R = new double[]{0.875D, 0.905D, 0.97D, 1.0D};
   private static final double[] FRAME_Z = new double[]{0.0D, 0.025D, 0.025D, 0.0D};
   private static final double[] FRAME_LEAN = new double[]{-0.7D, 0.0D, 0.0D, 0.7D};
   private static final int SHAFTS = 6;
   private static final int MOTES = 3;
   private static final double BEHIND = 0.4D;
   public final ColorSetting glass2 = (ColorSetting)this.setting((new ColorSetting("rosewindow.glass2", "Glass 2", "The second glass color (Tinted).", ColorSetting.Mode.STATIC, -4184513, -38273, 1.0D)).secondary());
   public final ColorSetting glass3 = (ColorSetting)this.setting((new ColorSetting("rosewindow.glass3", "Glass 3", "The third glass color (Tinted).", ColorSetting.Mode.STATIC, -868018, -6496, 1.0D)).secondary());
   public final ColorSetting leadColor = (ColorSetting)this.setting(new ColorSetting("rosewindow.lead", "Lead", "The lead between the panes.", ColorSetting.Mode.STATIC, -15066594, -12961212, 1.0D));
   public final ModeSetting<Glass.GlassColors> glassColors;
   public final ModeSetting<RoseWindowEffect.Pattern> pattern;
   public final ModeSetting<RoseWindowEffect.Position> position;
   public final SliderSetting shafts;
   public final BooleanSetting groundLight;
   public final SliderSetting opacity;
   public final BooleanSetting motes;
   private double ox;
   private double oy;
   private double oz;
   private double rx;
   private double rz;
   private double nx;
   private double nz;
   private double radius;
   private double turn;
   private final int[] palette = new int[4];
   private final double[] tu = new double[64];
   private final double[] tv = new double[64];
   private double tcu;
   private double tcv;
   private double dz;
   private double dy;
   private float cellAlpha;
   private final double[] sx = new double[6];
   private final double[] sy = new double[6];
   private final double[] sz = new double[6];
   private final int[] scol = new int[6];
   private final Glass.Pane pane = new Glass.Pane();

   private static Glass.Cells cells(RoseWindowEffect.Pattern p) {
      return p == RoseWindowEffect.Pattern.MANDALA?Glass.mandala():(p == RoseWindowEffect.Pattern.LANCET?Glass.lancet():Glass.rose());
   }

   public RoseWindowEffect(TargetAuraModule module) {
      super(module, EffectType.ROSE_WINDOW, "rosewindow", new ColorSetting("rosewindow.color", "Glass 1", "The first glass color (Tinted).", ColorSetting.Mode.STATIC, -13935399, -9794305, 1.0D), true);
      this.signature(this.glass2, -4184513);
      this.signature(this.glass3, -868018);
      this.signature(this.leadColor, -15066594);
      this.glassColors = (ModeSetting)this.setting(new ModeSetting("rosewindow.glassColors", "Glass Colors", "Cathedral: ruby, sapphire, emerald and gold. Tinted: the three glass colors. The light takes the glass colors.", Glass.GlassColors.class, Glass.GlassColors.CATHEDRAL));
      this.pattern = (ModeSetting)this.setting((new ModeSetting("rosewindow.pattern", "Pattern", "The window\'s design.", RoseWindowEffect.Pattern.class, RoseWindowEffect.Pattern.ROSE)).style(ModeSetting.Style.CHIPS));
      this.position = (ModeSetting)this.setting(new ModeSetting("rosewindow.position", "Position", "Behind the head and shoulders, or behind the whole body.", RoseWindowEffect.Position.class, RoseWindowEffect.Position.HEAD));
      this.shafts = this.look((SliderSetting)this.setting((new SliderSetting("rosewindow.shafts", "Light Shafts", "How strong the colored light through the glass is.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%")));
      this.groundLight = (BooleanSetting)this.setting(new BooleanSetting("rosewindow.groundLight", "Ground Light", "Tinted patches of light on the ground.", true));
      this.opacity = (SliderSetting)this.setting((new SliderSetting("rosewindow.opacity", "Glass Opacity", "How much of what is behind the glass it hides.", 40.0D, 100.0D, 85.0D, 5.0D)).unit("%"));
      this.motes = (BooleanSetting)this.setting(new BooleanSetting("rosewindow.motes", "Motes", "Dust drifting in the light.", true));
   }

   public void render(EffectContext c, FxPass pass) {
      float alpha = this.intensity(c);
      if(alpha > 0.003F) {
         if(pass == FxPass.SOLID) {
            this.layout(c);
            this.frame(c, alpha);
         } else if(pass == FxPass.TRANSLUCENT) {
            this.layout(c);
            this.glass(c, alpha);
         } else if(pass == FxPass.GLOW) {
            this.layout(c);
            this.light(c, alpha);
         }
      }

   }

   private void layout(EffectContext c) {
      double s = (double)this.size(c);
      Camera cam = c.cam != null?c.cam:Vfx.camera();
      double tx = cam.posX - c.x;
      double tz = cam.posZ - c.z;
      double tl = Math.sqrt(tx * tx + tz * tz);
      if(tl < 1.0E-6D) {
         tx = 0.0D;
         tz = 1.0D;
         tl = 1.0D;
      }

      this.nx = tx / tl;
      this.rx = this.nz = tz / tl;
      this.rz = -this.nx;
      double cy;
      if(this.position.get() == RoseWindowEffect.Position.BODY) {
         this.radius = 0.85D * s;
         cy = Math.max(c.y + 0.94D, c.groundY + this.radius + 0.05D);
      } else {
         this.radius = 0.58D * s;
         cy = c.y + (double)c.headTop - 0.15D;
         if((double)c.nameBottom > (double)c.headTop + 0.01D) {
            cy = Math.min(cy, c.y + (double)c.nameBottom - 0.03D - this.radius);
         }
      }

      this.ox = c.x - this.nx * 0.4D;
      this.oy = cy;
      this.oz = c.z - this.nz * 0.4D;
      this.turn = c.time * (double)this.speed() * Math.toRadians(4.0D);
      if(this.glassColors.get() == Glass.GlassColors.TINTED) {
         Glass.tinted(this.color(c, 0.5F), this.glass2.getColor(0.5F), this.glass3.getColor(0.5F), this.palette);
      } else {
         System.arraycopy(CATHEDRAL, 0, this.palette, 0, 4);
      }

   }

   public String stateForTest(EffectContext c) {
      this.layout(c);
      double dx = this.ox - c.x;
      double dz2 = this.oz - c.z;
      return String.format(Locale.ROOT, "%s, %d cells, center %.2f up and %.2f behind, radius %.2f (top %.2f; name tag from %.2f)", new Object[]{((RoseWindowEffect.Pattern)this.pattern.get()).displayName(), Integer.valueOf(cells((RoseWindowEffect.Pattern)this.pattern.get()).n), Double.valueOf(this.oy - c.y), Double.valueOf(Math.sqrt(dx * dx + dz2 * dz2)), Double.valueOf(this.radius), Double.valueOf(this.oy - c.y + this.radius), Float.valueOf(c.nameBottom)});
   }

   private double wx(double u) {
      return this.ox + this.rx * u * this.radius + this.nx * this.dz;
   }

   private double wy(double v) {
      return this.oy + v * this.radius + this.dy;
   }

   private double wz(double u) {
      return this.oz + this.rz * u * this.radius + this.nz * this.dz;
   }

   private boolean place(EffectContext c, Glass.Cells p, int i) {
      double t = c.time;
      double appear = 0.04D + 0.32D * Math.min(1.0D, p.rad[i] / 0.875D) + 0.04D * (double)hash(i, 3);
      float grow = Easing.clamp01((float)((t - appear) / 0.12D));
      if(grow <= 0.0F) {
         return false;
      } else {
         double sc = (double)Easing.outBack(grow, 1.6F);
         float a = Easing.clamp01(grow * 2.0F);
         float out = c.outro;
         double f = out <= 0.0F?0.0D:(double)Easing.clamp01((out - 0.2F * hash(i, 5)) / 0.8F);
         double spin = ((double)hash(i, 7) - 0.5D) * 3.0D * f;
         double drift = 0.3D * f;
         this.dy = -1.5D * f * f;
         this.dz = 0.12D * f * (double)hash(i, 11);
         float var52;
         this.cellAlpha = var52 = a * (1.0F - Easing.smoothstep(0.5F, 1.0F, (float)f));
         if(var52 <= 0.003F) {
            return false;
         } else {
            double ct = p.turns[i]?Math.cos(this.turn):1.0D;
            double st = p.turns[i]?Math.sin(this.turn):0.0D;
            double cu = p.cu[i] * ct - p.cv[i] * st;
            double cv = p.cu[i] * st + p.cv[i] * ct;
            double len = Math.sqrt(cu * cu + cv * cv);
            double ou = len > 1.0E-6D?cu / len:0.0D;
            double ov = len > 1.0E-6D?cv / len:-1.0D;
            this.tcu = cu + ou * drift;
            this.tcv = cv + ov * drift;
            double cs = Math.cos(spin);
            double ss = Math.sin(spin);
            int from = p.start[i];
            int n = Math.min(p.count[i], this.tu.length);

            for(int k = 0; k < n; ++k) {
               double ru = p.u[from + k] - p.cu[i];
               double rv = p.v[from + k] - p.cv[i];
               double au = ru * ct - rv * st;
               double av = ru * st + rv * ct;
               double bu = au * cs - av * ss;
               double bv = au * ss + av * cs;
               this.tu[k] = this.tcu + bu * sc;
               this.tv[k] = this.tcv + bv * sc;
            }

            return true;
         }
      }
   }

   private Glass.Pane pane() {
      this.pane.ox = this.ox;
      this.pane.oy = this.oy;
      this.pane.oz = this.oz;
      this.pane.rx = this.rx;
      this.pane.ry = 0.0D;
      this.pane.rz = this.rz;
      this.pane.ux = 0.0D;
      this.pane.uy = 1.0D;
      this.pane.uz = 0.0D;
      this.pane.nx = this.nx;
      this.pane.ny = 0.0D;
      this.pane.nz = this.nz;
      this.pane.scale = this.radius;
      this.pane.offX = this.nx * this.dz;
      this.pane.offY = this.dy;
      this.pane.offZ = this.nz * this.dz;
      return this.pane;
   }

   private void glass(EffectContext c, float alpha) {
      Glass.Cells p = cells((RoseWindowEffect.Pattern)this.pattern.get());
      float glassLight = Math.max(c.light, 0.6F);
      float op = this.opacity.getFloat() / 100.0F;
      int lead = ColorUtil.shade(this.leadColor.getColor(0.5F), c.light);
      RenderState.decal(true);

      try {
         for(int i = 0; i < p.n; ++i) {
            if(this.place(c, p, i)) {
               int base = ColorUtil.shade(this.palette[p.color[i]], glassLight * (0.9F + 0.2F * hash(i, 13)));
               float a = alpha * op * this.cellAlpha;
               int mid = alphaOf(ColorUtil.lerp(base, -1, 0.22F), a);
               int edge = alphaOf(ColorUtil.shade(base, 0.78F), a);
               Glass.glass(this.pane(), this.tu, this.tv, p.count[i], this.tcu, this.tcv, mid, edge);
            }
         }

         for(int var15 = 0; var15 < p.n; ++var15) {
            if(this.place(c, p, var15)) {
               Glass.lead(this.pane(), this.tu, this.tv, p.count[var15], 0.028D, alphaOf(lead, alpha * this.cellAlpha));
            }
         }

         this.dz = 0.0D;
         this.dy = 0.0D;
         Glass.ringLead(this.pane(), 0.655D, 0.705D, alphaOf(lead, alpha * this.bandAlpha(c)));
      } finally {
         RenderState.decal(false);
      }

   }

   private float bandAlpha(EffectContext c) {
      return Easing.clamp01((float)((c.time - 0.28D) / 0.1D)) * (1.0F - Easing.smoothstep(0.0F, 0.35F, c.outro));
   }

   private void frame(EffectContext c, float alpha) {
      double t = c.time;
      float in = Easing.clamp01((float)((t - 0.3D) / 0.15D));
      if(in > 0.0F) {
         Glass.Cells p = cells((RoseWindowEffect.Pattern)this.pattern.get());
         MeshBatch.begin(Material.CUSTOM, Finish.MATTE, c.light, 0.06F, -1, false, true);
         int per = 6;
         double grow = 0.92D + 0.08D * (double)Easing.outCubic(in);

         for(int piece = 0; piece < 8; ++piece) {
            float out = c.outro;
            double f = out <= 0.0F?0.0D:(double)Easing.clamp01((out - 0.15F * hash(piece, 17)) / 0.85F);
            float a2 = alpha * in * (1.0F - Easing.smoothstep(0.5F, 1.0F, (float)f));
            if(a2 > 0.003F) {
               int col = alphaOf(-7436419, a2);
               this.dy = -1.5D * f * f;
               this.dz = 0.1D * f * (double)hash(piece, 19);
               double mid = ((double)piece + 0.5D) * 6.283185307179586D / 8.0D;
               double du = Math.cos(mid) * 0.25D * f;
               double dv = Math.sin(mid) * 0.25D * f;
               int base = MeshBatch.stage((per + 1) * 4);

               for(int s = 0; s <= per; ++s) {
                  int seg = piece * per + s;
                  double cs = RING_COS[seg];
                  double sn = RING_SIN[seg];

                  for(int r = 0; r < 4; ++r) {
                     double rr = FRAME_R[r] * grow;
                     double u = cs * rr + du;
                     double v = sn * rr + dv;
                     double lean = FRAME_LEAN[r];
                     double nxv = this.nx * (1.0D - Math.abs(lean)) + this.rx * cs * lean;
                     double nyv = sn * lean;
                     double nzv = this.nz * (1.0D - Math.abs(lean)) + this.rz * cs * lean;
                     double l = Math.sqrt(nxv * nxv + nyv * nyv + nzv * nzv);
                     double zz = FRAME_Z[r] * this.radius;
                     MeshBatch.vertex(base + s * 4 + r, this.wx(u) + this.nx * zz, this.wy(v), this.wz(u) + this.nz * zz, nxv / l, nyv / l, nzv / l, col, 0.0F);
                  }
               }

               for(int var51 = 0; var51 < per; ++var51) {
                  for(int r = 0; r < 3; ++r) {
                     int a0 = base + var51 * 4 + r;
                     int b0 = base + (var51 + 1) * 4 + r;
                     MeshBatch.quad(a0, b0, b0 + 1, a0 + 1);
                  }
               }
            }
         }

         float a;
         if(p.stone && (a = alpha * in * (1.0F - Easing.smoothstep(0.0F, 0.4F, c.outro))) > 0.003F) {
            int col = alphaOf(ColorUtil.shade(-7436419, 0.86F), a);
            this.dy = 0.0D;
            this.dz = -0.004D / Math.max(0.01D, this.radius);
            int base = MeshBatch.stage(49);
            MeshBatch.vertex(base, this.wx(0.0D), this.wy(0.0D), this.wz(0.0D), this.nx, 0.0D, this.nz, col, 0.0F);

            for(int s = 0; s < 48; ++s) {
               double u = RING_COS[s] * 0.655D;
               double v = RING_SIN[s] * 0.655D;
               MeshBatch.vertex(base + 1 + s, this.wx(u), this.wy(v), this.wz(u), this.nx, 0.0D, this.nz, col, 0.0F);
            }

            for(int var50 = 0; var50 < 48; ++var50) {
               MeshBatch.tri(base, base + 1 + var50, base + 1 + (var50 + 1) % 48);
            }
         }

         MeshBatch.end();
         this.dz = 0.0D;
         this.dy = 0.0D;
      }

   }

   private void light(EffectContext c, float alpha) {
      float strength = this.shafts.getFloat() / 100.0F;
      float on = Easing.smoothstep(0.45F, 0.75F, (float)c.time) * (1.0F - Easing.smoothstep(0.0F, 0.25F, c.outro));
      float a = alpha * on;
      if(a > 0.003F && (strength > 0.0F || this.groundLight.get())) {
         Glass.Cells p = cells((RoseWindowEffect.Pattern)this.pattern.get());
         Camera cam = c.cam != null?c.cam:Vfx.camera();
         double s = (double)this.size(c);

         for(int k = 0; k < 6; ++k) {
            int i = p.shaftCells[k];
            double ct = p.turns[i]?Math.cos(this.turn):1.0D;
            double st = p.turns[i]?Math.sin(this.turn):0.0D;
            double cu = p.cu[i] * ct - p.cv[i] * st;
            double cv = p.cu[i] * st + p.cv[i] * ct;
            this.dz = 0.0D;
            this.dy = 0.0D;
            double px = this.wx(cu);
            double py = this.wy(cv);
            double pz = this.wz(cu);
            double len0 = Math.abs(cu) + 0.2D;
            double spread = cu / len0 * 0.45D;
            double dxv = this.nx * 0.35D + this.rx * spread;
            double dyv = -1.0D;
            double dzv = this.nz * 0.35D + this.rz * spread;
            double dl = Math.sqrt(dxv * dxv + dyv * dyv + dzv * dzv);
            dxv = dxv / dl;
            dyv = dyv / dl;
            dzv = dzv / dl;
            double drop = py - (c.groundY + 0.02D);
            if(drop >= 0.1D) {
               double len = drop / -dyv;
               double ex = px + dxv * len;
               double ey = py + dyv * len;
               double ez = pz + dzv * len;
               int col = this.palette[p.color[i]];
               this.sx[k] = px;
               this.sy[k] = py;
               this.sz[k] = pz;
               this.scol[k] = col;
               if(strength > 0.0F) {
                  Glass.shaft(cam, px, py, pz, ex, ey, ez, 0.1D * this.radius * 2.0D, 0.55D * s, alphaOf(col, a * 0.8F * strength));
               }

               if(this.groundLight.get()) {
                  Glass.patch(ex, ey + 0.015D, ez, 0.42F * (float)s, alphaOf(col, a * 0.5F * c.groundAlpha));
               }

               if(this.motes.get() && strength > 0.0F) {
                  for(int m = 0; m < 3; ++m) {
                     double q = c.time * 0.08D * (double)this.speed() + (double)k * 0.37D + (double)m * 0.33D;
                     q = q - Math.floor(q);
                     double side = Math.sin(c.time * 0.7D + (double)m * 2.1D + (double)k) * 0.06D;
                     double mx = px + (ex - px) * q + this.rx * side;
                     double my = py + (ey - py) * q + Math.sin(c.time * 1.3D + (double)m + (double)k) * 0.02D;
                     double mz = pz + (ez - pz) * q + this.rz * side;
                     float tw = (float)Math.sin(3.141592653589793D * q);
                     Vfx.sprite(Textures.SPARK, mx, my, mz, (double)(0.035F * (float)s), 0.0F, alphaOf(-3368, a * 0.75F * tw * strength));
                  }
               }
            }
         }

         this.dz = 0.0D;
         this.dy = 0.0D;
      }

   }

   private static int alphaOf(int color, float a) {
      return Math.round(255.0F * Easing.clamp01(a) * ((float)(color >>> 24) / 255.0F)) << 24 | color & 16777215;
   }

   public static enum Pattern implements Displayable {
      ROSE("Rose", "Petals round a golden heart"),
      MANDALA("Mandala", "Rings of pointed stars"),
      LANCET("Lancet trio", "Three tall pointed windows");

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

   public static enum Position implements Displayable {
      HEAD("Behind head", "A halo behind the head and shoulders"),
      BODY("Behind body", "Larger, behind the whole body");

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
