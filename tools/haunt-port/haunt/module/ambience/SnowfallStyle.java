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
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.util.MathHelper;

final class SnowfallStyle extends AmbientStyleBase {
   final ModeSetting<SnowfallStyle.Flakes> flakes = (ModeSetting)this.setting(new ModeSetting("snow.flakes", "Flakes", "Soft round flakes, crisp crystals, or both.", SnowfallStyle.Flakes.class, SnowfallStyle.Flakes.MIXED));
   final SliderSetting fallSpeed = (SliderSetting)this.setting((new SliderSetting("snow.fall", "Fall Speed", "How fast the flakes fall.", 0.2D, 3.0D, 1.0D, 0.05D)).unit("×"));
   final SliderSetting sway = (SliderSetting)this.setting((new SliderSetting("snow.sway", "Sway", "How much they swing from side to side.", 0.0D, 100.0D, 50.0D, 5.0D)).unit("%"));
   final SliderSetting depthOfField = (SliderSetting)this.setting((new SliderSetting("snow.dof", "Depth of Field", "Flakes right in front of the camera get bigger and softly blurred.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%"));
   final BooleanSetting landAndMelt = (BooleanSetting)this.setting(new BooleanSetting("snow.melt", "Land & Melt", "A flake that reaches a block stays on it and melts away.", true));
   private final float[] flow = new float[3];
   private static final float MELT_SECONDS = 1.5F;

   SnowfallStyle(AmbienceModule module) {
      super(module, AmbientStyle.SNOWFALL, spec());
   }

   private static AmbientStyleBase.Spec spec() {
      AmbientStyleBase.Spec s = new AmbientStyleBase.Spec();
      s.baseDensity = 0.028F;
      s.nearBias = 2.5F;
      s.opacity = 90.0F;
      s.glow = false;
      s.followWind = 40.0F;
      s.heightMin = 0.0F;
      s.heightMax = 14.0F;
      s.heightLimit = 60.0F;
      s.cap = 1500;
      s.collides = Boolean.valueOf(true);
      s.light = true;
      s.color = new ColorSetting("snow.color", "Color", "The flakes\' color (white by default).", ColorSetting.Mode.STATIC, -1, -2691841, 1.0D);
      s.densityHelp = "How much snow falls.";
      return s;
   }

   protected boolean spawn(AmbientEnv env, int i, float r, boolean fresh) {
      if(!this.randomPlace(env, i, r, 0.0F)) {
         return false;
      } else {
         AmbientPool p = this.pool;
         if(!fresh && !Double.isNaN(p.ground[i])) {
            double top = this.bandTop(env, i);
            double y = Math.max(p.ground[i] + (double)this.lowest(), top - (double)this.rng.next() * Math.min(2.0D, top - p.ground[i]));
            if(!env.solidAt(p.x[i], y, p.z[i])) {
               p.teleport(i, p.x[i], y, p.z[i]);
            }
         }

         p.a[i] = this.rng.next() * 6.283F;
         p.b[i] = this.rng.signed() * 1.4F;
         p.c[i] = this.rng.next() * 6.283F;
         p.vx[i] = MathHelper.cos(p.c[i] * 3.1F);
         p.vz[i] = MathHelper.sin(p.c[i] * 3.1F);
         SnowfallStyle.Flakes f = (SnowfallStyle.Flakes)this.flakes.get();
         p.d[i] = f == SnowfallStyle.Flakes.SOFT?0.0F:(f == SnowfallStyle.Flakes.CRYSTAL?1.0F:(this.rng.next() < 0.45F?1.0F:0.0F));
         p.e[i] = 0.0F;
         p.size[i] = this.rng.range(0.6F, 1.35F);
         p.life[i] = this.rng.range(0.75F, 1.25F);
         p.age[i] = 0.0F;
         return true;
      }
   }

   protected boolean step(AmbientEnv env, int i, float r, float dt) {
      AmbientPool p = this.pool;
      if(p.e[i] > 0.0F) {
         p.e[i] += dt / 1.5F;
         return p.e[i] < 1.0F;
      } else {
         float sp = this.speed.getFloat();
         this.windAt(env, i, this.followWind.getFloat() / 100.0F, this.flow);
         float fall = 1.15F * this.fallSpeed.getFloat() * p.life[i] * (p.d[i] > 0.5F?0.85F:1.0F);
         float sw = this.sway.getFloat() / 100.0F * 0.9F;
         float swing = sw * MathHelper.sin(p.age[i] * 2.1F * sp + p.c[i]);
         float swayX = p.vx[i] * swing;
         float swayZ = p.vz[i] * swing;
         double nx = p.x[i] + (double)((this.flow[0] * sp + swayX) * dt);
         double ny = p.y[i] + (double)((this.flow[1] * 0.3F - fall) * sp * dt);
         double nz = p.z[i] + (double)((this.flow[2] * sp + swayZ) * dt);
         p.a[i] += p.b[i] * dt * sp;
         double top = env.outdoorsOnly?env.groundAt(nx, nz):Double.NaN;
         boolean hit = !Double.isNaN(top) && ny <= top;
         boolean collide = this.collides.get();
         if(!hit && collide && !env.outdoorsOnly && env.solidAt(nx, ny, nz)) {
            top = Math.floor(ny) + 1.0D;
            if(env.solidAt(nx, top + 0.05D, nz)) {
               return false;
            }

            hit = true;
         }

         if(hit && collide) {
            if(this.landAndMelt.get() && !env.liquidAt(nx, top - 0.5D, nz)) {
               p.x[i] = nx;
               p.y[i] = top + 0.03D;
               p.z[i] = nz;
               p.e[i] = 0.001F;
               return true;
            } else {
               return false;
            }
         } else if(!Double.isNaN(p.ground[i]) && ny < p.ground[i] - 3.0D) {
            return false;
         } else {
            p.x[i] = nx;
            p.y[i] = ny;
            p.z[i] = nz;
            if(!Double.isNaN(top)) {
               p.ground[i] = top;
            }

            return true;
         }
      }
   }

   protected boolean wrap(AmbientEnv env, int i, float r) {
      return this.pool.e[i] > 0.0F || super.wrap(env, i, r);
   }

   void draw(AmbientEnv env) {
      float r = this.radius(env);
      int[] order = this.backToFront(env);
      float base = 0.13F * this.size.getFloat();
      float dof = this.depthOfField.getFloat() / 100.0F;
      float cell = 0.33333334F;
      RenderState.translucent(false);

      for(int k = 0; k < this.pool.count; ++k) {
         int i = order[k];
         float s = base * this.pool.size[i];
         if(this.place(env, i, s * 3.0F)) {
            float a = this.fade(env, i, r);
            float melt = this.pool.e[i];
            if(melt > 0.0F) {
               a *= 1.0F - Easing.smoothstep(0.1F, 1.0F, melt);
               s *= 1.0F - 0.55F * melt;
            }

            if(a > 0.004F && this.fogK < 1.0F) {
               int c = this.fogColor(env, this.lit(env, this.color.getColor(this.hash(i, 3))));
               int cellIndex = this.pool.d[i] > 0.5F?1:0;
               if(dof > 0.0F && this.dist < 2.6F) {
                  float k2 = (1.0F - this.dist / 2.6F) * dof;
                  s *= 1.0F + 2.2F * k2;
                  a *= 1.0F - 0.45F * k2;
                  if(k2 > 0.35F) {
                     cellIndex = 2;
                  }
               }

               float u0 = (float)cellIndex * cell;
               if(env.light > 0.5F && this.dist < 24.0F) {
                  float ub = 2.0F * cell;
                  Vfx.sprite(Textures.SNOW_ATLAS, this.rx, this.ry, this.rz, (double)s * 0.85D, (double)s * 0.85D, 0.0F, ub, 0.0F, ub + cell, 1.0F, ColorUtil.mulAlpha(-14011064, a * 0.16F));
               }

               Vfx.sprite(Textures.SNOW_ATLAS, this.rx, this.ry, this.rz, (double)s * 0.5D, (double)s * 0.5D, this.pool.a[i], u0, 0.0F, u0 + cell, 1.0F, ColorUtil.mulAlpha(c, a));
            }
         }
      }

      Vfx.flush();
      float glowK = this.glowK();
      if(glowK > 0.0F) {
         RenderState.additive();

         for(int i = 0; i < this.pool.count; ++i) {
            float s = base * this.pool.size[i];
            float ax;
            if(this.place(env, i, s * 3.0F) && (ax = this.fogged(this.fade(env, i, r)) * (1.0F - this.pool.e[i])) > 0.004F) {
               Vfx.sprite(Textures.GLOW, this.rx, this.ry, this.rz, (double)(s * 3.2F), 0.0F, ColorUtil.mulAlpha(this.color.getColor(this.hash(i, 3)), ax * 0.22F * glowK));
            }
         }

         Vfx.flush();
      }

   }

   static enum Flakes implements Displayable {
      SOFT("Soft", "Soft round flakes"),
      CRYSTAL("Crystal", "Crisp six-pointed crystals"),
      MIXED("Mixed", "Both");

      private final String displayName;
      private final String description;

      private Flakes(String displayName, String description) {
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
