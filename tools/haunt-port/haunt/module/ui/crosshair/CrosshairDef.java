package mix.client.mixui.integration.haunt.module.ui.crosshair;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.module.ui.crosshair.CrosshairStyle;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class CrosshairDef {
   public final CrosshairDef.Kind kind;
   private final List<Setting> all = new ArrayList();
   public final ModeSetting<CrosshairStyle> style;
   public final ColorSetting color;
   public final SliderSetting opacity;
   public final SliderSetting size;
   public final SliderSetting thickness;
   public final SliderSetting gap;
   public final ModeSetting<CrosshairDef.SizeMode> sizeMode;
   public final BooleanSetting pixelPerfect;
   public final ModeSetting<CrosshairDef.Blend> blend;
   public final BooleanSetting outline;
   public final SliderSetting outlineThickness;
   public final SliderSetting outlineOpacity;
   public final ColorSetting outlineColor;
   public final BooleanSetting dot;
   public final SliderSetting dotSize;
   public final SliderSetting dotOpacity;
   public final ModeSetting<CrosshairDef.Ends> dotShape;
   public final BooleanSetting glow;
   public final SliderSetting glowStrength;
   public final SliderSetting rotation;
   public final SliderSetting spinSpeed;
   public final SliderSetting pulseAmount;
   public final SliderSetting pulseSpeed;
   public final ModeSetting<CrosshairDef.Spin> spinDirection;
   public final SliderSetting segments;
   public final SliderSetting orbiters;
   public final CrosshairDef.LineSet inner;
   public final CrosshairDef.LineSet outer;
   public final BooleanSetting top;
   public final BooleanSetting bottom;
   public final BooleanSetting left;
   public final BooleanSetting right;
   public final ModeSetting<CrosshairDef.Ends> ends;
   public final BooleanSetting split;
   public final SliderSetting splitDistance;
   public final SliderSetting splitInner;
   public final SliderSetting splitOuter;
   public final SliderSetting splitRatio;

   CrosshairDef(CrosshairDef.Kind kind, String prefix, ModeSetting<CrosshairStyle> style) {
      this.kind = kind;
      boolean aiming = kind == CrosshairDef.Kind.AIMING;
      boolean zoomed = kind == CrosshairDef.Kind.ZOOMED;
      this.style = (ModeSetting)this.add(style, "Style");
      BooleanSupplier basics = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return ((CrosshairStyle)CrosshairDef.this.style.get()).usesBasics();
         }
      };
      BooleanSupplier classic = this.is(CrosshairStyle.CLASSIC);
      BooleanSupplier notVanilla = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CrosshairDef.this.style.get() != CrosshairStyle.VANILLA;
         }
      };
      this.color = (ColorSetting)this.add(((ColorSetting)(new ColorSetting(prefix + "color", "Color", "The crosshair\'s color.", ColorSetting.Mode.STATIC, zoomed?-49088:-1, -9806593, 1.0D)).visibleWhen(notVanilla)).keywords(new String[]{"crosshair color", "colour"}), "Color");
      this.opacity = (SliderSetting)this.add((new SliderSetting(prefix + "opacity", "Opacity", "How solid the crosshair is.", 0.05D, 1.0D, 1.0D, 0.05D)).visibleWhen(notVanilla), "Look");
      this.size = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(prefix + "size", "Size", "How big it is (pixels at 1080p).", 1.0D, 40.0D, !aiming && !zoomed?6.0D:14.0D, 0.5D)).unit(" px").visibleWhen(basics)).keywords(new String[]{"length", "scale"}), "Look");
      this.thickness = (SliderSetting)this.add((new SliderSetting(prefix + "thickness", "Thickness", "How thick its lines are.", 0.5D, 8.0D, zoomed?1.0D:(aiming?1.5D:2.0D), 0.5D)).unit(" px").visibleWhen(basics), "Look");
      this.gap = (SliderSetting)this.add((new SliderSetting(prefix + "gap", "Gap", "The empty space in the middle (where it applies).", -5.0D, 30.0D, !aiming && !zoomed?3.0D:3.0D, 0.5D)).unit(" px").visibleWhen(basics), "Look");
      this.sizeMode = (ModeSetting)this.add(((ModeSetting)(new ModeSetting(prefix + "sizeMode", "Size Mode", "Whether it keeps its pixel size or grows with the screen.", CrosshairDef.SizeMode.class, CrosshairDef.SizeMode.FIXED)).visibleWhen(notVanilla)).keywords(new String[]{"resolution", "scale"}), "Look");
      this.pixelPerfect = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(prefix + "pixelPerfect", "Pixel-Perfect", "Thin lines snap to whole pixels so they stay crisp (off: smooth, anti-aliased).", true)).visibleWhen(notVanilla)).keywords(new String[]{"crisp", "sharp", "antialias"}), "Look");
      this.blend = (ModeSetting)this.add(((ModeSetting)(new ModeSetting(prefix + "blend", "Blend", "How it mixes with what is behind it.", CrosshairDef.Blend.class, CrosshairDef.Blend.NORMAL)).visibleWhen(notVanilla)).keywords(new String[]{"invert", "additive"}), "Look");
      this.outline = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(prefix + "outline", "Outline", "A thin border around it, so it shows on any background.", !zoomed)).visibleWhen(notVanilla)).keywords(new String[]{"border", "stroke"}), "Outline");
      BooleanSupplier outlined = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CrosshairDef.this.outline.get() && CrosshairDef.this.style.get() != CrosshairStyle.VANILLA;
         }
      };
      this.outlineThickness = (SliderSetting)this.add((new SliderSetting(prefix + "outlineThickness", "Outline Thickness", "How thick the border is.", 0.5D, 3.0D, 1.0D, 0.5D)).unit(" px").visibleWhen(outlined), "Outline");
      this.outlineOpacity = (SliderSetting)this.add((new SliderSetting(prefix + "outlineOpacity", "Outline Opacity", "How solid the border is.", 0.05D, 1.0D, 0.6D, 0.05D)).visibleWhen(outlined), "Outline");
      this.outlineColor = (ColorSetting)this.add((new ColorSetting(prefix + "outlineColor", "Outline Color", "The border\'s color.", ColorSetting.Mode.STATIC, -16777216, -16777216, 1.0D)).skipSetAll().visibleWhen(outlined), "Outline");
      this.dot = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(prefix + "dot", "Center Dot", "A dot in the middle.", zoomed)).visibleWhen(notVanilla)).keywords(new String[]{"center dot", "middle"}), "Center Dot");
      BooleanSupplier dotted = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CrosshairDef.this.dot.get() && CrosshairDef.this.style.get() != CrosshairStyle.VANILLA;
         }
      };
      this.dotSize = (SliderSetting)this.add((new SliderSetting(prefix + "dotSize", "Dot Size", "How big the dot is.", 1.0D, 10.0D, zoomed?1.5D:2.0D, 0.5D)).unit(" px").visibleWhen(dotted), "Center Dot");
      this.dotOpacity = (SliderSetting)this.add((new SliderSetting(prefix + "dotOpacity", "Dot Opacity", "How solid the dot is.", 0.05D, 1.0D, 1.0D, 0.05D)).visibleWhen(dotted), "Center Dot");
      this.dotShape = (ModeSetting)this.add((new ModeSetting(prefix + "dotShape", "Dot Shape", "Square or round.", CrosshairDef.Ends.class, CrosshairDef.Ends.ROUNDED)).visibleWhen(dotted), "Center Dot");
      this.glow = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(prefix + "glow", "Glow", "A soft glow around it.", false)).visibleWhen(notVanilla)).keywords(new String[]{"bloom", "neon"}), "Glow");
      this.glowStrength = (SliderSetting)this.add((new SliderSetting(prefix + "glowStrength", "Glow Strength", "How bright the glow is.", 0.05D, 1.0D, 0.5D, 0.05D)).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CrosshairDef.this.glow.get() && CrosshairDef.this.style.get() != CrosshairStyle.VANILLA;
         }
      }), "Glow");
      this.rotation = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(prefix + "rotation", "Rotation", "A fixed angle.", -180.0D, 180.0D, 0.0D, 1.0D)).unit("°").visibleWhen(notVanilla)).keywords(new String[]{"angle", "tilt"}), "Motion");
      this.spinSpeed = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(prefix + "spinSpeed", "Spin Speed", "How fast it turns (0: still).", 0.0D, 720.0D, 0.0D, 5.0D)).unit("°/s").visibleWhen(notVanilla)).keywords(new String[]{"rotate", "spin"}), "Motion");
      this.spinDirection = (ModeSetting)this.add((new ModeSetting(prefix + "spinDirection", "Spin Direction", "Which way it turns.", CrosshairDef.Spin.class, CrosshairDef.Spin.CLOCKWISE)).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CrosshairDef.this.spinSpeed.get() > 0.0D && CrosshairDef.this.style.get() != CrosshairStyle.VANILLA;
         }
      }), "Motion");
      this.pulseAmount = (SliderSetting)this.add(((SliderSetting)(new SliderSetting(prefix + "pulseAmount", "Pulse", "How much its size and opacity breathe (0: steady).", 0.0D, 1.0D, 0.0D, 0.05D)).visibleWhen(notVanilla)).keywords(new String[]{"breathe", "breathing"}), "Motion");
      this.pulseSpeed = (SliderSetting)this.add((new SliderSetting(prefix + "pulseSpeed", "Pulse Speed", "Breaths a second.", 0.2D, 4.0D, 1.0D, 0.1D)).unit(" Hz").visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CrosshairDef.this.pulseAmount.get() > 0.0D && CrosshairDef.this.style.get() != CrosshairStyle.VANILLA;
         }
      }), "Motion");
      this.segments = (SliderSetting)this.add((new SliderSetting(prefix + "segments", "Segments", "How many arcs the spinner has.", 3.0D, 6.0D, 4.0D, 1.0D)).visibleWhen(this.is(CrosshairStyle.SPINNER)), "Look");
      this.orbiters = (SliderSetting)this.add((new SliderSetting(prefix + "orbiters", "Orbiting Dots", "How many dots orbit.", 2.0D, 3.0D, 3.0D, 1.0D)).visibleWhen(this.is(CrosshairStyle.ORBIT)), "Look");
      this.inner = new CrosshairDef.LineSet(this, prefix + "inner", "Inner Lines", true, 1.0D, aiming?8.0D:5.0D, 2.0D, 3.0D, "FPS Builder · Inner Lines");
      this.outer = new CrosshairDef.LineSet(this, prefix + "outer", "Outer Lines", false, 0.5D, 3.0D, 2.0D, 12.0D, "FPS Builder · Outer Lines");

      for(CrosshairDef.LineSet set : new CrosshairDef.LineSet[]{this.inner, this.outer}) {
         for(Setting s : new Setting[]{set.show, set.opacity, set.length, set.linked, set.lengthV, set.thickness, set.offset, set.moveError, set.moveMult, set.clickError, set.clickMult, set.fade}) {
            s.visibleWhen(classic);
         }
      }

      this.top = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(prefix + "top", "Top Line", "The line above the center (off: T-style).", true)).visibleWhen(classic)).keywords(new String[]{"t-style", "t style"}), "FPS Builder · Lines");
      this.bottom = (BooleanSetting)this.add((new BooleanSetting(prefix + "bottom", "Bottom Line", "The line below the center.", true)).visibleWhen(classic), "FPS Builder · Lines");
      this.left = (BooleanSetting)this.add((new BooleanSetting(prefix + "left", "Left Line", "The line left of the center.", true)).visibleWhen(classic), "FPS Builder · Lines");
      this.right = (BooleanSetting)this.add((new BooleanSetting(prefix + "right", "Right Line", "The line right of the center.", true)).visibleWhen(classic), "FPS Builder · Lines");
      this.ends = (ModeSetting)this.add((new ModeSetting(prefix + "ends", "Line Ends", "Square or rounded.", CrosshairDef.Ends.class, CrosshairDef.Ends.SQUARE)).visibleWhen(classic), "FPS Builder · Lines");
      this.split = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting(prefix + "split", "Split", "When you move or click, the outer part of each line breaks away and drifts out (like CS2\'s dynamic crosshair).", false)).visibleWhen(classic)).keywords(new String[]{"dynamic", "cs2"}), "FPS Builder · Split");
      BooleanSupplier splitOn = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CrosshairDef.this.split.get() && CrosshairDef.this.style.get() == CrosshairStyle.CLASSIC;
         }
      };
      this.splitDistance = (SliderSetting)this.add((new SliderSetting(prefix + "splitDistance", "Split Distance", "How far the broken-off parts drift.", 1.0D, 30.0D, 8.0D, 0.5D)).unit(" px").visibleWhen(splitOn), "FPS Builder · Split");
      this.splitInner = (SliderSetting)this.add((new SliderSetting(prefix + "splitInner", "Inner Opacity", "How solid the inner part stays while split.", 0.0D, 1.0D, 1.0D, 0.05D)).visibleWhen(splitOn), "FPS Builder · Split");
      this.splitOuter = (SliderSetting)this.add((new SliderSetting(prefix + "splitOuter", "Outer Opacity", "How solid the broken-off part is.", 0.0D, 1.0D, 0.5D, 0.05D)).visibleWhen(splitOn), "FPS Builder · Split");
      this.splitRatio = (SliderSetting)this.add((new SliderSetting(prefix + "splitRatio", "Split Ratio", "How much of each line breaks away.", 0.1D, 0.9D, 0.35D, 0.05D)).visibleWhen(splitOn), "FPS Builder · Split");
   }

   static CrosshairDef stored(CrosshairDef.Kind kind) {
      return new CrosshairDef(kind, "", new ModeSetting("style", "Style", "What the crosshair looks like.", CrosshairStyle.class, defaultStyle(kind)));
   }

   static CrosshairStyle defaultStyle(CrosshairDef.Kind kind) {
      return kind != CrosshairDef.Kind.AIMING && kind != CrosshairDef.Kind.ZOOMED?CrosshairStyle.CLASSIC:CrosshairStyle.RETICLE;
   }

   private BooleanSupplier is(final CrosshairStyle s) {
      return new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CrosshairDef.this.style.get() == s;
         }
      };
   }

   <S extends Setting> S add(S s, String group) {
      this.all.add(s.group(group));
      return (S)s;
   }

   public List<Setting> all() {
      return Collections.unmodifiableList(this.all);
   }

   public JsonObject toJson() {
      JsonObject o = new JsonObject();

      for(Setting s : this.all) {
         o.add(s.getId(), s.toJson());
      }

      return o;
   }

   public boolean fromJson(JsonObject o) {
      for(Setting s : this.all) {
         JsonElement e = o == null?null:o.get(s.getId());
         if(e != null) {
            s.fromJson(e);
         } else {
            s.reset();
         }
      }

      if(wasImage(o)) {
         this.style.set(CrosshairStyle.CLASSIC);
         return true;
      } else {
         return false;
      }
   }

   static boolean wasImage(JsonObject o) {
      JsonElement st = o == null?null:o.get("style");
      return st != null && st.isJsonPrimitive() && "IMAGE".equalsIgnoreCase(st.getAsString());
   }

   public void reset() {
      for(Setting s : this.all) {
         s.reset();
      }

   }

   public void copyFrom(CrosshairDef from) {
      for(int i = 0; i < this.all.size(); ++i) {
         ((Setting)this.all.get(i)).fromJson(((Setting)from.all.get(i)).toJson());
      }

   }

   public static enum Blend implements Displayable {
      NORMAL("Normal", "Drawn over the world as it is"),
      INVERT("Invert", "Like vanilla: the colors under it inverted, visible on any background"),
      ADDITIVE("Additive", "Adds light: bright and glowing, softer on light backgrounds");

      private final String displayName;
      private final String description;

      private Blend(String displayName, String description) {
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

   public static enum Ends implements Displayable {
      SQUARE("Square", "Crisp square ends"),
      ROUNDED("Rounded", "Soft round ends");

      private final String displayName;
      private final String description;

      private Ends(String displayName, String description) {
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

   public static enum Kind {
      MAIN,
      ITEM,
      AIMING,
      ZOOMED;
   }

   public static final class LineSet {
      public final BooleanSetting show;
      public final SliderSetting opacity;
      public final SliderSetting length;
      public final SliderSetting lengthV;
      public final SliderSetting thickness;
      public final SliderSetting offset;
      public final SliderSetting moveMult;
      public final SliderSetting clickMult;
      public final BooleanSetting linked;
      public final BooleanSetting moveError;
      public final BooleanSetting clickError;
      public final BooleanSetting fade;

      LineSet(CrosshairDef d, String p, String name, boolean show, double opacity, double length, double thickness, double offset, String group) {
         String ofWhat = name.toLowerCase(Locale.ROOT);
         this.show = (BooleanSetting)d.add(new BooleanSetting(p + "Show", "Show " + name, "The " + ofWhat + " set of four lines.", show), group);
         BooleanSupplier on = new BooleanSupplier() {
            public boolean getAsBoolean() {
               return LineSet.this.show.get();
            }
         };
         this.opacity = (SliderSetting)d.add((new SliderSetting(p + "Opacity", "Opacity", "How solid the " + ofWhat + " are.", 0.0D, 1.0D, opacity, 0.05D)).visibleWhen(on), group);
         this.length = (SliderSetting)d.add((new SliderSetting(p + "Length", "Length", "How long each line is (pixels at 1080p; the horizontal lines when not linked).", 0.0D, 30.0D, length, 0.5D)).unit(" px").visibleWhen(on), group);
         this.linked = (BooleanSetting)d.add((new BooleanSetting(p + "Linked", "Link Lengths", "The vertical lines as long as the horizontal ones.", true)).visibleWhen(on), group);
         this.lengthV = (SliderSetting)d.add(((SliderSetting)(new SliderSetting(p + "LengthV", "Vertical Length", "How long the top and bottom lines are.", 0.0D, 30.0D, length, 0.5D)).unit(" px").visibleWhen(on)).visibleWhen(new BooleanSupplier() {
            public boolean getAsBoolean() {
               return !LineSet.this.linked.get();
            }
         }), group);
         this.thickness = (SliderSetting)d.add((new SliderSetting(p + "Thickness", "Thickness", "How thick the lines are.", 0.5D, 10.0D, thickness, 0.5D)).unit(" px").visibleWhen(on), group);
         this.offset = (SliderSetting)d.add(((SliderSetting)(new SliderSetting(p + "Offset", "Offset", "How far from the center the lines start (below 0 they overlap into a solid plus).", -10.0D, 40.0D, offset, 0.5D)).unit(" px").visibleWhen(on)).keywords(new String[]{"gap"}), group);
         this.moveError = (BooleanSetting)d.add(((BooleanSetting)(new BooleanSetting(p + "MoveError", "Movement Error", "Spreads while you move, sprint or jump.", false)).visibleWhen(on)).keywords(new String[]{"dynamic", "spread"}), group);
         this.moveMult = (SliderSetting)d.add(((SliderSetting)(new SliderSetting(p + "MoveMult", "Movement Multiplier", "How far moving spreads it.", 0.1D, 3.0D, 1.0D, 0.1D)).unit("×").visibleWhen(on)).visibleWhen(new BooleanSupplier() {
            public boolean getAsBoolean() {
               return LineSet.this.moveError.get();
            }
         }), group);
         this.clickError = (BooleanSetting)d.add(((BooleanSetting)(new BooleanSetting(p + "ClickError", "Click Error", "Spreads on each attack click and recovers in about 150 ms.", false)).visibleWhen(on)).keywords(new String[]{"firing error", "dynamic", "spread"}), group);
         this.clickMult = (SliderSetting)d.add(((SliderSetting)(new SliderSetting(p + "ClickMult", "Click Multiplier", "How far a click spreads it.", 0.1D, 3.0D, 1.0D, 0.1D)).unit("×").visibleWhen(on)).visibleWhen(new BooleanSupplier() {
            public boolean getAsBoolean() {
               return LineSet.this.clickError.get();
            }
         }), group);
         this.fade = (BooleanSetting)d.add(((BooleanSetting)(new BooleanSetting(p + "Fade", "Fade with Error", "Fades as it spreads.", false)).visibleWhen(on)).visibleWhen(new BooleanSupplier() {
            public boolean getAsBoolean() {
               return LineSet.this.moveError.get() || LineSet.this.clickError.get();
            }
         }), group);
      }
   }

   public static enum SizeMode implements Displayable {
      FIXED("Fixed Pixels", "The same size at every resolution and GUI scale"),
      SCALED("Scale with Resolution", "Bigger on bigger screens (sizes are for 1080p)");

      private final String displayName;
      private final String description;

      private SizeMode(String displayName, String description) {
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

   public static enum Spin implements Displayable {
      CLOCKWISE("Clockwise", "Turns to the right"),
      COUNTER("Counter-clockwise", "Turns to the left");

      private final String displayName;
      private final String description;

      private Spin(String displayName, String description) {
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
