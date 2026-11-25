package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;

public class ShinyPotsMod extends Mod {

	private static ShinyPotsMod instance;
	
	public ShinyPotsMod() {
		super(TranslateText.SHINY_POTS, TranslateText.SHINY_POTS_DESCRIPTION, ModCategory.RENDER);
		
		instance = this;
	}

	public static ShinyPotsMod getInstance() {
		return instance;
	}
}
