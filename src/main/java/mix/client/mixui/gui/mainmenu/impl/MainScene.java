package mix.client.mixui.gui.mainmenu.impl;

import java.awt.Color;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.mainmenu.GuiMixMainMenu;
import mix.client.mixui.gui.mainmenu.MainMenuScene;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.utils.mouse.MouseUtils;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.ScaledResolution;

public class MainScene extends MainMenuScene {

	public MainScene(GuiMixMainMenu parent) {
		super(parent);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
	
		nvg.setupAndDraw(() -> drawNanoVG(nvg));
	}
	
	private void drawNanoVG(NanoVGManager nvg) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		float yPos = sr.getScaledHeight() / 2 - 22;
		
		nvg.drawCenteredText(Icon.MIX, sr.getScaledWidth() / 2, sr.getScaledHeight() / 2 - (nvg.getTextHeight(Icon.MIX, 54, Fonts.ICON) / 2) - 60, Color.WHITE, 54, Fonts.ICON);
		
		nvg.drawRoundedRect(sr.getScaledWidth() / 2 - (180 / 2), yPos, 180, 20, 4.5F, this.getBackgroundColor());
		nvg.drawCenteredText(TranslateText.SINGLEPLAYER.getText(), sr.getScaledWidth() / 2, yPos + 6.5F, Color.WHITE, 9.5F, Fonts.REGULAR);
		
		nvg.drawRoundedRect(sr.getScaledWidth() / 2 - (180 / 2), yPos + 26, 180, 20, 4.5F, this.getBackgroundColor());
		nvg.drawCenteredText(TranslateText.MULTIPLAYER.getText(), sr.getScaledWidth() / 2, yPos + 6.5F + 26, Color.WHITE, 9.5F, Fonts.REGULAR);
		
		nvg.drawRoundedRect(sr.getScaledWidth() / 2 - (180 / 2), yPos + (26 * 2), 180, 20, 4.5F, this.getBackgroundColor());
		nvg.drawCenteredText(TranslateText.SETTINGS.getText(), sr.getScaledWidth() / 2, yPos + 6.5F + (26 * 2), Color.WHITE, 9.5F, Fonts.REGULAR);
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		float yPos = sr.getScaledHeight() / 2 - 22;
		
		if(mouseButton == 0) {
			
			if(MouseUtils.isInside(mouseX, mouseY, sr.getScaledWidth() / 2 - (160 / 2), yPos, 160, 20)) {
				mc.displayGuiScreen(new GuiSelectWorld(this.getParent()));
			}
			
			if(MouseUtils.isInside(mouseX, mouseY, sr.getScaledWidth() / 2 - (180 / 2), yPos + 26, 180, 20)) {
				mc.displayGuiScreen(new GuiMultiplayer(this.getParent()));
			}
			
			if(MouseUtils.isInside(mouseX, mouseY, sr.getScaledWidth() / 2 - (180 / 2), yPos + (26 * 2), 180, 20)) {
				mc.displayGuiScreen(new GuiOptions(this.getParent(), mc.gameSettings));
			}
		}
	}
}
