package mix.client.mixui.integration.haunt.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.core.ModuleManager;
import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.setting.Setting;

public final class ConfigManager {
   private static final int VERSION = 3;
   private static final long SAVE_DELAY_MS = 500L;
   private final File file;
   private final ModuleManager modules;
   private final Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
   private boolean loading;
   private boolean dirty;
   private long lastChange;
   private int changeCount;
   private int migrateFrom;
   private boolean renamed;
   private boolean reshaped;
   private final List<Setting> general = new ArrayList();
   private final List<Setting> notInPresets = new ArrayList();
   private Runnable firstStart;
   private static int reading;
   private long lastBackup;

   public ConfigManager(File file, ModuleManager modules) {
      this.file = file;
      this.modules = modules;
   }

   public void addGeneral(Setting setting) {
      this.general.add(setting);
   }

   public void addGeneralNotInPresets(Setting setting) {
      this.general.add(setting);
      this.notInPresets.add(setting);
   }

   public void onFirstStart(Runnable firstStart) {
      this.firstStart = firstStart;
   }

   public List<Setting> presetGeneral() {
      ArrayList<Setting> out = new ArrayList();

      for(Setting s : this.general) {
         if(!this.notInPresets.contains(s)) {
            out.add(s);
         }
      }

      return out;
   }

   public void attachListeners() {
      Runnable listener = new Runnable() {
         public void run() {
            ConfigManager.this.markDirty();
         }
      };

      for(Module m : this.modules.getModules()) {
         for(Setting s : m.getSettings()) {
            s.addListener(listener);
         }
      }

      for(Setting s : this.general) {
         s.addListener(listener);
      }

   }

   public void markDirty() {
      ++this.changeCount;
      if(!this.loading) {
         this.dirty = true;
         this.lastChange = System.currentTimeMillis();
      }

   }

   public int changeCount() {
      return this.changeCount;
   }

   public boolean isLoading() {
      return this.loading;
   }

   public static boolean isReadingSettings() {
      return reading > 0;
   }

   public void tick() {
      if(this.dirty && System.currentTimeMillis() - this.lastChange >= 500L) {
         this.save();
         if(System.currentTimeMillis() - this.lastBackup > 900000L) {
            this.backup("your latest changes");
         }
      }

   }

   public void saveIfDirty() {
      if(this.dirty) {
         this.save();
      }

   }

