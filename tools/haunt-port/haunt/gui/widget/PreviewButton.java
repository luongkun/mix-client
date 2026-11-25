package mix.client.mixui.integration.haunt.gui.widget;

import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.module.watermark.IslandIcons;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;

final class PreviewButton {
   private static final float R = 6.5F;
   private float hover;
   private float cx = -10000.0F;
   private float cy = -10000.0F;

   void render(float x, float y, float mx, float my, float dt, int accent) {
      this.cx = x;
      this.cy = y;
      this.hover = Easing.approach(this.hover, this.contains(mx, my)?1.0F:0.0F, 18.0F, dt);
      Ui.registerControl(x - 6.5F, y - 6.5F, 13.0F, 13.0F, "▶");
      Ui.circle(x, y, 6.5F, ColorUtil.lerp(Theme.PILL, Theme.ACCENT_SOFT_HOVER, this.hover));
      IslandIcons.drawPlay(x + 0.7F, y, 8.45F, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hover));
   }

   boolean contains(float mx, float my) {
      float dx = mx - this.cx;
      float dy = my - this.cy;
      return dx * dx + dy * dy <= 72.25F;
   }

   static String tooltip() {
      return "Play it";
   }
}
