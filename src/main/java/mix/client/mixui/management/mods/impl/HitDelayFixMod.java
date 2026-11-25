package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;

public class HitDelayFixMod extends Mod {

	private static HitDelayFixMod instance;
	
	public HitDelayFixMod() {
		super(TranslateText.HIT_DELAY_FIX, TranslateText.HIT_DELAY_FIX_DESCRIPTION, ModCategory.PLAYER);
		
		instance = this;
	}

	public static HitDelayFixMod getInstance() {
		return instance;
	}
}
