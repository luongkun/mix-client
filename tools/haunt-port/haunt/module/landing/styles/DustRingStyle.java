package mix.client.mixui.integration.haunt.module.landing.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class DustRingStyle extends LandingStyleBase {
   public final BooleanSetting blockColor = (BooleanSetting)this.setting(new BooleanSetting("dust.blockColor", "Use Block Color", "Tints the dust with the color of the block you landed on.", false));
   public final SliderSetting puffs = (SliderSetting)this.setting(new SliderSetting("dust.puffs", "Puffs", "How many puffs roll out.", 8.0D, 28.0D, 16.0D, 1.0D));
   private final Rng rng = new Rng();

   public DustRingStyle(LandingImpactModule module) {
      super(module, LandingStyle.DUST_RING, 1.0F, 2.2F, new ColorSetting("dust.color", "Color", "Dust color (gradient: a random mix per puff).", -2504792));
   }

   private int puffColor(LandingFx fx, float t) {
      return this.blockColor.get()?ColorUtil.lerp(fx.land.blockColor, -1, 0.28F):this.color.getColor(t);
   }

   public void start(LandingFx fx) {
      this.rng.seed(fx.seed);
      int n = fx.count(this.puffs.getFloat() * (fx.lod?0.5F:1.0F));
      float spread = (float)Math.sqrt((double)fx.size);

      for(int i = 0; i < n; ++i) {
         double ang = 6.283185307179586D * ((double)i + (double)this.rng.next() * 0.8D) / (double)n;
         double cx = Math.cos(ang);
         double cz = Math.sin(ang);
         float out = this.rng.range(1.7F, 2.8F) * spread;
         float s = this.rng.range(0.4F, 0.55F) * spread;
         double x = fx.x() + cx * 0.3D;
         double z = fx.z() + cz * 0.3D;
         fx.particles.spawn(8, x, fx.y() + 0.14D + (double)this.rng.next() * 0.12D, z, (float)cx * out, this.rng.range(0.15F, 0.45F), (float)cz * out, fx.seconds(this.rng.range(0.75F, 1.0F)), s).end(s * 2.5F).drag(2.8F).gravity(-0.25F).spin(this.rng.next() * 6.0F, this.rng.signed() * 0.8F).alpha(0.68F * fx.opacity).fade(0.05F, 0.35F);
         if(this.blockColor.get()) {
            fx.particles.color(this.puffColor(fx, 0.0F));
         } else {
            fx.particles.color(this.color, this.rng.next());
         }
      }

   }

   public void render(LandingFx fx, FxFrame f) {
      float p = fx.progress();
      float a = fx.alpha() * 0.22F * (1.0F - Easing.smoothstep(0.3F, 0.9F, p));
      if(a > 0.003F) {
         double r = 0.4D + 1.5D * Math.sqrt((double)fx.size) * (double)Easing.outCubic(Easing.clamp01(p / 0.7F));
         RenderState.translucent(false);
         RenderState.decal(true);
         Ground.band(f, fx.ground, Textures.WAVE, fx.x(), fx.z(), Math.max(0.0D, r - 0.5D), r, 0.0D, 6.283185307179586D, 64, (ColorSetting)null, this.puffColor(fx, 0.5F), a, 0.0F);
         RenderState.decal(false);
         RenderState.additive();
      }

   }
}
