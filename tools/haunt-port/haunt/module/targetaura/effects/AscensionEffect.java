package mix.client.mixui.integration.haunt.module.targetaura.effects;

import java.util.Random;
import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.AuraState;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class AscensionEffect extends AuraEffect {
   private static final int SEGMENTS = 48;
   private static final int LEVELS = 6;
   public final SliderSetting height = (SliderSetting)this.setting((new SliderSetting("ascension.height", "Height", "Height of the light column.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   public final SliderSetting embers = (SliderSetting)this.setting((new SliderSetting("ascension.embers", "Embers", "Embers per second.", 0.0D, 60.0D, 24.0D, 1.0D)).unit("/s"));

   public AscensionEffect(TargetAuraModule module) {
      super(module, EffectType.ASCENSION, "ascension", new ColorSetting("ascension.color", "Color", "Light color (gradient: bottom to top).", -11413));
   }

   public void render(EffectContext c) {
      float sz = this.size(c);
      float inten = this.intensity(c);
      float spd = this.speed();
      float grow = Easing.outCubic(Easing.clamp01(c.intro / 0.8F));
      double radius = ((double)c.width * 0.5D + 0.28D) * (double)sz;
      double columnHeight = (double)c.height * 1.35D * this.height.get() * (double)sz * (double)grow;
      double base = c.groundY + 0.01D;
      float ga = c.groundAlpha;
      if(columnHeight > 0.01D) {
         this.shell(c, radius, columnHeight * 0.999D, base, inten * ga, c.time * 0.45D * (double)spd, 0.0F, 1.0F);
         this.shell(c, radius * 0.72D, columnHeight * 0.8D, base, inten * ga * 0.5F, c.time * 0.65D * (double)spd, 0.37F, 0.8F);
      }

      float a;
      if((a = inten * ga) > 0.003F) {
         RenderState.decal(true);
         Vfx.flatSprite(Textures.GROUND_GLOW, c.x, base + 0.005D, c.z, radius * 1.6D * (0.5D + 0.5D * (double)grow), 0.0D, alpha(this.color(c, 0.0F), 0.5F * a));
         Vfx.flatRing(Textures.LINE_GLOW, c.x, base + 0.005D, c.z, radius, 0.3D * (double)sz, 96, alpha(this.color(c, 0.0F), 0.55F * a));
         Vfx.flatRing(Textures.LINE_CORE, c.x, base + 0.005D, c.z, radius, 0.03D * (double)sz, 96, core(this.color(c, 0.0F), 0.9F * a));
         RenderState.decal(false);
      }

      this.emitEmbers(c, sz, radius, spd);
   }

   private void shell(EffectContext c, double radius, double h, double base, float alpha, double scroll, float uOffset, float streakScale) {
      if(alpha > 0.003F) {
         Camera cam = c.cam;

         for(int s = 0; s < 48; ++s) {
            double a0 = 6.283185307179586D * (double)s / 48.0D;
            double a1 = 6.283185307179586D * (double)(s + 1) / 48.0D;
            double c0 = Math.cos(a0);
            double s0 = Math.sin(a0);
            double c1 = Math.cos(a1);
            double s1 = Math.sin(a1);
            double x0 = c.x + c0 * radius;
            double z0 = c.z + s0 * radius;
            double x1 = c.x + c1 * radius;
            double z1 = c.z + s1 * radius;
            float edge0 = edge(cam, x0, z0, c0, s0);
            float edge1 = edge(cam, x1, z1, c1, s1);
            float u0 = (float)s / 48.0F * 3.0F + uOffset;
            float u1 = (float)(s + 1) / 48.0F * 3.0F + uOffset;

            for(int l = 0; l < 6; ++l) {
               float f0 = (float)l / 6.0F;
               float f1 = (float)(l + 1) / 6.0F;
               double y0 = base + h * (double)f0;
               double y1 = base + h * (double)f1;
               float fade0 = (float)Math.pow((double)(1.0F - f0), 1.7D);
               float fade1 = (float)Math.pow((double)(1.0F - f1), 1.7D);
               int col0 = this.color(c, f0);
               int col1 = this.color(c, f1);
               float v0 = (float)((double)f0 * 1.2D * (double)streakScale - scroll);
               float v1 = (float)((double)f1 * 1.2D * (double)streakScale - scroll);
               Vfx.quadUV(Textures.STREAKS, x0, y0, z0, (double)u0, (double)v0, alpha(col0, alpha * fade0 * edge0), x1, y0, z1, (double)u1, (double)v0, alpha(col0, alpha * fade0 * edge1), x1, y1, z1, (double)u1, (double)v1, alpha(col1, alpha * fade1 * edge1), x0, y1, z0, (double)u0, (double)v1, alpha(col1, alpha * fade1 * edge0));
            }
         }
      }

   }

   private static float edge(Camera cam, double x, double z, double nx, double nz) {
      double vx = cam.posX - x;
      double vz = cam.posZ - z;
      double len = Math.sqrt(vx * vx + vz * vz);
      if(len < 1.0E-9D) {
         return 1.0F;
      } else {
         float facing = (float)Math.abs((nx * vx + nz * vz) / len);
         return 0.3F + 0.7F * (float)Math.pow((double)(1.0F - facing), 1.2D);
      }
   }

   private void emitEmbers(EffectContext c, float sz, double radius, float spd) {
      float rate = this.embers.getFloat();
      if(rate > 0.0F && c.spawnParticles) {
         AuraState s = c.state;
         s.emitAccumulator += c.dt * rate;
         Random r = s.random;

         while(s.emitAccumulator >= 1.0F) {
            --s.emitAccumulator;
            double angle = r.nextDouble() * 6.283185307179586D;
            double dist = Math.sqrt(r.nextDouble()) * radius * 0.9D;
            float size = (0.045F + 0.03F * r.nextFloat()) * sz;
            float tangential = (r.nextFloat() - 0.5F) * 0.3F;
            c.particles.spawn(1, c.worldX + Math.cos(angle) * dist, c.worldGroundY + 0.05D + r.nextDouble() * 0.3D, c.worldZ + Math.sin(angle) * dist, (float)(-Math.sin(angle)) * tangential, (0.7F + 0.6F * r.nextFloat()) * spd, (float)Math.cos(angle) * tangential, 1.1F + 0.7F * r.nextFloat(), size, size * 0.3F, this.color(c, r.nextFloat() * 0.6F), -0.1F, 0.25F, 0.05F, r.nextFloat());
         }
      }

   }
}
