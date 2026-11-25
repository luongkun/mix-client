package mix.client.mixui.integration.haunt.module.watermark;

import mix.client.mixui.integration.haunt.module.watermark.IslandDraw;
import mix.client.mixui.integration.haunt.module.watermark.IslandIcons;
import mix.client.mixui.integration.haunt.module.watermark.IslandItem;
import mix.client.mixui.integration.haunt.module.watermark.IslandStyle;
import mix.client.mixui.integration.haunt.module.watermark.WaterMarkModule;

final class SettingsItem extends IslandItem {
   SettingsItem(WaterMarkModule wm) {
      super(wm, "settings", "Settings ⚙", "The gear: quick settings in the island (click it with chat open).", IslandIcons.Icon.GEAR);
   }

   float measure(IslandDraw d, IslandStyle s) {
      return s.icon;
   }

   void draw(IslandDraw d, IslandStyle s, float x, float cy) {
      boolean open = this.wm.isSettingsViewOpen();
      int accent = s.accentAt(x + s.icon / 2.0F);
      int color = open?accent:s.dim;
      if(open) {
         d.glow(x + s.icon / 2.0F, cy, s.icon * 2.6F, accent & 16777215 | 1073741824);
      }

      d.icon(IslandIcons.Icon.GEAR, x, cy - s.icon / 2.0F, s.icon, color);
   }

   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      hit.set(this, x - s.pad, cy - s.height / 2.0F, s.icon + s.pad * 2.0F, s.height, 1, this.wm.isSettingsViewOpen()?"Back to the island":"Quick settings", true);
      return true;
   }

   void click(IslandItem.Hit hit, float mx, float my) {
      this.wm.toggleSettingsView();
   }
}
