package mix.client.mixui.integration.haunt.module.weapons;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.imageio.ImageIO;
import mix.client.mixui.integration.haunt.compat.OpenFolder;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.image.ImageLoader;
import mix.client.mixui.integration.haunt.module.weapons.Mat4;
import mix.client.mixui.integration.haunt.module.weapons.ObjModel;
import mix.client.mixui.integration.haunt.module.weapons.PixelImport;
import mix.client.mixui.integration.haunt.module.weapons.WeaponLibrary;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.setting.Displayable;
import net.minecraft.client.Minecraft;

final class WeaponImport {
   private static volatile boolean picking;
   static volatile String lastId;
   private static volatile boolean reloading;

   static boolean isPicking() {
      return picking;
   }

   static boolean isReloading() {
      return reloading;
   }

   static void reload(WeaponsModule module) {
      reload(module, false);
   }

   static void reload(final WeaponsModule module, final boolean quiet) {
      if(!reloading) {
         reloading = true;
         Thread t = new Thread(new Runnable() {
            public void run() {
               final Map<String, String> problems;
               try {
                  problems = PixelImport.refreshAll(module.library.importFolder(), module.autoPixelate.get(), new ArrayList());
               } finally {
                  WeaponImport.reloading = false;
               }

               Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                  public void run() {
                     module.library.reloadImported();
                     if(!quiet || !problems.isEmpty()) {
                        int n = module.library.imported().size();
                        StringBuilder m = new StringBuilder(quiet?"":"Reloaded the weapons folder: " + n + (n == 1?" imported weapon.":" imported weapons."));
                        WeaponImport.sayProblems(module, m, problems);
                     }

                  }
               });
            }
         }, "HAUN’T VISUALS weapon reload");
         t.setDaemon(true);
         t.start();
      }

   }

   private static void sayProblems(WeaponsModule module, StringBuilder m, Map<String, String> problems) {
      for(Entry<String, String> e : problems.entrySet()) {
         m.append(m.length() == 0?"":" ").append('\"').append((String)e.getKey()).append("\": ").append((String)e.getValue()).append('.');
      }

      module.sayImport(m.toString(), !problems.isEmpty());
   }

   static void pixelateMissing(final WeaponsModule module) {
      if(!reloading) {
         reloading = true;
         Thread t = new Thread(new Runnable() {
            public void run() {
               ArrayList<String> made = new ArrayList();

               final Map<String, String> problems;
               try {
                  problems = PixelImport.refreshAll(module.library.importFolder(), true, made);
               } finally {
                  WeaponImport.reloading = false;
               }

               if(!made.isEmpty() || !problems.isEmpty()) {
                  Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                     public void run() {
                        module.library.reloadImported();
                        if(!problems.isEmpty()) {
                           WeaponImport.sayProblems(module, new StringBuilder(), problems);
                        }

                     }
                  });
               }

            }
         }, "HAUN’T VISUALS weapon pixelate");
         t.setDaemon(true);
         t.start();
      }

   }

   static void pick(final WeaponsModule module) {
      if(!picking) {
         if(!Minecraft.isRunningOnMac && !GraphicsEnvironment.isHeadless()) {
            picking = true;
            Thread t = new Thread(new Runnable() {
               public void run() {
                  Frame owner = null;

                  try {
                     owner = new Frame();
                     owner.setUndecorated(true);
                     owner.setAlwaysOnTop(true);
                     owner.setSize(1, 1);
                     owner.setLocationRelativeTo((Component)null);
                     owner.setVisible(true);
                     FileDialog dialog = new FileDialog(owner, "Import a weapon (OBJ, Blockbench, item model or MagicaVoxel; an item model\'s textures too)", 0);
                     dialog.setMultipleMode(true);
                     dialog.setFilenameFilter((dir, name) -> {
                        return WeaponImport.kind(name) != null || name.toLowerCase(Locale.ROOT).endsWith(".png");
                     });
                     if(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
                        dialog.setFile("*.obj;*.bbmodel;*.json;*.vox;*.png");
                     }

                     dialog.setVisible(true);
                     File[] files = dialog.getFiles();
                     dialog.dispose();
                     if(files != null && files.length > 0) {
                        WeaponImport.importFiles(module, files);
                     }
                  } catch (Throwable var7) {
                     FuriahsVisuals.LOG.warn("Weapons: the file picker failed, opening the folder instead", var7);
                     WeaponImport.openFolder(module);
                     module.sayImport("The file picker could not open: make a folder for your weapon in the one that just opened (see the README), then press Reload.", true);
                  } finally {
                     if(owner != null) {
                        owner.dispose();
                     }

                     WeaponImport.picking = false;
                  }

               }
            }, "HAUN’T VISUALS weapon import");
            t.setDaemon(true);
            t.start();
         } else {
            openFolder(module);
            module.sayImport("The file picker cannot open next to the game here: make a folder for your weapon in the one that just opened (model.obj, its texture and a weapon.json: see the README), then press Reload.", false);
         }
      }

   }

   static String kind(String name) {
      String n = name.toLowerCase(Locale.ROOT);

      for(String k : new String[]{"obj", "bbmodel", "json", "vox"}) {
         if(n.endsWith("." + k)) {
            return k;
         }
      }

      return null;
   }

   static void importFiles(WeaponsModule module, File[] files) {
      File model = null;
      ArrayList<File> images = new ArrayList();

      for(File f : files) {
         String k = kind(f.getName());
         if(k == null) {
            if(f.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
               images.add(f);
            }
         } else if(model == null || "json".equals(kind(model.getName())) && !"json".equals(k)) {
            model = f;
         }
      }

      if(model == null) {
         module.sayImport("Pick a model: an OBJ (.obj), a Blockbench model (.bbmodel), an item model (.json, with its textures) or a MagicaVoxel model (.vox).", true);
      } else if("obj".equals(kind(model.getName()))) {
         importFile(module, model);
      } else {
         importPixel(module, model, images);
      }

   }

   static void importPixel(final WeaponsModule module, File source, List<File> images) {
      String file = source.getName();
      String k = kind(file);
      ArrayList<String> notes = new ArrayList();
      LinkedHashMap<String, byte[]> picked = new LinkedHashMap();

      PixelImport.Grid grid;
      try {
         byte[] bytes = read(source, 16777216L);

         for(File img : images) {
            picked.put(img.getName(), read(img, 4194304L));
         }

         grid = "vox".equals(k)?PixelImport.readVox(bytes, notes):("bbmodel".equals(k)?PixelImport.readBlockbench(bytes, picked, notes):PixelImport.readItemModel(bytes, picked, notes));
      } catch (PixelImport.ImportException var16) {
         module.sayImport("\"" + file + "\": " + var16.getMessage() + ".", true);
         return;
      } catch (IOException var17) {
         module.sayImport("\"" + file + "\" can\'t be read (" + var17.getMessage() + ").", true);
         return;
      } catch (RuntimeException var18) {
         FuriahsVisuals.LOG.warn("Weapons: {} could not be imported", new Object[]{source, var18});
         module.sayImport("\"" + file + "\" can\'t be read: it may be broken, or from a newer version of its program.", true);
         return;
      }

      final String name = displayName(file);
      File dir = freeFolder(module.library.importFolder(), folderName(name));
      int voxels = grid.count();

      try {
         Files.createDirectories(dir.toPath(), new FileAttribute[0]);
         String kept = "pixel." + k;
         Files.copy(source.toPath(), (new File(dir, kept)).toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});

         for(Entry e : picked.entrySet()) {
            Files.write((new File(dir, folderName(((String)e.getKey()).replaceAll("(?i)\\.png$", "")) + ".png")).toPath(), (byte[])((byte[])e.getValue()), new OpenOption[0]);
         }

         float[] bounds = PixelImport.write(grid, dir, grid.unit, 80, PixelImport.signature(new File(dir, kept)));
         JsonObject o = newPixelJson(name, file, grid, bounds);
         write(new File(dir, "weapon.json"), o);
      } catch (PixelImport.ImportException var14) {
         module.sayImport("\"" + file + "\": " + var14.getMessage() + ".", true);
         return;
      } catch (IOException | RuntimeException var15) {
         FuriahsVisuals.LOG.warn("Weapons: could not import {}", new Object[]{source, var15});
         module.sayImport("Could not make \"" + file + "\" into a weapon (" + var15.getMessage() + ").", true);
         return;
      }

      final String id;
      lastId = id = WeaponLibrary.importId(dir);
      final String count = String.format(Locale.ROOT, "%,d", new Object[]{Integer.valueOf(voxels)});
      final String extra = notes.isEmpty()?"":" Note: " + String.join("; ", notes) + ".";
      Minecraft.getMinecraft().addScheduledTask(new Runnable() {
         public void run() {
            module.library.reloadImported();
            module.select(id);
            module.sayImport("Imported \"" + name + "\" (" + count + " voxels, a pixel weapon). Set its grip under Grip Setup, then Save." + extra, false);
         }
      });
   }

   static byte[] read(File f, long max) throws IOException {
      long n = f.length();
      if(n > max) {
         throw new IOException("it is bigger than " + max / 1048576L + " MB");
      } else {
         return Files.readAllBytes(f.toPath());
      }
   }

   static JsonObject newPixelJson(String name, String file, PixelImport.Grid grid, float[] bounds) {
      JsonObject o = new JsonObject();
      o.addProperty("name", name);
      o.addProperty("category", "imported");
      o.addProperty("description", "Imported from " + file);
      float[] line = PixelImport.handleLine(grid);
      int axis = Math.abs(line[5]) > 0.9F?2:(Math.abs(line[3]) > 0.9F?0:1);
      boolean diagonal = Math.abs(line[3 + axis]) < 0.9F;
      int a1 = (axis + 1) % 3;
      int a2 = (axis + 2) % 3;
      int edge = diagonal?0:(bounds[3 + a1] - bounds[a1] >= bounds[3 + a2] - bounds[a2]?a1:a2);
      int third = 3 - axis - edge;
      float roll = diagonal?(float)Math.toDegrees(Math.atan2((double)line[3], (double)line[4])):(line[3 + axis] < 0.0F?180.0F:0.0F);
      float[] size = new float[3];
      float[] mid = new float[3];

      for(int k = 0; k < 3; ++k) {
         size[k] = Math.max(1.0E-4F, bounds[3 + k] - bounds[k]);
         mid[k] = (bounds[3 + k] + bounds[k]) / 2.0F;
      }

      float along = (float)Math.round(Math.max(0.0F, Math.min(1.0F, (line[axis] - bounds[axis]) / size[axis])) * 100.0F) / 100.0F;
      float side = (float)Math.round(Math.max(-1.0F, Math.min(1.0F, (line[edge] - mid[edge]) / (size[edge] / 2.0F))) * 100.0F) / 100.0F;
      float depth = (float)Math.round(Math.max(-1.0F, Math.min(1.0F, (line[third] - mid[third]) / (size[third] / 2.0F))) * 100.0F) / 100.0F;
      JsonObject g = new JsonObject();
      g.add("min", array(new float[]{bounds[0], bounds[1], bounds[2]}));
      g.add("max", array(new float[]{bounds[3], bounds[4], bounds[5]}));
      g.addProperty("axis", Integer.valueOf(axis));
      g.addProperty("edgeAxis", Integer.valueOf(edge));
      g.addProperty("along", Float.valueOf(along));
      g.add("offset", array(new float[]{side, depth}));
      g.add("turn", array(new float[]{0.0F, 0.0F, (float)Math.round(roll)}));
      g.addProperty("size", Double.valueOf((double)Math.round((double)(bounds[3 + axis] - bounds[axis]) * 1000.0D) / 1000.0D));
      g.addProperty("hands", Integer.valueOf(1));
      g.addProperty("set", WeaponImport.AnimSet.SWORD.name().toLowerCase(Locale.ROOT));
      g.addProperty("pixel", Boolean.valueOf(true));
      o.add("import", g);
      JsonObject parts = new JsonObject();
      JsonObject pivots = new JsonObject();

      for(String p : grid.parts) {
         parts.addProperty(p, "blade".equals(p)?"blade":("guard".equals(p)?"metal":("handle".equals(p)?"handle":("accent".equals(p)?"accent":("energy".equals(p)?"energy":"fixed")))));
         float[] pv = (float[])grid.pivots.get(p);
         if(pv != null) {
            JsonObject po = new JsonObject();
            po.add("origin", array(new float[]{pv[0] * grid.unit, pv[1] * grid.unit, pv[2] * grid.unit}));
            po.add("axis", array(new float[]{0.0F, 0.0F, 1.0F}));
            pivots.add(p, po);
         }
      }

      o.add("parts", parts);
      if(!pivots.entrySet().isEmpty()) {
         o.add("pivots", pivots);
      }

      applyGrip(o);
      return o;
   }

   static void importFile(final WeaponsModule module, File obj) {
      String file = obj.getName();
      if(!file.toLowerCase(Locale.ROOT).endsWith(".obj")) {
         module.sayImport("\"" + file + "\" is not an OBJ model: export your model as Wavefront OBJ (.obj) and import that.", true);
      } else {
         ObjModel model;
         try {
            FileInputStream in = new FileInputStream(obj);
            Throwable image = null;

            try {
               model = ObjModel.read(in, 150000);
            } catch (Throwable var261) {
               image = var261;
               throw var261;
            } finally {
               if(in != null) {
                  if(image != null) {
                     try {
                        in.close();
                     } catch (Throwable var23) {
                        image.addSuppressed(var23);
                     }
                  } else {
                     in.close();
                  }
               }

            }
         } catch (IOException var28) {
            String m = var28.getMessage() != null && var28.getMessage().contains("triangles")?"\"" + file + "\" has more than " + String.format(Locale.ROOT, "%,d", new Object[]{Integer.valueOf(150000)}) + " triangles: make it lighter (for example Blender\'s Decimate) and import it again.":"\"" + file + "\" can\'t be read (" + var28.getMessage() + ").";
            module.sayImport(m, true);
            return;
         } catch (RuntimeException var29) {
            FuriahsVisuals.LOG.warn("Weapons: {} is not a valid OBJ model", new Object[]{obj, var29});
            module.sayImport("\"" + file + "\" is not a valid OBJ model (it has lines that can\'t be read): export it again from your 3D program as Wavefront OBJ.", true);
            return;
         }

         if(model.triangles() == 0) {
            module.sayImport("\"" + file + "\" has no faces: a weapon model needs at least one polygon.", true);
         } else {
            File texture = findTexture(obj);
            ImageLoader.Image image = null;
            if(texture != null) {
               try {
                  image = ImageLoader.load(texture, 4096, false);
               } catch (ImageLoader.LoadException var251) {
                  module.sayImport("Its texture \"" + texture.getName() + "\": " + var251.getMessage(), true);
                  return;
               }
            }

            final String name = displayName(file);
            File dir = freeFolder(module.library.importFolder(), folderName(name));
            String textureName = null;

            try {
               Files.createDirectories(dir.toPath(), new FileAttribute[0]);
               Files.copy(obj.toPath(), (new File(dir, "model.obj")).toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
               if(image != null) {
                  if(image.format != ImageLoader.Format.PNG && image.format != ImageLoader.Format.JPEG) {
                     textureName = "albedo.png";
                     ImageIO.write(image.toBufferedImage(), "png", new File(dir, textureName));
                  } else {
                     textureName = image.format == ImageLoader.Format.PNG?"albedo.png":"albedo.jpg";
                     Files.copy(texture.toPath(), (new File(dir, textureName)).toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                  }
               }

               JsonObject o = newJson(name, file, model, textureName);
               write(new File(dir, "weapon.json"), o);
            } catch (IOException | RuntimeException var30) {
               FuriahsVisuals.LOG.warn("Weapons: could not import {}", new Object[]{obj, var30});
               module.sayImport("Could not copy \"" + file + "\" into the weapons folder (" + var30.getMessage() + ").", true);
               return;
            }

            final String pixelNote = "";
            if(module.autoPixelate.get()) {
               try {
                  PixelImport.pixelateObj(dir);
               } catch (RuntimeException | PixelImport.ImportException | IOException var24) {
                  pixelNote = " Its Pixelated version couldn\'t be made (" + var24.getMessage() + "): it stays HD.";
               }
            }

            final String id;
            lastId = id = WeaponLibrary.importId(dir);
            final String tris = String.format(Locale.ROOT, "%,d", new Object[]{Integer.valueOf(model.triangles())});
            final boolean textured = textureName != null;
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
               public void run() {
                  module.library.reloadImported();
                  module.select(id);
                  module.sayImport("Imported \"" + name + "\" (" + tris + " triangles" + (textured?"":", no texture found: plain grey") + "). Set its grip under Grip Setup, then Save." + pixelNote, false);
               }
            });
         }
      }

   }

   static File findTexture(File obj) {
      File folder = obj.getAbsoluteFile().getParentFile();

      try {
         BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(obj), StandardCharsets.UTF_8));
         Throwable var4 = null;

         try {
            String line;
            try {
               while((line = r.readLine()) != null) {
                  File t;
                  if((line = line.trim()).startsWith("mtllib ") && (t = mapKd(new File(folder, line.substring(7).trim()))) != null) {
                     File var6 = t;
                     return var6;
                  }
               }
            } catch (Throwable var17) {
               var4 = var17;
               throw var17;
            }
         } finally {
            if(r != null) {
               if(var4 != null) {
                  try {
                     r.close();
                  } catch (Throwable var16) {
                     var4.addSuppressed(var16);
                  }
               } else {
                  r.close();
               }
            }

         }
      } catch (IOException | RuntimeException var19) {
         ;
      }

      String base = obj.getName().substring(0, obj.getName().length() - 4);

      for(String ext : new String[]{".png", ".jpg", ".jpeg", ".PNG", ".JPG", ".JPEG"}) {
         File f = new File(folder, base + ext);
         if(f.isFile()) {
            return f;
         }
      }

      return null;
   }

   private static File mapKd(File mtl) {
      if(!mtl.isFile()) {
         return null;
      } else {
         try {
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(mtl), StandardCharsets.UTF_8));
            Throwable var2 = null;

            try {
               String line;
               while((line = r.readLine()) != null) {
                  if((line = line.trim()).startsWith("map_Kd ")) {
                     String rest;
                     String[] t;
                     for(rest = line.substring(7).trim(); rest.startsWith("-"); rest = t.length > 2?t[2]:"") {
                        t = rest.split("\\s+", 3);
                     }

                     File f = new File(rest);
                     if(!f.isAbsolute()) {
                        f = new File(mtl.getAbsoluteFile().getParentFile(), rest.replace('\\', File.separatorChar));
                     }

                     File var6 = f.isFile()?f:null;
                     return var6;
                  }
               }

               Object var21 = null;
               return (File)var21;
            } catch (Throwable var17) {
               var2 = var17;
               throw var17;
            } finally {
               if(r != null) {
                  if(var2 != null) {
                     try {
                        r.close();
                     } catch (Throwable var16) {
                        var2.addSuppressed(var16);
                     }
                  } else {
                     r.close();
                  }
               }

            }
         } catch (IOException | RuntimeException var19) {
            return null;
         }
      }
   }

   static String displayName(String file) {
      int dot = file.lastIndexOf(46);
      String base = (dot > 0?file.substring(0, dot):file).replace('_', ' ').replace('-', ' ').trim();
      StringBuilder sb = new StringBuilder();

      for(String w : base.split("\\s+")) {
         if(!w.isEmpty()) {
            if(sb.length() > 0) {
               sb.append(' ');
            }

            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
         }
      }

      return sb.length() == 0?"Imported Weapon":sb.toString();
   }

   static String folderName(String name) {
      String s = name.replaceAll("[^\\p{L}\\p{N} _-]", "").trim();
      return s.isEmpty()?"weapon":s;
   }

   static File freeFolder(File parent, String name) {
      File dir = new File(parent, name);

      for(int i = 2; dir.exists(); ++i) {
         dir = new File(parent, name + " " + i);
      }

      return dir;
   }

   static JsonObject newJson(String name, String file, ObjModel model, String texture) {
      JsonObject o = new JsonObject();
      o.addProperty("name", name);
      o.addProperty("category", "imported");
      o.addProperty("description", "Imported from " + file);
      if(texture != null) {
         JsonObject t = new JsonObject();
         t.addProperty("albedo", texture);
         o.add("textures", t);
      }

      int axis = 0;

      for(int k = 1; k < 3; ++k) {
         if(model.max[k] - model.min[k] > model.max[axis] - model.min[axis]) {
            axis = k;
         }
      }

      int a1 = (axis + 1) % 3;
      int a2 = (axis + 2) % 3;
      int edge = model.max[a1] - model.min[a1] >= model.max[a2] - model.min[a2]?a1:a2;
      JsonObject g = new JsonObject();
      g.add("min", array(new float[]{model.min[0], model.min[1], model.min[2]}));
      g.add("max", array(new float[]{model.max[0], model.max[1], model.max[2]}));
      g.addProperty("axis", Integer.valueOf(axis));
      g.addProperty("edgeAxis", Integer.valueOf(edge));
      g.addProperty("along", Double.valueOf(0.12D));
      g.add("offset", array(new float[]{0.0F, 0.0F}));
      g.add("turn", array(new float[]{0.0F, 0.0F, 0.0F}));
      g.addProperty("size", Double.valueOf(1.0D));
      g.addProperty("hands", Integer.valueOf(1));
      g.addProperty("set", WeaponImport.AnimSet.SWORD.name().toLowerCase(Locale.ROOT));
      o.add("import", g);
      applyGrip(o);
      return o;
   }

   static void applyGrip(JsonObject o) {
      JsonObject g = o.getAsJsonObject("import");
      float[] min = floats(g, "min", 3);
      float[] max = floats(g, "max", 3);
      float[] off = floats(g, "offset", 2);
      float[] turn = floats(g, "turn", 3);
      int axis = Math.max(0, Math.min(2, g.get("axis").getAsInt()));
      float along = g.get("along").getAsFloat();
      float size = Math.max(0.05F, g.get("size").getAsFloat());
      boolean two = g.get("hands").getAsInt() >= 2;
      WeaponImport.AnimSet set = WeaponImport.AnimSet.of(g.get("set").getAsString());
      int a1 = g.has("edgeAxis")?g.get("edgeAxis").getAsInt():(axis + 1) % 3;
      if(a1 == axis || a1 < 0 || a1 > 2) {
         a1 = (axis + 1) % 3;
      }

      int a2 = 3 - axis - a1;
      float length = Math.max(1.0E-4F, max[axis] - min[axis]);
      float[] pos = new float[3];
      pos[axis] = min[axis] + length * along;
      pos[a1] = (min[a1] + max[a1]) / 2.0F + off[0] * (max[a1] - min[a1]) / 2.0F;
      pos[a2] = (min[a2] + max[a2]) / 2.0F + off[1] * (max[a2] - min[a2]) / 2.0F;
      float[] up = new float[3];
      float[] side = new float[3];
      float[] out = new float[3];
      up[axis] = 1.0F;
      side[a1] = 1.0F;
      Mat4.cross(side, up, out);
      Mat4 back = (new Mat4()).rotate(-turn[0], 1.0F, 0.0F, 0.0F).rotate(-turn[1], 0.0F, 1.0F, 0.0F).rotate(-turn[2], 0.0F, 0.0F, 1.0F);
      float[] u = new float[3];
      float[] e = new float[3];
      back.direction(0.0F, 1.0F, 0.0F, u);
      back.direction(1.0F, 0.0F, 0.0F, e);
      float[] gripAxis = combine(side, up, out, u);
      float[] gripEdge = combine(side, up, out, e);
      float scale = size * (two?1.5F:1.0F) / length;
      JsonObject grips = new JsonObject();
      grips.add("right", grip(pos, gripAxis, gripEdge));
      if(two) {
         float d = 0.3F / scale;
         grips.add("left", grip(new float[]{pos[0] - gripAxis[0] * d, pos[1] - gripAxis[1] * d, pos[2] - gripAxis[2] * d}, gripAxis, gripEdge));
      }

      o.add("grips", grips);
      o.addProperty("hands", Integer.valueOf(two?2:1));
      o.addProperty("grip", two?"two_handed":"hammer");
      o.addProperty("dualWield", Boolean.valueOf(!two));
      o.addProperty("animations", set.clips);
      JsonArray inspects = new JsonArray();

      for(String s : set.inspects) {
         inspects.add(new JsonPrimitive(s));
      }

      o.add("inspects", inspects);
      o.add("bounds", array(new float[]{min[0], min[1], min[2], max[0], max[1], max[2]}));
      JsonObject fp = new JsonObject();
      fp.addProperty("scale", Float.valueOf(scale));
      o.add("firstPerson", fp);
      JsonObject tp = new JsonObject();
      tp.addProperty("scale", Float.valueOf(scale));
      o.add("thirdPerson", tp);
   }

   private static float[] combine(float[] x, float[] y, float[] z, float[] d) {
      return new float[]{x[0] * d[0] + y[0] * d[1] + z[0] * d[2], x[1] * d[0] + y[1] * d[1] + z[1] * d[2], x[2] * d[0] + y[2] * d[1] + z[2] * d[2]};
   }

   private static JsonObject grip(float[] pos, float[] axis, float[] edge) {
      JsonObject g = new JsonObject();
      g.add("pos", array(pos));
      g.add("axis", array(axis));
      g.add("edge", array(edge));
      return g;
   }

   static float[] floats(JsonObject o, String key, int n) {
      float[] out = new float[n];
      JsonElement e = o.get(key);
      if(e != null && e.isJsonArray()) {
         JsonArray a = e.getAsJsonArray();

         for(int i = 0; i < n && i < a.size(); ++i) {
            out[i] = a.get(i).getAsFloat();
         }
      }

      return out;
   }

   static JsonArray array(float... v) {
      JsonArray a = new JsonArray();

      for(float f : v) {
         a.add(new JsonPrimitive(Double.valueOf((double)Math.round((double)f * 100000.0D) / 100000.0D)));
      }

      return a;
   }

   static void write(File f, JsonObject o) throws IOException {
      OutputStreamWriter w = new OutputStreamWriter(Files.newOutputStream(f.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);
      Throwable var3 = null;

      try {
         (new GsonBuilder()).setPrettyPrinting().create().toJson(o, w);
      } catch (Throwable var12) {
         var3 = var12;
         throw var12;
      } finally {
         if(w != null) {
            if(var3 != null) {
               try {
                  w.close();
               } catch (Throwable var11) {
                  var3.addSuppressed(var11);
               }
            } else {
               w.close();
            }
         }

      }

   }

   static void openFolder(WeaponsModule module) {
      final File dir = module.library.importFolder();
      Thread t = new Thread(new Runnable() {
         public void run() {
            try {
               OpenFolder.open(dir);
            } catch (Throwable var2) {
               FuriahsVisuals.LOG.warn("Weapons: could not open {}", new Object[]{dir, var2});
            }

         }
      }, "HAUN’T VISUALS open folder");
      t.setDaemon(true);
      t.start();
   }

   static enum AnimSet implements Displayable {
      KNIFE("Knife", "Quick stabs and slashes", "knife", new String[]{"toss_catch", "flip_present", "look_over"}),
      SWORD("Sword", "Slashes and a thrust", "saber", new String[]{"look_over", "twirl", "toss_catch"}),
      HEAVY("Heavy", "Big two-handed chops and sweeps", "greatsword", new String[]{"look_over"}),
      POLEARM("Polearm", "Thrusts and wide sweeps with a long shaft", "polearm", new String[]{"look_over"});

      final String label;
      final String description;
      final String clips;
      final String[] inspects;

      private AnimSet(String label, String description, String clips, String[] inspects) {
         this.label = label;
         this.description = description;
         this.clips = clips;
         this.inspects = inspects;
      }

      public String displayName() {
         return this.label;
      }

      public String description() {
         return this.description;
      }

      static WeaponImport.AnimSet of(String s) {
         for(WeaponImport.AnimSet x : values()) {
            if(x.name().equalsIgnoreCase(s)) {
               return x;
            }
         }

         return SWORD;
      }
   }
}
