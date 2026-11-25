package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.List;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.core.ClientClock;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.fx.trail.TrailShape;
import mix.client.mixui.integration.haunt.fx.trail.TrailStrip;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.module.watermark.WaterMarkModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class RibbonStyle extends TrailStyleBase {
   private static final float GAP = 0.35F;
   public final ModeSetting<RibbonStyle.Orientation> orientation = (ModeSetting)this.setting(new ModeSetting("ribbon.orientation", "Orientation", "Facing camera never looks paper-thin; Flat lies level like a scarf; Upright stands.", RibbonStyle.Orientation.class, RibbonStyle.Orientation.FACING));
   public final SliderSetting bands = (SliderSetting)this.setting(new SliderSetting("ribbon.bands", "Bands", "Parallel bands with small gaps; with Gradient or Chroma each band gets its own hue, for a prism look.", 1.0D, 5.0D, 1.0D, 1.0D));
   public final SliderSetting twist = (SliderSetting)this.setting((new SliderSetting("ribbon.twist", "Twist", "Turns the band around its path (100%: a full turn every block).", 0.0D, 100.0D, 0.0D, 1.0D)).unit("%").visibleWhen(new BooleanSupplier() {
      public boolean getAsBoolean() {
         return RibbonStyle.this.orientation.get() != RibbonStyle.Orientation.FACING;
      }
   }));
   public final SliderSetting core = (SliderSetting)this.setting((new SliderSetting("ribbon.core", "Core Brightness", "The white-hot line down the band\'s middle.", 0.0D, 100.0D, 70.0D, 1.0D)).unit("%"));
   public final BooleanSetting glint = (BooleanSetting)this.setting(new BooleanSetting("ribbon.glint", "Glint", "A light sweep runs from head to tail, at the same moments as the island\'s glint.", false));

   public RibbonStyle(TrailsModule module) {
      super(module, TrailStyle.RIBBON, spec().color("Trail", -9447169).size(0.05D, 1.0D, 0.25D, 0.01D).taper(70.0D).anchor(TrailAnchor.WAIST, false));
   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.core);
   }

   public void render(PlayerTrail p, TrailFrame f) {
      TrailShape s = p.shape;
      int count = Math.max(1, this.bands.getInt());
      float size = this.size.getFloat();
      float bandW = count == 1?size:size / ((float)count + (float)(count - 1) * 0.35F);
      int mode = this.orientation.get() == RibbonStyle.Orientation.FLAT?1:(this.orientation.get() == RibbonStyle.Orientation.UPRIGHT?2:0);
      float turns = this.twist.getFloat() / 100.0F;
      float glowAmount = this.glow.getFloat() / 100.0F;
      float coreAmount = this.core.getFloat() / 100.0F;
      float sweep = this.glint.get()?glintAt(s.length):Float.NaN;

      for(int k = 0; k < s.runs; ++k) {
         int n = this.fill(p, f, k, bandW, -1.0F, 0.0F);
         if(n >= 2) {
            TrailStrip.sides(0, n, mode, f.cam.posX, f.cam.posY, f.cam.posZ, turns, s.along, s.runStart[k]);

            for(int b = 0; b < count; ++b) {
               double offset = count == 1?0.0D:((double)b - (double)(count - 1) / 2.0D) * 1.3499999940395355D;
               if(b > 0) {
                  this.recolor(p, f, k, n, (float)b * 0.18F);
               }

               RenderState.translucent(false);
               TrailStrip.draw(Textures.LINE_GLOW, 0, n, 1.3F, offset, 0.6F);
               RenderState.additive();
               TrailStrip.draw(Textures.LINE_GLOW, 0, n, 1.4F + 2.4F * glowAmount, offset, 0.5F * glowAmount);
               TrailStrip.draw(Textures.LINE_GLOW, 0, n, 0.95F, offset, 0.55F);
               if(coreAmount > 0.0F || !Float.isNaN(sweep)) {
                  toCore(n);
                  TrailStrip.draw(Textures.LINE_CORE, 0, n, 0.26F, offset, 0.85F * coreAmount);
                  if(!Float.isNaN(sweep)) {
                     glintColors(s, k, n, sweep);
                     TrailStrip.draw(Textures.LINE_GLOW, 0, n, 1.25F, offset, 1.0F);
                  }
               }
            }
         }
      }

   }

   private static void toCore(int n) {
      for(int j = 0; j < n; ++j) {
         int c = Vfx.C[j];
         Vfx.C[j] = c & -16777216 | ColorUtil.core(c) & 16777215;
      }

   }

   private static void glintColors(TrailShape s, int k, int n, float at) {
      int from = s.runStart[k];

      for(int j = 0; j < n; ++j) {
         float d = (s.along[from + j] - at) / 0.45F;
         float bump = (float)Math.exp((double)(-d * d)) * (FuriahsVisuals.instance.isReduceFlashing()?0.45F:0.9F);
         int a = Math.round(255.0F * POINT_ALPHA[j] * Math.min(1.0F, bump));
         Vfx.C[j] = a << 24 | Vfx.C[j] & 16777215;
      }

   }

   private static float glintAt(float length) {
      WaterMarkModule wm = FuriahsVisuals.instance == null?null:FuriahsVisuals.instance.getWaterMark();
      float sweepTime = wm == null?0.9F:Math.max(0.2F, wm.glintSweep());
      float every = wm == null?4.0F:wm.glintEvery();
      float period = Math.max(every, sweepTime + 0.25F);
      double now = ClientClock.seconds();
      double t = (now - Math.floor(now / (double)period) * (double)period) / (double)sweepTime;
      return t > 0.0D && t < 1.0D?-0.5F + (length + 1.0F) * Easing.inOutSine((float)t):Float.NaN;
   }

   public static enum Orientation implements Displayable {
      FACING("Facing camera", "Always turned to the camera: it never looks paper-thin"),
      FLAT("Flat", "Lies level, like a scarf in the wind"),
      UPRIGHT("Upright", "A standing band");

      private final String name;
      private final String description;

      private Orientation(String name, String description) {
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
