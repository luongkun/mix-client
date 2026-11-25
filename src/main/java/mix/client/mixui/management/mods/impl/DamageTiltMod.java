package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;

public class DamageTiltMod extends Mod {

	private static DamageTiltMod instance;
	
	public DamageTiltMod() {
		super(TranslateText.DAMAGE_TILT, TranslateText.DAMAGE_TILT_DESCRIPTION, ModCategory.PLAYER);
		
		instance = this;
	}

	public static DamageTiltMod getInstance() {
		return instance;
	}
}
