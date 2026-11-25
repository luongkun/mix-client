package mix.client.mixui.integration.haunt.module.watermark;

import java.util.Calendar;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.module.watermark.IslandDraw;
import mix.client.mixui.integration.haunt.module.watermark.IslandIcons;
import mix.client.mixui.integration.haunt.module.watermark.IslandItem;
import mix.client.mixui.integration.haunt.module.watermark.IslandStyle;
import mix.client.mixui.integration.haunt.module.watermark.ItemLabel;
import mix.client.mixui.integration.haunt.module.watermark.RollingText;
import mix.client.mixui.integration.haunt.module.watermark.WaterMarkModule;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;

final class TimeItem extends IslandItem {
   final ModeSetting<TimeItem.Format> format;
   final BooleanSetting ampmText;
   final BooleanSetting seconds;
   final BooleanSetting blink;
   final ItemLabel label;
   private final RollingText value = new RollingText();
   private final Calendar calendar = Calendar.getInstance();
   private final StringBuilder sb = new StringBuilder(12);
   private String ampm = "";
   private float ampmW;
   private String measuredAmpm = "";
   private int measuredPx;

   TimeItem(WaterMarkModule wm) {
      super(wm, "time", "Local Time", "Your computer\'s clock.", IslandIcons.Icon.CLOCK);
      this.icons(IslandIcons.Icon.CLOCK, new IslandIcons.Icon[]{IslandIcons.Icon.CLOCK, IslandIcons.Icon.HOURGLASS});
      this.label = new ItemLabel(this, "time.label", "time", "Time", false, ItemLabel.Place.BEFORE);
      this.format = (ModeSetting)this.option(new ModeSetting("time.format", "Format", "24 or 12 hours.", TimeItem.Format.class, TimeItem.Format.H24));
      this.ampmText = (BooleanSetting)this.option((new BooleanSetting("time.ampm", "AM/PM", "Writes AM or PM after a 12-hour time.", true)).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return TimeItem.this.format.get() == TimeItem.Format.H12;
         }
      }));
      this.seconds = (BooleanSetting)this.option(new BooleanSetting("time.seconds", "Show Seconds", "Adds the seconds.", false));
      this.blink = (BooleanSetting)this.option(new BooleanSetting("time.blink", "Blinking Colon", "The colon blinks every second.", false));
   }

   void tick() {
      this.calendar.setTimeInMillis(System.currentTimeMillis());
      int h = this.calendar.get(11);
      int m = this.calendar.get(12);
      int sec = this.calendar.get(13);
      this.sb.setLength(0);
      if(this.format.get() == TimeItem.Format.H12) {
         int h12 = h % 12 == 0?12:h % 12;
         this.sb.append(h12);
         this.ampm = !this.ampmText.get()?"":(h < 12?" AM":" PM");
      } else {
         if(h < 10) {
            this.sb.append('0');
         }

         this.sb.append(h);
         this.ampm = "";
      }

      this.sb.append(':');
      if(m < 10) {
         this.sb.append('0');
      }

      this.sb.append(m);
      if(this.seconds.get()) {
         this.sb.append(':');
         if(sec < 10) {
            this.sb.append('0');
         }

         this.sb.append(sec);
      }

      if(!this.value.get().contentEquals(this.sb)) {
         this.value.set(this.sb.toString(), (double)(h * 3600 + m * 60 + sec), this.wm.rolling());
      }

   }

   void frame(float dt, IslandDraw d, IslandStyle s) {
      this.value.update(dt);
   }

   String panelValue() {
      return this.value.get().isEmpty()?null:this.value.get() + this.ampm;
   }

   String panelDescription() {
      return "Your clock";
   }

   float measure(IslandDraw d, IslandStyle s) {
      int px = d.pixels(s.small);
      if(px != this.measuredPx || !this.ampm.equals(this.measuredAmpm)) {
         this.measuredPx = px;
         this.measuredAmpm = this.ampm;
         this.ampmW = this.ampm.isEmpty()?0.0F:d.width(false, s.small, this.ampm);
      }

      return this.iconSpace(s) + this.label.width(d, s) + this.value.width(d, false, s.font) + this.ampmW;
   }

   void draw(IslandDraw d, IslandStyle s, float x, float cy) {
      x = this.drawIcon(d, s, x, cy);
      if(this.label.before()) {
         x = this.label.draw(d, s, x, cy);
      }

      int color = s.text;
      if(this.blink.get()) {
         this.value.drawBlinking(d, false, s.font, x, cy, color, System.currentTimeMillis() % 1000L < 500L?1.0F:0.2F);
      } else {
         this.value.draw(d, false, s.font, x, cy, color);
      }

      x = x + this.value.width(d, false, s.font);
      if(!this.ampm.isEmpty()) {
         d.text(false, s.small, this.ampm, x, d.sameBaseline(false, s.font, d.middleTop(false, s.font, cy), s.small), s.dim);
         x += this.ampmW;
      }

      if(this.label.after()) {
         this.label.draw(d, s, x, cy);
      }

   }

   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      hit.set(this, x - s.pad, cy - s.height / 2.0F, this.measure(this.wm.draw(), s) + s.pad * 2.0F, s.height, 0, "Local time", false);
      return true;
   }

   static enum Format implements Displayable {
      H24("24h", "13:05"),
      H12("12h", "1:05 PM");

      private final String displayName;
      private final String description;

      private Format(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }
}
