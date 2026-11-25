package mix.client.mixui.integration.haunt.module.watermark;

import mix.client.mixui.integration.haunt.module.watermark.IslandDraw;
import mix.client.mixui.integration.haunt.module.watermark.IslandIcons;
import mix.client.mixui.integration.haunt.module.watermark.IslandItem;
import mix.client.mixui.integration.haunt.module.watermark.IslandStyle;
import mix.client.mixui.integration.haunt.module.watermark.WaterMarkModule;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.TextSetting;

final class TextItem extends IslandItem {
   final TextSetting text = (TextSetting)this.option(new TextSetting("text.value", "Text", "What the island says (at most 32 characters).", "Have fun!", 32));
   final ModeSetting<TextItem.TextColor> color = (ModeSetting)this.option(new ModeSetting("text.color", "Text Color", "The color of your text.", TextItem.TextColor.class, TextItem.TextColor.TEXT));
   int lastColor;
   private float textW;
   private int measuredPx;
   private String measured;

   TextItem(WaterMarkModule wm) {
      super(wm, "text", "Custom Text", "Your own text.", (IslandIcons.Icon)null);
      this.icons((IslandIcons.Icon)null, new IslandIcons.Icon[]{IslandIcons.Icon.HEART, IslandIcons.Icon.STAR, IslandIcons.Icon.GLOW, IslandIcons.Icon.CROWN, IslandIcons.Icon.FLAME, IslandIcons.Icon.SWORD, IslandIcons.Icon.GEM, IslandIcons.Icon.NOTE, IslandIcons.Icon.BOLT}).tip(IslandIcons.Icon.GLOW, "Sparkle");
   }

   boolean available() {
      return !this.text.get().trim().isEmpty();
   }

   String panelValue() {
      return this.available()?this.text.get().trim():"Empty (write it in the menu)";
   }

   String panelDescription() {
      return "Your own text";
   }

   float measure(IslandDraw d, IslandStyle s) {
      int px = d.pixels(s.font);
      String t = this.text.get();
      if(px != this.measuredPx || !t.equals(this.measured)) {
         this.measuredPx = px;
         this.measured = t;
         this.textW = d.width(false, s.font, t);
      }

      return this.iconSpace(s) + this.textW;
   }

   void draw(IslandDraw d, IslandStyle s, float x, float cy) {
      x = this.drawIcon(d, s, x, cy);
      String t = this.text.get();
      IslandStyle.Paint p = ((TextItem.TextColor)this.color.get()).paint;
      this.lastColor = s.paint(p, x + this.textW / 2.0F);
      if(!s.uniform() && p != IslandStyle.Paint.TEXT) {
         float top = d.middleTop(false, s.font, cy);
         float px = x;

         for(int i = 0; i < t.length(); ++i) {
            char c = t.charAt(i);
            float w = d.charWidth(false, s.font, c);
            d.glyph(false, s.font, c, px, top, s.paint(p, px + w / 2.0F));
            px += w;
         }
      } else {
         d.textMiddle(false, s.font, t, x, cy, this.lastColor);
      }

   }

   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      hit.set(this, x - s.pad, cy - s.height / 2.0F, this.iconSpace(s) + this.textW + s.pad * 2.0F, s.height, 0, "Custom text (WaterMark > Modules)", false);
      return true;
   }

   static enum TextColor implements Displayable {
      TEXT("Text", "The theme\'s text color", IslandStyle.Paint.TEXT),
      ACCENT("Accent", "The theme\'s accent color", IslandStyle.Paint.ACCENT),
      GLOBAL("Global Color", "The Global Color (static, gradient or chroma)", IslandStyle.Paint.GLOBAL);

      private final String displayName;
      private final String description;
      final IslandStyle.Paint paint;

      private TextColor(String displayName, String description, IslandStyle.Paint paint) {
         this.displayName = displayName;
         this.description = description;
         this.paint = paint;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }
}
