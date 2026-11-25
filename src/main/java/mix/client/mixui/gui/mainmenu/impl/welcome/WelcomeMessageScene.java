package mix.client.mixui.gui.mainmenu.impl.welcome;

import java.awt.Color;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.mainmenu.GuiMixMainMenu;
import mix.client.mixui.gui.mainmenu.MainMenuScene;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.utils.TimerUtils;
import mix.client.mixui.utils.animation.normal.Animation;
import mix.client.mixui.utils.animation.normal.Direction;
import mix.client.mixui.utils.animation.normal.other.DecelerateAnimation;
import mix.client.mixui.utils.render.BlurUtils;
import net.minecraft.client.gui.ScaledResolution;

public class WelcomeMessageScene extends MainMenuScene {

	private Animation fadeAnimation;
	private int step;
	private String message;
	
	private TimerUtils timer = new TimerUtils();
	
	public WelcomeMessageScene(GuiMixMainMenu parent) {
		super(parent);
		
		step = 0;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		String welcomeMessage = "Welcome to Mix Client";
		String setupMessage = "Let's start setting up the Mix Client!";
		
		BlurUtils.drawBlurScreen(14);
		
		if(fadeAnimation == null && this.getParent().isDoneBackgroundAnimation()) {
			fadeAnimation = new DecelerateAnimation(800, 1);
			fadeAnimation.setDirection(Direction.FORWARDS);
			fadeAnimation.reset();
			timer.reset();
		}
		
		if(fadeAnimation != null) {
			
			switch(step) {
				case 0:
					message = welcomeMessage;
					break;
				case 1:
					message = setupMessage;
					break;
			}
			
			nvg.setupAndDraw(() -> {
				nvg.drawCenteredText(message, sr.getScaledWidth() / 2, 
						(sr.getScaledHeight() / 2) - (nvg.getTextHeight(message, 26, Fonts.REGULAR) / 2), 
						new Color(255, 255, 255, (int) (fadeAnimation.getValueFloat() * 255)), 26, Fonts.REGULAR);
			});
			
			if(timer.delay(5000) && fadeAnimation.getDirection().equals(Direction.FORWARDS)) {
				fadeAnimation.setDirection(Direction.BACKWARDS);
				timer.reset();
			}
			
			if(fadeAnimation.isDone(Direction.BACKWARDS)) {
				
				if(step == 1) {
					this.setCurrentScene(this.getSceneByClass(ThemeSelectScene.class));
					return;
				}
				
				step++;
				fadeAnimation.setDirection(Direction.FORWARDS);
			}
		}
	}
}
