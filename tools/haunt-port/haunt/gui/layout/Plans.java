package mix.client.mixui.integration.haunt.gui.layout;

import mix.client.mixui.integration.haunt.gui.layout.Plan;

final class Plans {
   static Plan of(String moduleId) {
      return "target_aura".equals(moduleId)?targetAura():("hit_effect".equals(moduleId)?hitEffect():("kill_effects".equals(moduleId)?killEffects():("landing_impact".equals(moduleId)?landingImpact():("trails".equals(moduleId)?trails():("particles".equals(moduleId)?particles():("weapons".equals(moduleId)?weapons():("glow".equals(moduleId)?glow():("ambience".equals(moduleId)?ambience():("dynamic_skies".equals(moduleId)?skies():("motion_blur".equals(moduleId)?motionBlur():("watermark".equals(moduleId)?waterMark():("ui".equals(moduleId)?ui():("misc".equals(moduleId)?misc():null)))))))))))));
   }

   private static Plan misc() {
      Plan p = new Plan();
      Plan.PlanPage skins = p.tab("skins", "Skins", "Change your skin without leaving the game: add PNG skins (HD too) and keep a library. Only on your screen: other players keep seeing your account\'s skin.");
      skins.main().add(new String[]{"skin.enabled", "skin.current", "skin.back"});
      skins.feature("Add skins").add(new String[]{"skin.add"});
      skins.feature("Library").add(new String[]{"skin.library"});
      skins.feature("Quick switch").add(new String[]{"skin.wheelKey", "skin.nextKey", "skin.prevKey", "skin.notify"});
      skins.advanced().add(new String[]{"skin.autoModel", "*"});
      p.data("skin.worn", "the skin picked in the library (its cards)").data("skin.sort", "the library\'s Sort").data("skin.favoritesFirst", "the library\'s ★ First");
      p.tab("saturation", "Saturation", "How colorful the world looks: saturation, contrast and vibrance. Only on your screen.").headerless().main().add(new String[]{"sat.preview", "sat.enabled", "sat.looks", "sat.dimension", "sat.saturation", "sat.contrast", "sat.vibrance", "sat.overworld.*", "sat.nether.*", "sat.end.*", "sat.applyTo", "sat.perDimension", "sat.toggleKey", "sat.transition", "*"});
      p.notSwitch("sat.perDimension").notSwitch("sat.dimension");
      String[][] dims = new String[][]{{"overworld", "Overworld"}, {"nether", "Nether"}, {"end", "End"}};
      String[][] values = new String[][]{{"saturation", "Saturation"}, {"contrast", "Contrast"}, {"vibrance", "Vibrance"}};

      for(String[] d : dims) {
         for(String[] v : values) {
            p.label("sat." + d[0] + "." + v[0], d[1] + " " + v[1]);
         }
      }

      p.tab("fullbright", "Fullbright", "Lights up everything, at night and in caves. Only on your screen: your Brightness setting never changes.").headerless().main().add(new String[]{"fullbright.enabled", "fullbright.strength", "fullbright.toggleKey", "*"});
      p.tab("time", "Time Changer", "The time of day you see: the sky, the sun and moon, the stars and the light. Only on your screen, in the Overworld.").headerless().main().add(new String[]{"time.enabled", "time.clock", "time.presets", "time.smooth", "*"});
      p.tab("weather", "Weather Changer", "The weather you see: clear, rain, a thunderstorm or snow. Only on your screen, in the Overworld.").headerless().main().add(new String[]{"weather.enabled", "weather.type", "weather.strength", "weather.smooth", "*"});
      p.notSwitch("weather.type");
      p.tab("armor", "Hide armor", "Hide pieces of armor, yours and other players\'. Only the look: the armor still counts.").headerless().main().add(new String[]{"armor.enabled", "armor", "armor.heads", "*"});
      p.tab("hurt", "Hurtcam", "How much the camera shakes and tilts when you take damage.").headerless().main().add(new String[]{"hurt.enabled", "hurtShake", "*"});
      p.data("armor.me.*", "the Hide Armor grid\'s cells (yours)").data("armor.others.*", "the Hide Armor grid\'s cells (other players\')");
      return p;
   }

