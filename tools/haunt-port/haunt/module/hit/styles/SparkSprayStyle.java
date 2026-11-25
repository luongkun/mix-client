package mix.client.mixui.integration.haunt.module.hit.styles;

import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class SparkSprayStyle extends HitStyleBase {
   public final SliderSetting count = (SliderSetting)this.setting(new SliderSetting("sparks.count", "Sparks", "How many sparks fly.", 10.0D, 60.0D, 30.0D, 1.0D));
   public final BooleanSetting bounce = (BooleanSetting)this.setting(new BooleanSetting("sparks.bounce", "Bounce", "Sparks bounce once off the ground.", true));
   private final Rng rng = new Rng();

   public SparkSprayStyle(HitEffectModule module) {
      super(module, HitStyle.SPARK_SPRAY, 0.5F, new ColorSetting("sparks.color", "Color", "Spark color (gradient: a random mix per spark).", -20165));
   }

   public void start(HitFx fx) {
      Basis b = fx.basis;
      this.rng.seed(fx.seed);
      int n = fx.count(this.count.getFloat() * (fx.lod?0.4F:1.0F), true);
      float hot = fx.crit?1.0F:0.3F;

      for(int i = 0; i < n; ++i) {
         double theta = Math.toRadians(50.0D) * Math.sqrt(0.15D + 0.85D * (double)this.rng.next());
         double cosT = Math.cos(theta);
         double sinT = Math.sin(theta);
         double phi = (double)this.rng.next() * 3.141592653589793D * 2.0D;
         double dx = b.fx * cosT + (b.rx * Math.cos(phi) + b.ux * Math.sin(phi)) * sinT;
         double dy = b.fy * cosT + (b.ry * Math.cos(phi) + b.uy * Math.sin(phi)) * sinT;
         double dz = b.fz * cosT + (b.rz * Math.cos(phi) + b.uz * Math.sin(phi)) * sinT;
         float speed = this.rng.range(4.5F, 10.5F) * (float)Math.sqrt((double)fx.size);
         fx.particles.spawn(2, fx.ix(), fx.iy(), fx.iz(), (float)dx * speed, (float)dy * speed + 1.0F, (float)dz * speed, fx.seconds(this.rng.range(0.3F, 0.5F)), this.rng.range(0.016F, 0.028F) * fx.size).gravity(15.0F).drag(1.4F).color(this.color, this.rng.next()).hot(hot).alpha(fx.opacity).fade(0.0F, 0.6F).follow(fx.anchor, 0.5F).ground(fx.hit.groundY, this.bounce.get()?1:0, 0.35F);
      }

   }

   public void render(HitFx fx, FxFrame f) {
      float p = fx.progress();
      float flash = (float)Math.pow((double)(1.0F - Easing.smoothstep(0.0F, 0.4F, p)), 2.0D);
      if(flash > 0.003F) {
         float a = fx.alpha() * flash;
         double x = f.rx(fx.ix() - fx.basis.fx * 0.1D);
         double y = f.ry(fx.iy() - fx.basis.fy * 0.1D);
         double z = f.rz(fx.iz() - fx.basis.fz * 0.1D);
         int c = this.color.getColor(0.5F);
         float s = fx.size * (fx.crit?1.45F:1.0F);
         RenderState.translucent(false);
         Vfx.sprite(Textures.FLARE, x, y, z, 0.8D * (double)s, 0.15F + p * 0.8F, alpha(c, 0.8F * a));
         RenderState.additive();
         Vfx.sprite(Textures.GLOW, x, y, z, 0.9D * (double)s, 0.0F, alpha(c, 0.45F * a));
         Vfx.sprite(Textures.FLARE, x, y, z, 0.55D * (double)s, 0.15F + p * 0.8F, alpha(Neon.tint(c), a));
         if(fx.crit) {
            Vfx.sprite(Textures.SPARK, x, y, z, 0.3D * (double)s, 0.0F, alpha(-1, a));
         }
      }

   }
}
