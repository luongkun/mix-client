package mix.client.mixui.integration.haunt.fx.look;

import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.look.NightVisibility;
import mix.client.mixui.integration.haunt.fx.trail.LightCache;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.world.World;

public final class WorldLight {
   public final BooleanSetting match;
   public final SliderSetting minimum;
   public final ModeSetting<NightVisibility> night;
   private final LightCache cache;

   public WorldLight(String what, String darkStyles) {
      this(what + " take the light where the effect is (block light and daylight), so gold doesn\'t glow in a dark cave. Glowing parts stay as bright as ever.", darkStyles, true);
   }

   public WorldLight(String matchDescription, String darkStyles, boolean wholeDescription) {
      this.cache = new LightCache();
      final BooleanSetting m = this.match = (BooleanSetting)((BooleanSetting)(new BooleanSetting("matchLighting", "Match World Lighting", matchDescription, true)).group("Advanced")).keywords(new String[]{"light", "lighting", "dark", "cave", "night", "brightness", "lit"});
      this.minimum = (SliderSetting)((SliderSetting)(new SliderSetting("minBrightness", "Minimum Brightness", "How dark materials, paper and strokes may get in the dark, so they always stay visible.", 0.0D, 100.0D, 25.0D, 5.0D)).unit("%").group("Advanced")).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return m.get();
         }
      });
      this.night = (ModeSetting)((ModeSetting)(new ModeSetting("nightVisibility", "Night Visibility for Dark Styles", "Dark styles (" + darkStyles + ") get a faint light rim so they read in the dark. Auto: only where it\'s dark.", NightVisibility.class, NightVisibility.AUTO)).group("Advanced")).keywords(new String[]{"night", "dark", "halo", "rim", "visible", "visibility"});
   }

   public void frame(World world, float partialTicks) {
      this.cache.frame(world, partialTicks);
   }

   public float level(World world, double x, double y, double z) {
      return world == null?1.0F:levelOf(this.cache.at(world, x, y, z));
   }

   public float light(float level) {
      return light(level, this.match.get(), this.minimum.getFloat() / 100.0F);
   }

   public float night(float level) {
      return night(level, (NightVisibility)this.night.get());
   }

   public float previewNight() {
      return this.night.get() == NightVisibility.ON?1.0F:0.0F;
   }

   public static float levelOf(float cacheLight) {
      return Easing.clamp01((cacheLight - 0.164F) / 0.836F);
   }

   public static float light(float level, boolean match, float minimum) {
      return match?minimum + (1.0F - minimum) * level:1.0F;
   }

   public static float night(float level, NightVisibility mode) {
      switch(mode) {
      case ON:
         return 1.0F;
      case OFF:
         return 0.0F;
      default:
         return 1.0F - Easing.smoothstep(0.15F, 0.42F, level);
      }
   }
}