   private static Plan targetAura() {
      Plan p = new Plan();
      Plan.PlanPage s = p.main;
      s.main().add(new String[]{"effect"});
      s.colors().add(new String[]{"*:color", "signatureColors", "*.material", "*.finish"});
      s.size().add(new String[]{"*.size", "*.intensity", "range"});
      s.motion().add(new String[]{"*.speed", "linger", "fadeIn", "fadeOut"});
      s.when().add(new String[]{"aimTolerance", "ignoreTeammates", "ignoreNpcs"});
      s.options().add(new String[]{"*"});
      s.advanced().add(new String[]{"keepVisible", "keepVisible.opacity", "keepVisible.dark", "matchLighting", "minBrightness", "nightVisibility"});
      return p;
   }

   private static Plan hitEffect() {
      Plan p = new Plan();
      Plan.PlanPage s = p.main;
      s.main().add(new String[]{"style"});
      s.colors().add(new String[]{"*:color", "signatureColors", "*.material", "*.finish", "bodyTint"});
      s.size().add(new String[]{"size", "opacity", "density", "closeRange"});
      s.motion().add(new String[]{"duration", "speed", "follow"});
      s.when().add(new String[]{"trigger", "critBoost", "ignoreTeammates", "alsoOnMobs", "again", "maxPerTarget"});
      s.options().add(new String[]{"*"});
      s.advanced().add(new String[]{"keepVisible", "keepVisible.opacity", "matchLighting", "minBrightness", "nightVisibility"});
      return p;
   }

   private static Plan killEffects() {
      Plan p = new Plan();
      Plan.PlanPage s = p.main;
      s.main().add(new String[]{"style", "randomStyle"});
      s.colors().add(new String[]{"*:color", "signatureColors", "*.material", "*.finish", "particleStyle"});
      s.size().add(new String[]{"size", "intensity", "opacity", "glow"});
      s.motion().add(new String[]{"speed", "linger", "bodyEnd"});
      s.when().add(new String[]{"showFor", "critBoost"});
      s.options().add(new String[]{"*"});
      s.feature("Add-ons").add(new String[]{"marker", "islandNotice", "slowmo", "pulse"});
      s.advanced().add(new String[]{"detectDeath", "detectChat", "detectVanish", "credit", "replaceHit", "maxEffects", "showArmor", "showItem", "showCape", "matchLighting", "minBrightness", "nightVisibility", "testCrit"});
      p.label("replaceHit", "Replace Hit Effect on the Killing Blow");
      return p;
   }

   private static Plan landingImpact() {
      Plan p = new Plan();
      Plan.PlanPage s = p.main;
      s.main().add(new String[]{"style", "trigger", "*:color", "signatureColors", "*.material", "*.finish"});
      s.size().add(new String[]{"size", "opacity", "density", "scaleWithFall"});
      s.motion().add(new String[]{"duration", "speed"});
      s.when().add(new String[]{"showFor", "firstPerson"});
      s.options().add(new String[]{"*"});
      s.advanced().add(new String[]{"conform", "maxEffects", "matchLighting", "minBrightness", "nightVisibility", "dummyJumps"});
      return p;
   }

   private static Plan trails() {
      Plan p = new Plan();
      Plan.PlanPage s = p.main;
      s.main().add(new String[]{"preview", "style", "combine"});
      s.colors().add(new String[]{"*:color", "signatureColors", "*.material", "*.finish", "*.colorAlong", "otherColor"});
      s.size().add(new String[]{"*.size", "*.length", "*.maxLength", "*.opacity", "*.glow", "*.density", "*.taper"});
      s.motion().add(new String[]{"*.whenStop", "*.speedScaling", "*.smoothing", "*.fadeCurve", "*.animSpeed", "*.followWind"});
      s.when().add(new String[]{"showFor", "maxDistance", "skipTeammates", "fairPlay", "movement", "minSpeed", "sneakDim", "firstPerson", "calm"});
      s.options().add(new String[]{"*.anchor", "*"});
      s.extras().add(new String[]{"sprintBurst", "bubbles", "swingTrails"});
      s.advanced().add(new String[]{"lighting", "matchLighting", "minBrightness", "nightVisibility", "nearFade", "maxPlayers", "detailOthers", "sampleSpacing", "curveDetail", "pieceCap"});
      p.notSwitch("style");
      p.data("move.*", "the Movement row\'s switches").data("*.on", "each style\'s on/off pill on its card (Combine Styles)");
      return p;
   }

