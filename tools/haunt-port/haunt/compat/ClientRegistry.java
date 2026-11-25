package mix.client.mixui.integration.haunt.compat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mix.client.mixui.integration.haunt.hook.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.apache.commons.lang3.ArrayUtils;

public final class ClientRegistry {
   private static final Field ALL_KEYS = Hooks.field(KeyBinding.class, new String[]{"keybindArray", "keybindArray"});
   private static Map<String, Integer> saved;

   public static void registerKeyBinding(KeyBinding key) {
      GameSettings settings = Minecraft.getMinecraft().gameSettings;
      String name = key.getKeyDescription();
      KeyBinding[] list = settings.keyBindings;

      for(int i = list.length - 1; i >= 0; --i) {
         KeyBinding k = list[i];
         if(k != key && name.equals(k.getKeyDescription()) && key.getKeyCategory().equals(k.getKeyCategory())) {
            list = (KeyBinding[])((KeyBinding[])ArrayUtils.remove(list, i));
            forget(k);
         }
      }

      settings.keyBindings = list;
      Integer code = (Integer)saved().get("key_" + name);
      if(code != null) {
         key.setKeyCode(code.intValue());
      }

      if(!ArrayUtils.contains(settings.keyBindings, key)) {
         settings.keyBindings = (KeyBinding[])((KeyBinding[])ArrayUtils.add(settings.keyBindings, key));
      }

   }

   private static void forget(KeyBinding old) {
      if(ALL_KEYS != null) {
         try {
            ((List)ALL_KEYS.get((Object)null)).remove(old);
         } catch (Throwable var2) {
            ;
         }
      }

   }

   private static Map<String, Integer> saved() {
      if(saved != null) {
         return saved;
      } else {
         saved = new HashMap();
         File file = new File(Minecraft.getMinecraft().mcDataDir, "options.txt");
         if(!file.isFile()) {
            return saved;
         } else {
            try {
               for(String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                  int colon = line.lastIndexOf(58);
                  if(line.startsWith("key_") && colon > 4) {
                     try {
                        saved.put(line.substring(0, colon), Integer.valueOf(Integer.parseInt(line.substring(colon + 1).trim())));
                     } catch (NumberFormatException var5) {
                        ;
                     }
                  }
               }
            } catch (IOException | RuntimeException var6) {
               ;
            }

            return saved;
         }
      }
   }
}
