package mix.client.mixui.management.mods.impl;

import java.util.ArrayList;

import mix.client.mixui.Mix;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.animation.normal.Animation;
import mix.client.mixui.utils.animation.normal.Direction;
import mix.client.mixui.utils.animation.normal.easing.EaseBackIn;

public class ClickEffectMod extends Mod {

	private static ClickEffectMod instance;
	
	private ArrayList<ClickEffect> effects = new ArrayList<ClickEffect>();
	private ArrayList<ClickEffect> removeEffects = new ArrayList<ClickEffect>();
	
	public ClickEffectMod() {
		super(TranslateText.CLICK_EFFECT, TranslateText.CLICK_EFFECT_DESCRIPTION, ModCategory.RENDER);
		
		instance = this;
	}
	
	public void drawClickEffects() {
		
		for(ClickEffect ce : effects) {
			
			if(ce.isDone()) {
				removeEffects.add(ce);
			}
			
			ce.draw();
		}
		
		effects.removeAll(removeEffects);
	}
	
	public void addClickEffect(int mouseX, int mouseY) {
		effects.add(new ClickEffect(mouseX, mouseY));
	}
	
	public static ClickEffectMod getInstance() {
		return instance;
	}

	private class ClickEffect {
		
		private Animation animation;
		private int x, y;
		
		private ClickEffect(int x, int y) {
			this.x = x;
			this.y = y;
			this.animation = new EaseBackIn(650, 1, 0.0F);
		}
		
		public void draw() {
			
			Mix instance = Mix.getInstance();
			NanoVGManager nvg = instance.getNanoVGManager();
			AccentColor currentColor = instance.getColorManager().getCurrentColor();
			
			nvg.setupAndDraw(() -> {
				nvg.drawArc(x, y, animation.getValueFloat() * 8, 0, 360, 2, ColorUtils.applyAlpha(currentColor.getInterpolateColor(0), (int) (255 - (animation.getValueFloat() * 255))));
			});
		}
		
		public boolean isDone() {
			return animation.isDone(Direction.FORWARDS);
		}
	}
}
