package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;

public class MinimalViewBobbingMod extends Mod {

	private static MinimalViewBobbingMod instance;
	
	public MinimalViewBobbingMod() {
		super(TranslateText.MINIMAL_VIEW_BOBBING, TranslateText.MINIMAL_VIEW_BOBBING_DESCRIPTION, ModCategory.RENDER);
		
		instance = this;
	}

	public static MinimalViewBobbingMod getInstance() {
		return instance;
	}
}
