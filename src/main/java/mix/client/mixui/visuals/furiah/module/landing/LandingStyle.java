package mix.client.mixui.visuals.furiah.module.landing;

import java.util.function.Function;
import mix.client.mixui.visuals.furiah.fx.Lifecycle;
import mix.client.mixui.visuals.furiah.module.landing.styles.BadSectorStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.BrambleBloomStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.CardSpreadStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.CathedralLightStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.ChainSlamStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.ChromeSplashStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.ClockfaceStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.DustRingStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.EnergyFissuresStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.FeatherFallStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.HexPulseStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.InkBlotStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.JumpCirclesStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.LightPillarStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.LiquidRippleStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.NeuralNetStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.PopUpStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.SealStampStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.ShadowPoolStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.SketchRingsStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.SparkleBurstStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.ThudStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.WarpDomeStyle;
import mix.client.mixui.visuals.furiah.module.landing.styles.XRayScanStyle;
import mix.client.mixui.visuals.furiah.setting.Displayable;

public enum LandingStyle implements Displayable {
   JUMP_CIRCLES("Jump Circles", "Glowing rings glide out", Lifecycle.FADE, JumpCirclesStyle::new),
   NEURAL_NET("Neural Net", "Tendrils crawl out, then spark", Lifecycle.ANIMATION, NeuralNetStyle::new),
   XRAY_SCAN("X-Ray Scan", "A neon afterimage and a scan wave", Lifecycle.ANIMATION, XRayScanStyle::new),
   HEX_PULSE("Hex Pulse", "A hexagon pops and turns", Lifecycle.FADE, HexPulseStyle::new),
   WARP_DOME("Warp Dome", "A jelly dome rises and pops", Lifecycle.ANIMATION, WarpDomeStyle::new),
   DUST_RING("Dust Ring", "Soft dust rolls outwards", Lifecycle.FADE, DustRingStyle::new),
   ENERGY_FISSURES("Energy Fissures", "Glowing cracks split the ground", Lifecycle.ANIMATION, EnergyFissuresStyle::new),
   SEAL_STAMP("Seal Stamp", "A magic seal slams down", Lifecycle.ANIMATION, SealStampStyle::new),
   LIGHT_PILLAR("Light Pillar", "A beam bursts upwards", Lifecycle.ANIMATION, LightPillarStyle::new),
   FEATHER_FALL("Feather Fall", "Feathers flutter down", Lifecycle.FADE, FeatherFallStyle::new),
   LIQUID_RIPPLE("Liquid Ripple", "Ripples, as if you landed on water", Lifecycle.ANIMATION, LiquidRippleStyle::new),
   SPARKLE_BURST("Sparkle Burst", "A lens flash and shooting sparkles", Lifecycle.FADE, SparkleBurstStyle::new),
   BRAMBLE_BLOOM(
      "Bramble Bloom",
      "Thorny vines burst out and bloom roses",
      Lifecycle.ANIMATION,
      true,
      BrambleBloomStyle::new,
      "thorns",
      "vines",
      "roses",
      "bloom",
      "bramble",
      "gothic"
   ),
   INK_BLOT(
      "Ink Blot",
      "A splash of ink that soaks in",
      Lifecycle.ANIMATION,
      true,
      InkBlotStyle::new,
      "ink",
      "splash",
      "splatter",
      "blot",
      "sumi-e",
      "japanese",
      "seal"
   ),
   POP_UP("Pop-Up", "Paper cutouts spring up in a ring", Lifecycle.ANIMATION, true, PopUpStyle::new, "paper", "pop-up", "origami", "craft", "cute", "pastel"),
   CATHEDRAL_LIGHT(
      "Cathedral Light",
      "Colored light falls as through a rose window",
      Lifecycle.FADE,
      true,
      CathedralLightStyle::new,
      "stained glass",
      "cathedral",
      "light",
      "window",
      "church",
      "gothic",
      "holy"
   ),
   BAD_SECTOR(
      "Bad Sector",
      "The ground glitches, then switches off",
      Lifecycle.FADE,
      true,
      BadSectorStyle::new,
      "glitch",
      "data",
      "error",
      "rgb",
      "pixel",
      "digital",
      "cyber"
   ),
   CHROME_SPLASH(
      "Chrome Splash",
      "A crown of liquid chrome splashes up",
      Lifecycle.ANIMATION,
      true,
      ChromeSplashStyle::new,
      "chrome",
      "liquid",
      "metal",
      "mercury",
      "splash",
      "crown",
      "y2k"
   ),
   SHADOW_POOL(
      "Shadow Pool", "A ring of black smoke and a dark pool", Lifecycle.FADE, true, ShadowPoolStyle::new, "shadow", "smoke", "dark", "mist", "embers", "pool"
   ),
   CLOCKFACE(
      "Clockface",
      "A brass dial slams down and ticks round",
      Lifecycle.ANIMATION,
      true,
      ClockfaceStyle::new,
      "clockwork",
      "brass",
      "gears",
      "clock",
      "steampunk"
   ),
   CARD_SPREAD(
      "Card Spread",
      "Tarot cards dealt in a circle, then flipped",
      Lifecycle.ANIMATION,
      true,
      CardSpreadStyle::new,
      "tarot",
      "cards",
      "spread",
      "occult",
      "mystic",
      "arcana"
   ),
   SKETCH_RINGS(
      "Sketch Rings",
      "Hand-drawn rings, doodles and an eraser",
      Lifecycle.FADE,
      true,
      SketchRingsStyle::new,
      "doodle",
      "sketch",
      "rings",
      "pencil",
      "chalk",
      "drawing",
      "cartoon"
   ),
   CHAIN_SLAM(
      "Chain Slam", "Golden chains whip out and snap back", Lifecycle.ANIMATION, true, ChainSlamStyle::new, "chains", "gold", "slam", "shackle", "links"
   ),
   THUD("Thud", "A comic splat and a bold word", Lifecycle.ANIMATION, true, ThudStyle::new, "comic", "thud", "boom", "cartoon", "pop art", "halftone");

   private final String displayName;
   private final String description;
   private final Lifecycle lifecycle;
   private final Function<LandingImpactModule, LandingStyleBase> factory;
   private final boolean isNew;
   private final String[] keywords;

   private LandingStyle(String displayName, String description, Lifecycle lifecycle, Function<LandingImpactModule, LandingStyleBase> factory) {
      this(displayName, description, lifecycle, false, factory);
   }

   private LandingStyle(
      String displayName, String description, Lifecycle lifecycle, boolean isNew, Function<LandingImpactModule, LandingStyleBase> factory, String... keywords
   ) {
      this.displayName = displayName;
      this.description = description;
      this.lifecycle = lifecycle;
      this.isNew = isNew;
      this.factory = factory;
      this.keywords = keywords;
   }

   @Override
   public String[] keywords() {
      return this.keywords.length == 0 ? null : (String[])this.keywords.clone();
   }

   @Override
   public boolean isNew() {
      return this.isNew;
   }

   @Override
   public String displayName() {
      return this.displayName;
   }

   @Override
   public String description() {
      return this.description;
   }

   @Override
   public String tag() {
      return this.lifecycle.tag();
   }

   public Lifecycle lifecycle() {
      return this.lifecycle;
   }

   LandingStyleBase create(LandingImpactModule module) {
      return this.factory.apply(module);
   }
}
