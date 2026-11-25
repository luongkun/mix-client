package mix.client.mixui.integration.haunt.module.weapons;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.weapons.FinishSetting;
import mix.client.mixui.integration.haunt.module.weapons.InspectWeightsSetting;
import mix.client.mixui.integration.haunt.module.weapons.PageRow;
import mix.client.mixui.integration.haunt.module.weapons.ViewModelEditorWidget;
import mix.client.mixui.integration.haunt.module.weapons.WeaponChoiceSetting;
import mix.client.mixui.integration.haunt.module.weapons.WeaponDef;
import mix.client.mixui.integration.haunt.module.weapons.WeaponOptions;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SettingStyle;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class WeaponSettings {
   public final WeaponDef def;
   public final String prefix;
   public ModeSetting<WeaponOptions.StyleChoice> style;
   public FinishSetting finish;
   public ModeSetting<WeaponOptions.BladeMaterial> customBlade;
   public ColorSetting paint;
   public ModeSetting<WeaponOptions.HandleMaterial> handleMaterial;
   public ColorSetting handleColor;
   public ColorSetting accent;
   public SliderSetting glow;
   public SliderSetting wear;
   public SliderSetting sizeFirst;
   public SliderSetting sizeThird;
   public final ModeSetting<WeaponOptions.Placement> placement;
   public final SliderSetting offsetX;
   public final SliderSetting offsetY;
   public final SliderSetting offsetZ;
   public final SliderSetting pitch;
   public final SliderSetting yaw;
   public final SliderSetting roll;
   public final SliderSetting size;
   public final SliderSetting fov;
   public final BooleanSetting leftHanded;
   public BooleanSetting weaponMotion;
   public final SliderSetting breathing;
   public final SliderSetting walkBob;
   public final SliderSetting lookSway;
   public final SliderSetting strafeTilt;
   public final SliderSetting jumpLand;
   public final SliderSetting crouch;
   public final SliderSetting sprintPose;
   public final SliderSetting hurt;
   public final BooleanSetting hitReaction;
   public final BooleanSetting fidgets;
   public BooleanSetting vibration;
   public ModeSetting<WeaponOptions.SwingPattern> swingPattern;
   public ModeSetting<WeaponOptions.Trail> trail;
   public ColorSetting trailColor;
   public final InspectWeightsSetting inspects;
   public final ModeSetting<WeaponOptions.InspectOrder> inspectOrder;
   public final WeaponChoiceSetting leftWeapon;
   public BooleanSetting sound;
   public BooleanSetting soundIdle;
   public BooleanSetting soundBite;
   public BooleanSetting soundOthers;
   public SliderSetting soundVolume;
   public BooleanSetting heatShimmer;
   final List<Setting> all = new ArrayList();
   private final Map<Setting, JsonElement> defaults = new IdentityHashMap();
   private boolean modifiedKnown;
   private boolean modified;
   private int version;

   WeaponSettings(final WeaponsModule module, final WeaponDef def, String prefix, SettingStyle owner) {
      final PageRow[] editorRow = new PageRow[1];
      this.def = def;
      this.prefix = prefix;
      boolean vanilla = def.isVanilla();
      String name = def.name;
      if(!vanilla) {
         this.style = (ModeSetting)this.add(((ModeSetting)(new ModeSetting(this.id("style"), "Style", "This weapon\'s own style: the section\'s Weapon Style, or always HD or Pixelated.", WeaponOptions.StyleChoice.class, WeaponOptions.StyleChoice.SECTION)).group("Look")).keywords(new String[]{"pixel", "pixelated", "hd", "style", "voxel"}), owner);
         final FinishSetting f = this.finish = (FinishSetting)this.add(((FinishSetting)(new FinishSetting(this.id("finish"), "Finish", "The blade\'s material and look.", def.finishes, def.finish)).group("Look")).keywords(new String[]{"skin", "material", "finish", "steel", "damascus", "gold", "nebula", "prism", "obsidian", "painted", "blackened"}), owner);
         BooleanSupplier custom = new BooleanSupplier() {
            public boolean getAsBoolean() {
               return f.get() == WeaponOptions.Finish.CUSTOM;
            }
         };
         this.customBlade = (ModeSetting)this.add(((ModeSetting)(new ModeSetting(this.id("customBlade"), "Blade Material", "Custom finish: what the blade is.", WeaponOptions.BladeMaterial.class, bladeFor(def.finish))).style(ModeSetting.Style.CHIPS).group("Look")).visibleWhen(custom), owner);
         this.paint = (ColorSetting)this.add(((ColorSetting)(new ColorSetting(this.id("paint"), "Paint Color", "The Painted finish\'s color (a gradient runs from the guard to the tip).", ColorSetting.Mode.STATIC, -45096, -9806593, 1.0D)).group("Look")).visibleWhen(new BooleanSupplier() {
            public boolean getAsBoolean() {
               return f.get() == WeaponOptions.Finish.PAINTED || f.get() == WeaponOptions.Finish.CUSTOM && WeaponSettings.this.customBlade.get() == WeaponOptions.BladeMaterial.PAINTED;
            }
         }), owner);
         this.handleMaterial = (ModeSetting)this.add(((ModeSetting)(new ModeSetting(this.id("handleMaterial"), "Handle", "What the handle is made of.", WeaponOptions.HandleMaterial.class, WeaponOptions.HandleMaterial.of(def.handleMaterial))).style(ModeSetting.Style.CHIPS).group("Look")).keywords(new String[]{"grip", "wrap", "leather", "wood", "rubber", "cord"}), owner);
         this.handleColor = (ColorSetting)this.add(((ColorSetting)(new ColorSetting(this.id("handleColor"), "Handle Color", "The handle\'s color.", ColorSetting.Mode.STATIC, def.handleColor, def.handleColor, 1.0D)).skipSetAll().group("Look")).keywords(new String[]{"grip color"}), owner);
         this.accent = (ColorSetting)this.add(((ColorSetting)(new ColorSetting(this.id("accent"), "Accent / Energy Color", "Glowing parts (energy, runes, crystals) and accents.", def.accentStatic?ColorSetting.Mode.STATIC:ColorSetting.Mode.GLOBAL, def.accentColor, def.accentColor, 1.0D)).group("Look")).keywords(new String[]{"glow color", "energy", "accent"}), owner);
         this.glow = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(this.id("glow"), "Glow Strength", "How brightly glowing parts glow.", 0.0D, 2.0D, 1.0D, 0.05D)).unit("x").group("Look")).keywords(new String[]{"bloom", "emissive", "brightness"}), owner);
         this.wear = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(this.id("wear"), "Wear", "Scratches, polished edges and dirt in the grooves.", 0.0D, 100.0D, 10.0D, 5.0D)).unit("%").group("Look")).keywords(new String[]{"scratches", "battle worn", "dirt"}), owner);
         if(def.shimmer != null) {
            this.heatShimmer = (BooleanSetting)this.add(((BooleanSetting)((BooleanSetting)(new BooleanSetting(this.id("heatShimmer"), "Heat Shimmer", "HD only: the air above the hot edge wavers (not with a shader pack; Pixelated flickers its edge instead).", true)).group("Look")).keywords(new String[]{"distortion", "haze", "heat", "refraction"})).visibleWhen(new BooleanSupplier() {
               public boolean getAsBoolean() {
                  return !module.pixelOf(def, WeaponOptions.StyleChoice.SECTION);
               }
            }), owner);
         }

         this.sizeFirst = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(this.id("sizeFirst"), "Size (First Person)", "The weapon\'s size in your hand.", 0.6D, 1.4D, 1.0D, 0.05D)).unit("x").group("Size")).keywords(new String[]{"scale"}), owner);
         this.sizeThird = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(this.id("sizeThird"), "Size (Third Person)", "Its size in F5 and in other players\' hands.", 0.6D, 1.4D, 1.0D, 0.05D)).unit("x").group("Size")).keywords(new String[]{"scale"}), owner);
      }

      editorRow[0] = (PageRow)this.add(((PageRow)(new PageRow(this.id("editor"), "View Model Editor", "Move, turn and size it on your real screen.", new Supplier<Widget>() {
         public Widget get() {
            return new ViewModelEditorWidget(module, editorRow[0], def);
         }
      })).group("View Model")).keywords(new String[]{"view model editor", "viewmodel", "split view"}), owner);
      this.placement = (ModeSetting)this.add(((ModeSetting)(new ModeSetting(this.id("placement"), "Placement", "Where " + name + " sits in first person.", WeaponOptions.Placement.class, WeaponOptions.Placement.DEFAULT)).group("View Model")).keywords(new String[]{"view model editor", "position", "viewmodel"}), owner);
      BooleanSupplier customPlacement = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return WeaponSettings.this.placement.get() == WeaponOptions.Placement.CUSTOM;
         }
      };
      this.offsetX = (SliderSetting)this.add(this.slider("offsetX", "X Offset", "Right (+) or left (-).", -0.3D, 0.3D, 0.0D, 0.01D, "", "View Model", customPlacement), owner);
      this.offsetY = (SliderSetting)this.add(this.slider("offsetY", "Y Offset", "Up (+) or down (-).", -0.3D, 0.3D, 0.0D, 0.01D, "", "View Model", customPlacement), owner);
      this.offsetZ = (SliderSetting)this.add(this.slider("offsetZ", "Z Offset", "Closer (+) or further (-).", -0.3D, 0.3D, 0.0D, 0.01D, "", "View Model", customPlacement), owner);
      this.pitch = (SliderSetting)this.add(this.slider("pitch", "Pitch", "Tilts the tip up (+) or down (-).", -45.0D, 45.0D, 0.0D, 1.0D, "°", "View Model", customPlacement), owner);
      this.yaw = (SliderSetting)this.add(this.slider("yaw", "Yaw", "Turns it left (+) or right (-).", -45.0D, 45.0D, 0.0D, 1.0D, "°", "View Model", customPlacement), owner);
      this.roll = (SliderSetting)this.add(this.slider("roll", "Roll", "Rolls it about its length.", -45.0D, 45.0D, 0.0D, 1.0D, "°", "View Model", customPlacement), owner);
      this.size = (SliderSetting)this.add(this.slider("size", "Size", "The whole view model\'s size.", 0.5D, 1.5D, 1.0D, 0.05D, "x", "View Model", customPlacement), owner);
      this.fov = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(this.id("fov"), "View Model FOV", "Its own field of view, apart from the game\'s (sprinting and potions never stretch it).", 50.0D, 90.0D, 70.0D, 1.0D)).unit("°").group("View Model")).keywords(new String[]{"field of view", "fov"}), owner);
      this.leftHanded = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(this.id("leftHanded"), "Left-Handed", "Mirrors everything to the left.", false)).group("View Model")).keywords(new String[]{"left hand", "mirror"}), owner);
      BooleanSupplier motionOn;
      if(vanilla) {
         this.weaponMotion = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(this.id("weaponMotion"), "Weapon Motion", "Breathing, bob, sway, the sprint pose and the rest on top of vanilla\'s hold.", false)).group("Motion")).keywords(new String[]{"sway", "bob", "breathing", "motion"}), owner);
         motionOn = new BooleanSupplier() {
            public boolean getAsBoolean() {
               return WeaponSettings.this.weaponMotion.get();
            }
         };
      } else {
         motionOn = new BooleanSupplier() {
            public boolean getAsBoolean() {
               return true;
            }
         };
      }

      this.breathing = (SliderSetting)this.add(this.slider("breathing", "Idle Breathing", "A slow drift and tilt.", 0.0D, 2.0D, 1.0D, 0.05D, "x", "Motion", motionOn), owner);
      this.walkBob = (SliderSetting)this.add(this.slider("walkBob", "Walk Bob", "A figure-eight in step with your walking.", 0.0D, 2.0D, 1.0D, 0.05D, "x", "Motion", motionOn), owner);
      this.lookSway = (SliderSetting)this.add(this.slider("lookSway", "Look Sway", "Lags behind when you turn, then settles.", 0.0D, 2.0D, 0.6D, 0.05D, "x", "Motion", motionOn), owner);
      this.strafeTilt = (SliderSetting)this.add(this.slider("strafeTilt", "Strafe Tilt", "Rolls toward the way you strafe.", 0.0D, 2.0D, 1.0D, 0.05D, "x", "Motion", motionOn), owner);
      this.jumpLand = (SliderSetting)this.add(this.slider("jumpLand", "Jump & Land", "Dips when you jump, bounces when you land.", 0.0D, 2.0D, 1.0D, 0.05D, "x", "Motion", motionOn), owner);
      this.crouch = (SliderSetting)this.add(this.slider("crouch", "Crouch", "Lower and closer while sneaking.", 0.0D, 2.0D, 1.0D, 0.05D, "x", "Motion", motionOn), owner);
      this.sprintPose = (SliderSetting)this.add(this.slider("sprintPose", "Sprint Pose", "Tilts down and to the side while sprinting.", 0.0D, 2.0D, 1.0D, 0.05D, "x", "Motion", motionOn), owner);
      this.hurt = (SliderSetting)this.add(this.slider("hurt", "Hurt Jolt", "A small jolt when you take damage.", 0.0D, 2.0D, 1.0D, 0.05D, "x", "Motion", motionOn), owner);
      this.hitReaction = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(this.id("hitReaction"), "Hit Reaction", "A small jolt back when your hit connects.", true)).group("Motion")).visibleWhen(motionOn), owner);
      this.fidgets = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(this.id("fidgets"), "Idle Fidgets", "After a while standing still, a small grip adjustment now and then.", true)).group("Motion")).visibleWhen(motionOn), owner);
      if(def.engine) {
         this.vibration = (BooleanSetting)this.add(((BooleanSetting)((BooleanSetting)(new BooleanSetting(this.id("vibration"), "Engine Vibration", "The saw shivers with its running engine: softly at idle, hard at full throttle.", true)).group("Motion")).keywords(new String[]{"shake", "rumble", "engine", "vibrate"})).foundAnywhere(), owner);
      }

      if(!vanilla) {
         this.swingPattern = (ModeSetting)this.add(((ModeSetting)(new ModeSetting(this.id("swingPattern"), "Swing Pattern", "Which swing plays when you attack.", WeaponOptions.SwingPattern.class, WeaponOptions.SwingPattern.ALTERNATE)).group("Swings")).keywords(new String[]{"attack", "swing", "directional"}), owner);
         this.trail = (ModeSetting)this.add(((ModeSetting)(new ModeSetting(this.id("trail"), "Swing Trail", "A ribbon from the tip while swinging.", WeaponOptions.Trail.class, WeaponOptions.Trail.DEFAULT)).group("Swings")).keywords(new String[]{"streak", "ribbon"}), owner);
         this.trailColor = (ColorSetting)this.add(((ColorSetting)(new ColorSetting(this.id("trailColor"), "Trail Color", "The swing trail\'s color.", def.accentColor)).group("Swings")).visibleWhen(new BooleanSupplier() {
            public boolean getAsBoolean() {
               return WeaponSettings.this.trail.get() != WeaponOptions.Trail.OFF;
            }
         }), owner);
      }

      this.inspects = (InspectWeightsSetting)this.add(((InspectWeightsSetting)(new InspectWeightsSetting(this.id("inspects"), "Inspects", "Which inspects play, and how often.", def.inspects)).group("Inspects")).keywords(new String[]{"inspect", "animation", "look over", "flip"}), owner);
      this.inspects.module = module;
      this.inspects.def = def;
      this.inspectOrder = (ModeSetting)this.add((new ModeSetting(this.id("inspectOrder"), "Order", "Which inspect comes next.", WeaponOptions.InspectOrder.class, WeaponOptions.InspectOrder.RANDOM)).group("Inspects"), owner);
      this.leftWeapon = (WeaponChoiceSetting)this.add(((WeaponChoiceSetting)(new WeaponChoiceSetting(this.id("leftWeapon"), "Left-Hand Weapon", "Dual Wield: a mirrored copy, or any other one-handed weapon.", "same")).group("Dual Wield")).keywords(new String[]{"dual wield", "off hand", "offhand"}), owner);
      this.leftWeapon.module = module;
      this.leftWeapon.def = def;
      if(def.engine) {
         this.sound = (BooleanSetting)this.add(((BooleanSetting)((BooleanSetting)((BooleanSetting)(new BooleanSetting(this.id("sound"), "Engine Sound", "The engine: the only weapon that makes a sound.", true)).group("Sound")).keywords(new String[]{"audio", "engine", "noise", "sounds", "chainsaw"})).foundAnywhere()).preview(new Runnable() {
            public void run() {
               module.chainsawSound.preview(WeaponSettings.this);
            }
         }), owner);
         BooleanSupplier soundOn = new BooleanSupplier() {
            public boolean getAsBoolean() {
               return WeaponSettings.this.sound.get();
            }
         };
         this.soundVolume = (SliderSetting)this.add(((SliderSetting)this.slider("soundVolume", "Volume", "How loud it is.", 0.0D, 100.0D, 60.0D, 5.0D, "%", "Sound", soundOn).keywords(new String[]{"chainsaw sound", "loudness"})).foundAnywhere(), owner);
         this.soundIdle = (BooleanSetting)this.add(((BooleanSetting)((BooleanSetting)((BooleanSetting)(new BooleanSetting(this.id("soundIdle"), "Engine Idle", "A low putter while you hold it.", true)).group("Sound")).visibleWhen(soundOn)).keywords(new String[]{"chainsaw sound"})).foundAnywhere(), owner);
         this.soundBite = (BooleanSetting)this.add(((BooleanSetting)((BooleanSetting)((BooleanSetting)(new BooleanSetting(this.id("soundBite"), "Bite", "A short grinding bite when your hit connects.", true)).group("Sound")).visibleWhen(soundOn)).keywords(new String[]{"chainsaw sound", "hit sound"})).foundAnywhere(), owner);
         this.soundOthers = (BooleanSetting)this.add(((BooleanSetting)((BooleanSetting)((BooleanSetting)(new BooleanSetting(this.id("soundOthers"), "Other Players\' Chainsaws", "Hear other players\' chainsaws (needs Other Players on): up to 3, the closest first.", false)).group("Sound")).visibleWhen(soundOn)).keywords(new String[]{"chainsaw sound", "multiplayer"})).foundAnywhere(), owner);
      }

      for(Setting s : this.all) {
         this.defaults.put(s, s.toJson());
         s.addListener(new Runnable() {
            public void run() {
               WeaponSettings.this.modifiedKnown = false;
               WeaponSettings.this.version++;
            }
         });
      }

   }

   int version() {
      return this.version;
   }

   private static WeaponOptions.BladeMaterial bladeFor(String finish) {
      WeaponOptions.Finish f = WeaponOptions.Finish.of(finish);

      for(WeaponOptions.BladeMaterial b : WeaponOptions.BladeMaterial.values()) {
         if(b.finish == f) {
            return b;
         }
      }

      return WeaponOptions.BladeMaterial.STEEL_POLISHED;
   }

   private String id(String name) {
      return this.prefix + "." + name;
   }

   private SliderSetting slider(String name, String label, String description, double min, double max, double def, double step, String unit, String group, BooleanSupplier visible) {
      SliderSetting s = new SliderSetting(this.id(name), label, description, min, max, def, step);
      if(!unit.isEmpty()) {
         s.unit(unit);
      }

      return (SliderSetting)((SliderSetting)s.group(group)).visibleWhen(visible);
   }

   private <S extends Setting> S add(S setting, SettingStyle owner) {
      setting.ownedBy(owner);
      this.all.add(setting);
      return (S)setting;
   }

   boolean isModified() {
      if(!this.modifiedKnown) {
         this.modified = false;

         for(Setting s : this.all) {
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

   List<Setting> placementSettings() {
      ArrayList<Setting> out = new ArrayList();
      out.add(this.placement);
      out.add(this.offsetX);
      out.add(this.offsetY);
      out.add(this.offsetZ);
      out.add(this.pitch);
      out.add(this.yaw);
      out.add(this.roll);
      out.add(this.size);
      out.add(this.fov);
      out.add(this.leftHanded);
      return out;
   }

   void copyPlacementFrom(WeaponSettings other) {
      List<Setting> mine = this.placementSettings();
      List<Setting> theirs = other.placementSettings();

      for(int i = 0; i < mine.size(); ++i) {
         ((Setting)mine.get(i)).fromJson(((Setting)theirs.get(i)).toJson());
      }

   }

   void resetPlacement() {
      for(Setting s : this.placementSettings()) {
         s.reset();
      }

   }

   WeaponOptions.Finish effectiveFinish() {
      if(this.finish == null) {
         return WeaponOptions.Finish.STEEL_POLISHED;
      } else {
         WeaponOptions.Finish f = this.finish.get();
         return f == WeaponOptions.Finish.CUSTOM?((WeaponOptions.BladeMaterial)this.customBlade.get()).finish:f;
      }
   }

   static String prefixOf(WeaponDef def) {
      return def.isImported()?"imported":def.id.toLowerCase(Locale.ROOT);
   }
}
