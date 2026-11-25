package mix.client.mixui.integration.haunt.module.targetaura.effects;

import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class PulseWaveEffect extends AuraEffect {
   public final SliderSetting interval = (SliderSetting)this.setting((new SliderSetting("pulse.interval", "Interval", "Time between waves.", 0.3D, 3.0D, 1.0D, 0.05D)).unit("s"));
   public final SliderSetting thickness = (SliderSetting)this.setting((new SliderSetting("pulse.thickness", "Thickness", "Width of each wave.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));

   public PulseWaveEffect(TargetAuraModule module) {
      super(module, EffectType.PULSE_WAVE, "pulse", new ColorSetting("pulse.color", "Color", "Wave color (gradient: young to old wave).", -13176371));
   }

   public void render(EffectContext c) {
      float sz = this.size(c);
      float a = this.intensity(c) * c.groundAlpha;
      if(a > 0.003F) {
         float spd = this.speed();
         float thick = this.thickness.getFloat();
         double every = this.interval.get();
         double duration = 1.35D / (double)spd;
         double maxRadius = 2.3D * (double)sz;
         double x = c.x;
         double y = c.groundY + 0.015D;
         double z = c.z;
         RenderState.decal(true);

         int newest;
         double age;
         for(int k = newest = (int)Math.floor(c.time / every); k >= 0 && (age = c.time - (double)k * every) < duration; --k) {
            float p = (float)(age / duration);
            double radius = Math.max(0.05D, maxRadius * (double)Easing.outCubic(p));
            double width = (0.14D + 0.45D * (double)p) * (double)thick * (double)sz;
            float fade = (float)Math.pow((double)(1.0F - p), 1.3D);
            int col = this.color(c, p);
            Vfx.flatBand(Textures.WAVE, x, y, z, Math.max(0.0D, radius - width), radius, 0.0D, 6.283185307179586D, 128, alpha(col, 0.85F * fade * a));
         }

         for(int var32 = newest; var32 >= 0 && (age = c.time - (double)var32 * every) < duration; --var32) {
            float p = (float)(age / duration);
            double radius = Math.max(0.05D, maxRadius * (double)Easing.outCubic(p));
            float fade = (float)Math.pow((double)(1.0F - p), 1.3D);
            Vfx.flatRing(Textures.LINE_CORE, x, y, z, radius, 0.032D * (double)sz * (double)thick, 128, core(this.color(c, p), 0.85F * fade * a));
         }

         float flash = (float)Math.exp(-(c.time - (double)newest * every) * 5.0D);
         Vfx.flatSprite(Textures.GLOW, x, y, z, 0.6D * (double)sz * (1.0D + 0.35D * (double)flash), 0.0D, alpha(this.color(c, 0.0F), (0.25F + 0.5F * flash) * a));
         Vfx.flatRing(Textures.LINE_GLOW, x, y, z, 0.42D * (double)sz, 0.16D * (double)sz, 96, alpha(this.color(c, 0.0F), (0.2F + 0.3F * flash) * a));
         Vfx.flatRing(Textures.LINE_CORE, x, y, z, 0.42D * (double)sz, 0.025D * (double)sz, 96, core(this.color(c, 0.0F), (0.35F + 0.45F * flash) * a));
         RenderState.decal(false);
      }

   }
}
