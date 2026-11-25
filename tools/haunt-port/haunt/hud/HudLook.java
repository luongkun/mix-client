package mix.client.mixui.integration.haunt.hud;

import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.module.watermark.IslandStyle;
import mix.client.mixui.integration.haunt.module.watermark.WaterMarkModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import net.minecraft.client.Minecraft;

public final class HudLook {
   public final IslandStyle colors = new IslandStyle();
   public boolean background = true;
   public boolean outline;
   public boolean shadow;
   public boolean glass;
   public boolean glint;
   public float radius;
   public int global;
   private static final float ISLAND_RADIUS_PX = 13.0F;

   public void update(HudLook.Mode mode, boolean customBackground, float customOpacity, float radiusUnits, boolean border, WaterMarkModule.GlassMode glassMode, HudLook.Glint glintMode, float px, float dt) {
      WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
      double time = ColorSetting.clock();
      boolean same = mode == HudLook.Mode.SAME;
      this.background = same || customBackground;
      float opacity = same?wm.lookOpacity():customOpacity;
      this.colors.colors((Theme.Style)FuriahsVisuals.instance.getTheme().get(), wm.lookAccent(time), opacity, dt);
      this.outline = same?wm.lookOutline():border;
      this.shadow = wm.lookShadow() && this.background;
      WaterMarkModule.GlassMode g = same?wm.lookGlass():glassMode;
      this.glass = this.background && (g == WaterMarkModule.GlassMode.ON || g == WaterMarkModule.GlassMode.AUTO && this.colors.glass);
      this.glint = glintMode == HudLook.Glint.SAME && wm.glintOn();
      this.radius = same?13.0F * (float)Minecraft.getMinecraft().displayHeight / 1080.0F / px:radiusUnits;
      ColorSetting gc = ColorSetting.getGlobal();
      this.global = gc == null?this.colors.accent:IslandStyle.readable(gc.getColor(0.0F, time), this.colors.light, this.colors.glass);
   }

   public int bg(float alpha) {
      return ColorUtil.mulAlpha(this.colors.bg, alpha);
   }

   public static enum Glint implements Displayable {
      SAME("Same as WaterMark", "The island\'s glint, in time with it"),
      OFF("Off", "No glint");

      private final String displayName;
      private final String description;

      private Glint(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }

   public static enum Mode implements Displayable {
      SAME("Same as WaterMark", "The island\'s own background, opacity, outline, glass and shadow"),
      CUSTOM("Custom", "Its own background, opacity, corner radius, border and glass");

      private final String displayName;
      private final String description;

      private Mode(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }
}
