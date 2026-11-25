package mix.client.mixui.integration.haunt.gui;

import com.google.gson.JsonParser;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.client.gui.GuiScreen;

public final class MenuClipboard {
   private static Setting source;
   private static String value;

   public static void copy(Setting s) {
      source = s;
      value = String.valueOf(s.toJson());
      if(s instanceof ColorSetting) {
         GuiScreen.setClipboardString(ColorUtil.toHex(((ColorSetting)s).getColorA()));
      } else if(s instanceof SliderSetting) {
         GuiScreen.setClipboardString(((SliderSetting)s).format());
      }

   }

   public static boolean canPaste(Setting s) {
      return value != null && source != null && s != null && source.getClass() == s.getClass()?!(s instanceof ModeSetting) || ((ModeSetting)source).getType() == ((ModeSetting)s).getType():false;
   }

   public static boolean paste(Setting s) {
      if(!canPaste(s)) {
         return false;
      } else {
         s.fromJson((new JsonParser()).parse(value));
         return true;
      }
   }

   public static String describe() {
      return source == null?null:source.getLabel();
   }
}
