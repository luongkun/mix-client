package mix.client.mixui.integration.haunt.module.sky;

import java.util.Locale;
import mix.client.mixui.integration.haunt.gui.MenuMemory;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.setting.Displayable;

public enum SkyStyle implements Displayable {
   CIRCUIT_MAZE("Circuit Maze", "A maze of tiny glowing circuits", -54314, -14293, SkyStyle.Kind.CRISP, 0.0F, 30.0F),
   KALEIDOSCOPE("Kaleidoscope", "Neon stars blooming out of the sky", -4174849, -42279, SkyStyle.Kind.CRISP, 0.0F, 62.0F),
   THE_WATCHER("The Watcher", "A giant halftone eye that watches you", -15461356, -856348, SkyStyle.Kind.CRISP, 0.0F, 26.0F),
   MEGASTRUCTURE("Megastructure", "Drifting through a vast lit structure", -49736, -1, SkyStyle.Kind.CRISP, 90.0F, 25.0F),
   CODE_RAIN("Code Rain", "Glowing glyphs raining down", -12714130, -3604520, SkyStyle.Kind.CRISP, 0.0F, 22.0F),
   PLASMA_VEINS("Plasma Veins", "A living web of plasma", -13703425, -8758017, SkyStyle.Kind.SOFT, 0.0F, 35.0F),
   NEON_FRACTURE("Neon Fracture", "Lightning cracks webbing the sky", -45128, -1, SkyStyle.Kind.CRISP, 0.0F, 16.0F),
   VIOLET_STORM("Violet Storm", "Bright clouds churning like liquid", -2867969, -1, SkyStyle.Kind.SOFT, 90.0F, 22.0F),
   SKY_BEAMS("Sky Beams", "Searchlights sweeping up from the horizon", -5878273, -38187, SkyStyle.Kind.SOFT, 0.0F, 20.0F),
   AMETHYST_CELLS("Amethyst Cells", "Glowing cell walls, slowly shifting", -6464257, -2047233, SkyStyle.Kind.SOFT, 0.0F, 35.0F),
   VOID_RIFT("Void Rift", "A black rift in boiling violet energy", -10805295, -2080517, SkyStyle.Kind.CRISP, 0.0F, 40.0F),
   FROZEN_TIDE("Frozen Tide", "A wave of frost frozen mid-crash", -8402689, -853249, SkyStyle.Kind.CRISP, 0.0F, 32.0F),
   FIREFALL("Firefall", "Fire raining from a smoldering sky", -5038066, -34278, SkyStyle.Kind.CRISP, 0.0F, 18.0F),
   GILDED_STORM("Gilded Storm", "Golden light breaking through a storm", -3566037, -3656, SkyStyle.Kind.CRISP, 0.0F, 16.0F),
   MOLTEN_MARBLE("Molten Marble", "Red liquid marble flowing in waves", -60396, -7733248),
   CHROME_INK("Chrome Ink", "Silver liquid curling through black", -854276, -10720382),
   CRIMSON_VEINS("Crimson Veins", "Glowing threads of red liquid", -56284, -10747896),
   NEON_LOTUS("Neon Lotus", "A bloom of neon light on the horizon", -4043777, -12755969),
   SILK("Silk", "Glossy silk folding into the horizon", -6140427, -8787894),
   ACRYLIC_POUR("Acrylic Pour", "Pearl cells and pink lace on black", -2079362, -792084),
   LIQUID_VORTEX("Liquid Vortex", "Glossy bands twisting overhead", -44316, -9148673),
   AURORA_NIGHT("Aurora Night", "Northern lights over a starry night", -13041764, -5222913),
   NEBULA("Nebula", "Glowing gas clouds of deep space", -2999156, -13987888),
   BLACK_HOLE("Black Hole", "A black hole bending the light", -26052, -5699),
   SYNTHWAVE("Synthwave", "A neon sunset over a racing grid", -50004, -14494721),
   GOLDEN_HOUR("Golden Hour", "A sunset that never ends", -30147, -2072433),
   ANIME_SKY("Anime Sky", "Vivid blue with cel-shaded clouds", -12743681, -5916438),
   STORM_FRONT("Storm Front", "Churning clouds and lightning", -12298118, -3483393),
   BLOOD_MOON("Blood Moon", "A giant red moon in crimson haze", -5238758, -46546),
   ECLIPSE("Eclipse", "A black sun with a blazing corona", -6472, -14998718),
   SEA_OF_CLOUDS("Sea of Clouds", "High above an ocean of clouds", -11563521, -11352),
   CHERRY_BLOSSOM("Cherry Blossom", "Pink sky with drifting petals", -19503, -3950849),
   SWIRLING_NIGHT("Swirling Night", "A painted night of swirling strokes", -14729319, -864434),
   CHROMA_SKY("Chroma Sky", "Colors flowing through the rainbow", -42406, -10813480),
   VOID("Void", "Almost black, nothing to distract", -7562300, -13752768),
   ORGANIC_LIQUID("Organic Liquid", "Black-and-white living liquid with glowing rims", -855310, -16119284, SkyStyle.Kind.CRISP, 0.0F, 25.0F),
   CHROMATIC_BUBBLES("Chromatic Bubbles", "Black spheres melting together on white", -723985, -15724526, SkyStyle.Kind.CRISP, 0.0F, 20.0F),
   MONOCHROME_HALFTONE("Monochrome Halftone", "Shapes of a thousand dots flowing in the dark", -1513760, -16448250, SkyStyle.Kind.CRISP, 0.0F, 25.0F),
   COSMIC_EYE("Cosmic Eye", "A giant eye inside a turning vortex of light", -1, -15987696, SkyStyle.Kind.CRISP, 0.0F, 30.0F),
   HAUNTED_MOON("Haunted Moon", "HALLOWEEN — Bats, a castle and a huge pale moon", -12965266, -858424, SkyStyle.Kind.CRISP, 20.0F, 22.0F),
   PUMPKIN_MOON("Pumpkin Moon", "HALLOWEEN — A carved pumpkin glowing as the moon", -1545710, -11686, SkyStyle.Kind.CRISP, 0.0F, 22.0F),
   WITCHS_BREW("Witch\'s Brew", "HALLOWEEN — Bubbling green fumes of a cauldron", -10748054, -6275841, SkyStyle.Kind.SOFT, 0.0F, 30.0F),
   GRAVEYARD_FOG("Graveyard Fog", "HALLOWEEN — Headstones and dead trees in rolling fog", -8413016, -2562336, SkyStyle.Kind.CRISP, 30.0F, 8.0F),
   SPIDER_SILK("Spider Silk", "HALLOWEEN — A giant dewy cobweb and its spider", -14005672, -2232331, SkyStyle.Kind.CRISP, 0.0F, 35.0F),
   SPECTRAL_STORM("Spectral Storm", "HALLOWEEN — Black clouds torn by ghost-green bolts", -12779596, -9819137, SkyStyle.Kind.SOFT, 0.0F, 35.0F),
   CURSED_ECLIPSE("Cursed Eclipse", "HALLOWEEN — A black sun ringed by turning runes", -54754, -15597562, SkyStyle.Kind.CRISP, 0.0F, 30.0F),
   RAVEN_DUSK("Raven Dusk", "HALLOWEEN — Ravens wheeling over a blood-orange dusk", -42454, -14020032, SkyStyle.Kind.CRISP, 330.0F, 15.0F),
   SOUL_LANTERNS("Soul Lanterns", "HALLOWEEN — Hundreds of soul lights rising", -10817281, -16116432, SkyStyle.Kind.CRISP, 0.0F, 18.0F),
   SEALED_DOMAIN("Sealed Domain", "ANIME — A domain of seal circles and crimson veins", -2093014, -15071726, SkyStyle.Kind.CRISP, 0.0F, 40.0F),
   SPIRIT_REALM("Spirit Realm", "ANIME — Floating islands under a crescent moon", -4201217, -14932920, SkyStyle.Kind.CRISP, 20.0F, 25.0F),
   SCATTERED_PAGES("Scattered Pages", "ANIME — Pages of a cursed notebook tumbling", -857904, -5238752, SkyStyle.Kind.CRISP, 0.0F, 20.0F),
   CUSTOM_IMAGE("Custom Image", "Your own panorama: sky.png", -1, -3616026);