   private static Plan particles() {
      Plan p = new Plan();
      Plan.PlanPage s = p.main;
      s.main().add(new String[]{"burstTest", "replaceWith"});
      s.colors().add(new String[]{"swatches", "crit.color", "sharp.color", "colorScope", "glow"});
      s.size().add(new String[]{"scale", "critMultiplier", "sharpMultiplier"});
      s.motion().add(new String[]{"pulse.amount", "spin", "flicker.amount", "fadeOut"});
      s.when().add(new String[]{"appearOn", "showCrit", "showSharp", "alwaysCrit", "alwaysSharp"});
      s.feature("Custom images").add(new String[]{"library", "crit.images", "sharp.images", "images.square", "images.maxSize", "images.filtering"});
      s.extras().add(new String[]{"fullbright", "smoothFiltering"});
      s.advanced().add(new String[]{"closeFade", "ignoreParticleSetting", "maxGlowing"});
      p.option("crit.imageScale", "crit.images").option("sharp.imageScale", "sharp.images");
      p.label("scale", "Size").label("scale.applies", "Size Applies To").label("glow", "Glow").label("glow.intensity", "Glow Strength");
      return p;
   }

   private static Plan weapons() {
      Plan p = new Plan();
      Plan.PlanPage s = p.main;
      s.main().add(new String[]{"style", "categories", "rules", "cards", "preview"});
      s.colors().add(new String[]{"*.style", "*.finish", "*.customBlade", "*.paint", "*.handleMaterial", "*.handleColor", "*.accent", "*.wear"});
      s.size().add(new String[]{"*.sizeFirst", "*.sizeThird", "*.glow"});
      s.motion().add(new String[]{"*.weaponMotion", "*.walkBob", "*.lookSway", "*.breathing", "*.sprintPose", "*.swingPattern", "*.trail", "*.strafeTilt", "*.jumpLand", "*.crouch", "*.hurt", "*.hitReaction", "*.fidgets", "*.vibration", ">inspects"});
      s.when().add(new String[]{"otherPlayers", "icons", "dropped", "enchant"});
      s.options().add(new String[]{"gripSetup", "*.editor", "*.placement", "*.fov", "*.leftHanded", "dualWield", "dualSwings", "*.leftWeapon", "wheel.key", ">wheel", "*.heatShimmer", "*.sound", "*"});
      s.feature("Pixelated").add(new String[]{"pixel.effects", "pixel.glowStyle", "pixel.shading", "pixel.switchAnimation", "pixel.key", "pixel.autoPixelate"});
      s.advanced().add(new String[]{"antiAliasing", "textureSize", "lodDistance", "draw", "blockHit", "emptyHand", "hideArms", "import"});
      p.notSwitch("style").notSwitch("*.style");
      Plan.PlanPage inspects = p.sub("inspects", "Inspects", "The weapon\'s inspect animations, and when they play on their own.");
      inspects.main().add(new String[]{"*.inspects", "*.inspectOrder"});
      inspects.motion().add(new String[]{"inspectSpeed", "motionBlur"});
      inspects.when().add(new String[]{"autoInspect", "afterEquip", "keyAgain", "cancelOnDamage"});
      Plan.PlanPage wheel = p.sub("wheel", "Weapon wheel", "A wheel in the middle of the screen for picking the weapon you hold: its slots, how it opens and how it looks.");
      wheel.main().add(new String[]{"wheel.try", "wheel.slots", "wheel.perPage"});
      wheel.colors().add(new String[]{"wheel.theme"});
      wheel.size().add(new String[]{"wheel.size", "wheel.hole", "wheel.gap"});
      wheel.when().add(new String[]{"wheel.mode", "wheel.freezeCamera", "wheel.closeOnDamage"});
      wheel.options().add(new String[]{"wheel.icons", "wheel.labels", "wheel.blur"});
      return p;
   }

