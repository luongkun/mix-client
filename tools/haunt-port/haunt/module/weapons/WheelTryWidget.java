package mix.client.mixui.integration.haunt.module.weapons;

import mix.client.mixui.integration.haunt.gui.Density;
import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.gui.popup.Popover;
import mix.client.mixui.integration.haunt.gui.popup.Popovers;
import mix.client.mixui.integration.haunt.gui.widget.Pill;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.weapons.WeaponWheel;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.module.weapons.WheelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Mouse;

final class WheelTryWidget extends Widget {
   private final WeaponsModule module;
   private final Pill button = new Pill();

   WheelTryWidget(WeaponsModule module) {
      this.module = module;
   }

   public float height() {
      return Density.compact()?Density.row() + 6.0F:28.0F;
   }

   public boolean isVisible() {
      return this.module.wheelSettings.tryIt.isVisible();
   }

   public void render(float mx, float my, float dt, int accent) {
      float h = this.height();
      float cy = this.y + h / 2.0F;
      String label = "Open the Wheel";
      this.button.layout(this.x + this.w - (Ui.width(label, 8.8F) + 20.0F), cy - 10.0F, label);
      this.animateHover(this.button.contains(mx, my), dt);
      float ts = Density.text();
      String name = Ui.ellipsize(this.module.wheelSettings.tryIt.getLabel(), ts, this.button.x - this.x - 10.0F, false);
      Ui.textMiddle(name, this.x, cy, ts, Theme.TEXT, false);
      float nw = Ui.width(name, ts) + 8.0F;
      String hint = "as in game: mouse, scroll, click";
      if(nw + Ui.width(hint, Density.small()) < this.button.x - this.x - 10.0F) {
         Ui.textMiddle(hint, this.x + nw, cy, Density.small(), Theme.TEXT_MUTED, false);
      }

      this.button.draw(label, mx, my, dt, accent, true, true);
   }

   public boolean mouseDown(float mx, float my, int button) {
      if(button == 0 && this.button.contains(mx, my)) {
         this.module.wheel.openInMenu();
         Popovers.open(new WheelTryWidget.TryPopover(this.module), this.x, this.y, this.w, this.height());
         return true;
      } else {
         return false;
      }
   }

   public String tooltip(float mx, float my) {
      return this.contains(mx, my)?this.module.wheelSettings.tryIt.getDescription():null;
   }

   static final class TryPopover extends Popover {
      private final WeaponsModule module;
      private boolean closing;
      private float lastMx = Float.NaN;
      private float lastMy = Float.NaN;

      TryPopover(WeaponsModule module) {
         this.module = module;
      }

      protected float preferredW() {
         return 10.0F;
      }

      protected float preferredH() {
         return 10.0F;
      }

      protected void renderContent(float mx, float my, float dt, int accent) {
      }

      public void render(float mx, float my, float dt, int accent) {
         WeaponWheel wheel = this.module.wheel;
         Minecraft mc = Minecraft.getMinecraft();
         float menuPx = Ui.px;
         float px = (float)mc.displayHeight / 1080.0F;
         WheelRenderer r = wheel.renderer;
         boolean moved = !Float.isNaN(this.lastMx) && (mx != this.lastMx || my != this.lastMy);
         this.lastMx = mx;
         this.lastMy = my;
         if(moved && wheel.state == WeaponWheel.State.OPEN && r.radius > 0.0F && Mouse.isInsideWindow()) {
            float wx = mx * menuPx / px;
            float wy = my * menuPx / px;
            wheel.pointSelector((wx - r.cx) / r.radius, (wy - r.cy) / r.radius);
         }

         r.drawInMenu(menuPx);
         if(wheel.state == WeaponWheel.State.CLOSED && !this.closing) {
            this.closing = true;
            Popovers.close(this);
         }

      }

      public boolean contains(float mx, float my) {
         return true;
      }

      public boolean mouseDown(float mx, float my, int button) {
         WeaponWheel wheel = this.module.wheel;
         if(wheel.state != WeaponWheel.State.OPEN) {
            return true;
         } else {
            if(button == 0) {
               wheel.pickOrCancel();
            } else {
               wheel.cancel(true);
            }

            return true;
         }
      }

      public boolean wheel(float mx, float my, int wheel) {
         if(this.module.wheel.state == WeaponWheel.State.OPEN && wheel != 0) {
            this.module.wheel.scroll(wheel < 0?1:-1);
         }

         return true;
      }

      public boolean wantsKeys() {
         return true;
      }

      public boolean keyTyped(char c, int key) {
         KeyBinding kb = this.module.wheelSettings.key.binding();
         if(kb != null && key == kb.getKeyCode() && this.module.wheel.state == WeaponWheel.State.OPEN) {
            this.module.wheel.pickOrCancel();
         }

         return true;
      }

      protected void closed() {
         this.module.wheel.closeInMenu();
      }
   }
}
