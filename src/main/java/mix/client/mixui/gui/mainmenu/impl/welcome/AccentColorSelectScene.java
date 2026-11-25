package mix.client.mixui.gui.mainmenu.impl.welcome;

import java.awt.Color;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.mainmenu.GuiMixMainMenu;
import mix.client.mixui.gui.mainmenu.MainMenuScene;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.color.ColorManager;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.animation.normal.Animation;
import mix.client.mixui.utils.animation.normal.Direction;
import mix.client.mixui.utils.animation.normal.other.DecelerateAnimation;
import mix.client.mixui.utils.buffer.ScreenAlpha;
import mix.client.mixui.utils.mouse.MouseUtils;
import mix.client.mixui.utils.mouse.Scroll;
import mix.client.mixui.utils.render.BlurUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

public class AccentColorSelectScene extends MainMenuScene {

	private int x, y, width, height;
	
	private AccentColor currentColor;
	
	private Animation fadeAnimation;
	private ScreenAlpha screenAlpha = new ScreenAlpha();
	
	private Scroll scroll = new Scroll();
	
	public AccentColorSelectScene(GuiMixMainMenu parent) {
		super(parent);
		
		currentColor = Mix.getInstance().getColorManager().getColorByName("Default");
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		width = 280;
		height = 172;
		x = sr.getScaledWidth() / 2 - (width / 2);
		y = sr.getScaledHeight() / 2 - (height / 2);
		
		if(fadeAnimation == null) {
			fadeAnimation = new DecelerateAnimation(800, 1);
			fadeAnimation.setDirection(Direction.FORWARDS);
			fadeAnimation.reset();
		}
		
		BlurUtils.drawBlurScreen(14);
		
		screenAlpha.wrap(() -> drawNanoVG(), fadeAnimation.getValueFloat());
		
		if(fadeAnimation.isDone(Direction.BACKWARDS)) {
			// The launcher already hands us a signed-in session (see AccountManager#adoptLauncherSession),
			// so the first-run "Finally, let's login to Minecraft!" prompt and the login form it led to
			// are skipped entirely. Theme + accent color stay, then straight to the finish message.
			this.setCurrentScene(this.getSceneByClass(LastMessageScene.class));
		}
	}
	
	private void drawNanoVG() {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorManager colorManager = instance.getColorManager();
		
		int offsetX = 0;
		int offsetY = 0;
		int index = 1;
		
		nvg.drawRoundedRect(x, y, width, height, 8, this.getBackgroundColor());
		nvg.drawCenteredText("Choose a accent color", x + (width / 2), y + 10, Color.WHITE, 16, Fonts.MEDIUM);
		nvg.drawRect(x, y + 27, width, 1, Color.WHITE);
		
		nvg.drawRoundedImage(new ResourceLocation("mixclient/backgrounds/example-vertical.png"), x + width - 108, y + 40, 96, 96, 6);
		drawExampleHud(x + width - 96, y + 70.5F, currentColor);
		
		scroll.onScroll();
		scroll.onAnimation();
		
		nvg.save();
		nvg.scissor(x, y + 28, width, height - 28);
		nvg.translate(0, scroll.getValue());
		
		for(AccentColor color : colorManager.getColors()) {
			
			nvg.drawGradientRoundedRect(x + offsetX + 10, y + offsetY + 40, 32, 32, 6, color.getColor1(), color.getColor2());
			
			color.getAnimation().setAnimation(color.equals(currentColor) ? 1.0F : 0.0F, 16);
			
			nvg.drawCenteredText(Icon.CHECK, x + offsetX + 10 + (32 / 2), y + offsetY + 40 + 8, new Color(255, 255, 255, (int) (color.getAnimation().getValue() * 255)), 16, Fonts.ICON);
			
			offsetX+=40;
			
			if(index % 4 == 0) {
				offsetX = 0;
				offsetY+=40;
			}
			
			index++;
		}
		
		scroll.setMaxScroll(Math.max(0, offsetY - (height - 82)));
		
		nvg.restore();
		
		nvg.drawRoundedRect(x + width - 108, y + height - 26, 96, 20, 6, this.getBackgroundColor());
		nvg.drawCenteredText("Next", x + width - 108 + (96 / 2), y + height - 20, Color.WHITE, 10, Fonts.REGULAR);
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		
		Mix instance = Mix.getInstance();
		ColorManager colorManager = instance.getColorManager();
		
		int offsetX = 0;
		int offsetY = (int) scroll.getValue();
		int index = 1;
		
		for(AccentColor color : colorManager.getColors()) {
			
			if(MouseUtils.isInside(mouseX, mouseY, x, y + 28, width, height - 28)
					&& MouseUtils.isInside(mouseX, mouseY, x + offsetX + 10, y + offsetY + 40, 32, 32) && mouseButton == 0) {
				currentColor = color;
			}
			
			offsetX+=40;
			
			if(index % 4 == 0) {
				offsetX = 0;
				offsetY+=40;
			}
			
			index++;
		}
		
		if(MouseUtils.isInside(mouseX, mouseY, x + width - 86, y + height - 26, 80, 20) && mouseButton == 0) {
			Mix.getInstance().getColorManager().setCurrentColor(currentColor);
			fadeAnimation.setDirection(Direction.BACKWARDS);
		}
	}
	
	private void drawExampleHud(float x, float y, AccentColor accentColor) {
		
		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		
		float width = 71;
		float height = 34F;
		
		nvg.drawGradientRoundedRect(x, y, width, height, 5, ColorUtils.applyAlpha(accentColor.getColor1(), 220),
				ColorUtils.applyAlpha(accentColor.getColor2(), 220));
		
		nvg.drawText("X: 190", x + 3.9F, y + 3.9F, Color.WHITE, 6.42F, Fonts.REGULAR);
		nvg.drawText("Y: 60", x + 3.9F, y + 3.9F + 7, Color.WHITE, 6.42F, Fonts.REGULAR);
		nvg.drawText("Z: 20", x + 3.9F, y + 3.9F + 14, Color.WHITE, 6.42F, Fonts.REGULAR);
		nvg.drawText("Biome: Plains", x + 3.9F, y + 3.9F + 21, Color.WHITE, 6.42F, Fonts.REGULAR);
	}
}
