package mix.client.mixui.integration.haunt.module.targetaura.effects;

import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class SoulOrbsEffect extends AuraEffect {
   private static final int SAMPLES = 24;
   public final SliderSetting orbs = (SliderSetting)this.setting(new SliderSetting("orbs.count", "Orbs", "Number of orbs.", 3.0D, 6.0D, 4.0D, 1.0D));
   public final SliderSetting trailLength = (SliderSetting)this.setting((new SliderSetting("orbs.trail", "Trail Length", "Length of the trails, in seconds of travel.", 0.0D, 0.8D, 0.35D, 0.05D)).unit("s"));

   public SoulOrbsEffect(TargetAuraModule module) {
      super(module, EffectType.SOUL_ORBS, "orbs", new ColorSetting("orbs.color", "Color", "Orb color (gradient: spread across the orbs).", -6005505));
   }

   private static void orb(EffectContext c, int i, int n, double tau, double radiusScale, float spd, float sz, int idx) {
      double phase = (double)i * 6.283185307179586D / (double)n;
      double angle = tau * 6.283185307179586D * 0.42D * (double)spd + phase;
      double r = (0.72D + 0.05D * Math.sin(1.7D * tau + (double)i * 1.3D)) * radiusScale;
      Vfx.X[idx] = c.x + Math.cos(angle) * r;
      Vfx.Y[idx] = c.y + (double)c.height * 0.6D + 0.14D * (double)sz * Math.sin(tau * 6.283185307179586D * 0.5D * (double)spd + phase * 1.5D);
      Vfx.Z[idx] = c.z + Math.sin(angle) * r;
   }

   public void render(EffectContext c) {
      float sz = this.size(c);
      float inten = this.intensity(c);
      float spd = this.speed();
      int n = this.orbs.getInt();
      double radiusScale = (double)(sz * Math.max(0.0F, Easing.outBack(Easing.clamp01(c.intro / 0.8F), 1.2F)));
      double tail = Math.min(this.trailLength.get(), c.time);
      int samples = tail > 0.01D?24:1;

      for(int i = 0; i < n; ++i) {
         for(int k = 0; k < samples; ++k) {
            orb(c, i, n, c.time - tail * (double)k / (double)Math.max(1, samples - 1), radiusScale, spd, sz, i * 24 + k);
         }
      }

      if(samples > 1) {
         for(int var17 = 0; var17 < n; ++var17) {
            int col = this.color(c, n > 1?(float)var17 / (float)(n - 1):0.0F);

            for(int k = 0; k < samples; ++k) {
               float remain = 1.0F - (float)k / (float)(samples - 1);
               Vfx.W[var17 * 24 + k] = 0.15F * sz * (float)Math.pow((double)remain, 0.8D);
               Vfx.C[var17 * 24 + k] = alpha(col, 0.5F * inten * (float)Math.pow((double)remain, 1.6D));
            }

            Vfx.ribbon(Textures.LINE_GLOW, var17 * 24, samples, false);
         }
      }

      for(int pass = 0; pass < 3; ++pass) {
         for(int i2 = 0; i2 < n; ++i2) {
            int idx = i2 * 24;
            int col = this.color(c, n > 1?(float)i2 / (float)(n - 1):0.0F);
            float pulse = 0.85F + 0.15F * (float)Math.sin(c.time * 4.0D + (double)i2 * 1.7D);
            if(pass == 0) {
               Vfx.sprite(Textures.GLOW, Vfx.X[idx], Vfx.Y[idx], Vfx.Z[idx], (double)(0.95F * sz * pulse), 0.0F, alpha(col, 0.5F * inten));
            } else if(pass == 1) {
               Vfx.sprite(Textures.GLOW, Vfx.X[idx], Vfx.Y[idx], Vfx.Z[idx], (double)(0.38F * sz), 0.0F, core(col, 0.55F * inten));
            } else {
               Vfx.sprite(Textures.SPARK, Vfx.X[idx], Vfx.Y[idx], Vfx.Z[idx], (double)(0.26F * sz), 0.0F, core(col, inten));
            }
         }
      }

   }
}
