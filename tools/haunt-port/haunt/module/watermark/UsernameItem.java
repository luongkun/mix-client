package mix.client.mixui.integration.haunt.module.watermark;

import mix.client.mixui.integration.haunt.module.misc.skins.SkinHooks;
import mix.client.mixui.integration.haunt.module.watermark.IslandDraw;
import mix.client.mixui.integration.haunt.module.watermark.IslandIcons;
import mix.client.mixui.integration.haunt.module.watermark.IslandItem;
import mix.client.mixui.integration.haunt.module.watermark.IslandStyle;
import mix.client.mixui.integration.haunt.module.watermark.WaterMarkModule;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

final class UsernameItem extends IslandItem {
   final BooleanSetting head = (BooleanSetting)this.option(new BooleanSetting("user.head", "Show Head", "Your player head before the name.", true));
   private String name = "";
   private float nameW;
   private int measuredPx;
   private String measuredName;
   private int skin;

   UsernameItem(WaterMarkModule wm) {
      super(wm, "user", "Username", "Your player head and name.", IslandIcons.Icon.APP);
   }

   void tick() {
      Minecraft mc = Minecraft.getMinecraft();
      this.name = mc.thePlayer != null?mc.thePlayer.getName():mc.getSession().getUsername();
      this.skin = this.head.get()?skinTexture():0;
   }

   String panelValue() {
      return this.name;
   }

   String panelDescription() {
      return "Your name and head";
   }

   private float headSize(IslandStyle s) {
      return (float)Math.round(s.icon * 1.25F);
   }

   float measure(IslandDraw d, IslandStyle s) {
      int px = d.pixels(s.font);
      if(px != this.measuredPx || !this.name.equals(this.measuredName)) {
         this.measuredPx = px;
         this.measuredName = this.name;
         this.nameW = d.width(false, s.font, this.name);
      }

      return (this.head.get()?this.headSize(s) + s.gap:0.0F) + this.nameW;
   }

   void draw(IslandDraw d, IslandStyle s, float x, float cy) {
      if(this.head.get()) {
         float size = this.headSize(s);
         int texture = this.skin;
         if(texture > 0) {
            float y = cy - size / 2.0F;
            float r = size * 0.24F;
            d.roundedImage(texture, x, y, size, size, r, 0.125F, 0.125F, 0.25F, 0.25F, -1);
            d.roundedImage(texture, x, y, size, size, r, 0.625F, 0.125F, 0.75F, 0.25F, -1);
         }

         x += size + s.gap;
      }

      d.textMiddle(false, s.font, this.name, x, cy, s.text);
   }

   private static int skinTexture() {
      Minecraft mc = Minecraft.getMinecraft();
      ResourceLocation loc = SkinHooks.ownSkin();
      ITextureObject tex = mc.getTextureManager().getTexture(loc);
      if(tex == null) {
         mc.getTextureManager().bindTexture(loc);
         tex = mc.getTextureManager().getTexture(loc);
      }

      return tex == null?0:tex.getGlTextureId();
   }

   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      hit.set(this, x - s.pad, cy - s.height / 2.0F, this.measure(this.wm.draw(), s) + s.pad * 2.0F, s.height, 0, "Signed in as " + this.name, false);
      return true;
   }
}
