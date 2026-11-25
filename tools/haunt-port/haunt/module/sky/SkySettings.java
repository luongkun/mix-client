package mix.client.mixui.integration.haunt.module.sky;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import mix.client.mixui.integration.haunt.module.sky.SkyModule;
import mix.client.mixui.integration.haunt.module.sky.SkyStyle;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SettingStyle;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class SkySettings {
   static final double SMOOTH_SECONDS = 1.5D;
   public final SkyStyle sky;
   public final SliderSetting speed;
   public final SliderSetting scale;
   public final SliderSetting rotation;
   public final SliderSetting parallax;
   public final SliderSetting fadeIn;
   public final ModeSetting<SkyModule.ColorSource> colorSource;
   public final ColorSetting color;
   public final SliderSetting brightness;
   public final SliderSetting saturation;
   public final SliderSetting contrast;
   public final BooleanSetting matchFog;
   public final SliderSetting fogBlend;
   public final ModeSetting<SkyModule.Show> show;
   public final SliderSetting dayBrightness;
   public final BooleanSetting weather;
   public final BooleanSetting reactMusic;
   public final SliderSetting musicAmount;
   public final ModeSetting<SkyModule.Element> sunMoon;
   public final ModeSetting<SkyModule.Clouds> clouds;
   public final BooleanSetting stars;
   public final SliderSetting starDensity;
   public final SliderSetting twinkle;
   public final ColorSetting starColor;
   public final SliderSetting shootingStars;
   public final BooleanSetting overworld;
   public final BooleanSetting nether;
   public final BooleanSetting end;
   public final ModeSetting<SkyModule.Blend> blend;
   public final SliderSetting opacity;
   public final ModeSetting<SkyModule.Quality> quality;
   public final BooleanSetting offWithShaders;
   public final ModeSetting<SkyModule.OptiFineSkies> optifineSkies;
   final List<Setting> common = new ArrayList();
   private Setting[] own = new Setting[0];
   private final Map<Setting, JsonElement> defaults = new IdentityHashMap();
   private boolean modifiedKnown;
   private boolean modified;

   SkySettings(SkyStyle sky, SettingStyle owner, Supplier<String> noMusic) {
      this.sky = sky;
      BooleanSupplier custom = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return SkySettings.this.colorSource.get() == SkyModule.ColorSource.CUSTOM;
         }
      };
      BooleanSupplier starsOn = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return SkySettings.this.stars.get();
         }
      };
      this.speed = (SliderSetting)this.add((new SliderSetting(this.id("speed"), "Animation Speed", "How fast the sky moves. 0 freezes it.", 0.0D, 3.0D, 1.0D, 0.05D)).unit("x").group("Motion"), owner);
      this.scale = (SliderSetting)this.add((new SliderSetting(this.id("scale"), "Pattern Size", "Size of the shapes in the sky.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x").group("Motion"), owner);
      this.rotation = (SliderSetting)this.add((new SliderSetting(this.id("rotation"), "Rotation", "Slowly turns the whole sky; below 0 it turns the other way.", -6.0D, 6.0D, 0.0D, 0.1D)).unit("°/s").group("Motion"), owner);
      this.parallax = (SliderSetting)this.add((new SliderSetting(this.id("parallax"), "Parallax", "The sky shifts a little as you move around, for depth.", 0.0D, 1.0D, 0.3D, 0.05D)).group("Motion"), owner);
      this.fadeIn = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(this.id("fadeIn"), "Fade-In", "How long this sky takes to crossfade in when you switch to it. 0 switches at once.", 0.0D, 5.0D, 1.5D, 0.1D)).unit("s").group("Motion")).keywords(new String[]{"crossfade", "fade", "transition", "smooth switching"}), owner);
      this.colorSource = (ModeSetting)this.add((new ModeSetting(this.id("colorSource"), "Colors", "Where the sky\'s colors come from.", SkyModule.ColorSource.class, SkyModule.ColorSource.STYLE)).group("Color"), owner);
      this.color = (ColorSetting)this.add(((ColorSetting)(new ColorSetting(this.id("color"), "Sky Color", "Your colors for the sky: one, a gradient of two, or chroma.", ColorSetting.Mode.GRADIENT, -50291, -9806593, 1.0D)).group("Color")).visibleWhen(custom), owner);
      this.brightness = (SliderSetting)this.add((new SliderSetting(this.id("brightness"), "Brightness", "How bright the sky is.", 0.2D, 1.5D, 1.0D, 0.05D)).group("Color"), owner);
      this.saturation = (SliderSetting)this.add((new SliderSetting(this.id("saturation"), "Saturation", "How vivid its colors are. 0 is black and white.", 0.0D, 1.5D, 1.0D, 0.05D)).group("Color"), owner);
      this.contrast = (SliderSetting)this.add((new SliderSetting(this.id("contrast"), "Contrast", "Deeper darks and brighter lights (below 1: softer).", 0.5D, 1.5D, 1.0D, 0.05D)).group("Color"), owner);
      this.matchFog = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(this.id("matchFog"), "Match Fog", "The fog takes the sky\'s horizon color, so the world melts into the sky (Ambience\'s Atmosphere tint wins where it is on).", true)).group("Color")).keywords(new String[]{"fog", "horizon", "tint"}), owner);
      this.fogBlend = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(this.id("fogBlend"), "Fog Blend", "How far the fog\'s color goes towards the sky\'s.", 0.0D, 1.0D, 0.6D, 0.05D)).group("Color")).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return SkySettings.this.matchFog.get();
         }
      }), owner);
      this.show = (ModeSetting)this.add(((ModeSetting)(new ModeSetting(this.id("show"), "Show", "When this sky shows.", SkyModule.Show.class, SkyModule.Show.ALWAYS)).group("When")).keywords(new String[]{"night", "day", "dusk"}), owner);
      this.dayBrightness = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(this.id("dayBrightness"), "Day Brightness", "How bright this sky is in the daytime (1: as at night).", 0.2D, 1.5D, 1.0D, 0.05D)).group("When")).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return SkySettings.this.show.get() == SkyModule.Show.ALWAYS;
         }
      }), owner);
      this.reactMusic = (BooleanSetting)this.add(((BooleanSetting)((BooleanSetting)(new BooleanSetting(this.id("reactMusic"), "React to Music", "The sky pulses brighter and moves faster on the bass of the music you play (heard by the WaterMark\'s Audio module; game sounds and ignored apps never count).", false)).group("Music")).keywords(new String[]{"audio", "beat", "bass", "pulse", "visualizer"})).unavailableWhen(noMusic), owner);
      this.musicAmount = (SliderSetting)this.add(((SliderSetting)((SliderSetting)(new SliderSetting(this.id("musicAmount"), "Music Amount", "How strongly it reacts.", 0.0D, 1.0D, 0.6D, 0.05D)).group("Music")).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return SkySettings.this.reactMusic.get();
         }
      })).unavailableWhen(noMusic), owner);
      this.sunMoon = (ModeSetting)this.add((new ModeSetting(this.id("sunMoon"), "Sun & Moon", "Minecraft\'s sun and moon, none, or the sky\'s own.", SkyModule.Element.class, SkyModule.Element.STYLE)).group("Sky Elements"), owner);
      this.clouds = (ModeSetting)this.add(((ModeSetting)(new ModeSetting(this.id("clouds"), "Clouds", "Minecraft\'s clouds (as they are, or tinted to the sky), none, or the sky\'s own.", SkyModule.Clouds.class, SkyModule.Clouds.STYLE)).group("Sky Elements")).keywords(new String[]{"vanilla clouds"}), owner);
      this.stars = (BooleanSetting)this.add((new BooleanSetting(this.id("stars"), "Stars", "Stars wherever the sky is dark.", true)).group("Sky Elements"), owner);
      this.starDensity = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(this.id("starDensity"), "Star Density", "How many stars there are.", 0.05D, 1.0D, 0.5D, 0.05D)).group("Sky Elements")).visibleWhen(starsOn), owner);
      this.twinkle = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(this.id("twinkle"), "Twinkle", "How much the stars flicker.", 0.0D, 1.0D, 0.5D, 0.05D)).group("Sky Elements")).visibleWhen(starsOn), owner);
      this.starColor = (ColorSetting)this.add(((ColorSetting)(new ColorSetting(this.id("starColor"), "Star Color", "Color of the stars and shooting stars.", ColorSetting.Mode.STATIC, -1, -2562817, 1.0D)).skipSetAll().group("Sky Elements")).visibleWhen(starsOn), owner);
      this.shootingStars = (SliderSetting)this.add((new SliderSetting(this.id("shootingStars"), "Shooting Stars", "How often they fall; now and then there is a meteor shower. 0 = never.", 0.0D, 1.0D, 0.3D, 0.05D)).group("Sky Elements"), owner);
      this.weather = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(this.id("weather"), "Dim During Rain", "Rain greys and dims the sky; lightning lights it up.", true)).group("Sky Elements")).keywords(new String[]{"weather", "react to weather", "lightning", "storm"}), owner);
      this.overworld = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(this.id("overworld"), "Overworld", "Show it in the Overworld (and other normal worlds).", true)).group("Where")).keywords(new String[]{"dimension", "dimensions"}), owner);
      this.nether = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(this.id("nether"), "Nether", "Show it in the Nether, where there is normally no sky.", false)).group("Where")).keywords(new String[]{"dimension", "dimensions"}), owner);
      this.end = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(this.id("end"), "The End", "Show it in the End instead of its dark sky.", false)).group("Where")).keywords(new String[]{"dimension", "dimensions"}), owner);
      this.blend = (ModeSetting)this.add((new ModeSetting(this.id("blend"), "Blend", "Replace the normal sky, or lay this one over it.", SkyModule.Blend.class, SkyModule.Blend.REPLACE)).group("Where"), owner);
      this.opacity = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(this.id("opacity"), "Opacity", "How much of this sky shows over the normal one.", 0.1D, 1.0D, 0.6D, 0.05D)).group("Where")).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return SkySettings.this.blend.get() == SkyModule.Blend.OVERLAY;
         }
      }), owner);
      this.quality = (ModeSetting)this.add((new ModeSetting(this.id("quality"), "Quality", "Lower is faster: fewer details, and Low and Medium draw at a lower resolution. Soft skies (clouds, plasma, cells) draw at half resolution below Ultra, smoothly scaled.", SkyModule.Quality.class, SkyModule.Quality.HIGH)).group("Performance"), owner);
      this.offWithShaders = (BooleanSetting)this.add((new BooleanSetting(this.id("offWithShaders"), "Off With Shader Packs", "Steps aside while an OptiFine shader pack is on (it draws its own sky). The skies with their own options always do.", true)).group("Performance"), owner);
      this.optifineSkies = (ModeSetting)this.add((new ModeSetting(this.id("optifineSkies"), "OptiFine Skies", "When a resource pack has its own OptiFine sky: draw this one anyway, or show the pack\'s.", SkyModule.OptiFineSkies.class, SkyModule.OptiFineSkies.OVERRIDE)).group("Performance"), owner);
   }

   private String id(String name) {
      return this.sky.name().toLowerCase(Locale.ROOT) + "." + name;
   }

   private <S extends Setting> S add(S setting, SettingStyle owner) {
      setting.ownedBy(owner);
      this.common.add(setting);
      return (S)setting;
   }

   void setOwn(Setting[] settings) {
      this.own = (Setting[])((Setting[])settings.clone());
   }

   List<Setting> all() {
      ArrayList<Setting> out = new ArrayList(this.common);
      out.addAll(Arrays.asList(this.own));
      return out;
   }

   void recordDefaults() {
      Runnable changed = new Runnable() {
         public void run() {
            SkySettings.this.modifiedKnown = false;
         }
      };

      for(Setting s : this.all()) {
         this.defaults.put(s, s.toJson());
         s.addListener(changed);
      }

   }

   boolean isModified() {
      if(!this.modifiedKnown) {
         this.modified = false;

         for(Setting s : this.all()) {
            JsonElement d = (JsonElement)this.defaults.get(s);
            if(d != null && !d.equals(s.toJson())) {
               this.modified = true;
               break;
            }
         }

         this.modifiedKnown = true;
      }

      return this.modified;
   }

   void copyFrom(SkySettings other) {
      for(int i = 0; i < this.common.size(); ++i) {
         ((Setting)this.common.get(i)).fromJson(((Setting)other.common.get(i)).toJson());
      }

   }

   void reset() {
      for(Setting s : this.all()) {
         s.reset();
      }

   }

   static String sharedId(Setting s) {
      String id = s.getId();
      return id.substring(id.indexOf(46) + 1);
   }

   static boolean hasSharedSettings(JsonObject json, SkySettings first) {
      if(json.has(first.speed.getId())) {
         return false;
      } else {
         for(Setting s : first.common) {
            String old = s == first.fadeIn?"smooth":sharedId(s);
            if(json.has(old)) {
               return true;
            }
         }

         return json.has("whenPerSky");
      }
   }

   void carryOver(JsonObject json, String when) {
      for(Setting s : this.common) {
         if(s == this.fadeIn) {
            JsonElement smooth = json.get("smooth");
            if(smooth != null && smooth.isJsonPrimitive()) {
               this.fadeIn.set(smooth.getAsBoolean()?1.5D:0.0D);
            }
         } else {
            JsonElement value;
            if(s != this.show && s != this.dayBrightness && (value = json.get(sharedId(s))) != null) {
               s.fromJson(value);
            }
         }
      }

      SkyModule.Show showValue = SkyModule.Show.ALWAYS;
      double day = 1.0D;
      String[] parts;
      if(when != null && (parts = when.split(":")).length == 2) {
         showValue = "NIGHT".equals(parts[0])?SkyModule.Show.NIGHT:SkyModule.Show.ALWAYS;

         try {
            day = Double.parseDouble(parts[1]);
         } catch (NumberFormatException var8) {
            day = 1.0D;
         }
      }

      this.show.set(showValue);
      this.dayBrightness.set(day);
   }
}