   private final String displayName;
   private final String description;
   public final int colorA;
   public final int colorB;
   final SkyStyle.Kind kind;
   final float previewYaw;
   final float previewPitch;

   private SkyStyle(String displayName, String description, int colorA, int colorB) {
      this(displayName, description, colorA, colorB, SkyStyle.Kind.CLASSIC, 0.0F, 20.0F);
   }

   private SkyStyle(String displayName, String description, int colorA, int colorB, SkyStyle.Kind kind, float previewYaw, float previewPitch) {
      this.displayName = displayName;
      this.description = description;
      this.colorA = colorA;
      this.colorB = colorB;
      this.kind = kind;
      this.previewYaw = previewYaw;
      this.previewPitch = previewPitch;
   }

   public String displayName() {
      return this.displayName;
   }

   public String description() {
      return this.description;
   }

   public String tag() {
      return this.isDynamic() && !MenuMemory.seen(this.seenKey())?"NEW":null;
   }

   public String seenKey() {
      return "sky." + this.shaderName();
   }

   public String[] keywords() {
      switch(this.ordinal()) {
      case 10:
         return new String[]{"void", "rift", "purple", "violet", "magenta", "portal", "energy", "black", "space"};
      case 11:
         return new String[]{"frost", "ice", "frozen", "wave", "tide", "winter", "cold", "snow", "spires", "blue"};
      case 12:
         return new String[]{"fire", "swords", "blades", "embers", "flames", "burning", "red", "smoke"};
      case 13:
         return new String[]{"gold", "golden", "holy", "rays", "light", "sunbeams", "sigil", "star", "divine", "clouds"};
      case 14:
      case 15:
      case 16:
      case 17:
      case 18:
      case 19:
      case 20:
      case 21:
      case 22:
      case 23:
      case 24:
      case 25:
      case 26:
      case 27:
      case 28:
      case 29:
      case 30:
      case 31:
      case 32:
      case 33:
      case 34:
      default:
         return null;
      case 35:
         return new String[]{"abstract", "liquid", "organic", "black", "white", "monochrome", "flow"};
      case 36:
         return new String[]{"abstract", "bubbles", "spheres", "white", "chromatic", "liquid", "minimal"};
      case 37:
         return new String[]{"abstract", "halftone", "dots", "black", "grey", "monochrome", "print"};
      case 38:
         return new String[]{"abstract", "eye", "cosmic", "vortex", "space", "black", "white"};
      case 39:
         return new String[]{"halloween", "moon", "bats", "castle", "night", "haunted"};
      case 40:
         return new String[]{"halloween", "pumpkin", "jack o lantern", "moon", "orange", "embers"};
      case 41:
         return new String[]{"halloween", "witch", "cauldron", "potion", "green", "bubbles", "toxic"};
      case 42:
         return new String[]{"halloween", "graveyard", "cemetery", "fog", "tombstones", "dead trees", "moon"};
      case 43:
         return new String[]{"halloween", "spider", "web", "cobweb", "silk", "dew"};
      case 44:
         return new String[]{"halloween", "storm", "ghost", "lightning", "green", "clouds"};
      case 45:
         return new String[]{"halloween", "eclipse", "runes", "black sun", "red", "cursed"};
      case 46:
         return new String[]{"halloween", "ravens", "crows", "birds", "sunset", "dusk"};
      case 47:
         return new String[]{"halloween", "souls", "lanterns", "wisps", "spirits", "lights"};
      case 48:
         return new String[]{"anime", "domain", "seal", "circles", "red", "void"};
      case 49:
         return new String[]{"anime", "spirit", "realm", "islands", "moon", "shinigami"};
      case 50:
         return new String[]{"anime", "pages", "notebook", "paper", "symbols", "cursed"};
      }
   }

