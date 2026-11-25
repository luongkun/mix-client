package mix.client.mixui.integration.haunt.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import java.util.function.Supplier;
import mix.client.mixui.integration.haunt.gui.widget.KeyBindWidget;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

public final class KeyBindSetting extends Setting implements HasWidget {
   private final Supplier<KeyBinding> binding;

   public KeyBindSetting(String id, String name, String description, Supplier<KeyBinding> binding) {
      super(id, name, description);
      this.binding = binding;
      this.personal();
   }

   public KeyBinding binding() {
      return (KeyBinding)this.binding.get();
   }

   public int getKeyCode() {
      KeyBinding kb = (KeyBinding)this.binding.get();
      return kb == null?0:kb.getKeyCode();
   }

   public void set(int keyCode) {
      KeyBinding kb = (KeyBinding)this.binding.get();
      if(kb != null && kb.getKeyCode() != keyCode) {
         Minecraft.getMinecraft().gameSettings.setOptionKeyBinding(kb, keyCode);
         KeyBinding.resetKeyBindingArrayAndHash();
         this.fireChanged();
      }

   }

   public String keyName() {
      int code = this.getKeyCode();
      return code == 0?"None":GameSettings.getKeyDisplayString(code);
   }

   public String conflict() {
      KeyBinding kb = (KeyBinding)this.binding.get();
      int code = this.getKeyCode();
      if(kb != null && code != 0) {
         for(KeyBinding other : Minecraft.getMinecraft().gameSettings.keyBindings) {
            if(other != kb && other.getKeyCode() == code) {
               return I18n.format(other.getKeyDescription(), new Object[0]);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public Widget createWidget() {
      return new KeyBindWidget(this);
   }

   public boolean isSaved() {
      return false;
   }

   public void reset() {
      KeyBinding kb = (KeyBinding)this.binding.get();
      if(kb != null) {
         this.set(kb.getKeyCodeDefault());
      }

   }

   public JsonElement toJson() {
      return JsonNull.INSTANCE;
   }

   public void fromJson(JsonElement json) {
   }
}
