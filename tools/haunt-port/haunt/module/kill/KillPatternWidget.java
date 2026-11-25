package mix.client.mixui.integration.haunt.module.kill;

import java.util.List;
import java.util.Set;
import mix.client.mixui.integration.haunt.core.ClientClock;
import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.gui.search.SearchList;
import mix.client.mixui.integration.haunt.gui.widget.Pill;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.module.kill.KillPatternSetting;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.setting.TextSetting;
import net.minecraft.client.gui.GuiScreen;

final class KillPatternWidget extends Widget implements SearchList {
   private static final float ROW_H = 18.0F;
   private static final float ROW_TEXT = 8.4F;
   private static final float FIELD_H = 20.0F;
   private static final int MAX_ROWS = 64;
   private final KillPatternSetting setting;
   private float lastHeight = 120.0F;
   private final float[] rowY = new float[64];
   private final float[] rowHover = new float[64];
   private final int[] rowIndex = new int[64];
   private int rows;
   private int hoverRow = -1;
   private Set<Object> filter;
   private float fieldX;
   private float fieldY;
   private float fieldW;
   private final Pill add = new Pill();
   private final Pill defaults = new Pill();
   private String field = "";
   private boolean focused;
   private float caret;
   private String error;
   private long errorUntil;

   KillPatternWidget(KillPatternSetting setting) {
      this.setting = setting;
   }

   public float height() {
      return this.lastHeight;
   }

   public boolean isVisible() {
      return this.setting.isVisible();
   }

   public void render(float mx, float my, float dt, int accent) {
      this.caret += dt;
      float cy = this.y + 4.0F;
      Ui.text(this.setting.getName(), this.x, cy, 10.0F, Theme.TEXT);
      String note = "{victim}  {me}  * = any text";
      Ui.text(note, this.x + this.w - Ui.width(note, 7.8F), cy + 1.5F, 7.8F, Theme.TEXT_MUTED);
      cy = cy + 18.0F;
      List<String> list = this.setting.get();
      this.rows = 0;

      for(int i = 0; i < list.size() && this.rows < 64; ++i) {
         if(this.filter == null || this.filter.contains(list.get(i))) {
            this.rowIndex[this.rows++] = i;
         }
      }

      this.hoverRow = -1;

      for(int var19 = 0; var19 < this.rows; ++var19) {
         this.rowY[var19] = cy;
         boolean over = inside(mx, my, this.x, cy, this.w, 18.0F);
         if(over) {
            this.hoverRow = var19;
         }

         this.rowHover[var19] = Easing.approach(this.rowHover[var19], over?1.0F:0.0F, 16.0F, dt);
         Ui.surface(this.x, cy, this.w, 18.0F, 6.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.rowHover[var19]));
         String text = Ui.ellipsize((String)list.get(this.rowIndex[var19]), 8.4F, this.w - 34.0F, false);
         Ui.textMiddle(text, this.x + 8.0F, cy + 9.0F, 8.4F, Theme.TEXT, false);
         float xx = this.x + this.w - 11.0F;
         float xy = cy + 9.0F;
         int xc = ColorUtil.lerp(Theme.TEXT_MUTED, Theme.DANGER, this.rowHover[var19]);

         for(int k = -3; k <= 3; ++k) {
            Ui.rect(xx + (float)k * 0.8F - 0.5F, xy + (float)k * 0.8F - 0.5F, 1.1F, 1.1F, xc);
            Ui.rect(xx + (float)k * 0.8F - 0.5F, xy - (float)k * 0.8F - 0.5F, 1.1F, 1.1F, xc);
         }

         cy += 21.0F;
      }

      if(list.isEmpty()) {
         Ui.textMiddle("None: kill messages in chat are not used", this.x, cy + 9.0F, 8.6F, Theme.TEXT_MUTED, false);
         cy += 21.0F;
      }

      String addLabel = "Add";
      String defaultsLabel = "Defaults";
      float buttonsW = Ui.width(addLabel, 8.8F) + 20.0F + 6.0F + Ui.width(defaultsLabel, 8.8F) + 20.0F;
      boolean below = this.w - buttonsW - 8.0F < 110.0F;
      float by = below?cy + 20.0F + 4.0F:(cy += 5.0F);
      this.defaults.layout(this.x + this.w - (Ui.width(defaultsLabel, 8.8F) + 20.0F), by, defaultsLabel);
      this.add.layout(this.defaults.x - 6.0F - (Ui.width(addLabel, 8.8F) + 20.0F), by, addLabel);
      this.fieldX = this.x;
      this.fieldY = cy;
      this.fieldW = below?this.w:this.add.x - 8.0F - this.x;
      boolean overField = inside(mx, my, this.fieldX, this.fieldY, this.fieldW, 20.0F);
      Ui.surface(this.fieldX, this.fieldY, this.fieldW, 20.0F, 6.0F, !overField && !this.focused?Theme.SURFACE:Theme.SURFACE_HOVER);
      Ui.outline(this.fieldX, this.fieldY, this.fieldW, 20.0F, 6.0F, 0.9F, this.focused?accent | -16777216:Theme.OUTLINE);
      long now = ClientClock.millis();
      if(this.error != null && now < this.errorUntil) {
         Ui.textMiddle(Ui.ellipsize(this.error, 8.4F, this.fieldW - 16.0F, false), this.fieldX + 8.0F, this.fieldY + 10.0F, 8.4F, Theme.DANGER, false);
      } else if(this.field.isEmpty() && !this.focused) {
         Ui.textMiddle(Ui.ellipsize("e.g. {victim} was eliminated by {me}", 8.4F, this.fieldW - 16.0F, false), this.fieldX + 8.0F, this.fieldY + 10.0F, 8.4F, Theme.TEXT_MUTED, false);
      } else {
         String shown = this.field;

         for(float room = this.fieldW - 18.0F; shown.length() > 1 && Ui.width(shown, 8.4F) > room; shown = shown.substring(1)) {
            ;
         }

         Ui.textMiddle(shown, this.fieldX + 8.0F, this.fieldY + 10.0F, 8.4F, Theme.TEXT, false);
         if(this.focused && this.caret % 1.0F < 0.55F) {
            Ui.rect(this.fieldX + 8.0F + Ui.width(shown, 8.4F) + 1.0F, this.fieldY + 4.5F, 0.8F, 11.0F, Theme.TEXT);
         }
      }