   public void load() {
      if(!this.file.isFile() && !this.restoreNewestBackup("the settings file was missing")) {
         FuriahsVisuals.LOG.info("No config at {}, using defaults", new Object[]{this.file});
         if(this.firstStart != null) {
            this.loading = true;
            ++reading;

            try {
               this.firstStart.run();
            } finally {
               this.loading = false;
               --reading;
            }
         }

         this.save();
      } else {
         boolean ok = this.parseInto(this.file);
         if(!ok) {
            File broken = new File(this.file.getParentFile(), this.file.getName() + ".broken-" + stamp());

            try {
               Files.copy(this.file.toPath(), broken.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
            } catch (IOException var15) {
               ;
            }

            FuriahsVisuals.LOG.error("Could not read {}; it was kept as {}", new Object[]{this.file, broken.getName()});
            if(this.restoreNewestBackup("the settings file could not be read")) {
               ok = this.parseInto(this.file);
            }

            if(!ok) {
               this.migrateFrom = 0;
               FuriahsVisuals.LOG.error("No readable backup either; using defaults for what could not be read");
               return;
            }
         }

         this.backup("start of the session");
         boolean wasReshaped = this.reshaped;
         this.reshaped = false;
         if(wasReshaped && this.migrateFrom == 0 && !this.renamed) {
            File backup2 = new File(this.file.getParentFile(), this.file.getName() + ".before-reshape");

            try {
               Files.copy(this.file.toPath(), backup2.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
            } catch (IOException var14) {
               FuriahsVisuals.LOG.warn("Could not keep a copy of the old config as {}", new Object[]{backup2.getName()});
            }

            this.save();
         }

         boolean wasRenamed = this.renamed;
         this.renamed = false;
         if(wasRenamed && this.migrateFrom == 0) {
            File backup = new File(this.file.getParentFile(), this.file.getName() + ".before-rename");

            try {
               Files.copy(this.file.toPath(), backup.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
            } catch (IOException var13) {
               FuriahsVisuals.LOG.warn("Could not keep a copy of the old config as {}", new Object[]{backup.getName()});
            }

            this.save();
         }

         if(this.migrateFrom > 0) {
            File backup = new File(this.file.getParentFile(), this.file.getName() + ".v" + this.migrateFrom);

            try {
               Files.copy(this.file.toPath(), backup.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
            } catch (IOException var12) {
               FuriahsVisuals.LOG.warn("Could not keep a copy of the old config as {}", new Object[]{backup.getName()});
            }

            FuriahsVisuals.LOG.info("Migrated config from version {} to {} (old file kept as {}); every setting you had was kept and only new options were added", new Object[]{Integer.valueOf(this.migrateFrom), Integer.valueOf(3), backup.getName()});
            this.migrateFrom = 0;
            this.save();
         }

      }
   }

   private boolean parseInto(File source) {
      this.loading = true;
      ++reading;

      boolean var29;
      try {
         InputStreamReader reader = new InputStreamReader(Files.newInputStream(source.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);

         try {
            JsonElement root = (new JsonParser()).parse(reader);
            if(root == null || !root.isJsonObject()) {
               throw new IOException("root is not a JSON object");
            }

            int version = root.getAsJsonObject().has("version") && root.getAsJsonObject().get("version").isJsonPrimitive()?root.getAsJsonObject().get("version").getAsInt():1;
            if(version < 3) {
               this.migrateFrom = version;
            }

            JsonObject generalJson = root.getAsJsonObject().has("general") && root.getAsJsonObject().get("general").isJsonObject()?root.getAsJsonObject().getAsJsonObject("general"):new JsonObject();

            for(Setting s : this.general) {
               if(generalJson.has(s.getId())) {
                  try {
                     s.fromJson(generalJson.get(s.getId()));
                  } catch (RuntimeException var22) {
                     FuriahsVisuals.LOG.warn("Ignoring invalid value for general.{}", new Object[]{s.getId()});
                  }
               }
            }

            JsonObject modulesJson = root.getAsJsonObject().has("modules") && root.getAsJsonObject().get("modules").isJsonObject()?root.getAsJsonObject().getAsJsonObject("modules"):new JsonObject();

            for(Module m : this.modules.getModules()) {
               JsonObject moduleJson = section(modulesJson, m);
               if(moduleJson != null) {
                  if(!modulesJson.has(m.getId())) {
                     this.renamed = true;
                     FuriahsVisuals.LOG.info("Config: the section saved as \'{}\' is now \'{}\' ({})", new Object[]{legacyKey(modulesJson, m), m.getId(), m.getName()});
                  }

                  for(Setting sx : m.getSettings()) {
                     if(sx.isSaved() && moduleJson.has(sx.getId())) {
                        try {
                           sx.fromJson(moduleJson.get(sx.getId()));
                        } catch (RuntimeException var21) {
                           FuriahsVisuals.LOG.warn("Ignoring invalid value for {}.{}", new Object[]{m.getId(), sx.getId()});
                        }
                     }
                  }

                  if(carryOver(m, moduleJson)) {
                     this.reshaped = true;
                     FuriahsVisuals.LOG.info("Config: carried the old settings of {} over to their new places", new Object[]{m.getName()});
                  }
               }
            }

            FuriahsVisuals.LOG.info("Loaded config from {}", new Object[]{source});
            var29 = true;
         } catch (Throwable var23) {
            try {
               reader.close();
            } catch (Throwable var20) {
               var23.addSuppressed(var20);
            }

            throw var23;
         }

         reader.close();
      } catch (Exception var24) {
         FuriahsVisuals.LOG.error("Could not read {}", new Object[]{source, var24});
         boolean root = false;
         return root;
      } finally {
         this.loading = false;
         --reading;
      }

      return var29;
   }

   private static String stamp() {
      return (new SimpleDateFormat("yyyyMMdd-HHmmss")).format(new Date());
   }

   private File backupDir() {
      File dir = this.file.getAbsoluteFile().getParentFile();
      return new File(dir, "hauntvisuals" + File.separator + "backups");
   }

   public void backup(String why) {
      if(this.file.isFile()) {
         try {
            File dir = this.backupDir();
            if(!dir.isDirectory() && !dir.mkdirs()) {
               return;
            }

            File target = new File(dir, "settings-" + stamp() + ".json");
            Files.copy(this.file.toPath(), target.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
            this.lastBackup = System.currentTimeMillis();
            File[] all = dir.listFiles();
            if(all != null && all.length > 20) {
               Arrays.sort(all, new Comparator<File>() {
                  public int compare(File a, File b) {
                     return a.getName().compareTo(b.getName());
                  }
               });

               for(int i = 0; i < all.length - 20; ++i) {
                  if(all[i].getName().startsWith("settings-")) {
                     all[i].delete();
                  }
               }
            }

            FuriahsVisuals.LOG.info("Config: backed up your settings ({}) as {}", new Object[]{why, target.getName()});
         } catch (IOException var6) {
            FuriahsVisuals.LOG.warn("Config: could not back up the settings ({})", new Object[]{why, var6});
         }
      }

   }

   private boolean restoreNewestBackup(String why) {
      File[] all = this.backupDir().listFiles();
      if(all != null && all.length != 0) {
         Arrays.sort(all, new Comparator<File>() {
            public int compare(File a, File b) {
               return b.getName().compareTo(a.getName());
            }
         });

         for(File f : all) {
            if(f.isFile() && f.getName().startsWith("settings-") && f.getName().endsWith(".json") && isValidJson(f)) {
               try {
                  Files.copy(f.toPath(), this.file.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                  FuriahsVisuals.LOG.warn("Config: {}, so your newest valid backup {} was put back", new Object[]{why, f.getName()});
                  return true;
               } catch (IOException var8) {
                  FuriahsVisuals.LOG.warn("Config: could not put back {}", new Object[]{f.getName(), var8});
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean isValidJson(File f) {
      try {
         InputStreamReader reader = new InputStreamReader(Files.newInputStream(f.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);

         boolean var3;
         try {
            JsonElement root = (new JsonParser()).parse(reader);
            var3 = root != null && root.isJsonObject() && root.getAsJsonObject().has("modules");
         } catch (Throwable var5) {
            try {
               reader.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         reader.close();
         return var3;
      } catch (Exception var6) {
         return false;
      }
   }

   private JsonObject build(boolean forPreset) {
      JsonObject root = new JsonObject();
      JsonObject generalJson = new JsonObject();

      for(Setting s : this.general) {
         if(!forPreset || !this.notInPresets.contains(s)) {
            generalJson.add(s.getId(), s.toJson());
         }
      }

      root.add("general", generalJson);
      JsonObject modulesJson = new JsonObject();

      for(Module m : this.modules.getModules()) {
         JsonObject moduleJson = new JsonObject();

         for(Setting sx : m.getSettings()) {
            if(sx.isSaved()) {
               moduleJson.add(sx.getId(), sx.toJson());
            }
         }

         modulesJson.add(m.getId(), moduleJson);
      }

      root.add("modules", modulesJson);
      return root;
   }

   public JsonObject snapshot() {
      return this.build(true);
   }

   public static int version() {
      return 3;
   }

   public void apply(JsonObject preset) {
      ++reading;

      try {
         JsonObject generalJson = preset.has("general") && preset.get("general").isJsonObject()?preset.getAsJsonObject("general"):new JsonObject();

         for(Setting s : this.general) {
            if(!this.notInPresets.contains(s)) {
               applyOne(s, generalJson, "general");
            }
         }

         JsonObject modulesJson = preset.has("modules") && preset.get("modules").isJsonObject()?preset.getAsJsonObject("modules"):new JsonObject();

         for(Module m : this.modules.getModules()) {
            JsonObject found = section(modulesJson, m);
            JsonObject moduleJson = found != null?found:new JsonObject();

            for(Setting sx : m.getSettings()) {
               if(sx.isSaved()) {
                  applyOne(sx, moduleJson, m.getId());
               }
            }

            carryOver(m, moduleJson);
         }
      } finally {
         --reading;
      }

      this.markDirty();
   }

   private static boolean carryOver(Module m, JsonObject moduleJson) {
      try {
         return m.afterLoad(moduleJson);
      } catch (RuntimeException var3) {
         FuriahsVisuals.LOG.warn("Could not carry the old settings of {} over", new Object[]{m.getName(), var3});
         return false;
      }
   }

   public static JsonObject section(JsonObject modulesJson, Module m) {
      if(modulesJson.has(m.getId()) && modulesJson.get(m.getId()).isJsonObject()) {
         return modulesJson.getAsJsonObject(m.getId());
      } else {
         String old = legacyKey(modulesJson, m);
         return old == null?null:modulesJson.getAsJsonObject(old);
      }
   }

   private static String legacyKey(JsonObject modulesJson, Module m) {
      for(String old : m.getLegacyIds()) {
         if(modulesJson.has(old) && modulesJson.get(old).isJsonObject()) {
            return old;
         }
      }

      return null;
   }

   private static void applyOne(Setting s, JsonObject json, String where) {
      if(json.has(s.getId())) {
         try {
            s.fromJson(json.get(s.getId()));
         } catch (RuntimeException var4) {
            FuriahsVisuals.LOG.warn("Preset: ignoring invalid value for {}.{}", new Object[]{where, s.getId()});
         }
      } else if(!s.isPersonal()) {
         s.reset();
      }

   }

   public void save() {
      this.dirty = false;
      JsonObject root = new JsonObject();
      root.addProperty("version", Integer.valueOf(3));
      JsonObject body = this.build(false);
      root.add("general", body.get("general"));
      root.add("modules", body.get("modules"));

      try {
         File dir = this.file.getAbsoluteFile().getParentFile();
         if(dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("cannot create " + dir);
         }

         File tmp = new File(dir, this.file.getName() + ".tmp");
         Files.write(tmp.toPath(), this.gson.toJson(root).getBytes(StandardCharsets.UTF_8), new OpenOption[0]);

         try {
            Files.move(tmp.toPath(), this.file.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE});
         } catch (AtomicMoveNotSupportedException var6) {
            Files.move(tmp.toPath(), this.file.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
         }
      } catch (IOException var7) {
         FuriahsVisuals.LOG.error("Could not save {}", new Object[]{this.file, var7});
      }

   }

   public File getFile() {
      return this.file;
   }
}
