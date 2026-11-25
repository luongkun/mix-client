package mix.client.mixui.integration.haunt.module.ambience;

import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.module.ambience.AmbienceModule;
import mix.client.mixui.integration.haunt.module.ambience.AmbientEnv;
import mix.client.mixui.integration.haunt.module.ambience.AmbientStyle;
import mix.client.mixui.integration.haunt.module.ambience.AmbientStyleBase;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Procedural;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.client.Minecraft;

final class AuroraStyle extends AmbientStyleBase {
   private static final int COLUMNS = 56;
   final SliderSetting brightness = (SliderSetting)this.setting((new SliderSetting("aurora.brightness", "Brightness", "How bright the curtains are.", 10.0D, 200.0D, 100.0D, 5.0D)).unit("%"));
   final SliderSetting altitude = (SliderSetting)this.setting((new SliderSetting("aurora.height", "Height", "How high in the sky, and how tall, the curtains are.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("×"));
   final SliderSetting curtains = (SliderSetting)this.setting(new SliderSetting("aurora.curtains", "Curtains", "How many curtains there are.", 1.0D, 6.0D, 3.0D, 1.0D));
   final BooleanSetting overworldOnly = (BooleanSetting)this.setting(new BooleanSetting("aurora.overworldOnly", "Overworld Only", "Only in the overworld\'s sky.", true));
   private final Rng rng2 = new Rng();

   AuroraStyle(AmbienceModule module) {
      super(module, AmbientStyle.AURORA, spec());
   }

   private static AmbientStyleBase.Spec spec() {
      AmbientStyleBase.Spec s = new AmbientStyleBase.Spec();
      s.volume = false;
      s.opacity = 85.0F;
      s.time = AmbientStyleBase.TimeRule.NIGHT;
      s.color = new ColorSetting("aurora.color", "Color", "The curtains\' colors; a gradient runs along each curtain.", ColorSetting.Mode.GRADIENT, -13176165, -6267649, 1.0D);
      return s;
   }

   int fixedCount() {
      return 0;
   }

   protected boolean spawn(AmbientEnv env, int i, float r, boolean fresh) {
      return false;
   }

   protected boolean step(AmbientEnv env, int i, float r, float dt) {
      return false;
   }

   void draw(AmbientEnv env) {
   }

   void drawSky(AmbientEnv env) {
      if(this.presence > 0.0F && (env.preview || env.world != null) && (env.preview || !this.overworldOnly.get() || env.world.provider.getDimensionId() == 0)) {
         float night = this.time.get() == AmbientStyleBase.TimeRule.NIGHT?1.0F - Easing.smoothstep(0.1F, 0.6F, env.daylight):(this.time.get() == AmbientStyleBase.TimeRule.DAY?Easing.smoothstep(0.4F, 0.9F, env.daylight):1.0F);
         float rain = env.preview?1.0F:1.0F - env.world.getRainStrength(env.partialTicks) * 0.85F;
         float a = this.presence * env.globalAlpha * this.opacity.getFloat() / 100.0F * this.brightness.getFloat() / 100.0F * night * rain;
         if(a > 0.003F) {
            float far = env.preview?env.previewFar:Math.max(64.0F, (float)Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16.0F);
            float clip = env.skyPass?far * 2.0F:far * 1.41F;
            double topMax = 1.4D * (double)this.altitude.getFloat();
            double dist = Math.min((double)far * 1.1D, (double)clip * 0.9D / Math.sqrt(1.0D + topMax * topMax));
            double t = env.time * (double)this.speed.getFloat();
            float glowK = this.glowK();
            RenderState.additive();
            int n = this.curtains.getInt();

            for(int k = 0; k < n; ++k) {
               this.curtain(k, dist, t, a, glowK, false);
            }

            Vfx.flush();
            if(glowK > 0.0F) {
               for(int var16 = 0; var16 < n; ++var16) {
                  this.curtain(var16, dist, t, a, glowK, true);
               }

               Vfx.flush();
            }
         }
      }

   }

   private void curtain(int k, double dist, double t, float a, float glowK, boolean edge) {
      this.rng2.seed(k * 7919 + 13, 5);
      int n = this.curtains.getInt();
      double center = -1.5707963267948966D + ((double)k - (double)(n - 1) / 2.0D) * 0.85D + ((double)this.rng2.next() - 0.5D) * 0.4D + t * 0.004D * (double)((double)this.rng2.next() < 0.5D?-1:1);
      double span = Math.toRadians((double)this.rng2.range(95.0F, 150.0F));
      float alt = this.altitude.getFloat();
      double bottom = dist * (0.36D + 0.06D * (double)k + 0.06D * (double)this.rng2.next()) * (double)alt;
      double height = dist * (0.6D + 0.25D * (double)this.rng2.next()) * (double)alt;
      int seed = k * 131 + 7;
      float scroll = (float)(t * 0.012D + (double)this.rng2.next());
      double prevX = 0.0D;
      double prevZ = 0.0D;
      double prevTop = 0.0D;
      double prevBottom = 0.0D;
      int prevC = 0;
      int prevCTop = 0;
      float prevU = 0.0F;

      for(int j = 0; j <= 56; ++j) {
         float s = (float)j / 56.0F;
         double phi = center - span / 2.0D + span * (double)s;
         double wave = dist * (0.07D * (double)Procedural.fbm((float)(phi * 1.7D + t * 0.03D), seed) + 0.03D * Math.sin(phi * 5.0D + t * 0.2D));
         double rad = dist + wave;
         double x = Math.cos(phi) * rad;
         double z = Math.sin(phi) * rad;
         float ripple = 0.55F + 0.45F * (float)Math.sin(phi * 9.0D - t * 0.6D + (double)k);
         float ends = Easing.smoothstep(0.0F, 0.18F, s) * Easing.smoothstep(0.0F, 0.18F, 1.0F - s);
         float lift = (float)((double)Procedural.fbm((float)(phi * 2.3D - t * 0.02D), seed + 5) * height * 0.18D);
         double b = bottom + (double)lift;
         double top = b + height * (0.8D + 0.2D * (double)Procedural.noise((float)(phi * 3.0D + t * 0.05D), seed + 9));
         int c = this.color.getColor(s);
         float edgeA = a * ends * ripple;
         int cBottom = ColorUtil.mulAlpha(c, Math.min(1.0F, edgeA * 0.9F));
         int cTop = ColorUtil.mulAlpha(ColorUtil.lerp(c, -1, 0.1F), 0.0F);
         float u = s * 7.0F + scroll;
         if(j > 0 && !edge) {
            Vfx.quadUV(Textures.STREAKS, prevX, prevBottom, prevZ, (double)prevU, 0.0D, prevC, x, b, z, (double)u, 0.0D, cBottom, x, top, z, (double)u, 1.0D, cTop, prevX, prevTop, prevZ, (double)prevU, 1.0D, prevCTop);
         } else if(j > 0) {
            double eh = height * 0.07D;
            float pa = (float)(prevC >>> 24) / 255.0F;
            int e0 = ColorUtil.mulAlpha(ColorUtil.lerp(prevC | -16777216, -1, 0.25F), pa * 0.85F * glowK);
            int e1 = ColorUtil.mulAlpha(ColorUtil.lerp(c, -1, 0.25F), edgeA * 0.8F * glowK);
            Vfx.quadUV(Textures.WHITE, prevX, prevBottom - eh * 0.25D, prevZ, 0.5D, 0.5D, e0 & 16777215, x, b - eh * 0.25D, z, 0.5D, 0.5D, e1 & 16777215, x, b, z, 0.5D, 0.5D, e1, prevX, prevBottom, prevZ, 0.5D, 0.5D, e0);
            Vfx.quadUV(Textures.WHITE, prevX, prevBottom, prevZ, 0.5D, 0.5D, e0, x, b, z, 0.5D, 0.5D, e1, x, b + eh, z, 0.5D, 0.5D, e1 & 16777215, prevX, prevBottom + eh, prevZ, 0.5D, 0.5D, e0 & 16777215);
         }

         prevX = x;
         prevZ = z;
         prevBottom = b;
         prevTop = top;
         prevC = cBottom;
         prevCTop = cTop;
         prevU = u;
      }

   }
}
