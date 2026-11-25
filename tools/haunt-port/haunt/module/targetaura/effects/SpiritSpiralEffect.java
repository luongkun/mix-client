package mix.client.mixui.integration.haunt.module.targetaura.effects;

import java.util.Random;
import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.AuraState;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class SpiritSpiralEffect extends AuraEffect {
   private static final int SAMPLES = 56;
   public final SliderSetting trails = (SliderSetting)this.setting(new SliderSetting("spiral.trails", "Trails", "Number of comet trails.", 1.0D, 3.0D, 2.0D, 1.0D));
   public final SliderSetting trailLength = (SliderSetting)this.setting((new SliderSetting("spiral.length", "Trail Length", "Length of each tail, in seconds of travel.", 0.2D, 1.5D, 0.8D, 0.05D)).unit("s"));
   public final BooleanSetting groundCircle = (BooleanSetting)this.setting(new BooleanSetting("spiral.ground", "Ground Circle", "Soft glowing circle under the target.", true));
   public final ModeSetting<SpiritSpiralEffect.Particles> particles = (ModeSetting)this.setting(new ModeSetting("spiral.particles", "Particles", "Icons that float upward and fade.", SpiritSpiralEffect.Particles.class, SpiritSpiralEffect.Particles.HEARTS));
   public final SliderSetting particleRate = (SliderSetting)this.setting((new SliderSetting("spiral.particleRate", "Particle Rate", "Icons per second.", 1.0D, 20.0D, 5.0D, 1.0D)).unit("/s"));

   public SpiritSpiralEffect(TargetAuraModule module) {
      super(module, EffectType.SPIRIT_SPIRAL, "spiral", new ColorSetting("spiral.color", "Color", "Trail color (gradient: head to tail).", ColorSetting.Mode.GRADIENT, -41781, -6591489, 1.0D));
   }

   private static void path(EffectContext c, int j, int n, double tau, double radiusScale, float spd, int idx) {
      double phase = (double)j * 6.283185307179586D / (double)n;
      double angle = tau * 6.283185307179586D * 0.85D * (double)spd + phase;
      double vertical = 0.5D - 0.5D * Math.cos(tau * 6.283185307179586D * 0.22D * (double)spd + phase);
      double r = (0.55D + 0.05D * Math.sin(tau * 3.1D + (double)j * 1.7D)) * radiusScale;
      Vfx.X[idx] = c.x + Math.cos(angle) * r;
      Vfx.Y[idx] = c.y + 0.1D + vertical * ((double)c.height - 0.05D);
      Vfx.Z[idx] = c.z + Math.sin(angle) * r;
   }

   public void render(EffectContext c) {
      float sz = this.size(c);
      float inten = this.intensity(c);
      float spd = this.speed();
      int n = this.trails.getInt();
      float grow = Easing.outCubic(c.intro);
      double tail = Math.min(this.trailLength.get(), c.time * 1.4D + 0.02D);
      double radiusScale = (double)sz * (0.35D + 0.65D * (double)grow);

      for(int j = 0; j < n; ++j) {
         for(int k = 0; k < 56; ++k) {
            path(c, j, n, c.time - tail * (double)k / 55.0D, radiusScale, spd, j * 56 + k);
         }
      }

      if(this.groundCircle.get()) {
         this.drawGround(c, sz, inten, grow);
      }

      for(int var21 = 0; var21 < n; ++var21) {
         for(int k = 0; k < 56; ++k) {
            float f = (float)k / 55.0F;
            float remain = 1.0F - f;
            Vfx.W[var21 * 56 + k] = 0.18F * sz * (float)Math.pow((double)remain, 0.7D);
            Vfx.C[var21 * 56 + k] = alpha(this.color(c, f), inten * 0.55F * (float)Math.pow((double)remain, 1.5D));
         }

         Vfx.ribbon(Textures.LINE_GLOW, var21 * 56, 56, false);
      }

      for(int var22 = 0; var22 < n; ++var22) {
         for(int k = 0; k < 56; ++k) {
            float f = (float)k / 55.0F;
            float remain = 1.0F - f;
            Vfx.W[var22 * 56 + k] = 0.05F * sz * (float)Math.pow((double)remain, 0.9D);
            Vfx.C[var22 * 56 + k] = core(this.color(c, f), inten * remain * remain);
         }

         Vfx.ribbon(Textures.LINE_CORE, var22 * 56, 56, false);
      }

      for(int var23 = 0; var23 < n; ++var23) {
         int i = var23 * 56;
         Vfx.sprite(Textures.GLOW, Vfx.X[i], Vfx.Y[i], Vfx.Z[i], (double)(0.6F * sz), 0.0F, alpha(this.color(c, 0.0F), 0.65F * inten));
      }

      for(int var24 = 0; var24 < n; ++var24) {
         int i = var24 * 56;
         Vfx.sprite(Textures.SPARK, Vfx.X[i], Vfx.Y[i], Vfx.Z[i], (double)(0.22F * sz), 0.0F, core(this.color(c, 0.0F), inten));
      }

      this.emitParticles(c, sz);
   }

   private void drawGround(EffectContext c, float sz, float inten, float grow) {
      float a = inten * c.groundAlpha;
      if(a > 0.003F) {
         float pulse = 0.85F + 0.15F * (float)Math.sin(c.time * 2.2D);
         double y = c.groundY + 0.02D;
         RenderState.decal(true);
         Vfx.flatSprite(Textures.GROUND_GLOW, c.x, y, c.z, 0.85D * (double)sz * (double)grow, c.time * 0.4D, alpha(this.color(c, 0.5F), 0.55F * a * pulse));
         Vfx.flatRing(Textures.LINE_GLOW, c.x, y, c.z, 0.7D * (double)sz * (double)grow, 0.16D * (double)sz, 96, alpha(this.color(c, 0.3F), 0.35F * a));
         Vfx.flatRing(Textures.LINE_CORE, c.x, y, c.z, 0.7D * (double)sz * (double)grow, 0.03D * (double)sz, 96, core(this.color(c, 0.3F), 0.8F * a * pulse));
         RenderState.decal(false);
      }

   }

   private void emitParticles(EffectContext c, float sz) {
      SpiritSpiralEffect.Particles kind = (SpiritSpiralEffect.Particles)this.particles.get();
      if(kind != SpiritSpiralEffect.Particles.NONE && c.spawnParticles) {
         AuraState s = c.state;
         s.emitAccumulator += c.dt * this.particleRate.getFloat();
         Random r = s.random;

         while(s.emitAccumulator >= 1.0F) {
            --s.emitAccumulator;
            double angle = r.nextDouble() * 6.283185307179586D;
            double radius = (0.3D + 0.35D * r.nextDouble()) * (double)sz;
            float size = (0.14F + 0.07F * r.nextFloat()) * sz;
            c.particles.spawn(kind == SpiritSpiralEffect.Particles.HEARTS?2:3, c.worldX + Math.cos(angle) * radius, c.worldY + 0.3D + r.nextDouble() * 1.4D, c.worldZ + Math.sin(angle) * radius, (float)Math.cos(angle) * 0.06F, 0.32F + 0.25F * r.nextFloat(), (float)Math.sin(angle) * 0.06F, 1.4F + 0.7F * r.nextFloat(), size, size * 0.75F, this.color(c, r.nextFloat()), 0.0F, 0.35F, 0.07F, r.nextFloat());
         }
      }

   }

   public static enum Particles implements Displayable {
      NONE("None"),
      HEARTS("Hearts"),
      STARS("Stars");

      private final String displayName;

      private Particles(String displayName) {
         this.displayName = displayName;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return "";
      }
   }
}
