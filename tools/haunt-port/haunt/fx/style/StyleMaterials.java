package mix.client.mixui.integration.haunt.fx.style;

import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.setting.ModeSetting;

public final class StyleMaterials {
   public static ModeSetting<Material> material(String key, Material defaultMaterial, String description) {
      return (ModeSetting)(new ModeSetting(key + ".material", "Material", description, Material.class, defaultMaterial)).style(ModeSetting.Style.CHIPS).keywords(new String[]{"material", "metal", "gold", "silver", "chrome", "brass", "bronze", "iron", "obsidian", "pearl", "jade", "custom"});
   }

   public static ModeSetting<Finish> finish(String key, Finish defaultFinish, final ModeSetting<Material> material) {
      return (ModeSetting)((ModeSetting)(new ModeSetting(key + ".finish", "Finish", "How a Custom material takes the light: Metallic, Satin, Matte, Glass, Pearlescent or Toon (flat bands, like a comic).", Finish.class, defaultFinish)).keywords(new String[]{"shine", "gloss", "matte", "glass", "pearl", "metallic", "satin", "toon", "cartoon", "cel"})).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return material.get() == Material.CUSTOM;
         }
      });
   }
}
