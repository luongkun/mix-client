package mix.client.mixui.integration.haunt.module.weapons;

import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.weapons.ChipRow;
import mix.client.mixui.integration.haunt.module.weapons.WeaponDef;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.sound.ModSounds;
import mix.client.mixui.integration.haunt.sound.Sfx;

final class WeaponCategoryWidget extends Widget {
   static final WeaponDef.Category[] CATEGORIES = new WeaponDef.Category[]{null, WeaponDef.Category.KNIVES, WeaponDef.Category.SWORDS, WeaponDef.Category.POLEARMS, WeaponDef.Category.AXES_HAMMERS, WeaponDef.Category.BOWS, WeaponDef.Category.MODERN, WeaponDef.Category.MAGIC, WeaponDef.Category.IMPORTED};
   private final WeaponsModule module;
   private final ChipRow chips = new ChipRow();
   private final String[] labels;
   private final boolean[] selected;
   private final boolean[] enabled;

   WeaponCategoryWidget(WeaponsModule module) {
      this.labels = new String[CATEGORIES.length];
      this.selected = new boolean[CATEGORIES.length];
      this.enabled = new boolean[CATEGORIES.length];
      this.module = module;

      for(int i = 0; i < CATEGORIES.length; ++i) {
         this.labels[i] = CATEGORIES[i] == null?"All":CATEGORIES[i].displayName;
      }

   }

   public float height() {
      this.chips.layout(this.x, this.y + 2.0F, this.w, this.labels, (boolean[])null);
      return this.chips.height + 8.0F;
   }

   public boolean isVisible() {
      return !this.module.editing();
   }

   public void render(float mx, float my, float dt, int accent) {
      this.chips.layout(this.x, this.y + 2.0F, this.w, this.labels, (boolean[])null);

      for(int i = 0; i < CATEGORIES.length; ++i) {
         this.selected[i] = CATEGORIES[i] == this.module.category;
         this.enabled[i] = CATEGORIES[i] == null || this.module.countIn(CATEGORIES[i]) > 0;
      }

      this.chips.draw(this.labels, this.selected, this.enabled, (int[])null, mx, my, dt, accent);
   }

   public boolean mouseDown(float mx, float my, int button) {
      if(this.contains(mx, my) && button == 0) {
         int i = this.chips.at(mx, my);
         if(i >= 0) {
            if(CATEGORIES[i] != null && this.module.countIn(CATEGORIES[i]) == 0) {
               ModSounds.gui(Sfx.ERROR);
            } else if(this.module.category != CATEGORIES[i]) {
               this.module.category = CATEGORIES[i];
               ModSounds.gui(Sfx.SELECT);
            }
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
         if(i < 0) {
            return null;
         } else {
            WeaponDef.Category c = CATEGORIES[i];
            if(c == null) {
               return "Every weapon";
            } else {
               int n = this.module.countIn(c);
               if(n == 0) {
                  return c == WeaponDef.Category.IMPORTED?"No imported weapons yet: import one under Import":"No weapons here";
               } else {
                  String what = n + (n == 1?" weapon":" weapons") + " (Vanilla stays first)";
                  return c == WeaponDef.Category.BOWS?what + ". Bows only replace bows":what;
               }
            }
         }
      }
   }
}