   public boolean isDynamic() {
      return this.kind != SkyStyle.Kind.CLASSIC;
   }

   public String shaderName() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   public int horizonColor(int a, int b, boolean custom) {
      switch(this.ordinal()) {
      case 0:
         return ColorUtil.lerp(-16514554, a, 0.1F);
      case 1:
         return ColorUtil.lerp(-16383216, ColorUtil.lerp(a, b, 0.5F), 0.16F);
      case 2:
         return ColorUtil.shade(b | -16777216, 0.86F);
      case 3:
         return ColorUtil.lerp(-16317172, a, 0.14F);
      case 4:
         return ColorUtil.lerp(-16710654, a, 0.1F);
      case 5:
         return ColorUtil.lerp(-16710648, a, 0.1F);
      case 6:
         return ColorUtil.lerp(-16252144, a, 0.22F);
      case 7:
         return ColorUtil.lerp(-15071706, a, 0.45F);
      case 8:
         return ColorUtil.lerp(-16055018, a, 0.3F);
      case 9:
         return ColorUtil.lerp(-15923431, a, 0.25F);
      case 10:
         return ColorUtil.lerp(-16252145, a, 0.2F);
      case 11:
         return ColorUtil.lerp(-15064791, a, 0.3F);
      case 12:
         return ColorUtil.lerp(-15727870, a, 0.25F);
      case 13:
         return ColorUtil.lerp(-15068407, a, 0.35F);
      case 14:
         return ColorUtil.lerp(-16777216, a, 0.3F);
      case 15:
         return ColorUtil.lerp(-16777216, b, 0.15F);
      case 16:
         return ColorUtil.lerp(-16252928, a, 0.12F);
      case 17:
         return ColorUtil.lerp(-16448756, ColorUtil.lerp(a, b, 0.5F), 0.12F);
      case 18:
         return ColorUtil.lerp(-16443105, ColorUtil.lerp(a, b, 0.5F), 0.35F);
      case 19:
         return ColorUtil.lerp(-16580350, a, 0.08F);
      case 20:
         return ColorUtil.lerp(-16580348, ColorUtil.lerp(a, b, 0.5F), 0.15F);
      case 21:
         return ColorUtil.lerp(-16445412, a, 0.06F);
      case 22:
         return ColorUtil.lerp(-16645627, ColorUtil.lerp(a, b, 0.5F), 0.12F);
      case 23:
         return -16580092;
      case 24:
         return ColorUtil.shade(ColorUtil.lerp(-32688, a, 0.5F), 0.7F);
      case 25:
         return ColorUtil.lerp(-24999, a, 0.4F);
      case 26:
         return -6236929;
      case 27:
         return ColorUtil.lerp(-16447734, a, 0.45F);
      case 28:
         return ColorUtil.lerp(-16777216, a, 0.5F);
      case 29:
         return -11717825;
      case 30:
         return -4006923;
      case 31:
         return -10552;
      case 32:
         return ColorUtil.lerp(-14596801, a, 0.4F);
      case 33:
         return custom?ColorUtil.shade(ColorUtil.lerp(a, b, 0.5F), 0.7F):-7701878;
      case 34:
         return -16250868;
      case 35:
         return ColorUtil.lerp(-16250868, b, 0.3F);
      case 36:
         return ColorUtil.shade(a | -16777216, 0.88F);
      case 37:
         return ColorUtil.lerp(-16316665, a, 0.05F);
      case 38:
         return ColorUtil.lerp(-16250868, a, 0.04F);
      case 39:
         return ColorUtil.lerp(-16250868, a, 0.35F);
      case 40:
         return ColorUtil.lerp(-16250868, a, 0.35F);
      case 41:
         return ColorUtil.lerp(-16250868, a, 0.12F);
      case 42:
         return ColorUtil.lerp(-16250868, a, 0.3F);
      case 43:
         return ColorUtil.lerp(-16250868, a, 0.25F);
      case 44:
         return ColorUtil.lerp(-16250868, a, 0.06F);
      case 45:
         return ColorUtil.lerp(-16250868, a, 0.2F);
      case 46:
         return ColorUtil.lerp(-16250868, a, 0.35F);
      case 47:
         return ColorUtil.lerp(-16250868, b, 0.6F);
      case 48:
         return ColorUtil.lerp(-16250868, a, 0.15F);
      case 49:
         return ColorUtil.lerp(-16250868, a, 0.3F);
      case 50:
         return -15987695;
      default:
         return -13420476;
      }
   }

