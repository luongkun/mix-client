package mix.client.mixui.integration.haunt.module.targetaura;

import java.util.function.Function;
import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.module.targetaura.effects.ArmillaryEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.AscensionEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.ChainBindEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.CrystalHaloEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.DoodleEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.GlitchFrameEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.GroundSigilEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.InkEnsoEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.LiquidChromeEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.LockOnEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.NeonIconEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.OrbitRingsEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.PaperCranesEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.PulseWaveEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.RoseWindowEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.ShadowVeilEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.SoulOrbsEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.SpiritSpiralEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.TarotEffect;
import mix.client.mixui.integration.haunt.module.targetaura.effects.ThornCrownEffect;
import mix.client.mixui.integration.haunt.setting.Displayable;

public enum EffectType implements Displayable {
   CRYSTAL_HALO("Crystal Halo", "Floating crystal, orbiting shards", false, CrystalHaloEffect::new, new String[]{"crystal", "gem", "diamond", "halo", "shards"}),
   ORBIT_RINGS("Orbit Rings", "Bright rings on tilted axes", false, OrbitRingsEffect::new, new String[]{"rings", "orbit", "atom", "gyroscope"}),
   SPIRIT_SPIRAL("Spirit Spiral", "Comet trails spiral around", false, SpiritSpiralEffect::new, new String[]{"spiral", "comet", "swirl", "spirit"}),
   SOUL_ORBS("Soul Orbs", "Glowing orbs with trails", false, SoulOrbsEffect::new, new String[]{"orbs", "souls", "wisps", "spheres"}),
   CHAIN_BIND("Chain Bind", "Chains wrap the body", false, ChainBindEffect::new, new String[]{"chains", "metal", "bound", "gold", "silver", "iron"}),
   NEON_ICON("Neon Icon", "Flickering neon sign", false, NeonIconEffect::new, new String[]{"neon", "sign", "icon", "dollar", "heart", "skull"}),
   GROUND_SIGIL("Ground Sigil", "Rotating magic circle", false, GroundSigilEffect::new, new String[]{"sigil", "magic circle", "runes", "summoning"}),
   LOCK_ON("Lock-On", "HUD corner brackets", false, LockOnEffect::new, new String[]{"lock on", "target", "brackets", "hud"}),
   ASCENSION("Ascension", "Light column and embers", false, AscensionEffect::new, new String[]{"column", "beam", "pillar", "holy", "embers"}),
   PULSE_WAVE("Pulse Wave", "Sonar ping on the ground", false, PulseWaveEffect::new, new String[]{"pulse", "wave", "sonar", "shockwave", "ripple"}),
   THORN_CROWN("Thorn Crown", "Thorny vines and a crown of roses", true, ThornCrownEffect::new, new String[]{"thorns", "vines", "roses", "brambles", "gothic", "crown"}),
   INK_ENSO("Ink Enso", "One brush stroke paints a circle", true, InkEnsoEffect::new, new String[]{"ink", "brush", "enso", "sumi-e", "calligraphy", "japanese", "seal"}),
   PAPER_CRANES("Paper Cranes", "Origami cranes circling round", true, PaperCranesEffect::new, new String[]{"paper", "origami", "cranes", "birds", "pastel", "cute"}),
   ROSE_WINDOW("Rose Window", "Stained glass casting colored light", true, RoseWindowEffect::new, new String[]{"stained glass", "window", "cathedral", "church", "gothic", "halo"}),
   GLITCH_FRAME("Glitch Frame", "Digital corruption round the target", true, GlitchFrameEffect::new, new String[]{"glitch", "error", "rgb", "datamosh", "digital", "vaporwave", "cyber"}),
   LIQUID_CHROME("Liquid Chrome", "Mercury drops that merge and split", true, LiquidChromeEffect::new, new String[]{"chrome", "liquid", "metal", "mercury", "y2k", "blobs"}),
   SHADOW_VEIL("Shadow Veil", "Dark smoke curling round the body", true, ShadowVeilEffect::new, new String[]{"shadow", "smoke", "dark", "umbra", "void", "demon"}),
   ARMILLARY("Armillary", "Brass rings ticking like clockwork", true, ArmillaryEffect::new, new String[]{"armillary", "brass", "clockwork", "steampunk", "astronomy", "rings", "gears"}),
   TAROT("Tarot", "Ornate cards that flip and float", true, TarotEffect::new, new String[]{"tarot", "cards", "occult", "mystic", "fortune", "arcana"}),
   DOODLE("Doodle", "Hand-drawn circle, arrows and sketches", true, DoodleEffect::new, new String[]{"doodle", "sketch", "pencil", "marker", "chalk", "drawing", "cartoon"});

   private final String displayName;
   private final String description;
   private final boolean isNew;
   private final Function<TargetAuraModule, AuraEffect> factory;
   private final String[] keywords;

   private EffectType(String displayName, String description, boolean isNew, Function<TargetAuraModule, AuraEffect> factory, String[] keywords) {
      this.displayName = displayName;
      this.description = description;
      this.isNew = isNew;
      this.factory = factory;
      this.keywords = keywords;
   }

   public String displayName() {
      return this.displayName;
   }

   public String description() {
      return this.description;
   }

   public String[] keywords() {
      return (String[])((String[])this.keywords.clone());
   }

   public boolean isNew() {
      return this.isNew;
   }

   AuraEffect create(TargetAuraModule module) {
      return (AuraEffect)this.factory.apply(module);
   }
}
