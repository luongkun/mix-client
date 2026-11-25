package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.List;
import mix.client.mixui.integration.haunt.fx.trail.GroundCache;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.fx.trail.TrailShape;
import mix.client.mixui.integration.haunt.fx.trail.TrailStrip;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class LightWallStyle extends TrailStyleBase {
   public final SliderSetting height = (SliderSetting)this.setting((new SliderSetting("light_wall.height", "Height", "How tall the wall stands.", 0.2D, 2.0D, 0.6D, 0.05D)).unit(" blocks"));
   public final SliderSetting edge = (SliderSetting)this.setting((new SliderSetting("light_wall.edge", "Edge Brightness", "How bright the top edge and the base line are.", 0.0D, 100.0D, 80.0D, 1.0D)).unit("%"));
   public final SliderSetting fillOpacity = (SliderSetting)this.setting((new SliderSetting("light_wall.fill", "Fill Opacity", "How strong the light between the edges is.", 0.0D, 100.0D, 35.0D, 1.0D)).unit("%"));
   public final ModeSetting<LightWallStyle.Variant> variant = (ModeSetting)this.setting(new ModeSetting("light_wall.variant", "Variant", "A standing wall, a line on the ground, or both.", LightWallStyle.Variant.class, LightWallStyle.Variant.WALL));
   public final BooleanSetting rise = (BooleanSetting)this.setting(new BooleanSetting("light_wall.rise", "Rise with Jumps", "The wall\'s top follows your feet when you jump.", false));
   public final BooleanSetting scan = (BooleanSetting)this.setting(new BooleanSetting("light_wall.scan", "Scan Lines", "Thin lines drifting up the wall.", false));
   public final BooleanSetting dashed = (BooleanSetting)this.setting(new BooleanSetting("light_wall.dashed", "Dashed", "The wall in dashes.", false));
   private final GroundCache ground = new GroundCache();
   private static final double[] PX = new double[1024];
   private static final double[] PY = new double[1024];
   private static final double[] PZ = new double[1024];
   private static final double[] BASE = new double[1024];
   private static final double[] TOP = new double[1024];
   private static final int[] COL = new int[1024];
   private static final float[] WID = new float[1024];
   private int wallPieces;

   public LightWallStyle(TrailsModule module) {
      super(module, TrailStyle.LIGHT_WALL, spec().color("Wall", -11540737).size(0.02D, 0.2D, 0.06D, 0.01D).taper(20.0D).anchor(TrailAnchor.FEET, true).animated());
   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.height);
      out.add(this.edge);
      out.add(this.fillOpacity);
   }

   public int wallPiecesForTest() {
      return this.wallPieces;
   }

   public void render(PlayerTrail p, TrailFrame f) {
      TrailShape s = p.shape;
      LightWallStyle.Variant v = (LightWallStyle.Variant)this.variant.get();
      this.wallPieces = 0;

      for(int k = 0; k < s.runs; ++k) {
         int n = this.fill(p, f, k, this.size.getFloat(), -1.0F, 0.0F);
         if(n >= 2) {
            this.groundOf(p, f, s, k, n);
            int start = 0;

            for(int j = 1; j <= n; ++j) {
               if(j == n || Math.abs(BASE[j] - BASE[j - 1]) > 1.05D) {
                  if(j - start >= 2) {
                     this.piece(p, f, s, k, start, j, v);
                     ++this.wallPieces;
                  }

                  start = j;
               }
            }
         }
      }

   }

   private void groundOf(PlayerTrail p, TrailFrame f, TrailShape s, int k, int n) {
      int from = s.runStart[k];
      double h = (double)this.height.getFloat() * Math.sqrt((double)p.widthScale);
      boolean dash = this.dashed.get();
      double last = Double.NaN;

      for(int j = 0; j < n; ++j) {
         PX[j] = Vfx.X[j];
         PY[j] = Vfx.Y[j];
         PZ[j] = Vfx.Z[j];
         WID[j] = Vfx.W[j];
         int c = Vfx.C[j];
         if(dash && ((int)Math.floor((double)(s.along[from + j] / 0.35F)) & 1) == 1) {
            c = ColorUtil.mulAlpha(c, 0.12F);
         }

         COL[j] = c;
         double wx = PX[j] + f.viewX;
         double feet = PY[j] + f.viewY - 0.06D;
         double wz = PZ[j] + f.viewZ;
         double g = this.ground.surface(f.world, wx, feet, wz, 0.6D, 3.0D);
         if(Double.isNaN(g)) {
            g = Double.isNaN(last)?feet:last;
         }

         last = g;
         BASE[j] = g - f.viewY + 0.012D;
         TOP[j] = this.rise.get()?Math.max(BASE[j], feet - f.viewY) + h:BASE[j] + h;
      }

   }

   private void piece(PlayerTrail p, TrailFrame f, TrailShape s, int k, int a, int b, LightWallStyle.Variant v) {
      float edges = this.edge.getFloat() / 100.0F;
      float fillA = this.fillOpacity.getFloat() / 100.0F;
      float glowAmount = this.glow.getFloat() / 100.0F;
      RenderState.additive();
      if(v != LightWallStyle.Variant.LINE) {
         if(fillA > 0.0F) {
            for(int pass = 0; pass < 2; ++pass) {
               if(pass == 0) {
                  RenderState.translucent(false);
               } else {
                  RenderState.additive();
               }

               float bottom = pass == 0?0.95F:0.45F;
               float top = pass == 0?0.12F:0.16F;

               for(int j = a + 1; j < b; ++j) {
                  int c0 = COL[j - 1];
                  int c1 = COL[j];
                  int b0 = ColorUtil.mulAlpha(c0, fillA * bottom);
                  int b1 = ColorUtil.mulAlpha(c1, fillA * bottom);
                  int t0 = ColorUtil.mulAlpha(c0, fillA * top);
                  int t1 = ColorUtil.mulAlpha(c1, fillA * top);
                  Vfx.quad(PX[j - 1], BASE[j - 1], PZ[j - 1], PX[j - 1], TOP[j - 1], PZ[j - 1], PX[j], TOP[j], PZ[j], PX[j], BASE[j], PZ[j], b0, t0, t1, b1);
               }
            }
         }

         RenderState.translucent(false);
         this.edgeLine(f, a, b, 1.0D, Textures.LINE_GLOW, 1.4F, 0.45F * edges, 0.0F);
         this.edgeLine(f, a, b, 0.0D, Textures.LINE_GLOW, 1.8F, 0.55F * edges, 0.0F);
         RenderState.additive();
         if(this.scan.get()) {
            double rate = 0.45D * (double)this.animSpeed.getFloat() / 100.0D;

            for(int line = 0; line < 3; ++line) {
               double u = (f.now * rate + (double)line / 3.0D) % 1.0D;
               this.edgeLine(f, a, b, u, Textures.LINE_GLOW, 0.6F, 0.35F * (float)(1.0D - u), 0.0F);
            }
         }

         this.edgeLine(f, a, b, 1.0D, Textures.LINE_GLOW, 2.2F + 3.0F * glowAmount, 0.35F * edges * (0.4F + glowAmount), 0.0F);
         this.edgeLine(f, a, b, 1.0D, Textures.LINE_CORE, 0.9F, 0.85F * edges, 1.0F);
         this.edgeLine(f, a, b, 0.0D, Textures.LINE_GLOW, 2.6F + 3.0F * glowAmount, 0.45F * edges * (0.4F + glowAmount), 0.0F);
         this.edgeLine(f, a, b, 0.0D, Textures.LINE_CORE, 1.2F, edges, 1.0F);
      }

      if(v != LightWallStyle.Variant.WALL) {
         int n = 0;

         for(int j = a; j < b; ++j) {
            Vfx.X[n] = PX[j];
            Vfx.Y[n] = BASE[j] + 0.004D;
            Vfx.Z[n] = PZ[j];
            Vfx.W[n] = WID[j] * 4.0F;
            Vfx.C[n] = COL[j];
            ++n;
         }

         TrailStrip.sides(0, n, 1, f.cam.posX, f.cam.posY, f.cam.posZ, 0.0F, (float[])null, 0);
         RenderState.translucent(false);
         TrailStrip.draw(Textures.LINE_GLOW, 0, n, 1.0F, 0.0D, 0.6F);
         RenderState.additive();
         TrailStrip.draw(Textures.LINE_GLOW, 0, n, 1.6F + 2.0F * glowAmount, 0.0D, 0.5F);
         TrailStrip.draw(Textures.LINE_CORE, 0, n, 0.45F, 0.0D, Math.max(0.3F, edges));
      }

   }

   private void edgeLine(TrailFrame f, int a, int b, double u, int texture, float widthScale, float alpha, float white) {
      if(alpha > 0.0F) {
         int n = 0;

         for(int j = a; j < b; ++j) {
            Vfx.X[n] = PX[j];
            Vfx.Y[n] = BASE[j] + (TOP[j] - BASE[j]) * u;
            Vfx.Z[n] = PZ[j];
            Vfx.W[n] = WID[j];
            int c = COL[j];
            Vfx.C[n] = white > 0.0F?c & -16777216 | ColorUtil.lerp(c, ColorUtil.core(c), white) & 16777215:c;
            ++n;
         }

         TrailStrip.sides(0, n, 0, f.cam.posX, f.cam.posY, f.cam.posZ, 0.0F, (float[])null, 0);
         TrailStrip.draw(texture, 0, n, widthScale, 0.0D, alpha);
      }

   }

   public static enum Variant implements Displayable {
      WALL("Wall", "A standing sheet of light"),
      LINE("Ground line", "A flat glowing line on the ground"),
      BOTH("Both", "The wall and the line");

      private final String name;
      private final String description;

      private Variant(String name, String description) {
         this.name = name;
         this.description = description;
      }

      public String displayName() {
         return this.name;
      }

      public String description() {
         return this.description;
      }
   }
}
