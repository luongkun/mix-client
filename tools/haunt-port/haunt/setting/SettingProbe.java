package mix.client.mixui.integration.haunt.setting;

import java.util.ArrayList;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class SettingProbe {
   private static final int MAX_CHOICES = 16;

   public static Object[] trials(Setting s) {
      if(s instanceof HasWidget) {
         return null;
      } else if(s instanceof BooleanSetting) {
         return new Object[]{Boolean.valueOf(!((BooleanSetting)s).get())};
      } else if(s instanceof ModeSetting) {
         ModeSetting m = (ModeSetting)s;
         if(m.getStyle() != ModeSetting.Style.CARDS && m.getValues().length <= 16) {
            ArrayList<Enum> out = new ArrayList();

            for(Enum v : m.getValues()) {
               if(v != m.get()) {
                  out.add(v);
               }
            }

            return out.toArray();
         } else {
            return null;
         }
      } else if(s instanceof SliderSetting) {
         SliderSetting sl = (SliderSetting)s;
         ArrayList<Double> out = new ArrayList();
         if(sl.get() != sl.getMin()) {
            out.add(Double.valueOf(sl.getMin()));
         }

         if(sl.get() != sl.getMax()) {
            out.add(Double.valueOf(sl.getMax()));
         }

         return out.toArray();
      } else if(!(s instanceof ColorSetting)) {
         return null;
      } else {
         ColorSetting c = (ColorSetting)s;
         ArrayList<ColorSetting.Mode> outx = new ArrayList();

         for(ColorSetting.Mode mode : ColorSetting.Mode.values()) {
            if(mode != c.getMode() && (mode != ColorSetting.Mode.GLOBAL || c.allowsGlobal())) {
               outx.add(mode);
            }
         }

         return outx.toArray();
      }
   }

   public static Object current(Setting s) {
      return s instanceof BooleanSetting?Boolean.valueOf(((BooleanSetting)s).get()):(s instanceof ModeSetting?((ModeSetting)s).get():(s instanceof SliderSetting?Double.valueOf(((SliderSetting)s).get()):(s instanceof ColorSetting?((ColorSetting)s).getMode():null)));
   }

   public static void set(Setting s, Object value) {
      if(s instanceof BooleanSetting) {
         ((BooleanSetting)s).setQuietly(((Boolean)value).booleanValue());
      } else if(s instanceof ModeSetting) {
         ((ModeSetting)s).setQuietly(value);
      } else if(s instanceof SliderSetting) {
         ((SliderSetting)s).setQuietly(((Double)value).doubleValue());
      } else if(s instanceof ColorSetting) {
         ((ColorSetting)s).setModeQuietly((ColorSetting.Mode)value);
      }

   }
}
