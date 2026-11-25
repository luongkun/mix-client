package mix.client.mixui.integration.haunt.gui.widget;

import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.style.Signature;
import mix.client.mixui.integration.haunt.gui.Density;
import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.gui.widget.Pill;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.setting.WidgetSetting;
import mix.client.mixui.integration.haunt.sound.ModSounds;
import mix.client.mixui.integration.haunt.sound.Sfx;

public final class SignatureColorsWidget extends Widget {
   private static final String LABEL = "Use signature colors";
   private final SignatureColorsWidget.Source source;
   private final Pill pill = new Pill();

   public SignatureColorsWidget(SignatureColorsWidget.Source source) {
      this.source = source;
   }

   public static WidgetSetting setting(final SignatureColorsWidget.Source source) {
      return (WidgetSetting)((WidgetSetting)((WidgetSetting)(new WidgetSetting("signatureColors", "Use Signature Colors", "Sets the picked style\'s colors back to its own (as Static colors), and its material back to the one it is made of; its other settings stay as they are.") {
         public Widget createWidget() {
            return new SignatureColorsWidget(source);
         }
      }).group("Colors")).keywords(new String[]{"signature", "default colors", "reset colors", "original colors"})).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return !source.selectedSignature().isEmpty();
         }
      });
   }

   public float height() {
      return Density.compact()?Density.row() + 2.0F:28.0F;
   }

   public void render(float mx, float my, float dt, int accent) {
      Signature s = this.source.selectedSignature();
      boolean has = s.inUse();
      float py = this.y + (this.height() - 20.0F) / 2.0F;
      float pw = this.pill.layout(this.x, py, "Use signature colors");
      this.pill.draw("Use signature colors", mx, my, dt, accent, false, !has);
      float cx = this.x + pw + 12.0F;
      float cy = this.y + this.height() / 2.0F;
      int[] colors = s.colors();
      float room = this.x + this.w - 4.6F - cx;
      float step = 11.0F;
      if(colors.length > 1 && (float)(colors.length - 1) * step > room) {
         step = Math.max(7.0F, room / (float)(colors.length - 1));
      }

      for(int c : colors) {
         Ui.circle(cx, cy, 4.6F, Theme.OUTLINE);
         Ui.circle(cx, cy, 3.8F, c | -16777216);
         cx += step;
      }

      if(has && cx + 2.0F + Ui.width("In use", Density.small()) <= this.x + this.w) {
         Ui.textMiddle("In use", cx + 2.0F, cy, Density.small(), Theme.TEXT_MUTED, false);
      }

   }

   public boolean mouseDown(float mx, float my, int button) {
      if(button == 0 && this.pill.contains(mx, my)) {
         Signature s = this.source.selectedSignature();
         if(!s.inUse()) {
            s.use();
            ModSounds.gui(Sfx.CLICK);
         }

         return true;
      } else {
         return this.contains(mx, my);
      }
   }

   public String tooltip(float mx, float my) {
      return !this.pill.contains(mx, my)?null:"Sets " + this.source.selectedStyleName() + "\'s colors back to its own, as Static colors" + (this.source.selectedSignature().hasMaterial()?", and its material back to the one it is made of":"") + " (its other settings stay as they are)";
   }

   public interface Source {
      Signature selectedSignature();

      String selectedStyleName();
   }
}
