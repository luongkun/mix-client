package mix.client.mixui.integration.haunt.hook;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;

public final class Srg {
   private static final Map<String, List<String>> FIELDS = new HashMap();
   private static final Map<String, List<String>> METHODS = new HashMap();
   private static boolean loaded;
   private static int entries;

   private static synchronized void load() {
      if(!loaded) {
         loaded = true;

         try {
            InputStream in = Srg.class.getResourceAsStream("/furiah_srg.txt");
            Throwable var1 = null;

            try {
               if(in != null) {
                  BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

                  String line;
                  while((line = r.readLine()) != null) {
                     String[] p = line.trim().split(" ");
                     if(p.length == 4) {
                        Map<String, List<String>> target = p[0].equals("F")?FIELDS:(p[0].equals("M")?METHODS:null);
                        if(target != null) {
                           List<String> list = (List)target.get(p[1] + "/" + p[2]);
                           if(list == null) {
                              list = new ArrayList(1);
                              target.put(p[1] + "/" + p[2], list);
                           }

                           if(!list.contains(p[3])) {
                              list.add(p[3]);
                           }

                           ++entries;
                        }
                     }
                  }

                  FuriahsVisuals.LOG.info("Hooks: loaded {} MCP->SRG names", new Object[]{Integer.valueOf(entries)});
                  return;
               }

               FuriahsVisuals.LOG.warn("Hooks: furiah_srg.txt is missing from the jar; private Minecraft members are only found by their MCP names (works in runClient, NOT in a normal launcher)");
            } catch (Throwable var16) {
               var1 = var16;
               throw var16;
            } finally {
               if(in != null) {
                  if(var1 != null) {
                     try {
                        in.close();
                     } catch (Throwable var15) {
                        var1.addSuppressed(var15);
                     }
                  } else {
                     in.close();
                  }
               }

            }

         } catch (Throwable var18) {
            FuriahsVisuals.LOG.warn("Hooks: could not read furiah_srg.txt", var18);
         }
      }
   }

   public static String[] names(Class<?> owner, boolean method, String... names) {
      load();
      Set<String> out = new LinkedHashSet();

      for(String n : names) {
         out.add(n);
      }

      String base = owner.getName().replace('.', '/') + "/";
      Map<String, List<String>> table = method?METHODS:FIELDS;

      for(String n : names) {
         List<String> srg = (List)table.get(base + n);
         if(srg != null) {
            out.addAll(srg);
         }
      }

      return (String[])out.toArray(new String[0]);
   }
}
