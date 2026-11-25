package mix.client.mixui.integration.haunt.module.particles;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.module.particles.LibraryImageWidget;
import mix.client.mixui.integration.haunt.module.particles.ParticlesModule;
import mix.client.mixui.integration.haunt.setting.HasWidget;
import mix.client.mixui.integration.haunt.setting.Setting;

public class LibraryImageSetting extends Setting implements HasWidget {
   private String name = "";
   private String checkedName;
   private String warnedName;
   private int checkedVersion = -1;

   public LibraryImageSetting(String id, String name, String description) {
      super(id, name, description);
   }

   public String get() {
      return this.name;
   }

   public void set(String image) {
      String v = image == null?"":image;
      if(!v.equals(this.name)) {
         this.name = v;
         this.fireChanged();
      }

   }

   static ParticlesModule particles() {
      Module m = FuriahsVisuals.instance == null?null:FuriahsVisuals.instance.getModules().get("particles");
      return m instanceof ParticlesModule?(ParticlesModule)m:null;
   }

   public int texture() {
      ParticlesModule p = particles();
      if(p != null && !this.name.isEmpty()) {
         p.ensureLibrary();
         return p.imageTexture(this.name);
      } else {
         return -1;
      }
   }

   public String warnedForTest() {
      return this.warnedName;
   }

   public void warnIfMissing(String where, String instead) {
      ParticlesModule p = particles();
      if(p != null && !this.name.isEmpty() && !this.name.equals(this.warnedName)) {
         int version = p.libraryVersion();
         if(!this.name.equals(this.checkedName) || version != this.checkedVersion) {
            this.checkedName = this.name;
            this.checkedVersion = version;
            p.ensureLibrary();
            if(!p.imageNames().contains(this.name)) {
               this.warnedName = this.name;
               FuriahsVisuals.chat(where + ": the image §f" + this.name + "§7 isn\'t in the Particles section\'s image library any more; " + instead + ".");
            }
         }
      }

   }

   public Widget createWidget() {
      return new LibraryImageWidget(this);
   }

   public void reset() {
      this.set("");
   }

   public JsonElement toJson() {
      return new JsonPrimitive(this.name);
   }

   public void fromJson(JsonElement json) {
      if(json != null && json.isJsonPrimitive()) {
         this.set(json.getAsString());
      }

   }
}