   private static Plan glow() {
      Plan p = new Plan();
      Plan.PlanPage s = p.main;
      s.main().add(new String[]{"preset"});
      s.colors().add(new String[]{"color", "rim.colorMode", "inner.tint"});
      s.size().add(new String[]{"rim.width", "rim.intensity", "halo.radius", "halo.intensity", "inner.glow", "render.opacity"});
      s.motion().add(new String[]{"pulse.depth", "flicker.amount", "flow.amount", "shimmer"});
      s.when().add(new String[]{"heldItems", "bareHand", "onlyFor", "overrides"});
      s.extras().add(new String[]{"trail.length", "hitFlash", "targetBoost"});
      s.advanced().add(new String[]{"render.blend", "render.quality", "rim.softness", "rim.hotCore", "halo.falloff", "halo.curve", "render.alphaCutoff"});
      p.label("rim.width", "Rim Width").label("rim.intensity", "Rim Intensity").label("rim.softness", "Rim Softness").label("rim.hotCore", "Rim Hot Core").label("halo.radius", "Halo Radius").label("halo.intensity", "Halo Intensity").label("halo.falloff", "Halo Falloff").label("halo.curve", "Halo Curve").label("trail.length", "Trail").label("render.alphaCutoff", "Alpha Cutoff");
      return p;
   }

   private static Plan ambience() {
      Plan p = new Plan();
      Plan.PlanPage s = p.main;
      s.main().add(new String[]{"styles"});
      s.size().add(new String[]{"distance", "totalCap"});
      s.motion().add(new String[]{"windMode", "windStrength", "gustFrequency", "gustStrength", "turbulence"});
      s.when().add(new String[]{"overworld", "nether", "end", "location", "calm"});
      s.feature("Atmosphere").add(new String[]{"atmosphere"});
      s.feature("Player wake").add(new String[]{"wake"});
      s.advanced().add(new String[]{"hideUnderwater", "adaptive"});
      Plan.PlanPage style = p.stylePages();
      style.main().add(new String[]{"*.preview"});
      style.colors().add(new String[]{"*.color", "*.glow"});
      style.size().add(new String[]{"*.size", "*.density", "*.opacity", "*.cap"});
      style.motion().add(new String[]{"*.speed", "*.followWind"});
      style.when().add(new String[]{"*.time", "*.distanceMode", "*.heightMin", "*.heightMax"});
      style.options().add(new String[]{"*"});
      p.label("totalCap", "Particle Cap").label("windMode", "Wind Direction").label("windStrength", "Wind Strength").label("*.glowIntensity", "Glow Strength");
      p.data("editing", "which style\'s page is open").data("*.on", "each style\'s on/off, on its card and its page\'s header");
      return p;
   }

   private static Plan skies() {
      Plan p = new Plan();
      Plan.PlanPage s = p.main;
      s.main().add(new String[]{"style", "skyTools"});
      s.colors().add(new String[]{"@Color"});
      s.size().add(new String[]{"*.scale@Motion", "*.blend@Where", "*.opacity@Where"});
      s.motion().add(new String[]{"*.speed@Motion", "*.rotation@Motion", "*.parallax@Motion", "*.fadeIn@Motion", "@Music"});
      s.when().add(new String[]{"@When", "*.overworld@Where", "*.nether@Where", "*.end@Where", "*.weather@Sky Elements"});
      s.options().add(new String[]{"*"});
      s.feature("Sky elements").add(new String[]{"@Sky Elements"});
      s.advanced().add(new String[]{"@Performance", "perServer", "hoverPreview"});
      p.label("*.scale@Motion", "Size").label("*.speed@Motion", "Speed");
      p.data("serverStyles", "the sky picked on each server (Remember Per Server)");
      return p;
   }

   private static Plan motionBlur() {
      Plan p = new Plan();
      Plan.PlanPage s = p.main;
      s.main().add(new String[]{"style", "*.strength", "hybrid.trails"});
      s.motion().add(new String[]{"fpsIndependent", "*.movement", "*.maxLength", "onlyTurning", "turnSpeed"});
      s.when().add(new String[]{"firstPerson", "thirdPerson", "inMenus", "blurHand", "toggleKey"});
      s.advanced().add(new String[]{"*.quality", "trailLook", "naturalLight", "matchRefresh", "precision", "debug"});
      p.notSwitch("style");
      return p;
   }

