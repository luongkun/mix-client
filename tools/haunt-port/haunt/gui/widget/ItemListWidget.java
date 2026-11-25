package mix.client.mixui.integration.haunt.gui.widget;

import java.util.List;
import java.util.Set;
import mix.client.mixui.integration.haunt.core.ClientClock;
import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.gui.search.SearchList;
import mix.client.mixui.integration.haunt.gui.widget.Pill;
import mix.client.mixui.integration.haunt.gui.widget.Widget;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.setting.ItemListSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public final class ItemListWidget extends Widget implements SearchList {
   private static final float CHIP_H = 22.0F;
   private static final float GAP = 6.0F;
   private static final int MAX = 64;
   private final ItemListSetting setting;
   private final Pill add = new Pill();
   private final float[] chipX = new float[64];
   private final float[] chipY = new float[64];
   private final float[] chipW = new float[64];
   private final float[] chipHover = new float[64];
   private int count;
   private float chipsH;
   private String status;
   private long statusUntil;

   public ItemListWidget(ItemListSetting setting) {
      this.setting = setting;
   }

   public float height() {
      return 24.0F + (this.count == 0?16.0F:this.chipsH + 6.0F) + 4.0F;
   }

   public boolean isVisible() {
      return this.setting.isVisible();
   }

   static String nameOf(String item) {
      ItemStack stack = ItemListSetting.stackOf(item);
      return stack != null?stack.getDisplayName():item;
   }

   public void render(float mx, float my, float dt, int accent) {
      Ui.textMiddle(this.setting.getLabel(), this.x, this.y + 10.0F, 10.0F, Theme.TEXT, false);
      String label = "Add item in hand";
      this.add.layout(this.x + this.w - (Ui.width(label, 8.8F) + 20.0F), this.y, label);
      this.add.draw(label, mx, my, dt, accent, true, Minecraft.getMinecraft().thePlayer != null);
      if(this.status != null && ClientClock.millis() < this.statusUntil) {
         float sx = this.x + Ui.width(this.setting.getLabel(), 10.0F) + 12.0F;
         Ui.textMiddleHeavy(Ui.ellipsizeHeavy(this.status, 8.2F, this.add.x - sx - 8.0F), sx, this.y + 10.0F, 8.2F, accent | -16777216);
      }

      List<String> items = this.setting.get();
      this.count = Math.min(64, items.size());
      float cy0 = this.y + 26.0F;
      if(this.count == 0) {
         Ui.text("No items yet: hold one and click Add item in hand.", this.x, cy0 + 2.0F, 8.4F, Theme.TEXT_MUTED);
      } else {
         float cx = this.x;
         float cy = cy0;

         for(int i = 0; i < this.count; ++i) {
            String name = nameOf((String)items.get(i));
            float cw = 30.0F + Ui.width(name, 8.8F) + 18.0F;
            if(cx > this.x && cx + cw > this.x + this.w) {
               cx = this.x;
               cy += 28.0F;
            }

            this.chipX[i] = cx;
            this.chipY[i] = cy;
            this.chipW[i] = cw;
            boolean over = mx >= cx && mx < cx + cw && my >= cy && my < cy + 22.0F;
            this.chipHover[i] = Easing.approach(this.chipHover[i], over?1.0F:0.0F, 16.0F, dt);
            Ui.surface(cx, cy, cw, 22.0F, 11.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.chipHover[i]));
            Ui.item(ItemListSetting.stackOf((String)items.get(i)), cx + 6.0F, cy + 3.0F, 16.0F);
            Ui.textMiddle(name, cx + 26.0F, cy + 11.0F, 8.8F, Theme.TEXT, false);
            Ui.textMiddle("×", cx + cw - 13.0F, cy + 11.0F - 0.5F, 9.0F, ColorUtil.lerp(Theme.TEXT_MUTED, Theme.DANGER, this.chipHover[i]), false);
            cx += cw + 6.0F;
         }

         this.chipsH = cy - cy0 + 22.0F;
      }

   }

   public boolean mouseDown(float mx, float my, int button) {
      if(!this.contains(mx, my)) {
         return false;
      } else if(button == 0 && this.add.contains(mx, my)) {
         Minecraft mc = Minecraft.getMinecraft();
         ItemStack held = mc.thePlayer == null?null:mc.thePlayer.getHeldItem();
         String id = ItemListSetting.idOf(held);
         if(id == null) {
            this.say("Hold an item first");
         } else {
            this.say(this.setting.add(id)?"Added " + held.getDisplayName():held.getDisplayName() + " is already in the list");
         }

         return true;
      } else {
         List<String> items = this.setting.get();

         for(int i = 0; i < this.count && i < items.size(); ++i) {
            if(button == 0 && mx >= this.chipX[i] + this.chipW[i] - 22.0F && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 22.0F) {
               this.setting.remove((String)items.get(i));
               return true;
            }
         }

         return true;
      }
   }

   private void say(String message) {
      this.status = message;
      this.statusUntil = ClientClock.millis() + 3000L;
   }

   public String tooltip(float mx, float my) {
      return this.add.contains(mx, my)?"Adds the item you are holding":(this.contains(mx, my) && my < this.y + 22.0F?this.setting.getDescription():null);
   }

   public void searchItems(SearchList.Items out) {
      for(String item : this.setting.get()) {
         out.item(item, nameOf(item), (String)null, new String[0]);
      }

   }

   public void searchFilter(Set<Object> keys) {
   }

   public float reveal(Object key) {
      List<String> items = this.setting.get();

      for(int i = 0; i < this.count && i < items.size(); ++i) {
         if(((String)items.get(i)).equals(key)) {
            return this.chipY[i] - this.y;
         }
      }

      return 0.0F;
   }

   public float revealHeight(Object key) {
      return 22.0F;
   }

   public int searchVersion() {
      return this.setting.get().hashCode();
   }
}
