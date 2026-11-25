package mix.client.mixui.management.mods;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.utils.animation.ColorAnimation;
import mix.client.mixui.utils.animation.simple.SimpleAnimation;

public enum ModCategory {
	ALL(TranslateText.ALL),
	FAVORITE(TranslateText.FAVORITE),
	HUD(TranslateText.HUD),
	RENDER(TranslateText.RENDER),
	PLAYER(TranslateText.PLAYER),
	OTHER(TranslateText.MISC),
	SCRIPT(TranslateText.SCRIPT),
	WORLD(TranslateText.WORLD);
	
	private TranslateText nameTranslate;
	private ColorAnimation textColorAnimation;
	private SimpleAnimation backgroundAnimation;
	
	private ModCategory(TranslateText nameTranslate) {
		this.nameTranslate = nameTranslate;
		this.backgroundAnimation = new SimpleAnimation();
		this.textColorAnimation = new ColorAnimation();
	}

	public String getName() {
		return nameTranslate.getText();
	}

	public SimpleAnimation getBackgroundAnimation() {
		return backgroundAnimation;
	}

	public ColorAnimation getTextColorAnimation() {
		return textColorAnimation;
	}
}
