package mix.client.mixui.management.cape;

import mix.client.mixui.utils.animation.ColorAnimation;
import mix.client.mixui.utils.animation.simple.SimpleAnimation;

public enum CapeCategory {
	ALL("All"), MINECON("Minecon"), FLAG("Flag"), CUSTOM("Custom");
	
	private String name;
	private SimpleAnimation backgroundAnimation = new SimpleAnimation();
	private ColorAnimation textColorAnimation = new ColorAnimation();
	
	private CapeCategory(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public SimpleAnimation getBackgroundAnimation() {
		return backgroundAnimation;
	}

	public ColorAnimation getTextColorAnimation() {
		return textColorAnimation;
	}
}
