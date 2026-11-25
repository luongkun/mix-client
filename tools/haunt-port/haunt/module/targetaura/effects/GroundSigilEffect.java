package mix.client.mixui.integration.haunt.module.targetaura.effects;

import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class GroundSigilEffect extends AuraEffect {
   private static final int RUNES = 12;
   private static final int TICKS = 48;
   public final BooleanSetting runes = (BooleanSetting)this.setting(new BooleanSetting("sigil.runes", "Runes", "Band of runes around the outer ring.", true));
   public final SliderSetting pulse = (SliderSetting)this.setting(new SliderSetting("sigil.pulse", "Pulse", "How much the brightness breathes.", 0.0D, 1.0D, 0.5D, 0.05D));

   public GroundSigilEffect(TargetAuraModule module) {
      super(module, EffectType.GROUND_SIGIL, "sigil", new ColorSetting("sigil.color", "Color", "Sigil color (gradient: center to edge).", ColorSetting.Mode.GRADIENT, -6591489, -12201729, 1.0D));
   }

   public void render(EffectContext c) {
      float sz = this.size(c);
      float a = this.intensity(c) * c.groundAlpha;
      if(a > 0.003F) {
         float spd = this.speed();
         float grow = Easing.outBack(Easing.clamp01(c.intro / 0.9F), 1.1F);
         double R = 1.05D * (double)sz * (double)grow;
         if(R > 0.001D) {
            double x = c.x;
            double y = c.groundY + 0.015D;
            double z = c.z;
            float breathe = 1.0F - this.pulse.getFloat() * 0.35F * (0.5F + 0.5F * (float)Math.sin(c.time * 6.283185307179586D * 0.5D));
            float b = a * breathe;
            double spinUp = (double)(1.0F - Easing.outCubic(c.intro)) * 2.5D;
            double outer = c.time * 0.35D * (double)spd + spinUp;
            double inner = -c.time * 0.5D * (double)spd - spinUp;
            RenderState.decal(true);
            Vfx.flatSprite(Textures.GROUND_GLOW, x, y, z, R * 1.12D, outer, alpha(this.color(c, 0.6F), 0.28F * b));
            Vfx.flatSprite(Textures.GLOW, x, y, z, R * 0.45D, 0.0D, alpha(this.color(c, 0.0F), 0.45F * b));
            this.ring(c, Textures.LINE_GLOW, x, y, z, R * 0.985D, 0.12D * (double)sz, 0.35F * b, 1.0F, false);
            this.ring(c, Textures.LINE_GLOW, x, y, z, R * 0.72D, 0.1D * (double)sz, 0.3F * b, 0.7F, false);
            this.ring(c, Textures.LINE_CORE, x, y, z, R, 0.022D * (double)sz, 0.9F * b, 1.0F, true);
            this.ring(c, Textures.LINE_CORE, x, y, z, R * 0.9D, 0.013D * (double)sz, 0.7F * b, 0.95F, true);
            this.ring(c, Textures.LINE_CORE, x, y, z, R * 0.72D, 0.016D * (double)sz, 0.85F * b, 0.7F, true);
            this.ring(c, Textures.LINE_CORE, x, y, z, R * 0.34D, 0.013D * (double)sz, 0.8F * b, 0.35F, true);
            this.ring(c, Textures.LINE_CORE, x, y, z, R * 0.12D, 0.01D * (double)sz, 0.7F * b, 0.1F, true);
            if(this.runes.get()) {
               double rr = R * 0.945D;
               double half = R * 0.035D;

               for(int k = 0; k < 12; ++k) {
                  double ang = outer + (double)k * 6.283185307179586D / 12.0D;
                  double cos = Math.cos(ang);
                  double sin = Math.sin(ang);
                  float u0 = (float)(k % 8) / 8.0F;
                  float u1 = u0 + 0.125F;
                  Vfx.flatQuad(Textures.RUNES, x + cos * rr, y, z + sin * rr, -sin * half, cos * half, -cos * half, -sin * half, u0, 0.0F, u1, 1.0F, alpha(this.color(c, 0.95F), 0.9F * b));
               }
            } else {
               for(int k = 0; k < 24; ++k) {
                  double ang = outer + (double)k * 6.283185307179586D / 24.0D;
                  double cos = Math.cos(ang);
                  double sin = Math.sin(ang);
                  Vfx.flatSprite(Textures.SPARK, x + cos * R * 0.945D, y, z + sin * R * 0.945D, R * 0.03D, 0.0D, core(this.color(c, 0.95F), 0.8F * b));
               }
            }

            int tickColor = core(this.color(c, 0.78F), 0.75F * b);

            for(int k = 0; k < 48; ++k) {
               double ang = inner + (double)k * 6.283185307179586D / 48.0D;
               double cos = Math.cos(ang);
               double sin = Math.sin(ang);
               double r1 = R * (k % 4 == 0?0.74D:0.77D);
               double r2 = R * 0.85D;
               Vfx.flatLine(Textures.LINE_CORE, x + cos * r1, z + sin * r1, x + cos * r2, z + sin * r2, y, 0.011D * (double)sz, tickColor, tickColor);
            }

            double sr = R * 0.72D;
            float draw = Easing.clamp01((c.intro - 0.2F) / 0.7F);

            for(int pass = 0; pass < 2; ++pass) {
               int tex = pass == 0?Textures.LINE_GLOW:Textures.LINE_CORE;
               double width = pass == 0?0.07D * (double)sz:0.015D * (double)sz;

               for(int k = 0; k < 8; ++k) {
                  float line = Easing.outCubic(Easing.clamp01(draw * 1.8F - (float)k * 0.1F));
                  if(line > 0.0F) {
                     double a1 = inner + (double)k * 6.283185307179586D / 8.0D;
                     double a2 = inner + (double)(k + 3) * 6.283185307179586D / 8.0D;
                     double x1 = x + Math.cos(a1) * sr;
                     double z1 = z + Math.sin(a1) * sr;
                     double x2 = x + Math.cos(a2) * sr;
                     double z2 = z + Math.sin(a2) * sr;
                     x2 = x1 + (x2 - x1) * (double)line;
                     z2 = z1 + (z2 - z1) * (double)line;
                     int col = this.color(c, 0.5F);
                     int lc = pass == 0?alpha(col, 0.3F * b):core(col, 0.85F * b);
                     Vfx.flatLine(tex, x1, z1, x2, z2, y, width, lc, lc);
                  }
               }
            }

            int hex = core(this.color(c, 0.3F), 0.75F * b);

            for(int kx = 0; kx < 6; ++kx) {
               double a1 = outer * 1.5D + (double)kx * 6.283185307179586D / 6.0D;
               double a2 = outer * 1.5D + (double)(kx + 1) * 6.283185307179586D / 6.0D;
               double r = R * 0.34D;
               Vfx.flatLine(Textures.LINE_CORE, x + Math.cos(a1) * r, z + Math.sin(a1) * r, x + Math.cos(a2) * r, z + Math.sin(a2) * r, y, 0.012D * (double)sz, hex, hex);
            }

            RenderState.decal(false);
         }
      }

   }

   private void ring(EffectContext c, int tex, double x, double y, double z, double radius, double width, float a, float radial, boolean crisp) {
      int col = this.color(c, radial);
      Vfx.flatRing(tex, x, y, z, radius, width, 128, crisp?core(col, a):alpha(col, a));
   }
}
