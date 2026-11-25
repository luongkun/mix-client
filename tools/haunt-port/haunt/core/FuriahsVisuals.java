package mix.client.mixui.integration.haunt.core;

import java.io.File;
import java.util.Arrays;
import mix.client.mixui.integration.haunt.core.Combat;
import mix.client.mixui.integration.haunt.core.ConfigManager;
import mix.client.mixui.integration.haunt.core.EventHandler;
import mix.client.mixui.integration.haunt.core.FuriahCommand;
import mix.client.mixui.integration.haunt.core.HitConfirmer;
import mix.client.mixui.integration.haunt.core.JarvisCommand;
import mix.client.mixui.integration.haunt.core.Keybinds;
import mix.client.mixui.integration.haunt.core.ModuleManager;
import mix.client.mixui.integration.haunt.core.PresetManager;
import mix.client.mixui.integration.haunt.fx.kit.Comic;
import mix.client.mixui.integration.haunt.fx.kit.KitTextures;
import mix.client.mixui.integration.haunt.gui.FuriahScreen;
import mix.client.mixui.integration.haunt.gui.MenuMemory;
import mix.client.mixui.integration.haunt.gui.MenuPrefs;
import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.haunt.HauntMigration;
import mix.client.mixui.integration.haunt.haunt.HitReg;
import mix.client.mixui.integration.haunt.haunt.fx.HauntTextures;
import mix.client.mixui.integration.haunt.hook.Bridge;
import mix.client.mixui.integration.haunt.hook.Install;
import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.module.ambience.AmbienceModule;
import mix.client.mixui.integration.haunt.module.fps.FpsOptimizationModule;
import mix.client.mixui.integration.haunt.module.glow.GlowModule;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.misc.MiscModule;
import mix.client.mixui.integration.haunt.module.motionblur.MotionBlurModule;
import mix.client.mixui.integration.haunt.module.particles.ParticlesModule;
import mix.client.mixui.integration.haunt.module.sky.SkyModule;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.module.ui.UiModule;
import mix.client.mixui.integration.haunt.module.watermark.WaterMarkModule;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.render.material.Matcaps;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SettingDefaults;
import mix.client.mixui.integration.haunt.setting.StringMapSetting;
import mix.client.mixui.integration.haunt.setting.TextSetting;
import mix.client.mixui.integration.haunt.sound.ModSounds;
import mix.client.mixui.integration.haunt.sound.SoundSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FuriahsVisuals {
   public static final String MODID = "furiahsvisuals";
   public static final String NAME = "HAUN’T VISUALS";
   public static final String VERSION = "2.0.0";
   public static final Logger LOG = LogManager.getLogger("HAUN\'T VISUALS");
   public static FuriahsVisuals instance;
   private final ModuleManager modules = new ModuleManager();
   private final HitConfirmer hitConfirmer = new HitConfirmer();
   private ConfigManager config;
   private final ModeSetting<Theme.Style> theme = new ModeSetting("theme", "Theme", "Look of this menu.", Theme.Style.class, Theme.Style.HAUNT);
   private final ColorSetting globalColor = ColorSetting.global("globalColor", "Global Color", "The color every effect set to Global uses.", ColorSetting.Mode.GRADIENT, -2529761, -8699979, 1.0D);
   private final BooleanSetting reduceFlashing = (BooleanSetting)(new BooleanSetting("reduceFlashing", "Reduce Flashing", "No effect flashes the screen (Kill Effects\' screen pulse stays off).", false)).personal();
   private final SoundSettings sounds = new SoundSettings();
   private final TextSetting activePreset = new TextSetting("activePreset", "Active preset", "", "", 32);
   private final StringMapSetting menuMemory = new StringMapSetting("menu");
   private PresetManager presets;
   private WaterMarkModule watermark;
   private AmbienceModule ambience;
   private SkyModule sky;
   private KillEffectsModule killEffects;
   private UiModule ui;
   private FpsOptimizationModule fpsOptimization;
   private File configDir;
   private boolean openMenuPending;
   private boolean openMoveHudPending;
   private boolean running;

   public static FuriahsVisuals start(File configDir) {
      FuriahsVisuals mod;
      instance = mod = new FuriahsVisuals();
      mod.preInit(configDir);
      mod.init();
      return mod;
   }

   private void preInit(File configDir) {
      this.configDir = configDir;
      HauntMigration.run(configDir);
      this.config = new ConfigManager(new File(this.configDir, "hauntvisuals.json"), this.modules);
   }

   private void init() {
      ColorSetting.setGlobal(this.globalColor);
      this.registerModules();
      HauntTextures.warmUp();
      this.hitConfirmer.addListener(HitReg.INSTANCE);
      this.config.addGeneral(this.theme);
      this.config.addGeneral(this.globalColor);
      this.config.addGeneral(this.reduceFlashing);

      for(Setting s : this.sounds.all()) {
         if(s.isSaved()) {
            this.config.addGeneral(s);
         }
      }

      ModSounds.init(this.sounds);
      this.config.addGeneralNotInPresets(this.activePreset);
      this.config.addGeneralNotInPresets(this.menuMemory);
      MenuMemory.attach(this.menuMemory);

      for(Setting sx : MenuPrefs.all()) {
         this.config.addGeneralNotInPresets(sx);
      }

      this.presets = new PresetManager(new File(this.configDir, "hauntvisuals/presets"), this.config, this.modules, this.activePreset);

      for(Module m : this.modules.getModules()) {
         SettingDefaults.capture(m.getSettings());
      }

      SettingDefaults.capture(Arrays.asList(new Setting[]{this.theme, this.globalColor, this.reduceFlashing}));
      SettingDefaults.capture(Arrays.asList(this.sounds.all()));
      SettingDefaults.capture(MenuPrefs.all());
      this.config.onFirstStart(new Runnable() {
         public void run() {
            for(Module m : FuriahsVisuals.this.modules.getModules()) {
               if(m != FuriahsVisuals.this.watermark) {
                  m.setEnabled(false);
               }
            }

            FuriahsVisuals.LOG.info("First start: every section is off but the WaterMark");
         }
      });
      this.config.load();
      this.config.attachListeners();
      this.watermark.connect(this.config, this.presets);
      Keybinds.register(this.modules);
      Bridge.attach(new EventHandler());
      ClientCommandHandler.instance.registerCommand(new FuriahCommand());
      ClientCommandHandler.instance.registerCommand(new JarvisCommand());
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
         public void run() {
            FuriahsVisuals.this.config.saveIfDirty();
            FuriahsVisuals.this.watermark.shutdown();
         }
      }, "HAUN\'T VISUALS config save"));
      Matcaps.prefetch();
      Comic.warmUp();
      KitTextures.warmUp();
   }

   public void resume() {
      this.running = true;
      Install.install();
   }

   public void pause() {
      this.running = false;
      Install.uninstall();

      try {
         this.config.saveIfDirty();
      } catch (RuntimeException var2) {
         LOG.error("Could not save the settings", var2);
      }

   }

   public boolean isRunning() {
      return this.running;
   }

   private void registerModules() {
      this.modules.register(new TargetAuraModule());
      this.modules.register(new HitEffectModule());
      this.killEffects = new KillEffectsModule();
      this.modules.register(this.killEffects);
      this.modules.register(new LandingImpactModule());
      this.modules.register(new TrailsModule());
      this.modules.register(new WeaponsModule());
      this.modules.register(new GlowModule());
      this.modules.register(new ParticlesModule());
      this.ambience = new AmbienceModule();
      this.modules.register(this.ambience);
      this.sky = new SkyModule();
      this.modules.register(this.sky);
      this.modules.register(new MotionBlurModule());
      this.watermark = new WaterMarkModule();
      this.modules.register(this.watermark);
      this.ui = new UiModule();
      this.modules.register(this.ui);
      this.fpsOptimization = new FpsOptimizationModule();
      this.modules.register(this.fpsOptimization);
      this.modules.register(new MiscModule());
   }

   public WaterMarkModule getWaterMark() {
      return this.watermark;
   }

   public FpsOptimizationModule getFpsOptimization() {
      return this.fpsOptimization;
   }

   public UiModule getUi() {
      return this.ui;
   }

   public AmbienceModule getAmbience() {
      return this.ambience;
   }

   public String activePresetName() {
      return this.activePreset.get();
   }

   public SkyModule getSky() {
      return this.sky;
   }

   public KillEffectsModule getKillEffects() {
      return this.killEffects;
   }

   public BooleanSetting getReduceFlashing() {
      return this.reduceFlashing;
   }

   public boolean isReduceFlashing() {
      return this.reduceFlashing.get();
   }

   public SoundSettings getSounds() {
      return this.sounds;
   }

   public void toggleSoundMute() {
      this.sounds.mute.set(!this.sounds.mute.get());
      if(this.watermark != null) {
         this.watermark.soundMuteChanged(this.sounds.mute.get());
      }

   }

   public ModuleManager getModules() {
      return this.modules;
   }

   public HitConfirmer getHitConfirmer() {
      return this.hitConfirmer;
   }

   void onLocalAttack(EntityPlayer attacker, Entity target) {
      HitReg.INSTANCE.onAttack();
      this.hitConfirmer.attack(target);
      Combat.attacked();
      this.modules.attack(attacker, target);
   }

   public ConfigManager getConfig() {
      return this.config;
   }

   public PresetManager getPresets() {
      return this.presets;
   }

   public File getDataDir() {
      return new File(this.configDir, "hauntvisuals");
   }

   public ModeSetting<Theme.Style> getTheme() {
      return this.theme;
   }

   public ColorSetting getGlobalColor() {
      return this.globalColor;
   }

   public int setAllEffectsToGlobal() {
      int changed = 0;

      for(Module m : this.modules.getModules()) {
         for(Setting s : m.getSettings()) {
            ColorSetting c;
            if(s instanceof ColorSetting && (c = (ColorSetting)s).followsSetAll() && c.getMode() != ColorSetting.Mode.GLOBAL) {
               c.setMode(ColorSetting.Mode.GLOBAL);
               ++changed;
            }
         }
      }

      return changed;
   }

   public void openMenu() {
      Minecraft.getMinecraft().displayGuiScreen(new FuriahScreen((GuiScreen)null));
   }

   public static void chat(String message) {
      Minecraft mc = Minecraft.getMinecraft();
      if(mc.thePlayer != null) {
         mc.thePlayer.addChatMessage(new ChatComponentText("§5[§6HAUN\'T§5] §7" + message));
      }

   }

   public void openMenuNextTick() {
      this.openMenuPending = true;
   }

   void openMoveHudNextTick() {
      this.openMoveHudPending = true;
   }

   void tick() {
      if(this.openMenuPending) {
         this.openMenuPending = false;
         this.openMenu();
      }

      if(this.openMoveHudPending) {
         this.openMoveHudPending = false;
         this.ui.openMoveHud();
      }

   }
}
