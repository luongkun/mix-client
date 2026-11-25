package mix.client.mixui.integration.haunt.module.landing.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class JumpCirclesStyle extends LandingStyleBase {
   private static final int SEGMENTS = 96;
   private static final int DASHES = 26;
   public final SliderSetting rings = (SliderSetting)this.setting(new SliderSetting("circles.rings", "Rings", "How many rings, 80 ms apart.", 1.0D, 3.0D, 2.0D, 1.0D));
   public final SliderSetting radius = (SliderSetting)this.setting((new SliderSetting("circles.radius", "Radius", "How far the rings reach.", 0.5D, 3.0D, 1.5D, 0.05D)).unit(" blocks"));
   public final SliderSetting thickness = (SliderSetting)this.setting((new SliderSetting("circles.thickness", "Thickness", "Width of the rings.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   public final ModeSetting<JumpCirclesStyle.Line> line = (ModeSetting)this.setting(new ModeSetting("circles.line", "Line", "How the rings are drawn.", JumpCirclesStyle.Line.class, JumpCirclesStyle.Line.SOLID));
   private int offset;

   public JumpCirclesStyle(LandingImpactModule module) {
      super(module, LandingStyle.JUMP_CIRCLES, 1.2F, 1.6F, new ColorSetting("circles.color", "Color", "Ring glow (gradient: around each ring).", -5215233));
   }

   public void render(LandingFx fx, FxFrame f) {
      float p = fx.progress();
      float fadeAll = 1.0F - Easing.smoothstep(0.5F, 1.0F, p);
      int n = this.rings.getInt();
      double maxR = this.radius.get() * (double)fx.size;
      float th = this.thickness.getFloat();
      float stagger = 0.08F / fx.style.baseSeconds();
      RenderState.decal(true);
      this.offset = 0;

      for(int k = 0; k < n; ++k) {
         float q = Easing.clamp01((p - (float)k * stagger) / 0.55F);
         if(q > 0.0F) {
            double r = Math.max(0.04D, maxR * (double)Easing.outQuint(q) * (1.0D - 0.12D * (double)k));
            float a = fx.alpha() * fadeAll * Easing.smoothstep(0.0F, 0.05F, q) * (1.0F - 0.18F * (float)k);
            if(a > 0.003F) {
               float shift = (float)k * 0.21F;
               if(k == 0) {
                  RenderState.translucent(false);
                  Ground.disc(f, fx.ground, Textures.RADIAL_FILL, fx.x(), fx.z(), r, 0.0D, alpha(this.color.getColor(0.5F), 0.3F * a), 4, 32);
                  RenderState.additive();
               }

               double w = 0.115D * (double)th * Math.sqrt((double)fx.size);
               switch((JumpCirclesStyle.Line)this.line.get()) {
               case DASHED:
                  for(int d = 0; d < 26; ++d) {
                     double a0 = 6.283185307179586D * (double)d / 26.0D + (double)p * 0.4D;
                     double span = 0.14016336454477538D;
                     int c = alpha(this.color.getColor(loop((float)d / 26.0F + shift)), a);

                     for(int i = 0; i <= 4; ++i) {
                        double ang = a0 + span * (double)i / 4.0D;
                        Vfx.X[this.offset + i] = fx.x() + Math.cos(ang) * r;
                        Vfx.Z[this.offset + i] = fx.z() + Math.sin(ang) * r;
                        Vfx.W[this.offset + i] = (float)w;
                        Vfx.C[this.offset + i] = c;
                     }

                     Ground.toRender(f, fx.ground, this.offset, 5);
                     Neon.addFlat(this.offset, 5);
                     this.offset += 5;
                  }
                  break;
               case DOUBLE:
                  this.ring(fx, f, r, w * 0.8D, a, shift);
                  this.ring(fx, f, r * 0.84D, w * 0.6D, a * 0.75F, shift + 0.3F);
                  break;
               default:
                  this.ring(fx, f, r, w, a, shift);
               }
            }
         }
      }

      Neon.flush();
      RenderState.decal(false);
   }

   private void ring(LandingFx fx, FxFrame f, double r, double w, float a, float shift) {
      int count = Ground.ringPoints(f, fx.ground, this.offset, fx.x(), fx.z(), r, w, 96, this.color, 0, a, shift);
      Neon.addFlat(this.offset, count);
      this.offset += count;
   }

   public static enum Line implements Displayable {
      SOLID("Solid", "One unbroken ring"),
      DASHED("Dashed", "A ring of dashes"),
      DOUBLE("Double", "Two rings side by side");

      private final String displayName;
      private final String description;

      private Line(String displayName, String description) {
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
