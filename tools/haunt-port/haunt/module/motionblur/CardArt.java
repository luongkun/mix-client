package mix.client.mixui.integration.haunt.module.motionblur;

import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.module.motionblur.BlurStyle;
import mix.client.mixui.integration.haunt.render.ColorUtil;

final class CardArt {
   private static final float PERIOD = 1.8F;
   private static final int COPIES = 5;
   private static final float COPY_GAP = 0.035F;
   private static final float STREAK = 0.1F;

   static void draw(BlurStyle style, float x, float y, float w, float h, double time, boolean selected, int accent) {
      float r = Math.min(4.0F, h * 0.25F);
      Ui.roundRect(x, y, w, h, r, Theme.PREVIEW_STAGE);
      float phase = (float)(time % 1.7999999523162842D / 1.7999999523162842D);
      float barW = Math.max(2.0F, h * 0.16F);
      float barH = h * 0.62F;
      float pad = h * 0.3F;
      float travel = w - pad * 2.0F - barW;
      float by = y + (h - barH) / 2.0F;
      int bar = selected?accent:-920584;
      Ui.pushScissor(x, y, w, h);
      float now = x + pad + travel * ease(phase);
      if(style.trails()) {
         for(int k = 5; k >= 1; --k) {
            float p = phase - (float)k * 0.035F;
            if(p >= 0.0F) {
               float a = (float)Math.pow(0.55D, (double)k) * (style.camera()?0.6F:1.0F);
               Ui.roundRect(x + pad + travel * ease(p), by, barW, barH, barW / 2.0F, ColorUtil.withAlpha(bar, a));
            }
         }
      }

      float from;
      if(style.camera() && now - (from = x + pad + travel * ease(Math.max(0.0F, phase - 0.1F))) > 0.5F) {
         Ui.gradientH(from, by, now - from + barW * 0.5F, barH, barW / 2.0F, ColorUtil.withAlpha(bar, 0.0F), ColorUtil.withAlpha(bar, 0.75F));
      }

      Ui.roundRect(now, by, barW, barH, barW / 2.0F, bar);
      Ui.popScissor();
   }

   private static float ease(float t) {
      float c = t < 0.0F?0.0F:(t > 1.0F?1.0F:t);
      return c * c * (3.0F - 2.0F * c);
   }
}
