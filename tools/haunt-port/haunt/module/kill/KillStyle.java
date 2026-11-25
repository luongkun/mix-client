package mix.client.mixui.integration.haunt.module.kill;

import java.util.Locale;
import java.util.function.Function;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.module.kill.styles.BeamStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.BlackHoleStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.ChainedStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.ConsumedStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.CursedTombstoneStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.DecapitationStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.DerezzStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.DisintegrateStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.ErasedStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.FateCardStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.FireworkStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.FoldAwayStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.FrozenDesperationStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.GraveEscapeStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.GraveHandsStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.InkWashStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.KnockoutStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.MeltdownStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.OvergrowthStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.RagdollStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.RestInPeaceStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.SanctifyStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.SanguineDeathStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.ShatterStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.ShockwaveStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.SignalLostStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.SliceStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.SoulAscendStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.TombstoneStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.TornadoStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.VoidSphereStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.WindDownStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.ZombieAwakeningStyle;
import mix.client.mixui.integration.haunt.setting.Displayable;

public enum KillStyle implements Displayable {
   RAGDOLL("Ragdoll", "Goes limp and flies with the killing blow", 2.4F, RagdollStyle::new),
   DECAPITATION("Decapitation", "The head pops off and rolls away", 2.6F, DecapitationStyle::new),
   BEAM("Beam", "Pillar of light, lightning or an orbital laser", 2.3F, BeamStyle::new),
   SHOCKWAVE("Shockwave", "A blast ring throws the body and the ground", 1.9F, ShockwaveStyle::new),
   DISINTEGRATE("Disintegrate", "Crumbles to dust from a burning edge", 3.0F, DisintegrateStyle::new),
   SLICE("Slice", "Blade slashes cut the body in two", 2.6F, SliceStyle::new),
   SHATTER("Shatter", "Freezes into ice, then bursts into shards", 2.5F, ShatterStyle::new),
   SOUL_ASCEND("Soul Ascend", "A glowing soul rises and fades away", 3.4F, SoulAscendStyle::new),
   BLACK_HOLE("Black Hole", "Swallowed by a tiny black hole", 2.8F, BlackHoleStyle::new),
   DEREZZ("Derezz", "Glitches, then breaks into floating voxels", 2.8F, DerezzStyle::new),
   FIREWORK("Firework", "Rockets up and bursts into a firework", 2.7F, FireworkStyle::new),
   TOMBSTONE("Tombstone", "Sinks away as a gravestone rises", 6.1F, TombstoneStyle::new),
   OVERGROWTH("Overgrowth", "Brambles and roses wrap it, then wither away", 2.4F, true, OvergrowthStyle::new, new String[]{"thorns", "vines", "roses", "overgrown", "bramble", "wither", "gothic"}),
   INK_WASH("Ink Wash", "Repainted in ink, then diffuses away", 2.0F, true, InkWashStyle::new, new String[]{"ink", "wash", "sumi-e", "brush", "painting", "japanese", "seal"}),
   FOLD_AWAY("Fold Away", "Folds into a paper crane that flies off", 2.2F, true, FoldAwayStyle::new, new String[]{"paper", "origami", "fold", "crane", "plane", "cute"}),
   SANCTIFY("Sanctify", "Becomes a stained-glass window, then light", 2.4F, true, SanctifyStyle::new, new String[]{"stained glass", "window", "cathedral", "saint", "holy", "light", "gothic"}),
   SIGNAL_LOST("Signal Lost", "Glitches out, then switches off like an old TV", 1.4F, true, SignalLostStyle::new, new String[]{"glitch", "crt", "signal", "error", "rgb", "digital", "cyber"}),
   MELTDOWN("Meltdown", "Melts into a chrome puddle, then a sphere", 2.2F, true, MeltdownStyle::new, new String[]{"chrome", "liquid", "metal", "melt", "mercury", "y2k"}),
   CONSUMED("Consumed", "Charred by black smoke, then crumbles away", 2.0F, true, ConsumedStyle::new, new String[]{"shadow", "smoke", "dark", "ash", "embers", "char", "consumed"}),
   WIND_DOWN("Wind Down", "A brass automaton winds down and falls apart", 2.2F, true, WindDownStyle::new, new String[]{"clockwork", "brass", "automaton", "gears", "wind-up", "steampunk"}),
   FATE_CARD("Fate Card", "Drawn into a tarot card that burns away", 2.0F, true, FateCardStyle::new, new String[]{"tarot", "card", "fate", "occult", "mystic", "arcana", "portrait"}),
   ERASED("Erased", "Sketched in pencil, then rubbed out", 1.8F, true, ErasedStyle::new, new String[]{"doodle", "sketch", "pencil", "eraser", "erased", "drawing", "cartoon"}),
   CHAINED("Chained", "Golden chains drag the body under", 2.2F, true, ChainedStyle::new, new String[]{"chains", "chained", "gold", "bound", "shackles", "drag"}),
   KNOCKOUT("Knockout", "K.O.! Launched into the sky like a cartoon", 1.8F, true, KnockoutStyle::new, new String[]{"comic", "ko", "knockout", "cartoon", "pop art", "halftone", "launch"}),
   SANGUINE_DEATH("Sanguine Death", "HALLOWEEN — A small red skull swells, laughs and bursts into blood-red sparks", 2.6F, true, SanguineDeathStyle::new, new String[]{"halloween", "skull", "blood", "red", "sanguine", "explode", "laugh", "haunt"}),
   TORNADO("Vortex Execution", "HALLOWEEN — A dark tornado drags the body up, spins it and hurls it away", 4.2F, true, TornadoStyle::new, new String[]{"halloween", "tornado", "vortex", "wind", "ragdoll", "spin", "storm", "haunt"}),
   VOID_SPHERE("Void Sphere", "HALLOWEEN — A sphere of nothing tears open and swallows the body", 3.6F, true, VoidSphereStyle::new, new String[]{"halloween", "void", "sphere", "black hole", "implode", "ragdoll", "dark", "haunt"}),
   FROZEN_DESPERATION("Frozen Desperation", "HALLOWEEN — Frozen in panic until you hit it, then it shatters free and flies", 6.0F, true, FrozenDesperationStyle::new, new String[]{"halloween", "frozen", "ice", "panic", "ragdoll", "shatter", "hit", "haunt"}),
   GRAVE_ESCAPE("Grave Escape", "HALLOWEEN — A zombie hand claws up behind a rising headstone, grips it and slips", 4.2F, true, GraveEscapeStyle::new, new String[]{"halloween", "graveyard", "zombie", "hand", "headstone", "grave", "cemetery", "haunt"}),
   REST_IN_PEACE("Rest in Peace", "HALLOWEEN — A headstone bursts up, a shadow with glowing eyes looms behind it", 4.5F, true, RestInPeaceStyle::new, new String[]{"halloween", "graveyard", "rip", "headstone", "shadow", "ghost", "cemetery", "haunt"}),
   ZOMBIE_AWAKENING("Zombie Awakening", "HALLOWEEN — The ground cracks and a rotten arm punches out up to the elbow", 4.6F, true, ZombieAwakeningStyle::new, new String[]{"halloween", "graveyard", "zombie", "arm", "hand", "cracks", "cemetery", "haunt"}),
   CURSED_TOMBSTONE("Cursed Tombstone", "HALLOWEEN — A dark obelisk rises, its runes ignite one by one, a shadow hand grips it", 5.6F, true, CursedTombstoneStyle::new, new String[]{"halloween", "graveyard", "cursed", "runes", "obelisk", "shadow", "cemetery", "haunt"}),
   GRAVE_HANDS("Grave Hands", "HALLOWEEN — A ring of rotten hands claws out of the ground around a stone cross", 4.8F, true, GraveHandsStyle::new, new String[]{"halloween", "graveyard", "zombie", "hands", "cross", "ring", "cemetery", "haunt"});

