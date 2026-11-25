package mix.client.mixui.integration.haunt.module.weapons;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import mix.client.mixui.integration.haunt.asset.AssetSource;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.module.weapons.Clip;
import mix.client.mixui.integration.haunt.module.weapons.WeaponLibrary;

final class ClipLibrary {
   static final String ROOT = "weapons/animations/";
   private final Map<String, Clip> clips = new LinkedHashMap();
   private final List<String> problems = new ArrayList();
   private final File overrides;

   ClipLibrary(File overrides) {
      this.overrides = overrides;
      this.reload();
   }

   int reload() {
      this.clips.clear();
      this.problems.clear();
      JsonObject index = WeaponLibrary.readJson("weapons/animations/index.json");
      if(index != null && index.has("files")) {
         for(JsonElement f : index.getAsJsonArray("files")) {
            String file = f.getAsString();
            this.read(file, WeaponLibrary.readJson("weapons/animations/" + file));
         }

         Object[] own = this.overrides == null?null:this.overrides.listFiles();
         if(own != null) {
            Arrays.sort(own);

            for(Object f : own) {
               if(((File)f).isFile() && ((File)f).getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
                  try {
                     InputStreamReader r = new InputStreamReader(new FileInputStream((File)f), StandardCharsets.UTF_8);
                     Throwable var8 = null;

                     try {
                        JsonElement e = (new JsonParser()).parse(r);
                        this.read(((File)f).getName(), e != null && e.isJsonObject()?e.getAsJsonObject():null);
                     } catch (Throwable var18) {
                        var8 = var18;
                        throw var18;
                     } finally {
                        if(r != null) {
                           if(var8 != null) {
                              try {
                                 r.close();
                              } catch (Throwable var17) {
                                 var8.addSuppressed(var17);
                              }
                           } else {
                              r.close();
                           }
                        }

                     }
                  } catch (IOException | RuntimeException var20) {
                     this.problems.add(((File)f).getName() + " can\'t be read (" + var20.getMessage() + ")");
                  }
               }
            }
         }

         return this.clips.size();
      } else {
         this.problems.add("animations/index.json is missing");
         if(AssetSource.ready()) {
            FuriahsVisuals.LOG.warn("Weapon animations: the index is missing from the mod\'s files");
         }

         return 0;
      }
   }

   private void read(String file, JsonObject o) {
      if(o != null && o.has("clips") && o.get("clips").isJsonObject()) {
         for(Entry e : o.getAsJsonObject("clips").entrySet()) {
            try {
               Clip.Ease.takeUnknown();
               this.clips.put((String)e.getKey(), new Clip((String)e.getKey(), ((JsonElement)e.getValue()).getAsJsonObject()));
               List<String> eases = Clip.Ease.takeUnknown();
               if(!eases.isEmpty()) {
                  this.problems.add(file + ": clip " + (String)e.getKey() + " names easings that don\'t exist " + eases + " (inOutSine used)");
               }
            } catch (RuntimeException var6) {
               this.problems.add(file + ": clip " + (String)e.getKey() + " (" + var6.getMessage() + ")");
               FuriahsVisuals.LOG.warn("Weapon animation {} in {} is invalid", new Object[]{e.getKey(), file, var6});
            }
         }
      } else {
         this.problems.add(file + " is missing or not a clip file");
      }

   }

   Clip get(String name) {
      return name == null?null:(Clip)this.clips.get(name);
   }

   boolean has(String name) {
      return this.clips.containsKey(name);
   }

   List<String> names() {
      return new ArrayList(this.clips.keySet());
   }

   List<String> problems() {
      return Collections.unmodifiableList(this.problems);
   }

   Clip find(String suffix, String... prefixes) {
      for(String p : prefixes) {
         Clip c;
         if(p != null && (c = (Clip)this.clips.get(p + "_" + suffix)) != null) {
            return c;
         }
      }

      return null;
   }

   List<Clip> swings(String prefix) {
      ArrayList<Clip> out = new ArrayList();

      for(Entry<String, Clip> e : this.clips.entrySet()) {
         if(((String)e.getKey()).startsWith(prefix + "_swing_")) {
            out.add(e.getValue());
         }
      }

      return out;
   }
}
