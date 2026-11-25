package mix.client.mixui.integration.haunt.module.ambience;

import mix.client.mixui.integration.haunt.gui.MenuMemory;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.gui.widget.Pill;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.ambience.AmbienceModule;
import mix.client.mixui.integration.haunt.module.ambience.AmbientStyle;
import mix.client.mixui.integration.haunt.module.ambience.AmbientStyleBase;
import mix.client.mixui.integration.haunt.sound.ModSounds;
import mix.client.mixui.integration.haunt.sound.Sfx;

final class AmbiencePreviewWidget extends Widget {
   private static final String[] BUTTONS = new String[]{"Dark", "Day", "Gust"};
   private static final float RADIUS = 8.0F;
   static AmbiencePreviewWidget lastShown;
   private final AmbienceModule module;
   private final AmbientStyle type;
   private final Pill[] pills;
   private boolean day;
   private float buttonsH;

   AmbiencePreviewWidget(AmbienceModule module, AmbientStyle type) {
      this.pills = new Pill[BUTTONS.length];
      this.buttonsH = 20.0F;
      this.module = module;
      this.type = type;

      for(int i = 0; i < this.pills.length; ++i) {
         this.pills[i] = new Pill();
      }

      this.day = MenuMemory.previewDay(module.getId());
   }

   private float stageH() {
      return Math.max(96.0F, Math.min(230.0F, (float)Math.round(this.w * 0.42F)));
   }

   public float height() {
      return this.stageH() + 6.0F + this.buttonsH + 2.0F;
   }

   public void render(float mx, float my, float dt, int accent) {
      lastShown = this;
      float sx = this.x;
      float sy = this.y;
      float sw = this.w;
      float sh = this.stageH();
      AmbientStyleBase style = this.module.style(this.type);
      if(!Ui.clippedOut(sx, sy, sw, sh)) {
         int tex = this.module.previews.drawBig(this.type, Math.max(16, Math.round(sw * Ui.px)), Math.max(16, Math.round(sh * Ui.px)), this.day);
         if(tex > 0) {
            Ui.roundedImage(tex, sx, sy, sw, sh, 8.0F, 0.0F, 1.0F, 1.0F, 0.0F, -1);
         } else {
            Ui.gradientV(sx, sy, sw, sh, 8.0F, this.day?-13873268:-15723493, this.day?-12820940:-15065305);
         }

         Ui.outline(sx, sy, sw, sh, 8.0F, 0.8F, 620756991);
      }

      String note;
      if((note = this.note(style)) != null) {
         float ns = 7.4F;
         float nw = Ui.width(note, ns) + 12.0F;
         Ui.roundRect(sx + 6.0F, sy + 6.0F, nw, 13.0F, 6.5F, -1610612736);
         Ui.textMiddle(note, sx + 12.0F, sy + 12.5F, ns, -251658241, false);
      }

      float tx = this.x;
      float ty = this.y + sh + 6.0F;

      for(int i = 0; i < BUTTONS.length; ++i) {
         float tw = Ui.width(BUTTONS[i], 8.8F) + 20.0F;
         if(tx > this.x && tx + tw > this.x + this.w) {
            tx = this.x;
            ty += 25.0F;
         }

         this.pills[i].layout(tx, ty, BUTTONS[i]);
         boolean on = i == 0 && !this.day || i == 1 && this.day;
         this.pills[i].draw(BUTTONS[i], mx, my, dt, accent, on, true);
         tx += tw + 5.0F;
      }

      this.buttonsH = ty - (this.y + sh + 6.0F) + 20.0F;
   }

   private String note(AmbientStyleBase style) {
      return style.isOn() && this.module.isEnabled()?(style.time.get() == AmbientStyleBase.TimeRule.NIGHT && this.day?"Night only: it hides by day":null):(this.module.isEnabled()?"Switched off: its switch is in the header":"Ambience is off");
   }

   public boolean mouseDown(float mx, float my, int button) {
      if(button == 0 && this.contains(mx, my)) {
         for(int i = 0; i < BUTTONS.length; ++i) {
            if(this.pills[i].contains(mx, my)) {
               this.press(i);
               ModSounds.gui(Sfx.CLICK);
               return true;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   void press(int i) {
      if(i < 2) {
         this.day = i == 1;
         MenuMemory.previewDay(this.module.getId(), this.day);
      } else {
         this.module.previews.gust();
      }

   }

   boolean dayForTest() {
      return this.day;
   }

   public String tooltip(float mx, float my) {
      for(int i = 0; i < BUTTONS.length; ++i) {
         if(this.pills[i].contains(mx, my)) {
            switch(i) {
            case 0:
               return "A dark backdrop, so the effect shows clearly (night-only styles as at night)";
            case 1:
               return "The preview by day, to check its colors in daylight (night-only styles hide, as by day)";
            default:
               return "A gust of the shared wind now: everything it carries speeds up, and Wind sends a bunch of streaks";
            }
         }
      }

      return my < this.y + this.stageH()?this.type.displayName() + " as it looks with its settings now":null;
   }
}