   private final String displayName;
   private final String description;
   private final float seconds;
   private final Function<KillEffectsModule, KillStyleBase> factory;
   private final boolean isNew;
   private final String[] keywords;

   private KillStyle(String displayName, String description, float seconds, Function<KillEffectsModule, KillStyleBase> factory) {
      this(displayName, description, seconds, false, factory, new String[0]);
   }

   private KillStyle(String displayName, String description, float seconds, boolean isNew, Function<KillEffectsModule, KillStyleBase> factory, String[] keywords) {
      this.displayName = displayName;
      this.description = description;
      this.seconds = seconds;
      this.isNew = isNew;
      this.factory = factory;
      this.keywords = keywords;
   }

   public String[] keywords() {
      return this.keywords.length == 0?null:(String[])this.keywords.clone();
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
      return String.format(Locale.ROOT, this.seconds < 10.0F?"%.1f s":"%.0f s", new Object[]{Float.valueOf(this.seconds)});
   }

   KillStyleBase create(KillEffectsModule module) {
      return (KillStyleBase)this.factory.apply(module);
   }

   // $FF: synthetic method
   private static KillStyle[] $values() {
      return new KillStyle[]{RAGDOLL, DECAPITATION, BEAM, SHOCKWAVE, DISINTEGRATE, SLICE, SHATTER, SOUL_ASCEND, BLACK_HOLE, DEREZZ, FIREWORK, TOMBSTONE, OVERGROWTH, INK_WASH, FOLD_AWAY, SANCTIFY, SIGNAL_LOST, MELTDOWN, CONSUMED, WIND_DOWN, FATE_CARD, ERASED, CHAINED, KNOCKOUT, SANGUINE_DEATH, TORNADO, VOID_SPHERE, FROZEN_DESPERATION, GRAVE_ESCAPE, REST_IN_PEACE, ZOMBIE_AWAKENING, CURSED_TOMBSTONE, GRAVE_HANDS};
   }
}
