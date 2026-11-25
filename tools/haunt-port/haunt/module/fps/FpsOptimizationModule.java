package mix.client.mixui.integration.haunt.module.fps;

import java.util.List;
import java.util.Locale;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.module.fps.FpsOptimizer;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import mix.client.mixui.integration.haunt.setting.WidgetSetting;
import mix.client.mixui.integration.haunt.sound.ModSounds;
import net.minecraft.client.Minecraft;

public final class FpsOptimizationModule extends Module {
   final SliderSetting target;
   final BooleanSetting allowVisual;
   final BooleanSetting watch;
   private long lastWarn;
   private int ticks;

   public FpsOptimizationModule() {
      super("fps_optimization", "FPS Optimization", "Powered by Artificial Intelligence.", true);
      this.add(new FpsOptimizationModule.Panel());
      this.target = (SliderSetting)this.add((SliderSetting)(new SliderSetting("target", "Target FPS", "The frame rate the optimizer aims for (it only goes as far as your computer can).", 60.0D, 400.0D, 300.0D, 10.0D)).unit(" FPS").keywords(new String[]{"fps", "target", "300", "400"}));
      this.allowVisual = (BooleanSetting)this.add((BooleanSetting)(new BooleanSetting("allowVisual", "Allow Visual Trade-offs", "Off: it only tries changes you can\'t see (frame-rate cap, VSync, vertex buffers). On: it may also lower particles, smooth lighting, render distance and fancy graphics while you\'re below the target. Every change is measured and undone if it doesn\'t help.", false)).keywords(new String[]{"quality", "graphics", "render distance", "particles"}));
      this.watch = (BooleanSetting)this.add((BooleanSetting)(new BooleanSetting("watch", "Watch Frame Rate", "Tells you in chat when your frame rate stays far below what it measured after the last run. It never changes anything by itself.", true)).keywords(new String[]{"monitor", "warn", "drop"}));
   }

   public void clientTick(Minecraft mc) {
      FpsOptimizer o = FpsOptimizer.INSTANCE;
      o.configure(this.allowVisual.get(), this.target.getFloat());
      o.tick(mc);
      if(this.isEnabled() && this.watch.get() && !o.running() && ++this.ticks % 200 == 0 && mc.currentScreen == null && mc.theWorld != null) {
         float live = o.liveFps();
         long now = System.currentTimeMillis();
         if(live > 0.0F && live < this.target.getFloat() * 0.35F && now - this.lastWarn > 600000L) {
            this.lastWarn = now;
            FuriahsVisuals.chat(String.format(Locale.ROOT, "FPS Optimization: running at %.0f FPS, far below your %.0f target. Open the menu and press OPTIMIZE NOW if you want it to try something (nothing changes until you do).", new Object[]{Float.valueOf(live), Float.valueOf(this.target.getFloat())}));
         }
      }

   }

   static final class Panel extends WidgetSetting {
      Panel() {
         super("panel", "Optimizer", "Live frame rate and the optimizer\'s buttons.");
      }

      public Widget createWidget() {
         return new FpsOptimizationModule.PanelWidget();
      }
   }

   static final class PanelWidget extends Widget {
      private float btnY;
      private final float[] bx = new float[3];
      private final float[] bw = new float[3];
      private static final String[] LABELS = new String[]{"OPTIMIZE NOW", "STOP", "RESTORE"};

      public float height() {
         return 66.0F + (float)Math.min(8, FpsOptimizer.INSTANCE.log().size()) * 10.0F;
      }

      public void render(float mx, float my, float dt, int accent) {
         FpsOptimizer o = FpsOptimizer.INSTANCE;
         Ui.roundRect(this.x, this.y + 2.0F, this.w, this.height() - 6.0F, 8.0F, ColorUtil.withAlpha(accent, 0.08F));
         Ui.outline(this.x, this.y + 2.0F, this.w, this.height() - 6.0F, 8.0F, 0.8F, ColorUtil.withAlpha(accent, 0.35F));
         String fps = String.format(Locale.ROOT, "%.0f FPS now", new Object[]{Float.valueOf(o.liveFps())});
         Ui.textBold(fps, this.x + 12.0F, this.y + 10.0F, 10.0F, Theme.TEXT);
         Ui.text(o.status(), this.x + 12.0F + Ui.widthBold(fps, 10.0F) + 10.0F, this.y + 11.0F, 7.6F, Theme.TEXT_DIM);
         this.btnY = this.y + 28.0F;
         float cx = this.x + 12.0F;

         for(int i = 0; i < 3; ++i) {
            boolean enabled = i == 0?!o.running():(i == 1?o.running():o.historySize() > 0 || o.running());
            float w = Ui.widthBold(LABELS[i], 8.4F) + 24.0F;
            this.bx[i] = cx;
            this.bw[i] = w;
            boolean over = mx >= cx && mx < cx + w && my >= this.btnY && my < this.btnY + 20.0F;
            int fill = i == 0?accent:(i == 1?Theme.DANGER:Theme.SURFACE_HOVER);
            Ui.roundRect(cx, this.btnY, w, 20.0F, 6.0F, ColorUtil.withAlpha(fill, enabled?(over?0.95F:0.75F):0.25F));
            Ui.textMiddle(LABELS[i], cx + 12.0F, this.btnY + 10.0F, 8.4F, enabled?-1:Theme.TEXT_MUTED, true);
            Ui.registerControl(cx, this.btnY, w, 20.0F, "fps " + LABELS[i]);
            cx += w + 8.0F;
         }

         List<String> log = o.log();
         float ly = this.btnY + 28.0F;

         for(int i = Math.max(0, log.size() - 8); i < log.size(); ++i) {
            Ui.text((String)log.get(i), this.x + 12.0F, ly, 7.4F, Theme.TEXT_DIM);
            ly += 10.0F;
         }

      }

      public boolean mouseDown(float mx, float my, int button) {
         if(button == 0 && my >= this.btnY && my < this.btnY + 20.0F) {
            FpsOptimizer o = FpsOptimizer.INSTANCE;

            for(int i = 0; i < 3; ++i) {
               if(mx >= this.bx[i] && mx < this.bx[i] + this.bw[i]) {
                  if(i == 0 && !o.running()) {
                     o.start();
                  } else if(i == 1 && o.running()) {
                     o.stop();
                  } else {
                     if(i != 2) {
                        return true;
                     }

                     o.restoreLast();
                  }

                  ModSounds.toggle(i == 0);
                  return true;
               }
            }
         }

         return false;
      }

      public String tooltip(float mx, float my) {
         if(my >= this.btnY && my < this.btnY + 20.0F) {
            if(mx >= this.bx[0] && mx < this.bx[0] + this.bw[0]) {
               return "Measures your frame rate, then tries one change at a time and keeps only what makes it faster. Close the menu and play while it runs.";
            }

            if(mx >= this.bx[1] && mx < this.bx[1] + this.bw[1]) {
               return "Ends the run now; the change being tested is put back.";
            }

            if(mx >= this.bx[2] && mx < this.bx[2] + this.bw[2]) {
               return "Puts back the settings you had before the last run (press again to go further back).";
            }
         }

         return null;
      }
   }
}