      this.add.draw(addLabel, mx, my, dt, accent, true, !this.field.trim().isEmpty());
      this.defaults.draw(defaultsLabel, mx, my, dt, accent, false, !list.equals(this.setting.getDefaults()));
      this.lastHeight = cy + 26.0F + (below?24.0F:0.0F) - this.y;
   }

   private static boolean inside(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   public boolean mouseDown(float mx, float my, int button) {
      if(this.contains(mx, my) && button == 0) {
         List<String> list = this.setting.get();

         for(int i = 0; i < this.rows; ++i) {
            if(this.rowIndex[i] < list.size()) {
               if(inside(mx, my, this.x + this.w - 22.0F, this.rowY[i], 22.0F, 18.0F)) {
                  this.setting.remove((String)list.get(this.rowIndex[i]));
                  return true;
               }

               if(inside(mx, my, this.x, this.rowY[i], this.w - 22.0F, 18.0F)) {
                  this.field = (String)list.get(this.rowIndex[i]);
                  this.focused = true;
                  this.caret = 0.0F;
                  return true;
               }
            }
         }

         if(inside(mx, my, this.fieldX, this.fieldY, this.fieldW, 20.0F)) {
            this.focused = true;
            this.caret = 0.0F;
            return true;
         } else if(this.add.contains(mx, my)) {
            this.commit();
            return true;
         } else if(this.defaults.contains(mx, my)) {
            this.setting.reset();
            return true;
         } else {
            this.focused = false;
            return false;
         }
      } else {
         if(this.focused && !inside(mx, my, this.fieldX, this.fieldY, this.fieldW, 20.0F)) {
            this.focused = false;
         }

         return false;
      }
   }

   private void commit() {
      String p = this.field.trim();
      if(!p.isEmpty()) {
         String problem = KillPatternSetting.problem(p);
         if(problem == null && this.setting.contains(p)) {
            problem = "That pattern is already in the list";
         }

         if(problem != null) {
            this.error = problem;
            this.errorUntil = ClientClock.millis() + 3500L;
         } else {
            this.setting.add(p);
            this.field = "";
         }
      }

   }

   public boolean keyTyped(char c, int key) {
      if(!this.focused) {
         return false;
      } else {
         this.caret = 0.0F;
         this.error = null;
         if(key == 1) {
            this.focused = false;
         } else if(key != 28 && key != 156) {
            if(key == 14) {
               this.field = this.field.isEmpty()?this.field:this.field.substring(0, this.field.length() - 1);
            } else if(GuiScreen.isKeyComboCtrlA(key)) {
               this.field = "";
            } else if(GuiScreen.isKeyComboCtrlV(key)) {
               this.field = TextSetting.clean(this.field + GuiScreen.getClipboardString(), 120);
            } else if(c >= 32 && c != 127) {
               this.field = TextSetting.clean(this.field + c, 120);
            }
         } else {
            this.commit();
         }

         return true;
      }
   }

   public boolean isFocused() {
      return this.focused;
   }

   public void unfocus() {
      this.focused = false;
   }

   public String tooltip(float mx, float my) {
      List<String> list = this.setting.get();
      return this.hoverRow >= 0 && this.hoverRow < this.rows && this.rowIndex[this.hoverRow] < list.size()?(inside(mx, my, this.x + this.w - 22.0F, this.rowY[this.hoverRow], 22.0F, 18.0F)?"Remove this pattern":(String)list.get(this.rowIndex[this.hoverRow]) + "  (click to edit a copy)"):(this.contains(mx, my)?this.setting.getDescription():null);
   }

   public void searchItems(SearchList.Items out) {
      for(String p : this.setting.get()) {
         out.item(p, p, (String)null, new String[0]);
      }

   }

   public void searchFilter(Set<Object> keys) {
      this.filter = keys;
   }

   public float reveal(Object key) {
      int i = this.setting.get().indexOf(key);
      return i < 0?-1.0F:22.0F + (float)i * 21.0F;
   }

   public float revealHeight(Object key) {
      return 18.0F;
   }

   public int searchVersion() {
      return this.setting.version();
   }
}