   // $FF: synthetic method
   private static SkyStyle[] $values() {
      return new SkyStyle[]{CIRCUIT_MAZE, KALEIDOSCOPE, THE_WATCHER, MEGASTRUCTURE, CODE_RAIN, PLASMA_VEINS, NEON_FRACTURE, VIOLET_STORM, SKY_BEAMS, AMETHYST_CELLS, VOID_RIFT, FROZEN_TIDE, FIREFALL, GILDED_STORM, MOLTEN_MARBLE, CHROME_INK, CRIMSON_VEINS, NEON_LOTUS, SILK, ACRYLIC_POUR, LIQUID_VORTEX, AURORA_NIGHT, NEBULA, BLACK_HOLE, SYNTHWAVE, GOLDEN_HOUR, ANIME_SKY, STORM_FRONT, BLOOD_MOON, ECLIPSE, SEA_OF_CLOUDS, CHERRY_BLOSSOM, SWIRLING_NIGHT, CHROMA_SKY, VOID, ORGANIC_LIQUID, CHROMATIC_BUBBLES, MONOCHROME_HALFTONE, COSMIC_EYE, HAUNTED_MOON, PUMPKIN_MOON, WITCHS_BREW, GRAVEYARD_FOG, SPIDER_SILK, SPECTRAL_STORM, CURSED_ECLIPSE, RAVEN_DUSK, SOUL_LANTERNS, SEALED_DOMAIN, SPIRIT_REALM, SCATTERED_PAGES, CUSTOM_IMAGE};
   }

   static enum Kind {
      CLASSIC,
      CRISP,
      SOFT;

      // $FF: synthetic method
      private static SkyStyle.Kind[] $values() {
         return new SkyStyle.Kind[]{CLASSIC, CRISP, SOFT};
      }
   }
}
