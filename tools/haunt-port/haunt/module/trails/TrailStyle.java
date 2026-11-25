package mix.client.mixui.integration.haunt.module.trails;

import java.util.function.Function;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.module.trails.styles.AfterimageStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.BrushstrokeStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.CardTrailStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.CartoonDustStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.CogsStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.CometStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.DatamoshStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.DoodleLineStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.EmbersStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.FootprintsStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.GildedChainStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.HelixStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.IconsStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.LightWallStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.LightningStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.MercuryStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.PaperTrailStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.RibbonStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.RosePathStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.SmokeStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.SpeedLinesStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.StainedGlassStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.StardustStyle;
import mix.client.mixui.integration.haunt.module.trails.styles.WraithStyle;
import mix.client.mixui.integration.haunt.setting.Displayable;

public enum TrailStyle implements Displayable {
   RIBBON("Ribbon", "A smooth band of light flowing behind you", true, RibbonStyle::new),
   COMET("Comet", "A bright orb with a long tail of light", true, CometStyle::new),
   FOOTPRINTS("Footprints", "Glowing prints where each foot lands", false, FootprintsStyle::new),
   AFTERIMAGE("Afterimage", "Speed ghosts of your body left behind as you run", false, AfterimageStyle::new),
   EMBERS("Embers", "Sparks, embers or flames left behind your feet", false, EmbersStyle::new),
   SMOKE("Smoke", "Soft puffs that rise, drift with the wind and fade", false, SmokeStyle::new),
   LIGHT_WALL("Light Wall", "A light-cycle wall standing on the ground along your path", true, LightWallStyle::new),
   HELIX("Helix", "Orbs circle your waist and twist into glowing strands", true, HelixStyle::new),
   LIGHTNING("Lightning", "Jagged electric arcs crackling along your path", true, LightningStyle::new),
   STARDUST("Stardust", "A cosmic ribbon of drifting nebula and twinkling stars", true, StardustStyle::new),
   SPEED_LINES("Speed Lines", "Anime motion lines streaming back as you sprint", false, SpeedLinesStyle::new),
   ICONS("Icons", "Hearts, stars or notes pop out and float away", false, IconsStyle::new),
   ROSE_PATH("Rose Path", "A thorny vine with roses growing along the ground", true, true, RosePathStyle::new, new String[]{"roses", "vines", "thorns", "bramble", "gothic", "path"}),
   BRUSHSTROKE("Brushstroke", "One long calligraphic stroke of ink", true, true, BrushstrokeStyle::new, new String[]{"ink", "brush", "stroke", "calligraphy", "sumi-e", "japanese"}),
   PAPER_TRAIL("Paper Trail", "Paper planes, cranes, stars and confetti flutter off behind you", false, true, PaperTrailStyle::new, new String[]{"paper", "origami", "planes", "cranes", "confetti", "pastel", "cute"}),
   STAINED_GLASS("Stained Glass", "A low wall of stained glass rising along your path", true, true, StainedGlassStyle::new, new String[]{"stained glass", "mosaic", "cathedral", "glass", "wall", "gothic"}),
   DATAMOSH("Datamosh", "A smear of corrupted video: pixel-sorted bars of color", true, true, DatamoshStyle::new, new String[]{"glitch", "datamosh", "pixel sort", "rgb", "digital", "cyber", "vaporwave"}),
   MERCURY("Mercury", "Drops of liquid chrome that merge, splash and ripple", false, true, MercuryStyle::new, new String[]{"chrome", "mercury", "liquid", "metal", "drops", "y2k"}),
   WRAITH("Wraith", "Smoke tendrils streaming like a tattered cloak", true, true, WraithStyle::new, new String[]{"shadow", "smoke", "wraith", "dark", "cloak", "tendrils", "embers"}),
   COGS("Cogs", "Little brass gears spinning out in meshing clusters", false, true, CogsStyle::new, new String[]{"clockwork", "gears", "cogs", "brass", "steampunk"}),
   CARD_TRAIL("Card Trail", "Tarot cards dealt behind you, floating and turning", false, true, CardTrailStyle::new, new String[]{"tarot", "cards", "deck", "occult", "mystic", "arcana"}),
   DOODLE_LINE("Doodle Line", "A hand-drawn line that wobbles and boils, with doodles along it", true, true, DoodleLineStyle::new, new String[]{"doodle", "sketch", "pencil", "marker", "chalk", "drawing", "line"}),
   GILDED_CHAIN("Gilded Chain", "A golden chain strung along your path", true, true, GildedChainStyle::new, new String[]{"chain", "chains", "gold", "gilded", "links"}),
   CARTOON_DUST("Cartoon Dust", "Comic dust clouds puffing out behind your feet", false, true, CartoonDustStyle::new, new String[]{"comic", "cartoon", "dust", "clouds", "puff", "run"});

   private final String displayName;
   private final String description;
   private final boolean path;
   private final Function<TrailsModule, TrailStyleBase> factory;
   private final boolean isNew;
   private final String[] keywords;

   private TrailStyle(String displayName, String description, boolean path, Function<TrailsModule, TrailStyleBase> factory) {
      this(displayName, description, path, false, factory, new String[0]);
   }

   private TrailStyle(String displayName, String description, boolean path, boolean isNew, Function<TrailsModule, TrailStyleBase> factory, String[] keywords) {
      this.displayName = displayName;
      this.description = description;
      this.path = path;
      this.isNew = isNew;
      this.factory = factory;
      this.keywords = keywords;
   }

   public String[] keywords() {
      return this.keywords.length == 0?null:(String[])((String[])this.keywords.clone());
   }

   public boolean isNew() {
      return this.isNew;
   }

   public String displayName() {
      return this.displayName;
   }

   public String description() {
      return this.description;
   }

   public String tag() {
      return this.path?"PATH":"EMITTER";
   }

   public boolean isPath() {
      return this.path;
   }

   TrailStyleBase create(TrailsModule module) {
      return (TrailStyleBase)this.factory.apply(module);
   }
}
