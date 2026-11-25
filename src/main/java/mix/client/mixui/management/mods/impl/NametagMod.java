package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;

public class NametagMod extends Mod {

	private static NametagMod instance;
	
	public NametagMod() {
		super(TranslateText.NAMETAG, TranslateText.NAMETAG_DESCRIPTION, ModCategory.PLAYER);
		
		instance = this;
	}

	public static NametagMod getInstance() {
		return instance;
	}
}
