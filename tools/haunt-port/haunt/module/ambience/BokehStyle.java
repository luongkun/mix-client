package mix.client.mixui.integration.haunt.module.ambience;

import mix.client.mixui.integration.haunt.module.ambience.AmbienceModule;
import mix.client.mixui.integration.haunt.module.ambience.AmbientEnv;
import mix.client.mixui.integration.haunt.module.ambience.AmbientPool;
import mix.client.mixui.integration.haunt.module.ambience.AmbientStyle;
import mix.client.mixui.integration.haunt.module.ambience.AmbientStyleBase;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

final class BokehStyle extends AmbientStyleBase {
   private static final float NEAR = 5.0F;
   private static final float FAR = 30.0F;
   final ModeSetting<BokehStyle.Shape> shape = (ModeSetting)this.setting(new ModeSetting("bokeh.shape", "Shape", "Round or hexagonal (a hexagon aperture).", BokehStyle.Shape.class, BokehStyle.Shape.CIRCLE));
   final SliderSetting orbSize = (SliderSetting)this.setting((new SliderSetting("bokeh.orbSize", "Orb Size", "How big the orbs are.", 0.5D, 5.0D, 1.8D, 0.1D)).unit(" blocks"));
   final SliderSetting softness = (SliderSetting)this.setting((new SliderSetting("bokeh.softness", "Softness", "From a crisp disc with a bright rim to a very soft glow.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%"));
   private final float[] flow = new float[3];

   BokehStyle(AmbienceModule module) {
      super(module, AmbientStyle.BOKEH, spec());
   }

   private static AmbientStyleBase.Spec spec() {
      AmbientStyleBase.Spec s = new AmbientStyleBase.Spec();
      s.baseDensity = 0.0012F;
      s.opacity = 28.0F;
      s.followWind = 20.0F;
      s.heightMin = 1.0F;
      s.heightMax = 14.0F;
      s.heightLimit = 40.0F;
      s.distance = 40;
      s.cap = 250;
      s.glow = false;
      s.densityHelp = "How many orbs there are.";
      return s;
   }

   double volume(float r) {
      float far = Math.min(30.0F, r);
      float near = Math.min(5.0F, far * 0.5F);
      float band = Math.max(0.5F, this.highest() - this.lowest());
      return 3.141592653589793D * (double)(far * far - near * near) * (double)Math.min(band, 2.0F * far);
   }

   protected boolean spawn(AmbientEnv env, int i, float r, boolean fresh) {
      float far = Math.min(30.0F, r);
      if(!this.randomPlace(env, i, far, Math.min(5.0F, far * 0.5F))) {
         return false;
      } else {
         AmbientPool p = this.pool;
         p.size[i] = this.rng.range(0.6F, 1.5F);
         double ang = (double)this.rng.next() * 6.283D;
         p.a[i] = (float)Math.cos(ang) * this.rng.range(0.1F, 0.3F);
         p.b[i] = (float)Math.sin(ang) * this.rng.range(0.1F, 0.3F);
         p.c[i] = this.rng.next() * 6.283F;
         return true;
      }
   }

   protected boolean step(AmbientEnv env, int i, float r, float dt) {
      AmbientPool p = this.pool;
      float sp = this.speed.getFloat();
      this.windAt(env, i, this.followWind.getFloat() / 100.0F, this.flow);
      p.x[i] += (double)((p.a[i] + this.flow[0] * 0.3F) * sp * dt);
      p.z[i] += (double)((p.b[i] + this.flow[2] * 0.3F) * sp * dt);
      p.y[i] += (double)(0.08F * (float)Math.sin((double)(p.age[i] * 0.4F + p.c[i])) * sp * dt);
      return true;
   }

   protected boolean wrap(AmbientEnv env, int i, float r) {
      return super.wrap(env, i, Math.min(30.0F, r));
   }

   void draw(AmbientEnv env) {
      float r = Math.min(30.0F, this.radius(env));
      float soft = this.softness.getFloat() / 100.0F;
      boolean hex = this.shape.get() == BokehStyle.Shape.HEXAGON;
      RenderState.additive();
      if(soft < 1.0F) {
         this.layer(env, r, hex?Textures.BOKEH_HEX:Textures.BOKEH_CIRCLE, 1.0F - soft);
      }

      if(soft > 0.0F) {
         this.layer(env, r, hex?Textures.BOKEH_HEX_SOFT:Textures.BOKEH_CIRCLE_SOFT, soft);
      }

   }

   private void layer(AmbientEnv env, float r, int texture, float weight) {
      float s0 = this.orbSize.getFloat() * this.size.getFloat();
      float glowK = this.glowK();

      for(int i = 0; i < this.pool.count; ++i) {
         float s = s0 * this.pool.size[i];
         if(this.place(env, i, s)) {
            float near = Easing.smoothstep(3.0F, 6.0F, this.dist);
            float shimmer = 0.85F + 0.15F * (float)Math.sin(env.time * 0.7D + (double)this.pool.c[i]);
            float a = this.fogged(this.fade(env, i, r)) * near * shimmer * weight * (1.0F + 0.5F * glowK);
            if(a > 0.003F) {
               Vfx.sprite(texture, this.rx, this.ry, this.rz, (double)s, 0.0F, ColorUtil.mulAlpha(this.color.getColor(this.hash(i, 7)), a));
            }
         }
      }

      Vfx.flush();
   }

   static enum Shape implements Displayable {
      CIRCLE("Circle", "Round discs"),
      HEXAGON("Hexagon", "Six-sided, like a camera\'s aperture");

      private final String displayName;
      private final String description;

      private Shape(String displayName, String description) {
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
