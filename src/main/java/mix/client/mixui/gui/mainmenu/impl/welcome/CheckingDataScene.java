package mix.client.mixui.gui.mainmenu.impl.welcome;

import java.awt.Color;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.mainmenu.GuiMixMainMenu;
import mix.client.mixui.gui.mainmenu.MainMenuScene;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.utils.animation.normal.Animation;
import mix.client.mixui.utils.animation.normal.Direction;
import mix.client.mixui.utils.animation.normal.other.DecelerateAnimation;
import mix.client.mixui.utils.render.BlurUtils;
import net.minecraft.client.gui.ScaledResolution;

public class CheckingDataScene extends MainMenuScene {

	private Animation fadeAnimation;
	
	public CheckingDataScene(GuiMixMainMenu parent) {
		super(parent);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		String message = "Checking the data...";
		
		BlurUtils.drawBlurScreen(14);
		
		if(fadeAnimation == null && this.getParent().isDoneBackgroundAnimation()) {
			fadeAnimation = new DecelerateAnimation(800, 1);
			fadeAnimation.setDirection(Direction.FORWARDS);
			fadeAnimation.reset();
		}
		
		if(fadeAnimation != null) {
			
			nvg.setupAndDraw(() -> {
				nvg.drawCenteredText(message, sr.getScaledWidth() / 2, 
						(sr.getScaledHeight() / 2) - (nvg.getTextHeight(message, 26, Fonts.REGULAR) / 2), 
						new Color(255, 255, 255, (int) (fadeAnimation.getValueFloat() * 255)), 26, Fonts.REGULAR);
			});
			
			if(Mix.getInstance().getDownloadManager().isDownloaded() && fadeAnimation.getDirection().equals(Direction.FORWARDS)) {
				fadeAnimation.setDirection(Direction.BACKWARDS);
			}
			
			if(fadeAnimation.isDone(Direction.BACKWARDS)) {
				this.setCurrentScene(this.getSceneByClass(FirstLoginScene.class));
			}
		}
	}
}