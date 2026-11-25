package mix.client.mixui.gui.modmenu.category.impl.setting.impl;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.modmenu.ModMenuTheme;
import mix.client.mixui.gui.modmenu.category.impl.SettingCategory;
import mix.client.mixui.gui.modmenu.category.impl.setting.SettingScene;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.language.Language;
import mix.client.mixui.management.language.LanguageManager;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.mouse.MouseUtils;

public class LanguageScene extends SettingScene {

	public LanguageScene(SettingCategory parent) {
		super(parent, TranslateText.LANGUAGE, TranslateText.LANGUAGE_DESCRIPTION, Icon.TRANSLATE);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {

		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		AccentColor currentColor = instance.getColorManager().getCurrentColor();
		LanguageManager languageManager = instance.getLanguageManager();

		float offsetY = 0;

		for(Language lang : Language.values()) {

			float cardX = this.getX();
			float cardY = this.getY() + offsetY;
			float cardWidth = this.getWidth();
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, cardX, cardY, cardWidth, 64F);

			nvg.drawRoundedRect(cardX, cardY, cardWidth, 64F, 12,
					hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());

			nvg.drawRoundedImage(lang.getFlag(), cardX + 14F, cardY + 16F, 48F, 32F, 6F);
			nvg.drawText(lang.getName(), cardX + 78F, cardY + 22F,
					ModMenuTheme.onSurface(), 12, Fonts.MEDIUM);
			nvg.drawText(lang.getId().toUpperCase(), cardX + 78F, cardY + 40F,
					ModMenuTheme.onSurfaceVariant(), 8.5F, Fonts.REGULAR);

			lang.getAnimation().setAnimation(lang.equals(languageManager.getCurrentLanguage()) ? 1.0F : 0.0F, 16);

			float selected = lang.getAnimation().getValue();
			float checkX = cardX + cardWidth - 44F;

			nvg.drawRoundedRect(checkX, cardY + 18F, 28F, 28F, 14F,
					selected > 0.01F ? ModMenuTheme.primaryContainer() : ModMenuTheme.surfaceContainerHigh());
			nvg.drawCenteredText(Icon.CHECK, checkX + 14F, cardY + 24F,
					ColorUtils.applyAlpha(currentColor.getInterpolateColor(), (int) (selected * 255)), 15, Fonts.ICON);

			offsetY += 74F;
		}
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {

		if(mouseButton != 0) {
			return;
		}

		LanguageManager languageManager = Mix.getInstance().getLanguageManager();

		float offsetY = 0;

		for(Language lang : Language.values()) {

			if(MouseUtils.isInside(mouseX, mouseY, this.getX(), this.getY() + offsetY, this.getWidth(), 64F)) {
				languageManager.setCurrentLanguage(lang);
			}

			offsetY += 74F;
		}
	}
}
