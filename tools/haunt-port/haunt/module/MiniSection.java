package mix.client.mixui.integration.haunt.module;

import mix.client.mixui.integration.haunt.module.MiniSections;
import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.module.watermark.IslandIcons;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.SettingStyle;

public final class MiniSection implements SettingStyle {
   private final MiniSections owner;
   private final String id;
   private final String name;
   private final IslandIcons.Icon icon;
   private final boolean dividerBefore;
   private BooleanSetting enabled;

   MiniSection(MiniSections owner, String id, String name, IslandIcons.Icon icon, boolean dividerBefore) {
      this.owner = owner;
      this.id = id;
      this.name = name;
      this.icon = icon;
      this.dividerBefore = dividerBefore;
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public IslandIcons.Icon getIcon() {
      return this.icon;
   }

   public boolean dividerBefore() {
      return this.dividerBefore;
   }

   public BooleanSetting getSwitch() {
      return this.enabled;
   }

   public void setSwitch(BooleanSetting enabled) {
      this.enabled = enabled == null?null:enabled.sectionSwitch();
   }

   public Module module() {
      return this.owner.module;
   }

   public boolean on() {
      return this.owner.module.isEnabled() && (this.enabled == null || this.enabled.get());
   }

   public String styleName() {
      return this.name;
   }

   public boolean isShown() {
      return this.owner.current() == this;
   }

   public boolean isActive() {
      return this.enabled == null || this.enabled.get();
   }

   public boolean isMultiple() {
      return true;
   }

   public void show() {
      this.owner.select(this);
   }

   public String kind() {
      return "mini section";
   }

   public String kinds() {
      return "mini sections";
   }

   public String toString() {
      return this.id;
   }
}
