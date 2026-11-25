package mix.client.mixui.integration.haunt.sound;

import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.NoteSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import mix.client.mixui.integration.haunt.sound.ModSounds;
import mix.client.mixui.integration.haunt.sound.Sfx;
import mix.client.mixui.integration.haunt.sound.SoundTheme;

public final class SoundSettings {
   private static final String[] KEYWORDS = new String[]{"sound", "sounds", "audio", "volume", "mute", "sfx"};
   public final BooleanSetting mute;
   public final SliderSetting volume;
   public final ModeSetting<SoundTheme> theme;
   public final NoteSetting creamyTip;
   public final BooleanSetting gui;
   public final SliderSetting guiVolume;
   public final BooleanSetting sliderTicks;
   public final BooleanSetting noResults;
   public final BooleanSetting hover;
   public final BooleanSetting typing;
   public final BooleanSetting openSound;
   public final SliderSetting openVolume;
   public final BooleanSetting closeSound;
   public final SliderSetting closeVolume;

   public SoundSettings() {
      this.mute = (BooleanSetting)(new BooleanSetting("soundMute", "Mute Mod Sounds", "Silences every sound of this mod, the menu\'s and the island\'s (Minecraft\'s own sounds stay). Also on a key (Options > Controls) and on the island\'s quick settings.", false)).keywords(KEYWORDS);
      this.volume = (SliderSetting)(new SliderSetting("soundVolume", "Volume", "How loud all of the mod\'s sounds are (Minecraft\'s Master volume applies on top).", 0.0D, 100.0D, 60.0D, 1.0D)).unit("%").keywords(KEYWORDS);
      this.theme = (ModeSetting)(new ModeSetting("soundTheme", "Sound Theme", "The same sounds in five voices: Haunt (music-box chimes, low bells and a ghostly breath), Soft (warm plops and chimes), Glass (bright pings and bells), Minimal (tiny dry clicks) or Creamy (deep, soft keystrokes, like creamy mechanical keyboard switches).", SoundTheme.class, SoundTheme.HAUNT)).keywords(KEYWORDS);
      this.creamyTip = new NoteSetting("soundCreamyTip", "Tip: try Typing sounds with this theme.");
      this.gui = (BooleanSetting)(new BooleanSetting("guiSounds", "Menu Sounds", "Sounds for this menu: opening it, pages, switches, sliders, picks, saving and errors.", true)).keywords(KEYWORDS);
      this.guiVolume = (SliderSetting)(new SliderSetting("guiVolume", "Menu Volume", "How loud the menu\'s sounds are.", 0.0D, 100.0D, 100.0D, 1.0D)).unit("%").keywords(KEYWORDS);
      this.sliderTicks = (BooleanSetting)(new BooleanSetting("sliderTicks", "Slider Ticks", "Soft ticks while a slider or color moves, higher as its value rises.", true)).keywords(KEYWORDS);
      this.noResults = (BooleanSetting)(new BooleanSetting("noResultsSound", "No-Results Sound", "A soft low tone when a search stops finding anything.", true)).keywords(KEYWORDS);
      this.hover = (BooleanSetting)(new BooleanSetting("hoverSounds", "Hover Sounds", "A very faint tick when the mouse comes onto something you can click.", false)).keywords(KEYWORDS);
      this.typing = (BooleanSetting)(new BooleanSetting("typingSounds", "Typing Sounds", "A soft tick for each key typed into a text field or the search (Creamy: a keystroke, never the same twice, Space, Enter and Backspace their own, and the key coming back up).", false)).keywords(new String[]{"sound", "sounds", "audio", "keyboard", "keystroke", "typing"});
      this.openSound = (BooleanSetting)(new BooleanSetting("openSound", "Opening Sound", "The sound when the menu opens (played once, never repeated while it stays open).", true)).keywords(KEYWORDS);
      this.openVolume = (SliderSetting)(new SliderSetting("openVolume", "Opening Volume", "How loud the opening sound is.", 0.0D, 100.0D, 85.0D, 1.0D)).unit("%").keywords(KEYWORDS);
      this.closeSound = (BooleanSetting)(new BooleanSetting("closeSound", "Closing Sound", "The sound when the menu closes.", true)).keywords(KEYWORDS);
      this.closeVolume = (SliderSetting)(new SliderSetting("closeVolume", "Closing Volume", "How loud the closing sound is.", 0.0D, 100.0D, 80.0D, 1.0D)).unit("%").keywords(KEYWORDS);
      BooleanSupplier menuOn = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return SoundSettings.this.gui.get();
         }
      };
      this.creamyTip.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return SoundSettings.this.theme.get() == SoundTheme.CREAMY;
         }
      });
      this.guiVolume.visibleWhen(menuOn);
      this.sliderTicks.visibleWhen(menuOn);
      this.noResults.visibleWhen(menuOn);
      this.hover.visibleWhen(menuOn);
      this.typing.visibleWhen(menuOn);
      this.openSound.visibleWhen(menuOn);
      this.closeSound.visibleWhen(menuOn);
      this.openVolume.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return SoundSettings.this.gui.get() && SoundSettings.this.openSound.get();
         }
      });
      this.closeVolume.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return SoundSettings.this.gui.get() && SoundSettings.this.closeSound.get();
         }
      });
      this.openSound.preview(previewOf(Sfx.GUI_OPEN));
      this.openVolume.preview(previewOf(Sfx.GUI_OPEN));
      this.closeSound.preview(previewOf(Sfx.GUI_CLOSE));
      this.closeVolume.preview(previewOf(Sfx.GUI_CLOSE));
      this.volume.preview(previewOf(Sfx.SELECT));
      this.theme.preview(new Runnable() {
         public void run() {
            ModSounds.previewTheme();
         }
      });
      this.gui.preview(previewOf(Sfx.CLICK));
      this.guiVolume.preview(previewOf(Sfx.CLICK));
      this.sliderTicks.preview(previewOf(Sfx.SLIDER_TICK));
      this.noResults.preview(previewOf(Sfx.EMPTY));
      this.hover.preview(previewOf(Sfx.HOVER));
      this.typing.preview(previewOf(Sfx.KEY));
   }

   private static Runnable previewOf(final Sfx sfx) {
      return new Runnable() {
         public void run() {
            ModSounds.previewGui(sfx);
         }
      };
   }

   public Setting[] all() {
      return new Setting[]{this.mute, this.volume, this.theme, this.creamyTip, this.gui, this.guiVolume, this.openSound, this.openVolume, this.closeSound, this.closeVolume, this.sliderTicks, this.noResults, this.hover, this.typing};
   }
}
