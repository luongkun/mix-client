package mix.client.mixui.gui.mainmenu.impl.welcome;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.mainmenu.GuiMixMainMenu;
import mix.client.mixui.gui.mainmenu.impl.AccountScene;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.utils.animation.normal.Animation;
import mix.client.mixui.utils.animation.normal.Direction;
import mix.client.mixui.utils.animation.normal.other.DecelerateAnimation;
import mix.client.mixui.utils.buffer.ScreenAlpha;
import mix.client.mixui.utils.render.BlurUtils;
import net.minecraft.client.gui.ScaledResolution;

public class FirstLoginScene extends AccountScene {
	
	private Animation fadeAnimation;
	private ScreenAlpha screenAlpha = new ScreenAlpha();
	
	public FirstLoginScene(GuiMixMainMenu parent) {
		super(parent);
	}

	@Override
	public void initScene() {
		super.initScene();
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		if(fadeAnimation == null) {
			fadeAnimation = new DecelerateAnimation(800, 1);
			fadeAnimation.setDirection(Direction.FORWARDS);
			fadeAnimation.reset();
		}
		
		if(fadeAnimation.isDone(Direction.BACKWARDS)) {
			this.setCurrentScene(this.getSceneByClass(LastMessageScene.class));
		}
		
		BlurUtils.drawBlurScreen(14);
		
		screenAlpha.wrap(() -> this.drawNanoVG(mouseX, mouseY, partialTicks, sr, instance, nvg), fadeAnimation.getValueFloat());
	}
	
	/*@Override
	public Runnable afterMicrosoftLogin() {
		
		AccountManager accountManager = Mix.getInstance().getAccountManager();
		
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if(accountManager.getCurrentAccount() != null) {
					accountManager.save();
					fadeAnimation.setDirection(Direction.BACKWARDS);
				}
			}
		};
		
		return runnable;
	}*/
	
	@Override
	public void keyTyped(char typedChar, int keyCode) {
		textBox.keyTyped(typedChar, keyCode);
	}
}