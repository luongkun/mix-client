package mix.client.mixui.integration.haunt.module.weapons;

import java.util.List;
import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.weapons.ChipRow;
import mix.client.mixui.integration.haunt.module.weapons.FinishSetting;
import mix.client.mixui.integration.haunt.module.weapons.WeaponOptions;
import mix.client.mixui.integration.haunt.sound.ModSounds;
import mix.client.mixui.integration.haunt.sound.Sfx;

final class FinishWidget extends Widget {
   private final FinishSetting setting;
   private final ChipRow chips = new ChipRow();
   private String[] labels;
   private int[] dots;
   private boolean[] withDot;
   private boolean[] selected;

   FinishWidget(FinishSetting setting) {
      this.setting = setting;
      List<WeaponOptions.Finish> options = setting.options();
      int n = options.size();
      this.labels = new String[n];
      this.dots = new int[n];
      this.withDot = new boolean[n];
      this.selected = new boolean[n];

      for(int i = 0; i < n; ++i) {
         WeaponOptions.Finish f = (WeaponOptions.Finish)options.get(i);
         this.labels[i] = f.displayName;
         this.dots[i] = f == WeaponOptions.Finish.CUSTOM?0:f.swatch;
         this.withDot[i] = f != WeaponOptions.Finish.CUSTOM;
      }

   }

   public float height() {
      this.chips.layout(this.x, this.y + 22.0F, this.w, this.labels, this.withDot);
      return 22.0F + this.chips.height + 6.0F;
   }

   public boolean isVisible() {
      return this.setting.isVisible();
   }

   public void render(float mx, float my, float dt, int accent) {
      this.chips.layout(this.x, this.y + 22.0F, this.w, this.labels, this.withDot);
      Ui.text(this.setting.getName(), this.x, this.y + 4.0F, 10.0F, Theme.TEXT);
      List<WeaponOptions.Finish> options = this.setting.options();

      for(int i = 0; i < options.size(); ++i) {
         this.selected[i] = options.get(i) == this.setting.get();
      }

      this.chips.draw(this.labels, this.selected, (boolean[])null, this.dots, mx, my, dt, accent);
   }

   public boolean mouseDown(float mx, float my, int button) {
      if(this.contains(mx, my) && button == 0) {
         int i = this.chips.at(mx, my);
         if(i >= 0) {
            this.setting.set((WeaponOptions.Finish)this.setting.options().get(i));
            ModSounds.gui(Sfx.SELECT);
         }

         return true;
      } else {
         return false;
      }
   }

   public String tooltip(float mx, float my) {
      if(!this.contains(mx, my)) {
         return null;
      } else {
         int i = this.chips.at(mx, my);
         return i >= 0?((WeaponOptions.Finish)this.setting.options().get(i)).description:this.setting.getDescription();
      }
   }
}
