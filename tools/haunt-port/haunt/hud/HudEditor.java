package mix.client.mixui.integration.haunt.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public interface HudEditor {
   default boolean isOpen() {
      return is(Minecraft.getMinecraft().currentScreen);
   }

   default boolean is(GuiScreen screen) {
      return screen instanceof HudEditor;
   }

   default boolean dragging(Object element) {
      GuiScreen s = Minecraft.getMinecraft().currentScreen;
      return s instanceof HudEditor && ((HudEditor)s).isDragging(element);
   }

   boolean isDragging(Object var1);
}
