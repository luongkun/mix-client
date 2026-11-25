package mix.client.mixui.integration.haunt.module.particles;

import mix.client.mixui.integration.haunt.core.TestDummy;
import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.gui.widget.Pill;
import mix.client.mixui.integration.haunt.gui.widget.ToggleWidget;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.particles.ParticlesModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import net.minecraft.client.Minecraft;

final class BurstTestWidget extends Widget {
   private final ParticlesModule module;
   private final Pill burst = new Pill();
   private float knob = -1.0F;
   private float repeatHover;

   BurstTestWidget(ParticlesModule module) {
      this.module = module;
   }

   public float height() {
      return 36.0F;
   }

   private float switchX() {
      return this.x + this.w - 28.0F;
   }

   private boolean overRepeat(float mx, float my) {
      float lx = this.switchX() - 8.0F - Ui.width("Repeat", 8.8F);
      return mx >= lx && mx < this.x + this.w && my >= this.y + 6.0F && my < this.y + 26.0F;
   }

   public void render(float mx, float my, float dt, int accent) {
      this.module.pageShown();
      boolean world = Minecraft.getMinecraft().theWorld != null;
      Ui.textMiddle("Burst Test", this.x, this.y + 10.0F, 10.0F, Theme.TEXT, false);
      String where = !world?"Join a world to try it":(TestDummy.get() != null?"On the test dummy":"Next to you (or on /haunt dummy)");
      Ui.text(where, this.x, this.y + 20.0F, 8.2F, Theme.TEXT_DIM);
      boolean on = this.module.isRepeating();
      this.knob = this.knob < 0.0F?(on?1.0F:0.0F):Easing.approach(this.knob, on?1.0F:0.0F, 18.0F, dt);
      this.repeatHover = Easing.approach(this.repeatHover, this.overRepeat(mx, my)?1.0F:0.0F, 16.0F, dt);
      ToggleWidget.drawSwitch(this.switchX(), this.y + 8.5F, 28.0F, 15.0F, this.knob, accent);
      String label = "Repeat";
      Ui.textMiddle(label, this.switchX() - 8.0F - Ui.width(label, 8.8F), this.y + 16.0F, 8.8F, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.repeatHover), false);
      String b = "Burst";
      this.burst.layout(this.switchX() - 18.0F - Ui.width(label, 8.8F) - (Ui.width(b, 8.8F) + 20.0F), this.y + 6.0F, b);
      this.burst.draw(b, mx, my, dt, accent, true, world);
   }

   public boolean mouseDown(float mx, float my, int button) {
      if(this.contains(mx, my) && button == 0) {
         if(this.burst.contains(mx, my)) {
            this.module.burst();
         } else if(this.overRepeat(mx, my)) {
            this.module.setRepeating(!this.module.isRepeating());
         }

         return true;
      } else {
         return false;
      }
   }

   public String tooltip(float mx, float my) {
      return this.burst.contains(mx, my)?"A crit and a sharpness burst, on your screen only (hidden types stay hidden)":(this.overRepeat(mx, my)?"A burst every second while this page is open":null);
   }
}