   private static Plan waterMark() {
      Plan p = new Plan();
      Plan.PlanPage s = p.main;
      s.main().add(new String[]{"preview", "positionPicker", "offsetX", "offsetY", "scale"});
      s.colors().add(new String[]{"opacity", "outline", "glass", "glint", "fvColor", "accent"});
      s.feature("Modules").add(new String[]{"modules", "panelColumns", "panelLiveValues"});
      s.motion().add(new String[]{"speed", "rolling"});
      s.when().add(new String[]{"hideF3", "hideTab", "showInMenus"});
      s.feature("Notifications").add(new String[]{"notifications"});
      s.feature("Sounds").add(new String[]{"islandSounds"});
      s.advanced().add(new String[]{"fontSize", "shadow", "icons", "separators", "gradients"});
      p.sub("module:fv", "HAUN’T VISUALS", "The FV logo\'s effect.").main().add(new String[]{"fv.*"});
      p.sub("module:user", "Username", "Your name, with or without your head.").main().add(new String[]{"user.*"});
      p.sub("module:server", "Server IP", "The server\'s address, or a label of your own.").main().add(new String[]{"server.*"});
      p.sub("module:fps", "FPS", "Frames per second.").main().add(new String[]{"fps.*"});
      p.sub("module:ping", "Ping", "Your ping to the server.").main().add(new String[]{"ping.*"});
      p.sub("module:bps", "BPS", "How fast you move, in blocks per second.").main().add(new String[]{"bps.*"});
      p.sub("module:xyz", "XYZ", "Your coordinates.").main().add(new String[]{"xyz.*"});
      p.sub("module:system", "CPU & GPU", "How busy your computer is.").main().add(new String[]{"system.*"});
      p.sub("module:time", "Local Time", "Your clock.").main().add(new String[]{"time.*"});
      p.sub("module:weather", "Local Weather", "The weather where you are, or the game\'s.").main().add(new String[]{"weather.*", "@Weather"});
      p.sub("module:text", "Custom Text", "A text of your own.").main().add(new String[]{"text.*"});
      p.sub("module:audio", "Audio", "What\'s playing on your computer, with controls.").main().add(new String[]{"@Audio"});
      Plan.PlanPage discord = p.sub("module:discord", "Discord", "Your voice call and DM notifications, through Discord\'s app.");
      discord.main().add(new String[]{"@Discord"});
      discord.feature("Notifications").add(new String[]{"@Discord Notifications"});
      p.label("scale", "Size").label("speed", "Speed").label("opacity", "Background Opacity");
      p.data("item.*", "a module\'s switch, in the modules list").data("order", "the modules\' order, kept by the modules list").data("position", "kept by the position picker");
      return p;
   }

