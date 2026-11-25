package mix.client.mixui.integration.haunt.module.weapons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import mix.client.mixui.integration.haunt.core.Keybinds;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.weapons.PageRow;
import mix.client.mixui.integration.haunt.module.weapons.WeaponOptions;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.module.weapons.WheelSlotsSetting;
import mix.client.mixui.integration.haunt.module.weapons.WheelTryWidget;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.KeyBindSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.client.settings.KeyBinding;

final class WheelSettings {
   static final String GROUP = "Weapon Wheel";
   final KeyBindSetting key = (KeyBindSetting)(new KeyBindSetting("wheel.key", "Weapon Wheel Key", "The key that opens the wheel (in Options › Controls too).", new Supplier<KeyBinding>() {
      public KeyBinding get() {
         return Keybinds.weaponWheel;
      }
   })).group("Weapon Wheel").keywords(new String[]{"keybind", "hotkey", "bind", "key", "wheel", "weapon wheel", "open"});
   final PageRow tryIt;
   final WheelSlotsSetting slots;
   final SliderSetting perPage;
   final ModeSetting<WeaponOptions.WheelMode> mode;
   final BooleanSetting quickSwap;
   final BooleanSetting freezeCamera;
   final BooleanSetting closeOnDamage;
   final ModeSetting<WeaponOptions.WheelTheme> theme;
   final ColorSetting background;
   final SliderSetting backgroundOpacity;
   final ColorSetting slotColor;
   final ColorSetting highlight;
   final BooleanSetting border;
   final BooleanSetting glass;
   final SliderSetting size;
   final SliderSetting hole;
   final SliderSetting gap;
   final ModeSetting<WeaponOptions.WheelIcons> icons;
   final ModeSetting<WeaponOptions.WheelLabels> labels;
   final BooleanSetting blur;
   final SliderSetting blurStrength;
   final List<Setting> all;

