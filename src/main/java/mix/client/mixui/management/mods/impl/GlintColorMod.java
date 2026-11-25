package mix.client.mixui.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import mix.client.mixui.Mix;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.ColorSetting;
import mix.client.mixui.management.mods.settings.impl.ComboSetting;
import mix.client.mixui.management.mods.settings.impl.combo.Option;
import mix.client.mixui.utils.ColorUtils;

public class GlintColorMod extends Mod {

	private static GlintColorMod instance;
	
	private ComboSetting typeSetting = new ComboSetting(TranslateText.TYPE, this, TranslateText.SYNC, new ArrayList<Option>(Arrays.asList(
			new Option(TranslateText.SYNC), new Option(TranslateText.RAINBOW), new Option(TranslateText.CUSTOM))));
	
	private ColorSetting colorSetting = new ColorSetting(TranslateText.COLOR, this, Color.RED, false);	
	
	public GlintColorMod() {
		super(TranslateText.GLINT_COLOR, TranslateText.GLINT_COLOR_DESCRIPTION, ModCategory.RENDER);
		
		instance = this;
	}

	public static GlintColorMod getInstance() {
		return instance;
	}

	public Color getGlintColor() {
		
		Option type = typeSetting.getOption();
		
		if(type.getTranslate().equals(TranslateText.SYNC)) {
			
			AccentColor currentColor = Mix.getInstance().getColorManager().getCurrentColor();
			
			return currentColor.getInterpolateColor();
		}else if(type.getTranslate().equals(TranslateText.RAINBOW)) {
			return ColorUtils.getRainbow(0, 25, 255);
		}else if(type.getTranslate().equals(TranslateText.CUSTOM)) {
			return ColorUtils.applyAlpha(colorSetting.getColor(), 255);
		}else {
			return Color.RED;
		}
	}
}