   private static Plan ui() {
      Plan p = new Plan();
      Plan.PlanPage crosshair = p.tab("crosshair", "Crosshair", "Your crosshair: its shape, color and how it reacts.");
      crosshair.main().add(new String[]{"crosshair.enabled", "crosshair.preview", "crosshair.show", "crosshair.style", "crosshair.profileName", "crosshair.profilesView"});
      crosshair.colors().add(new String[]{"crosshair.color", "crosshair.shooterColors", "crosshair.outline", "crosshair.glow"});
      crosshair.size().add(new String[]{"crosshair.size", "crosshair.thickness", "crosshair.gap", "crosshair.opacity", "crosshair.dot", "crosshair.sizeMode", "crosshair.pixelPerfect", "crosshair.blend"});
      crosshair.motion().add(new String[]{"crosshair.rotation", "crosshair.spinSpeed", "crosshair.spinDirection", "crosshair.pulseAmount", "crosshair.pulseSpeed"});
      crosshair.when().add(new String[]{"@Situational Crosshairs", "crosshair.thirdPerson", "crosshair.f3"});
      crosshair.options().add(new String[]{"crosshair.segments", "crosshair.orbiters", ">fps"});
      crosshair.feature("Hit markers").add(new String[]{"@Hit Markers"});
      crosshair.feature("Reactions").add(new String[]{"@Reactions", "*"});
      Plan.PlanPage fps = p.sub("fps", "FPS Builder", "Build the crosshair from lines: inner and outer lines, their sides, and a split.", crosshair);
      fps.feature("Inner lines").add(new String[]{"@FPS Builder · Inner Lines"});
      fps.feature("Outer lines").add(new String[]{"@FPS Builder · Outer Lines"});
      fps.feature("Lines").add(new String[]{"@FPS Builder · Lines"});
      fps.feature("Split").add(new String[]{"@FPS Builder · Split"});
      Plan.PlanPage scoreboard = p.tab("scoreboard", "Scoreboard", "The sidebar scoreboard: its panel, title and lines.");
      scoreboard.main().add(new String[]{"scoreboard.enabled", "scoreboard.preview", "scoreboard.style"});
      scoreboard.colors().add(new String[]{"scoreboard.background", "scoreboard.look", "scoreboard.numbers"});
      scoreboard.size().add(new String[]{"scoreboard.scale", "scoreboard.padding", "scoreboard.spacing"});
      scoreboard.feature("Title").add(new String[]{"@Title"});
      scoreboard.feature("Hide lines").add(new String[]{"@Hide Lines"});
      scoreboard.extras().add(new String[]{"scoreboard.font", "scoreboard.shadow", "scoreboard.align", "scoreboard.animate", "*"});
      Plan.PlanPage tab = p.tab("tab", "Tab", "The player list: its panel, heads, ping and scores.");
      tab.main().add(new String[]{"tab.enabled", "tab.preview", "tab.style"});
      tab.colors().add(new String[]{"tab.background", "tab.look", "tab.alternate", "tab.highlightMe"});
      tab.feature("Player heads").add(new String[]{"@Player Heads"});
      tab.extras().add(new String[]{"tab.font", "tab.shadow", "tab.scores", "tab.headerFooter", "tab.ping", "tab.animate", "*"});
      Plan.PlanPage chat = p.tab("chat", "Chat", "The chat: its box, typing bar, size and messages.");
      chat.main().add(new String[]{"chat.enabled", "chat.preview", "chat.style"});
      chat.colors().add(new String[]{"chat.background", "chat.look", "chat.typingBar", "chat.scrollbar"});
      chat.size().add(new String[]{"chat.width", "chat.heightClosed", "chat.heightOpen", "chat.scale", "chat.spacing"});
      chat.extras().add(new String[]{"chat.timestamps", "chat.stack", "chat.mentions", "chat.newPill", "chat.font", "chat.shadow", "chat.animate", "*"});
      Plan.PlanPage nametag = p.tab("nametag", "Nametag", "Names over players: your name\'s color, their backgrounds and font.");
      nametag.main().add(new String[]{"nametag.enabled", "nametag.preview"});
      nametag.colors().add(new String[]{"nametag.nameColor", "nametag.shimmer", "nametag.alsoTabChat", "nametag.glow"});
      nametag.when().add(new String[]{"nametag.showOwn", "nametag.holograms"});
      nametag.feature("Background").add(new String[]{"@Background"});
      nametag.extras().add(new String[]{"nametag.size", "nametag.font", "nametag.shadow", "*"});
      Plan.PlanPage itemHud = p.tab("itemhud", "Item HUD", "The hotbar, hearts, hunger, armor and XP.");
      itemHud.main().add(new String[]{"itemhud.enabled", "itemhud.preview", "itemhud.style", "itemhud.layout"});
      itemHud.colors().add(new String[]{"itemhud.opacity", "itemhud.look", "itemhud.glint", "itemhud.heartColor", "itemhud.foodColor", "itemhud.armorColor", "itemhud.airColor", "itemhud.selectionGlow", "itemhud.dividers"});
      itemHud.size().add(new String[]{"itemhud.scale", "itemhud.barsScale"});
      itemHud.motion().add(new String[]{"itemhud.animate", "itemhud.instant", "itemhud.lift", "itemhud.shake"});
      itemHud.when().add(new String[]{"@Hide"});
      itemHud.feature("Icons").add(new String[]{"@Icons"});
      itemHud.extras().add(new String[]{"itemhud.xp", "itemhud.counts", "itemhud.durability", "itemhud.itemName", "*"});
      Plan.PlanPage look = p.tab("look", "Shared look", "Shared by every mini section: the themed look, the glint and the animations.");
      look.main().add(new String[]{"lookPreview", "glintSync", "animations", "*"});
      p.notSwitch("*.style");
      p.label("scoreboard.scale", "Size").label("animationSpeed", "Speed");
      p.data("tabs", "the row of tabs under the page\'s header").data("quick", "the page header\'s Apply mod theme to all and Back to vanilla").data("crosshair.defs", "the crosshairs themselves, edited by the controls above").data("crosshair.profiles", "the saved crosshair profiles (Profiles)").data("scoreboard.rules", "the hidden lines (Hide lines)").data("*.position", "where Move HUD put it").data("itemhud.barsPosition", "where Move HUD put the hearts and hunger").data("chat.input", "Themed Input Line from before 1.32.0 (off becomes Typing Bar: Full width)").data("chat.sizeTaken", "whether Width and the heights started from Minecraft\'s chat settings");
      return p;
   }
}