   WheelSettings(final WeaponsModule module) {
      this.tryIt = (PageRow)((PageRow)(new PageRow("wheel.try", "Try It", "Opens the wheel here in the menu: move the mouse or scroll to light a weapon, click to pick it, right-click or Esc to close.", new Supplier<Widget>() {
         public Widget get() {
            return new WheelTryWidget(module);
         }
      })).group("Weapon Wheel")).keywords(new String[]{"preview", "test", "try", "weapon wheel"});
      this.slots = (WheelSlotsSetting)(new WheelSlotsSetting(module)).group("Weapon Wheel").keywords(new String[]{"slots", "weapons", "finish", "add", "order", "reorder", "weapon wheel"});
      this.perPage = (SliderSetting)((SliderSetting)(new SliderSetting("wheel.perPage", "Slots Per Page", "With more slots than this, the wheel gets pages (dots under it); scrolling past the last slot turns the page.", 2.0D, 12.0D, 8.0D, 1.0D)).group("Weapon Wheel")).keywords(new String[]{"pages", "page", "slots"});
      this.mode = (ModeSetting)((ModeSetting)(new ModeSetting("wheel.mode", "Opens With", "Hold the key, or press it once.", WeaponOptions.WheelMode.class, WeaponOptions.WheelMode.HOLD)).group("Weapon Wheel")).keywords(new String[]{"hold", "toggle", "press"});
      this.quickSwap = (BooleanSetting)((BooleanSetting)((BooleanSetting)(new BooleanSetting("wheel.quickSwap", "Quick Swap", "A short tap of the key (under 200 ms) goes straight back to your previous weapon, without the wheel.", true)).group("Weapon Wheel")).keywords(new String[]{"tap", "previous", "last weapon", "swap back", "quick switch"})).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return WheelSettings.this.mode.get() == WeaponOptions.WheelMode.HOLD;
         }
      });
      this.freezeCamera = (BooleanSetting)((BooleanSetting)(new BooleanSetting("wheel.freezeCamera", "Freeze Camera While Open", "The mouse moves the wheel\'s selector instead of the camera. Off: the camera keeps turning and only the scroll wheel picks.", true)).group("Weapon Wheel")).keywords(new String[]{"camera", "mouse", "look", "freeze", "selector"});
      this.closeOnDamage = (BooleanSetting)((BooleanSetting)(new BooleanSetting("wheel.closeOnDamage", "Close When Hurt", "Taking damage closes the wheel without changing anything.", false)).group("Weapon Wheel")).keywords(new String[]{"damage", "hurt", "hit"});
      this.theme = (ModeSetting)((ModeSetting)(new ModeSetting("wheel.theme", "Look", "How the wheel looks.", WeaponOptions.WheelTheme.class, WeaponOptions.WheelTheme.MATCH)).group("Weapon Wheel")).keywords(new String[]{"theme", "style", "glass", "minimal", "custom"});
      BooleanSupplier custom = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return WheelSettings.this.theme.get() == WeaponOptions.WheelTheme.CUSTOM;
         }
      };
      this.background = (ColorSetting)((ColorSetting)((ColorSetting)(new ColorSetting("wheel.background", "Background", "The wheel\'s middle and the ring behind its slots.", ColorSetting.Mode.STATIC, -15395300, -14408146, 1.0D)).skipSetAll().group("Weapon Wheel")).keywords(new String[]{"background", "fill"})).visibleWhen(custom);
      this.backgroundOpacity = (SliderSetting)((SliderSetting)((SliderSetting)(new SliderSetting("wheel.backgroundOpacity", "Background Opacity", "How solid the background is.", 0.0D, 100.0D, 72.0D, 1.0D)).unit("%").group("Weapon Wheel")).keywords(new String[]{"transparency", "alpha"})).visibleWhen(custom);
      this.slotColor = (ColorSetting)((ColorSetting)((ColorSetting)(new ColorSetting("wheel.slotColor", "Slots", "Each slot\'s fill.", ColorSetting.Mode.STATIC, -14013128, -12960180, 1.0D)).skipSetAll().group("Weapon Wheel")).keywords(new String[]{"slot color", "fill"})).visibleWhen(custom);
      this.highlight = (ColorSetting)((ColorSetting)((ColorSetting)(new ColorSetting("wheel.highlight", "Highlight", "The lit slot\'s glow and edge.", -45096)).group("Weapon Wheel")).keywords(new String[]{"highlight", "accent", "selected", "glow"})).visibleWhen(custom);
      this.border = (BooleanSetting)((BooleanSetting)((BooleanSetting)(new BooleanSetting("wheel.border", "Border", "A thin edge around the wheel and its slots.", true)).group("Weapon Wheel")).keywords(new String[]{"outline", "edge"})).visibleWhen(custom);
      this.glass = (BooleanSetting)((BooleanSetting)((BooleanSetting)(new BooleanSetting("wheel.glass", "Glass", "Frosted glass: the world shows through it, blurred (the island\'s glass).", false)).group("Weapon Wheel")).keywords(new String[]{"frosted", "blur", "transparent"})).visibleWhen(custom);
      this.size = (SliderSetting)((SliderSetting)(new SliderSetting("wheel.size", "Size", "How tall the wheel is, against the screen\'s height (it shrinks to keep clear of the hotbar and the island).", 20.0D, 60.0D, 36.0D, 1.0D)).unit("%").group("Weapon Wheel")).keywords(new String[]{"scale", "big", "small", "radius"});
      this.hole = (SliderSetting)((SliderSetting)(new SliderSetting("wheel.hole", "Inner Hole", "How big its middle is, against the whole wheel.", 25.0D, 65.0D, 44.0D, 1.0D)).unit("%").group("Weapon Wheel")).keywords(new String[]{"center", "middle", "inner"});
      this.gap = (SliderSetting)((SliderSetting)(new SliderSetting("wheel.gap", "Gap Between Slots", "The space between two slots (pixels at 1080p).", 0.0D, 16.0D, 5.0D, 1.0D)).unit(" px").group("Weapon Wheel")).keywords(new String[]{"spacing", "gap", "space"});
      this.icons = (ModeSetting)((ModeSetting)(new ModeSetting("wheel.icons", "Pictures", "How the wheel shows its weapons.", WeaponOptions.WheelIcons.class, WeaponOptions.WheelIcons.LIVE)).group("Weapon Wheel")).keywords(new String[]{"icons", "3d", "models", "static", "live"});
      this.labels = (ModeSetting)((ModeSetting)(new ModeSetting("wheel.labels", "Names", "Where the weapons\' names show.", WeaponOptions.WheelLabels.class, WeaponOptions.WheelLabels.CENTER)).group("Weapon Wheel")).keywords(new String[]{"labels", "names", "text", "center"});
      this.blur = (BooleanSetting)((BooleanSetting)(new BooleanSetting("wheel.blur", "Blur Behind", "Blurs and dims the world behind the wheel (the island\'s glass blur).", true)).group("Weapon Wheel")).keywords(new String[]{"blur", "dim", "background", "darken"});
      this.blurStrength = (SliderSetting)((SliderSetting)((SliderSetting)(new SliderSetting("wheel.blurStrength", "Blur Strength", "How much the world behind is blurred and dimmed.", 5.0D, 100.0D, 60.0D, 5.0D)).unit("%").group("Weapon Wheel")).keywords(new String[]{"blur", "dim", "strength"})).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return WheelSettings.this.blur.get();
         }
      });
      ArrayList list = new ArrayList();
      Collections.addAll(list, new Setting[]{this.key, this.tryIt, this.slots, this.perPage, this.mode, this.quickSwap, this.freezeCamera, this.closeOnDamage, this.theme, this.background, this.backgroundOpacity, this.slotColor, this.highlight, this.border, this.glass, this.size, this.hole, this.gap, this.icons, this.labels, this.blur, this.blurStrength});
      this.all = Collections.unmodifiableList(list);
   }
}
