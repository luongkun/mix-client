package mix.client.mixui.integration.haunt.gui.widget;

import mix.client.mixui.integration.haunt.gui.Density;
import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.gui.widget.PageLinkWidget;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.sound.ModSounds;
import mix.client.mixui.integration.haunt.sound.Sfx;

public final class SectionLinkWidget extends Widget {
   private static final float H = 30.0F;
   private final String title;
   private final String where;
   private final String tooltip;
   private final Runnable go;

   public SectionLinkWidget(String title, String where, String tooltip, Runnable go) {
      this.title = title;
      this.where = where;
      this.tooltip = tooltip;
      this.go = go;
   }

   public float height() {
      return Density.compact()?Density.row() + 3.0F:30.0F;
   }

   public void render(float mx, float my, float dt, int accent) {
      boolean over = this.contains(mx, my);
      this.animateHover(over, dt);
      float h = this.height();
      float cy = this.y + h / 2.0F;
      float ts = Density.text();
      Ui.roundRect(this.x - 6.0F, this.y + 2.0F, this.w + 12.0F, h - 4.0F, 8.0F, ColorUtil.lerp(ColorUtil.mulAlpha(Theme.SURFACE, 0.6F), Theme.SURFACE_HOVER, this.hover));
      Ui.registerControl(this.x - 6.0F, this.y + 2.0F, this.w + 12.0F, h - 4.0F, this.title);
      Ui.textMiddle(this.title, this.x + 4.0F, cy, ts, Theme.TEXT, false);
      float tw = Ui.width(this.title, ts);
      Ui.textMiddle(Ui.ellipsize(this.where, Density.small(), Math.max(0.0F, this.w - tw - 30.0F), false), this.x + 4.0F + tw + 10.0F, cy, Density.small(), Theme.TEXT_MUTED, false);
      float ax = this.x + this.w - 8.0F + Easing.outCubic(this.hover) * 2.0F;
      PageLinkWidget.chevron(ax, cy, ColorUtil.lerp(Theme.TEXT_MUTED, accent, 0.35F + 0.65F * this.hover));
   }

   public boolean mouseDown(float mx, float my, int button) {
      if(button == 0 && this.contains(mx, my)) {
         ModSounds.gui(Sfx.SECTION);
         this.go.run();
         return true;
      } else {
         return false;
      }
   }

   public String tooltip(float mx, float my) {
      return this.contains(mx, my)?this.tooltip:null;
   }
}
